# Go para Gateways, Proxies e Networking: Guia de Estudo

> Artigo de estudo — Kora Project
> Data: 2026-03-22

---

## Por que Go para networking?

Go foi criado no Google para resolver problemas de **infraestrutura de rede em larga escala**. Suas características nativas são ideais para gateways e proxies:

| Característica | Por que importa para networking |
|---------------|-------------------------------|
| **Goroutines** | Milhares de conexões simultâneas com custo mínimo de memória (~2KB por goroutine vs ~1MB por thread Java) |
| **net/http nativo** | HTTP server e client de alta performance sem dependências externas |
| **Binary único** | Deploy como um único binário. Sem JVM, sem runtime. Container mínimo (~5MB scratch) |
| **Garbage Collector otimizado** | Pausas de GC sub-millisegundo — crítico para proxies que precisam de latência baixa |
| **Compilação rápida** | Rebuild em segundos, ciclo de feedback rápido |
| **Concurrency nativa** | Channels + goroutines para pipeline de processamento de requests |

---

## O que Go faz melhor que Java nesse contexto?

| Aspecto | Java | Go |
|---------|------|-----|
| Startup time | 2-5s (Spring Boot) | ~50ms |
| Memória base | ~200MB (JVM) | ~10MB |
| Container size | ~300MB (JRE) | ~5MB (scratch) |
| Concorrência | Virtual Threads (Java 21+) resolve, mas é recente | Goroutines desde o dia 1 |
| HTTP proxy | Precisa de Netty/Spring WebFlux | `httputil.ReverseProxy` nativo |

**Importante:** Java 21 com Virtual Threads fechou muito a diferença em concorrência. Mas para **infra pura** (proxies, gateways, CLIs), Go ainda tem vantagem em footprint e simplicidade.

---

## Onde Go entra no Kora?

Go NÃO substitui Java no backend de negócio. Go entra em camadas de **infraestrutura**:

```
Internet
    │
    ▼
┌──────────────────┐
│  API Gateway (Go) │  ← rate limiting, auth, routing, logging
└────────┬─────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌───────┐ ┌───────┐
│ Kora  │ │ Kora  │
│Backend│ │Content│    ← Java/Spring Boot (lógica de negócio)
│(Java) │ │(Java) │
└───────┘ └───────┘
```

Possíveis usos de Go no Kora (fases futuras):
1. **API Gateway**: roteamento, rate limiting, autenticação JWT
2. **Reverse Proxy**: balanceamento de carga entre réplicas
3. **Sidecar de observabilidade**: coletor de logs/métricas
4. **CLI tools**: scripts de administração, migrations, health checks

---

## Conceitos Fundamentais para Estudar

### 1. Reverse Proxy em Go

O pacote `net/http/httputil` já tem `ReverseProxy` pronto:

```go
// Proxy simples que roteia para o backend Java
target, _ := url.Parse("http://localhost:8080")
proxy := httputil.NewSingleHostReverseProxy(target)

http.HandleFunc("/api/", func(w http.ResponseWriter, r *http.Request) {
    proxy.ServeHTTP(w, r)
})
```

Conceitos a entender:
- Como `ReverseProxy` funciona internamente (lê request, faz request ao target, copia response)
- Modificar requests/responses em trânsito (middleware)
- Handling de WebSockets e streaming

### 2. Middleware Pattern em Go

Em Go, middleware é uma **função que envolve um Handler**:

```go
func loggingMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        start := time.Now()
        next.ServeHTTP(w, r)
        slog.Info("request", "method", r.Method, "path", r.URL.Path, "duration", time.Since(start))
    })
}

func rateLimitMiddleware(next http.Handler) http.Handler {
    limiter := rate.NewLimiter(100, 10) // 100 req/s, burst 10
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        if !limiter.Allow() {
            http.Error(w, "Too Many Requests", http.StatusTooManyRequests)
            return
        }
        next.ServeHTTP(w, r)
    })
}

// Composição:
handler := loggingMiddleware(rateLimitMiddleware(apiHandler))
```

