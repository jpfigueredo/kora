# Padrões de Implementação do Kora — Guia Completo

> Artigo de referência — Kora Project
> Data: 2026-03-22

---

## 1. Visão Geral da Arquitetura

O Kora usa **hexagonal simplificada** (Ports & Adapters) dentro de cada módulo Maven:

```
kora-identity/
├── api/              → Adapters de entrada (Controllers, DTOs de request/response)
├── application/      → Use Cases (orquestração, @Service, @Transactional)
├── domain/           → Core (Entities, Value Objects, Repository interfaces, Domain Events)
├── infrastructure/   → Adapters de saída (JPA, configs, clients externos)
└── event/            → Domain Events compartilhados (records imutáveis)
```

### Regra de Dependência

```
                    api
                     │
                     ▼
               application
                     │
                     ▼
                  domain  ← infrastructure
```

- `api` depende de `application` e `domain`
- `application` depende de `domain`
- `infrastructure` depende de `domain` (implementa os ports)
- **domain NÃO depende de nada** (puro Java, sem Spring, sem JPA)

---

## 2. Camada API (Controllers)

### Princípio: FINO

O controller é um **tradutor HTTP ↔ Use Case**. Ele:
1. Recebe o request HTTP
2. Valida a entrada (`@Valid`)
3. Delega para o Use Case
4. Retorna o response HTTP

### Padrão

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final RegisterUserUseCase registerUser;
    private final FindUserUseCase findUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return registerUser.execute(request);
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable UUID id) {
        return findUser.execute(id);
    }
}
```

### Regras

| Regra | Correto | Errado |
|-------|---------|--------|
| try/catch | Delegar para @ControllerAdvice | try/catch no controller |
| Lógica | Nenhuma | if/else de negócio no controller |
| Validação | @Valid + Bean Validation | Validação manual no controller |
| Retorno | ResponseEntity ou @ResponseStatus | String literal |
| Logging | `log.info("msg: [{}]", id)` | StringBuilder, concatenação |

### Request/Response DTOs

```java
// Request — validação via Bean Validation
public record RegisterUserRequest(
    @NotBlank @Size(min = 2, max = 50) String name,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password
) {}

// Response — sem dados sensíveis
public record UserResponse(
    UUID id,
    String name,
    String email,
    Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId().value(),
            user.getName().value(),
            user.getEmail().value(),
            user.getCreatedAt()
        );
    }
}
```

### Exception Handler Global

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        log.warn("Recurso não encontrado: [{}]", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), "NOT_FOUND");
    }

    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDomain(DomainException ex) {
        log.warn("Erro de domínio: [{}]", ex.getMessage());
        return new ErrorResponse(ex.getMessage(), "DOMAIN_ERROR");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return new ErrorResponse(message, "VALIDATION_ERROR");
    }
}

public record ErrorResponse(String message, String code) {}
```

---

## 3. Camada Application (Use Cases)

### Princípio: 1 classe = 1 caso de uso (SRP)

Não criar `UserService` com 15 métodos. Cada ação de negócio é uma classe:

```
application/
├── RegisterUserUseCase.java
├── FindUserUseCase.java
├── UpdateUserProfileUseCase.java
├── DeactivateUserUseCase.java
└── FollowUserUseCase.java
```

### Padrão

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    @Transactional
    public UserResponse execute(RegisterUserRequest request) {
        log.info("Registrando usuário: [{}]", request.email());

        userRepository.findByEmail(Email.of(request.email()))
            .ifPresent(existing -> {
                throw new UserAlreadyExistsException(request.email());
            });

        User user = User.create(
            request.name(),
            request.email(),
            passwordEncoder.encode(request.password())
        );

        User saved = userRepository.save(user);

        events.publishEvent(new UserRegistered(saved.getId().value(), Instant.now()));

        log.info("Usuário registrado com sucesso: [{}]", saved.getId());
        return UserResponse.from(saved);
    }
}
```

### Regras

| Regra | Detalhe |
|-------|---------|
| @Transactional | AQUI, nunca no repository |
| Eventos | Publicar via ApplicationEventPublisher |
| Retorno | DTO de response (nunca domain entity) |
| Dependências | Interfaces do domain (ports), não implementações |
| Exceções | Lançar domain exceptions descritivas |

---

## 4. Camada Domain (Core)

### Princípio: Lógica de negócio vive aqui. PURO JAVA.

### Entities (com lógica, não anêmicas)

```java
public class User {

    private UserId id;
    private UserName name;
    private Email email;
    private HashedPassword password;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // Factory method (não construtor público)
    public static User create(String name, String email, String hashedPassword) {
        User user = new User();
        user.id = UserId.generate();
        user.name = UserName.of(name);
        user.email = Email.of(email);
        user.password = HashedPassword.of(hashedPassword);
        user.status = UserStatus.ACTIVE;
        user.createdAt = Instant.now();
        user.updatedAt = Instant.now();
        return user;
    }

