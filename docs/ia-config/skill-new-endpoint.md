# /new-endpoint — Criar Endpoint REST (Kora)

Crie um novo endpoint seguindo os padrões do Kora.

## Parâmetros
- `$ARGUMENTS` — descrição do endpoint (ex: "POST /api/v1/users register user")

## Checklist de criação

### 1. Request/Response DTOs (em api/request/ e api/response/)
```java
// Request: validações com Bean Validation
public record CreatePostRequest(
    @NotBlank @Size(max = 5000) String content,
    List<String> tags
) {}

// Response: factory method from() para converter do domain
public record PostResponse(
    UUID id, String content, String authorUsername, Instant createdAt
) {
    public static PostResponse from(Post post, String authorUsername) {
        return new PostResponse(post.getId(), post.getContent().value(),
                               authorUsername, post.getCreatedAt());
    }
}
```

### 2. Controller (em api/)
```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public PostResponse create(@RequestBody @Valid CreatePostRequest request) {
    return createPostUseCase.execute(request);
}
```
- Sem try/catch, sem lógica
- @Valid no request body
- HTTP status correto

### 3. Use Case (em application/)
```java
@Service @RequiredArgsConstructor @Slf4j
public class CreatePostUseCase {
    @Transactional
    public PostResponse execute(CreatePostRequest request) {
        // validar, criar entidade, salvar, publicar evento, retornar response
    }
}
```

### 4. Domain (se necessário)
- Nova entidade ou Value Object
- Exceção de domínio específica

### 5. OpenAPI
- Endpoint aparece automaticamente via springdoc-openapi
- Adicionar @Tag e @Operation se necessário

### 6. Teste de integração
```java
@Test void create_withValidRequest_returns201() {
    var request = new CreatePostRequest("Hello Kora!", List.of("test"));
    mockMvc.perform(post("/api/v1/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("Hello Kora!"));
}
```