Isso é o **Decorator Pattern** aplicado a HTTP handlers.

### 3. Concorrência para Networking

```go
// Fan-out: processar N requests em paralelo
func fetchAll(urls []string) []Response {
    ch := make(chan Response, len(urls))

    for _, url := range urls {
        go func(u string) {
            resp, err := http.Get(u)
            ch <- Response{URL: u, Resp: resp, Err: err}
        }(url)
    }

    results := make([]Response, 0, len(urls))
    for range urls {
        results = append(results, <-ch)
    }
    return results
}
```

Conceitos a entender:
- Goroutines vs threads
- Channels (buffered vs unbuffered)
- `select` para multiplexação
- `context.Context` para cancelamento e timeout
- `sync.WaitGroup`, `sync.Mutex`, `sync.Once`

### 4. gRPC (comunicação entre serviços)

Para comunicação interna entre gateway Go e backend Java:

```protobuf
service UserService {
    rpc GetUser (GetUserRequest) returns (UserResponse);
    rpc ListUsers (ListUsersRequest) returns (stream UserResponse);
}
```

Go e Java ambos têm excelente suporte a gRPC. Mais eficiente que REST para comunicação interna.

---

## Plano de Estudo

### Nível 1: Fundamentos Go (2-3 semanas)

**Leitura:**
- Donovan, Kernighan — *The Go Programming Language*: Caps. 1-5 (fundamentos)
- *Effective Go* (doc oficial — leitura obrigatória, curta)

**Exercícios:**
- Hello world → HTTP server → REST API simples
- Goroutines + channels: fan-out, pipeline

### Nível 2: Networking em Go (2-3 semanas)

**Leitura:**
- Donovan, Kernighan — *The Go Programming Language*: Caps. 8-9 (goroutines, concurrency)
- *100 Go Mistakes*: Caps. sobre concurrency (erros comuns com goroutines)
- *Concurrency in Go*: Caps. 1-4

**Exercícios:**
- Construir um reverse proxy simples
- Adicionar middleware (logging, rate limiting)
- Testar com `ab` ou `wrk` (benchmarking)

### Nível 3: Gateway para o Kora (quando chegar a fase)

**Leitura:**
- Alex Xu — *System Design Interview*: Cap. sobre Rate Limiting, API Gateway
- *Concurrency in Go*: Caps. 5-6 (patterns avançados)

**Exercícios:**
- Gateway que roteia para múltiplos backends
- JWT validation no gateway
- Circuit breaker em Go (sem lib, implementação manual)
- Métricas Prometheus expostas pelo gateway

### Projetos de referência (open-source)

| Projeto | O que aprender |
|---------|---------------|
| **Caddy** | Reverse proxy em Go, plugin architecture |
| **Traefik** | Dynamic routing, service discovery |
| **KrakenD** | API Gateway em Go, aggregation |
| **Go kit** | Microservices toolkit, middleware pattern |

Não precisa ler todo o código — olhar a arquitetura, como organizam packages, como implementam middleware.

---

## Quando Começar

Go entra no Kora nas **fases 15+** (após o monólito modular estar sólido e o frontend funcionando). Antes disso:

1. Estude os fundamentos (Nível 1) em paralelo, nos intervalos
2. Faça exercícios isolados na pasta `golang/projects/`
3. Quando a arquitetura do Kora justificar um gateway, aplique o Nível 3

**Não** comece a construir o gateway antes do backend Java estar maduro. Gateway sem backend é infraestrutura sem propósito.

---

## Referências

- Donovan, Kernighan — *The Go Programming Language* (2015)
- Teiva Harsanyi — *100 Go Mistakes and How to Avoid Them* (2022)
- *Concurrency in Go* — Katherine Cox-Buday
- *Effective Go* — https://go.dev/doc/effective_go
- Hoare — *Communicating Sequential Processes* (1978) — base teórica dos channels