    // Lógica de negócio NA entidade
    public void deactivate() {
        if (this.status == UserStatus.DEACTIVATED) {
            throw new UserAlreadyDeactivatedException(this.id);
        }
        this.status = UserStatus.DEACTIVATED;
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String name, String email) {
        this.name = UserName.of(name);
        this.email = Email.of(email);
        this.updatedAt = Instant.now();
    }

    // Getters (sem setters públicos — estado muda via métodos de negócio)
    public UserId getId() { return id; }
    public UserName getName() { return name; }
    public Email getEmail() { return email; }
}
```

### Value Objects (records imutáveis, validam no construtor)

```java
public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "UserId não pode ser null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }
}

public record Email(String value) {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public Email {
        Objects.requireNonNull(value, "Email não pode ser null");
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new InvalidEmailException(value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }
}

public record UserName(String value) {
    public UserName {
        Objects.requireNonNull(value, "Nome não pode ser null");
        if (value.isBlank() || value.length() < 2 || value.length() > 50) {
            throw new InvalidUserNameException(value);
        }
    }

    public static UserName of(String value) {
        return new UserName(value);
    }
}
```

### Repository (interface = port)

```java
// No pacote domain — é uma INTERFACE, não implementação
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(Email email);
    boolean existsByEmail(Email email);
}
```

### Domain Exceptions

```java
// Base
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}

// Específicas
public class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String email) {
        super("Usuário já existe com email: " + email);
    }
}

public class InvalidEmailException extends DomainException {
    public InvalidEmailException(String email) {
        super("Email inválido: " + email);
    }
}
```

---

## 5. Camada Infrastructure

### JPA Entity (SEPARADA da Domain Entity)

```java
@Entity
@Table(name = "users", schema = "identity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

### Mapper (Domain ↔ JPA)

```java
@Component
public class UserMapper {

    public UserJpaEntity toJpa(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId().value());
        entity.setName(user.getName().value());
        entity.setEmail(user.getEmail().value());
        entity.setPassword(user.getPassword().value());
        entity.setStatus(user.getStatus().name());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }

    public User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
            UserId.of(entity.getId()),
            UserName.of(entity.getName()),
            Email.of(entity.getEmail()),
            HashedPassword.of(entity.getPassword()),
            UserStatus.valueOf(entity.getStatus()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
```

### Repository Implementation

```java
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpa;
    private final UserMapper mapper;

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toJpa(user);
        UserJpaEntity saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }
}

// JPA Repository (Spring Data)
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

---

## 6. Domain Events

### Padrão

```java
// Record imutável, nome no passado, contém IDs
public record UserRegistered(
    UUID userId,
    Instant occurredAt
) {}

public record PostCreated(
    UUID postId,
    UUID authorId,
    Instant occurredAt
) {}

public record UserFollowed(
    UUID followerId,
    UUID followedId,
    Instant occurredAt
) {}
```

### Publicação (no Use Case)

```java
events.publishEvent(new UserRegistered(user.getId().value(), Instant.now()));
```

### Consumo (Event Handler)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredHandler {

    private final NotificationService notificationService;

    @EventListener
    public void handle(UserRegistered event) {
        log.info("Processando UserRegistered: [{}]", event.userId());
        notificationService.sendWelcomeEmail(event.userId());
    }
}
```

---

## 7. Comunicação entre Módulos

### Via Interfaces (kora-common)

```java
// Em kora-common — interface que qualquer módulo pode usar
public interface UserQueryPort {
    Optional<UserSummary> findSummaryById(UUID userId);
}

// Em kora-identity — implementação
@Service
public class UserQueryAdapter implements UserQueryPort { ... }

// Em kora-content — uso
@RequiredArgsConstructor
public class CreatePostUseCase {
    private final UserQueryPort userQuery;

    public PostResponse execute(CreatePostRequest req) {
        UserSummary author = userQuery.findSummaryById(req.authorId())
            .orElseThrow(() -> new AuthorNotFoundException(req.authorId()));
        // ...
    }
}
```

### Via Eventos (Spring Events)

Módulos publicam eventos, outros escutam. **Sem dependência direta.**

---

## 8. Formato de Comunicação (JSON)

### Serialização

- **Jackson** para JSON (vem com Spring Boot)
- **NUNCA** usar `String.format` para construir JSON
- **NUNCA** usar StringBuilder para construir JSON
- DTOs (records) são serializados automaticamente pelo Jackson

### Convenções de JSON

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userName": "alice",
  "email": "alice@mail.com",
  "createdAt": "2026-03-22T10:30:00Z",
  "status": "ACTIVE"
}
```

- camelCase para campos
- UUID como string
- Datas em ISO 8601 (UTC)
- Enums como string (não ordinal)

---

## 9. Logging

### Padrão SLF4J com Logback

```java
// CORRETO
log.info("Usuário registrado: [{}]", user.getId());
log.debug("Detalhes do pedido: [{}]", order);
log.warn("Tentativa de login falhou para: [{}]", email);
log.error("Erro ao processar pagamento: [{}]", paymentId, exception);

