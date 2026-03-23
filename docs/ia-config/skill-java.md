# Java – Contexto de Desenvolvimento (Kora)

Você está trabalhando com **Java 21** e **Spring Boot 3.3.x** no projeto **Kora** (rede social).

## Arquitetura Hexagonal Simplificada

Cada módulo tem 5 pacotes. **A regra de dependência é obrigatória:**

```
api → application → domain ← infrastructure
                      ↑
                    event
```

`domain/` NÃO importa Spring, JPA, ou qualquer framework.

## Padrões por camada

### api/ — Controller (FINO)
```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final RegisterUserUseCase registerUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody @Valid RegisterUserRequest request) {
        return registerUser.execute(request);
    }
}
```
- Sem try/catch (usar @ControllerAdvice)
- Sem lógica de negócio
- Request/Response DTOs separados do domain

### application/ — Use Case (1 classe = 1 caso de uso)
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponse execute(RegisterUserRequest request) {
        Email email = Email.of(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = User.create(email, Username.of(request.username()),
                                passwordEncoder.encode(request.password()));
        userRepository.save(user);
        eventPublisher.publishEvent(new UserRegistered(user.getId(), email.value(), Instant.now()));
        return UserResponse.from(user);
    }
}
```

### domain/ — Entidades com lógica + Value Objects como record
```java
// Aggregate Root (NÃO tem @Entity, NÃO importa Spring)
public class User {
    private UUID id;
    private Email email;
    private Username username;
    private String passwordHash;
    private Instant createdAt;

    private User(UUID id, Email email, Username username, String passwordHash) {
        this.id = id; this.email = email;
        this.username = username; this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public static User create(Email email, Username username, String encodedPassword) {
        return new User(UUID.randomUUID(), email, username, encodedPassword);
    }

    public static User reconstitute(UUID id, Email email, Username username,
                                     String passwordHash, Instant createdAt) {
        User user = new User(id, email, username, passwordHash);
        user.createdAt = createdAt;
        return user;
    }
}

// Value Object
public record Email(String value) {
    public Email {
        if (value == null || value.isBlank()) throw new InvalidEmailException("vazio");
        if (!value.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            throw new InvalidEmailException(value);
        value = value.toLowerCase().trim();
    }
    public static Email of(String value) { return new Email(value); }
}

// Repository = interface (port)
public interface UserRepository {
    void save(User user);
    Optional<User> findById(UUID id);
    boolean existsByEmail(Email email);
}
```

### infrastructure/ — JPA separada do domain
```java
@Entity @Table(name = "users", schema = "identity")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false, unique = true) private String username;
    @Column(name = "password_hash") private String passwordHash;
    @Column(name = "created_at") private Instant createdAt;
}

@Repository @RequiredArgsConstructor
public class JpaUserRepository implements UserRepository {
    private final SpringDataUserRepository springData;
    private final UserMapper mapper;

    @Override
    public void save(User user) { springData.save(mapper.toJpa(user)); }
    @Override
    public Optional<User> findById(UUID id) {
        return springData.findById(id).map(mapper::toDomain);
    }
}
```

### event/ — Domain Events
```java
public record UserRegistered(UUID userId, String email, Instant occurredAt) {}
```

## Exception Handling Global
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleEmailExists(EmailAlreadyExistsException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Email já cadastrado");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
    }
}
```

## Testes
```java
// Unitário (domain)
@Test void create_withValidEmail_returnsUser() {
    User user = User.create(Email.of("test@kora.com"), Username.of("testuser"), "hash");
    assertThat(user.getEmail().value()).isEqualTo("test@kora.com");
}

// Integração (Testcontainers)
@SpringBootTest @Testcontainers
class RegisterUserUseCaseIT {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) { r.add("spring.datasource.url", pg::getJdbcUrl); }
}
```

## Convenções Kora
- Maven para build
- Records para DTOs, Value Objects, e Domain Events
- `@Valid` em todos os request bodies
- Lombok: apenas `@RequiredArgsConstructor`, `@Slf4j`, `@Getter` (JPA entity). Nunca `@Data` em JPA
- Logging: `log.info("msg: [{}]", value)` — nunca concatenação/StringBuilder
- Flyway para migrations, `ddl-auto: validate`
- Schema por contexto: `identity.*`, `content.*`, `social.*`
- Referência completa: `docs/articles/011-padroes-implementacao-kora.md`
