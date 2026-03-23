# Guia de Estudo: Go para Networking, Gateways e Proxies

> Kora Project — Guia para aprender onde Go brilha no ecossistema
> Data: 2026-03-22

---

## Por que Go para networking?

Go foi criado no Google especificamente para resolver problemas de **infraestrutura de rede em escala**. As características que o tornam ideal:

| Característica | Por que importa para networking |
|---------------|-------------------------------|
| **Goroutines** | Milhares de conexões simultâneas com custo mínimo (~2KB por goroutine vs ~1MB por thread Java) |
| **net/http nativo** | HTTP server/client production-ready na stdlib, sem dependência externa |
| **Binary estático** | 1 binário, sem runtime, sem JVM. Deploy = copiar 1 arquivo para o container |
| **Startup ~ms** | Inicia em milissegundos (vs segundos de Spring Boot). Ideal para scaling rápido |
| **Compilação rápida** | Ciclo de feedback curto em desenvolvimento |
| **Garbage collector otimizado** | Latência previsível, pausas sub-milissegundo |

---

## O que estudar (em ordem)

### Nível 1: Fundamentos de Go (2-3 semanas)

**Objetivo:** escrever Go idiomático, entender goroutines e channels.

1. **Go Tour** (oficial): https://go.dev/tour/
   - Faça tudo, inclusive os exercícios

2. **Effective Go**: https://go.dev/doc/effective_go
   - Leitura obrigatória, explica a "filosofia" da linguagem

3. **"The Go Programming Language" (Donovan & Kernighan)**
   - Você já tem esse livro! Capítulos essenciais:
     - Cap 1-5: Fundamentos
     - Cap 8: Goroutines e Channels
     - Cap 9: Concurrency com shared variables

4. **"100 Go Mistakes and How to Avoid Them" (Teiva Harsanyi)**
   - Você já tem esse livro! Ler os capítulos sobre:
     - Concurrency mistakes
     - Error handling
     - HTTP/networking mistakes

### Nível 2: Networking em Go (2-3 semanas)

**Objetivo:** construir servidores HTTP, proxies, e entender o pacote `net`.

1. **Package `net/http`** — ler a doc oficial e implementar:
   - Um HTTP server simples
   - Middleware chain (logging, auth, rate limiting)
   - Graceful shutdown com `context.Context`

2. **Package `net`** — entender:
   - TCP listeners e connections
   - UDP basics
   - DNS resolution

3. **Reverse Proxy** — implementar um proxy reverso:
   ```go
   // Go tem isso built-in:
   httputil.NewSingleHostReverseProxy(targetURL)
   ```
   - Depois: implementar um do zero para entender o protocolo

4. **Artigos/Talks:**
   - "Go Concurrency Patterns" (Rob Pike) — YouTube
   - "Advanced Go Concurrency Patterns" (Sameer Ajmani) — YouTube
   - Você tem o paper "Communicating Sequential Processes" (Hoare, 1978) — a base teórica dos channels de Go

### Nível 3: API Gateways e Proxies (3-4 semanas)

**Objetivo:** entender e construir componentes de infraestrutura de rede.

#### O que é um API Gateway?

```
Clients (web, mobile, IoT)
    │
    ▼
┌──────────────────────┐
│     API Gateway      │  ← Go é excelente aqui
│                      │
│ • Rate limiting      │
│ • Authentication     │
│ • Load balancing     │
│ • Request routing    │
│ • Response caching   │
│ • Circuit breaking   │
│ • Request/Response   │
│   transformation     │
│ • SSL termination    │
│ • Logging/Metrics    │
└──────────┬───────────┘
           │
    ┌──────┼──────┐
    ▼      ▼      ▼
 Service  Service  Service
    A       B       C
```

#### Projetos open-source para estudar (leia o código):

| Projeto | O que é | O que aprender |
|---------|---------|---------------|
| **Traefik** | Reverse proxy / load balancer | Como Go lida com routing dinâmico, middleware chain, hot reload de config |
| **Caddy** | Web server com HTTPS automático | Modular plugin architecture em Go, TLS management |
| **KrakenD** | API Gateway | Aggregation de backends, response transformation |
| **Go kit** | Microservices toolkit | Transport layer, middleware, service discovery |

#### O que implementar como exercício:

1. **Mini API Gateway** (para o Kora):
   ```
   Client → Go Gateway (:3000)
       → /api/identity/*   → kora-api (:8080) /identity/*
       → /api/content/*    → kora-api (:8080) /content/*
       → /api/feed/*       → kora-api (:8080) /feed/*
   ```
   Mesmo apontando para o mesmo backend (monólito), o gateway adiciona:
   - Rate limiting por IP/user
   - Request logging com trace ID
   - Response caching para GET /feed
   - Métricas (requests/s, latência por rota)

2. **Sidecar proxy** (conceito Kora):
   - Um processo Go que roda ao lado do serviço Java
   - Intercepta tráfego de entrada/saída
   - Adiciona retry, circuit breaking, mTLS
   - Conceito similar ao que **Envoy/Istio** fazem (mas simplificado)

### Nível 4: Ferramentas de Plataforma em Go (ongoing)

**Objetivo:** entender por que o ecossistema Cloud Native é quase todo Go.

| Ferramenta | Escrita em Go | O que faz |
|-----------|--------------|-----------|
| **Docker** | Go | Containers |
| **Kubernetes** | Go | Orquestração de containers |
| **Prometheus** | Go | Métricas |
| **Grafana Agent** | Go | Coleta de telemetria |
| **Terraform** | Go | Infrastructure as Code |
| **Consul** | Go | Service discovery |
| **Vault** | Go | Secrets management |
| **etcd** | Go | Key-value distribuído |
| **CockroachDB** | Go | Banco distribuído |

**Exercício avançado (Fase 15+):** escrever um Kubernetes Operator em Go para gerenciar deploy do Kora.

---

## Onde Go entra no Kora (timeline)

| Fase | Uso de Go | O que implementar |
|------|-----------|-------------------|
| 1-10 | Nenhum | Foco em Java + Angular |
| 11 | Estudo | Começar Nível 1-2 do guia |
| 13 | API Gateway | Mini gateway em Go na frente do Kora |
| 15+ | Sidecar/Proxy | Proxy sidecar para observabilidade |
| 15+ | CLI | Ferramenta CLI para deploy/management do Kora |

---

## Livros que você já tem (ordem de leitura para Go)

1. **"The Go Programming Language"** (Donovan & Kernighan) — fundamentos
2. **"100 Go Mistakes"** (Harsanyi) — armadilhas práticas
3. **"Concurrency in Go"** — aprofundamento em goroutines/channels
4. **"Effective Go"** (online) — estilo idiomático
5. **"Communicating Sequential Processes"** (Hoare, 1978) — teoria por trás dos channels

## Recursos adicionais gratuitos

- **Go by Example**: https://gobyexample.com/ — referência rápida com exemplos
- **Go Blog**: https://go.dev/blog/ — artigos oficiais
- **GopherCon talks** no YouTube — palestras da conferência principal de Go
- Código fonte do **net/http** package — é surpreendentemente legível