// ERRADO — NUNCA
log.info("Usuário registrado: " + user.getId());
log.info(new StringBuilder().append("...").toString());
log.info(String.format("Usuário: %s", user.getId()));
log.info("Senha do usuário: [{}]", user.getPassword());
```

### Regras de Logging

| Nível | Quando usar |
|-------|-------------|
| ERROR | Falha que impede a operação |
| WARN | Situação anormal mas recuperável |
| INFO | Marcos importantes (início/fim de operação) |
| DEBUG | Detalhes para troubleshooting |
| TRACE | Ultra-detalhado (raramente) |

---

## 10. Testes

### Unitários (domain/)

```java
class UserTest {

    @Test
    void create_validData_returnsActiveUser() {
        User user = User.create("Alice", "alice@mail.com", "hashed");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getName().value()).isEqualTo("Alice");
    }

    @Test
    void deactivate_alreadyDeactivated_throwsException() {
        User user = User.create("Alice", "alice@mail.com", "hashed");
        user.deactivate();
        assertThatThrownBy(user::deactivate)
            .isInstanceOf(UserAlreadyDeactivatedException.class);
    }
}
```

### Unitários (application/ com Mockito)

```java
@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher events;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks RegisterUserUseCase useCase;

    @Test
    void execute_newUser_savesAndPublishesEvent() {
        var request = new RegisterUserRequest("Alice", "alice@mail.com", "password");
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UserResponse response = useCase.execute(request);

        assertThat(response.name()).isEqualTo("Alice");
        verify(events).publishEvent(any(UserRegistered.class));
    }
}
```

### Integração (Testcontainers)

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class UserControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired TestRestTemplate rest;

    @Test
    void register_validRequest_returns201() {
        var request = new RegisterUserRequest("Alice", "alice@mail.com", "password123");
        ResponseEntity<UserResponse> response = rest.postForEntity(
            "/api/v1/users", request, UserResponse.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

### Naming Convention

```
methodUnderTest_scenario_expectedBehavior
```

---

## 11. Estrutura de Pacotes (um módulo completo)

```
kora-identity/
└── src/main/java/com/kora/identity/
    ├── api/
    │   ├── controller/
    │   │   └── UserController.java
    │   ├── dto/
    │   │   ├── RegisterUserRequest.java
    │   │   ├── UserResponse.java
    │   │   └── ErrorResponse.java
    │   └── handler/
    │       └── GlobalExceptionHandler.java
    ├── application/
    │   ├── RegisterUserUseCase.java
    │   ├── FindUserUseCase.java
    │   └── DeactivateUserUseCase.java
    ├── domain/
    │   ├── entity/
    │   │   └── User.java
    │   ├── vo/
    │   │   ├── UserId.java
    │   │   ├── Email.java
    │   │   ├── UserName.java
    │   │   └── HashedPassword.java
    │   ├── repository/
    │   │   └── UserRepository.java
    │   ├── exception/
    │   │   ├── DomainException.java
    │   │   ├── UserAlreadyExistsException.java
    │   │   ├── InvalidEmailException.java
    │   │   └── UserAlreadyDeactivatedException.java
    │   └── UserStatus.java
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── UserJpaEntity.java
    │   │   ├── UserJpaRepository.java
    │   │   ├── UserRepositoryImpl.java
    │   │   └── UserMapper.java
    │   └── config/
    │       └── IdentityConfig.java
    └── event/
        ├── UserRegistered.java
        └── UserRegisteredHandler.java
```

---

## 12. Checklist de Review

Antes de fazer commit, verificar:

### Arquitetura
- [ ] Controller é fino? (sem lógica, sem try/catch)
- [ ] Use Case é 1 classe = 1 caso de uso?
- [ ] @Transactional no Use Case, não no repository?
- [ ] Domain entity tem lógica? (não anêmica)
- [ ] Domain NÃO importa Spring/JPA?
- [ ] JPA Entity separada da Domain Entity?

### Anti-Patterns
- [ ] Sem N+1 queries?
- [ ] Sem StringBuilder/concatenação em logs?
- [ ] Sem ResponseEntity no service?
- [ ] Sem acesso direto a repositório de outro módulo?
- [ ] Sem God Class? (< 500 linhas)

### Qualidade
- [ ] Testes para domain e application?
- [ ] Logging com SLF4J e `{}`?
- [ ] Sem dados sensíveis em logs?
- [ ] @Valid em request bodies?
- [ ] Exceções descritivas?

---

## Referências

- Joshua Bloch — *Effective Java*, 3rd Ed: Records, factory methods, immutability
- Eric Evans — *Domain-Driven Design*: Entities, Value Objects, Aggregates, Repositories
- Vlad Khononov — *Learning Domain-Driven Design*: Bounded Contexts, Domain Events
- Robert C. Martin — SOLID principles
- Alistair Cockburn — Hexagonal Architecture (Ports & Adapters)
