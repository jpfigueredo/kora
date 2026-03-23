# KORA — Plano Mestre de Construção

> Rede social com funcionalidades para vivência do dia-a-dia.
> Projeto educacional, open-source, zero custo.
> Cada commit ensina algo. Toda decisão é documentada.

---

## Índice

1. [Visão do Produto](#1-visão-do-produto)
2. [Tech Stack](#2-tech-stack)
3. [Domínios e Bounded Contexts](#3-domínios-e-bounded-contexts)
4. [Arquitetura — Monólito Modular](#4-arquitetura--monólito-modular)
5. [Elasticidade dos Domínios — Quando Separar](#5-elasticidade-dos-domínios--quando-separar)
6. [Estrutura de Diretórios](#6-estrutura-de-diretórios)
7. [Git Flow e Estratégia de Commits](#7-git-flow-e-estratégia-de-commits)
8. [Fases de Construção](#8-fases-de-construção)
9. [Design Patterns — Onde e Por Quê](#9-design-patterns--onde-e-por-quê)
10. [Anti-Patterns — O Que Evitar](#10-anti-patterns--o-que-evitar)
11. [Observabilidade e Métricas](#11-observabilidade-e-métricas)
12. [Segurança](#12-segurança)
13. [DevOps e Plataforma](#13-devops-e-plataforma)
14. [Configuração de IA (Claude + Cursor)](#14-configuração-de-ia-claude--cursor)
15. [ADRs — Registro de Decisões Arquiteturais](#15-adrs--registro-de-decisões-arquiteturais)
16. [Artigos e Guias de Estudo](#16-artigos-e-guias-de-estudo)
17. [Glossário](#17-glossário)

---

## 1. Visão do Produto

### 1.1 O que é o Kora?

Uma rede social onde pessoas e comunidades podem:

| Feature | Descrição | Prioridade |
|---------|-----------|------------|
| **Perfil Pessoal** | Feed de posts das conexões, informações pessoais | P0 (core) |
| **Comunidades** | Página 100% customizável (HTML + SCSS restrito), gestão por membros | P0 (core) |
| **Loja** | Sessão de loja dentro de comunidades | P1 |
| **Eventos** | Criação e gestão de eventos futuros | P1 |
| **Blog** | Posts com vídeos embutidos, conversas | P1 |
| **Financeiro Pessoal** | Dashboard financeiro, pagamentos recorrentes, assistente IA | P2 |
| **Commerce (Loja)** | Loja da comunidade, produtos, vendas, relatórios | P2 |
| **Autenticação** | Login, registro, perfis de acesso | P0 (core) |

### 1.2 Objetivo técnico

- Demonstrar habilidades em arquitetura, patterns e plataforma
- Aprender Kafka para gestão de tráfego em rede social
- Preparação para entrevistas técnicas e mercado
- Portfólio vivo com commits didáticos

---

## 2. Tech Stack

### 2.1 Decisões definidas

| Camada | Tecnologia | Justificativa |
|--------|------------|---------------|
| **Backend** | Java 21 + Spring Boot 3.3.x | LTS estável, ecossistema maduro, mercado forte |
| **Frontend** | Angular 18+ | Prioridade para Aubay, framework enterprise |
| **Mensageria (fase 7)** | RabbitMQ | MQ simples, primeiro passo antes de Kafka |
| **Event Streaming (fase 11+)** | Apache Kafka (OSS) + Avro + Schema Registry | Event-driven para tráfego alto, replay, CQRS |
| **Banco de dados** | PostgreSQL | Relacional robusto, gratuito, JSON support |
| **Cache** | Redis | Session, cache de feed, rate limiting |
| **Build** | Maven | Domínio profundo do usuário |
| **Containers** | Docker + Docker Compose | Dev local, zero custo |
| **CI/CD** | GitHub Actions | Gratuito para open-source |
| **K8s local** | Kind ou Minikube | Aprendizado de plataforma, zero custo |
| **Observabilidade** | Micrometer + Prometheus + Grafana | Stack OSS padrão |
| **Testes** | JUnit 5 + Testcontainers + Mockito | Testes de integração reais |

### 2.2 Decisões a refinar (conversar antes de implementar)

| Tópico | Opções | Status |
|--------|--------|--------|
| **Spring Boot versão** | 3.3.x (estável) vs 4.1.x (snapshot) | **Recomendação: 3.3.x** — snapshot causa dor desnecessária |
| **GraalVM** | Usar native image? | **Deixar para fase posterior** — foco em funcionalidade primeiro |
| **Gradle** | Migrar de Maven? | **Não agora** — exercício separado depois do Kora funcional |
| **Schema Registry** | Avro + Apicurio/Confluent | **JSON String fases 1-10**, Avro na fase 11+ (big techs usam, é obrigatório aprender) |
| **Search** | Elasticsearch/OpenSearch | **Fase posterior** — quando tiver volume de posts |
| **Object Storage** | MinIO (S3-compatible OSS) | **Para imagens/vídeos** — definir quando implementar mídia |
| **Mobile** | React Native | **Fase 15+** — portfolio multi-plataforma, BFF quando tiver mobile |
| **Go (Gateway/Proxy)** | Go + net/http | **Fase 13+** — API Gateway, rate limiting, proxies. Ver guia: `docs/study-guides/go-networking-gateways-proxies.md` |
| **Micro-frontend** | Module Federation | **Provavelmente não** — 1 dev, sem ganho. POC separado se quiser aprender |
| **NoSQL (MongoDB)** | Feed materializado / analytics | **Fase 13+** se necessário. PostgreSQL JSONB cobre fases 1-10 |

### 2.3 Por que Spring Boot 3.3.x e não 4.1.0-SNAPSHOT?

- [ ] **TODO: Migrar de 4.1.0-SNAPSHOT para 3.3.x** no pom.xml
- Spring Boot 4.x ainda está em snapshot — breaking changes frequentes
- Java 25 não existe no ambiente; Java 21 é LTS e suportado por 3.3.x
- Ferramentas (IDE, plugins, Testcontainers) têm suporte estável para 3.3.x
- **Princípio: não lutar contra a ferramenta enquanto se aprende arquitetura**

---

## 3. Domínios e Bounded Contexts

Usando vocabulário de DDD (Domain-Driven Design):

```
┌─────────────────────────────────────────────────────────┐
│                        KORA                             │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Identity │  │  Social  │  │ Content  │             │
│  │ Context  │  │ Context  │  │ Context  │             │
│  │          │  │          │  │          │             │
│  │ - User   │  │ - Conn.  │  │ - Post   │             │
│  │ - Auth   │  │ - Follow │  │ - Feed   │             │
│  │ - Profile│  │ - Block  │  │ - Media  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │Community │  │ Commerce │  │ Personal │             │
│  │ Context  │  │ Context  │  │ Finance  │             │
│  │          │  │          │  │          │             │
│  │ - Group  │  │ - Shop   │  │ - Budget │             │
│  │ - Page   │  │ - Product│  │ - Expense│             │
│  │ - Event  │  │ - Order  │  │ - Recur. │             │
│  │ - Blog   │  │ - Payment│  │ - AI Sug.│             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                         │
│  ┌──────────────────────────────────────┐              │
│  │         Notification Context         │              │
│  │  - Push, Email, In-App, WebSocket    │              │
│  └──────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
```

### 3.1 Contextos Core (P0) — Construir primeiro

| Contexto | Agregados principais | Por quê é core? |
|----------|---------------------|-----------------|
| **Identity** | User, Profile, Credential | Sem identidade não há rede social |
| **Social** | Connection, Follow | Relacionamentos são a base do feed |
| **Content** | Post, Feed, Comment | O produto principal é o conteúdo |

### 3.2 Contextos de Suporte (P1)

| Contexto | Agregados principais | Depende de |
|----------|---------------------|------------|
| **Community** | Community, CommunityPage, Event, BlogPost | Identity, Social, Content |
| **Notification** | Notification, NotificationPreference | Todos os outros |

### 3.3 Contextos Genéricos (P2)

| Contexto | Agregados principais | Depende de |
|----------|---------------------|------------|
| **Commerce** | Shop, Product, Order, CommunityWallet | Identity, Community |
| **Personal Finance** | Budget, Expense, RecurringPayment, FinancialGoal | Identity |

> **Nota:** Commerce e Personal Finance são bounded contexts separados — linguagem ubíqua diverge, perfis de acesso diferentes, dependências externas diferentes. Ver [ADR-005](docs/adrs/005-financeiro-modulos-separados.md) e [Artigo 004](docs/articles/004-financeiro-separar-ou-unir.md).

### 3.4 Comunicação entre contextos

No **monólito modular**, contextos se comunicam via:

1. **Chamada direta de serviço** (síncrona, dentro do mesmo processo)
   - Ex: `ContentService` chama `SocialService.getConnections(userId)` para montar feed
2. **Eventos de domínio internos** (assíncrono, in-process)
   - Ex: `PostCreated` → `NotificationService` notifica seguidores
   - Implementação: Spring `ApplicationEventPublisher` (não precisa Kafka ainda)
3. **Interfaces/contratos entre módulos** (DIP — Dependency Inversion)
   - Cada módulo expõe interfaces; nunca acessa diretamente o repositório de outro

**Evolução da comunicação (cada passo documentado em artigo + commit):**

1. **Fases 1-6:** Spring Events (in-process, monólito) — sem infra extra
2. **Fases 7-10:** RabbitMQ — message queue simples, desacoplar Notification
3. **Fase 11+:** Kafka + Avro + Schema Registry — event streaming, CQRS, replay

Ver [ADR-003](docs/adrs/003-mensageria-evolucao-events-rabbitmq-kafka.md) e [Artigo 003](docs/articles/003-rabbitmq-vs-kafka-evolucao-mensageria.md).

---

## 4. Arquitetura — Monólito Modular

### 4.1 Por que monólito modular e não microserviços?

| Fator | Monólito Modular | Microserviços |
|-------|-----------------|---------------|
| **Complexidade operacional** | Baixa (1 deploy) | Alta (N deploys, service mesh, etc.) |
| **Latência** | Chamadas in-process | Rede (HTTP/gRPC/Kafka) |
| **Consistência de dados** | Transações ACID locais | Sagas, eventual consistency |
| **Time size** | 1 pessoa | Equipe por serviço |
| **Custo de infra** | 1 container | N containers + infra auxiliar |
| **Refatoração** | Fácil (mesmo processo) | Cara (contratos de API) |

**Decisão:** começar monólito modular. A modularidade interna (pacotes, interfaces, eventos) prepara a extração futura sem o custo operacional prematuro.

### 4.2 Estrutura de módulos Maven

```
kora/
├── pom.xml                          # Parent POM (aggregator)
├── kora-api/                        # Módulo principal — Spring Boot app
│   ├── pom.xml
│   └── src/
│       ├── main/java/org/gustalab/kora/
│       │   ├── KoraApplication.java
│       │   └── config/              # Beans globais, Security, CORS
│       └── main/resources/
│           ├── application.yml
│           └── db/migration/        # Flyway migrations
│
├── kora-identity/                   # Módulo: Identity Context
│   ├── pom.xml
│   └── src/main/java/org/gustalab/kora/identity/
│       ├── api/                     # Controllers (REST)
│       ├── domain/                  # Entities, Value Objects, Domain Services
│       ├── application/             # Use Cases / Application Services
│       ├── infrastructure/          # Repositories (JPA), external integrations
│       └── event/                   # Domain Events publicados por este contexto
│
├── kora-social/                     # Módulo: Social Context
│   └── (mesma estrutura)
│
├── kora-content/                    # Módulo: Content Context
│   └── (mesma estrutura)
│
├── kora-community/                  # Módulo: Community Context (P1)
│   └── (mesma estrutura)
│
├── kora-notification/               # Módulo: Notification Context (P1)
│   └── (mesma estrutura)
│
├── kora-commerce/                   # Módulo: Commerce Context (P2)
│   └── (mesma estrutura)
│
├── kora-personal-finance/           # Módulo: Personal Finance Context (P2)
│   └── (mesma estrutura)
│
├── kora-common/                     # Lib interna: DTOs compartilhados, utils, security filters
│   ├── pom.xml
│   └── src/main/java/org/gustalab/kora/common/
│       ├── dto/                     # DTOs usados em múltiplos módulos
│       ├── security/                # Filtros de segurança por perfil
│       ├── exception/               # Exception handling global
│       └── event/                   # Interfaces base de eventos de domínio
│
├── kora-ui/                         # Frontend Angular (workspace separado)
│   ├── angular.json
│   ├── package.json
│   └── src/
│
├── compose.yaml                     # Docker Compose (Postgres, Redis, Kafka, app)
├── Dockerfile                       # Multi-stage build do backend
├── .github/workflows/               # CI/CD
└── docs/
    ├── adrs/                        # Architecture Decision Records
    ├── api/                         # OpenAPI specs
    └── diagrams/                    # Diagramas C4, sequência, etc.
```

### 4.3 Regras de dependência entre módulos

```
kora-api  →  depende de todos os módulos (é o "assembler")
kora-identity  →  depende de kora-common
kora-social  →  depende de kora-common + kora-identity (via interface)
kora-content  →  depende de kora-common + kora-social (via interface)
kora-community  →  depende de kora-common + kora-identity + kora-content (via interface)
kora-notification  →  depende de kora-common (consome eventos de todos)
kora-common  →  NÃO depende de nenhum outro módulo kora
```

**Regra de ouro:** dependências sempre apontam para abstrações (interfaces em `kora-common` ou no próprio módulo), nunca para implementações concretas de outro módulo.

### 4.4 Respondendo: sidecar ou BFF para o serviço de posts?

**Nenhum dos dois, por enquanto.**

- **Sidecar:** é um processo auxiliar que roda ao lado do serviço principal (ex: Envoy para mTLS, Fluentd para logs). Não faz sentido para lógica de negócio como fan-out de posts.
- **BFF (Backend For Frontend):** é um backend dedicado a servir um frontend específico. Faz sentido quando você tem múltiplos frontends (web, mobile, TV) com necessidades diferentes de dados.

**O que faz sentido para posts/feed:**
- No monólito: `kora-content` com um `FeedService` que monta o feed
- Quando separar: um **serviço de domínio dedicado** (microserviço `kora-content-service`), não um sidecar nem BFF
- **Fan-out** é um pattern interno (fan-out on write vs fan-out on read) — discutiremos na fase de Kafka

---

## 5. Elasticidade dos Domínios — Quando Separar

### 5.1 Critérios para extrair um microserviço

Um contexto vira microserviço quando **pelo menos 2** destes critérios são verdadeiros:

| # | Critério | Indicador |
|---|----------|-----------|
| 1 | **Escala diferente** | Posts/Feed tem 100x mais leituras que Identity |
| 2 | **Deploy independente** | Precisa atualizar Content sem derrubar Identity |
| 3 | **Equipe dedicada** | Time diferente cuida desse domínio |
| 4 | **Tecnologia diferente** | Content precisa de Elasticsearch, Identity não |
| 5 | **Falha isolada** | Se Commerce cai, Feed deve continuar |

### 5.2 Ordem provável de extração

```
Monólito Modular
    │
    ├── 1º a sair: Content (Post/Feed)
    │   Motivo: escala de leitura, fan-out, cache agressivo
    │   Kafka: PostCreated, PostLiked, CommentAdded
    │
    ├── 2º a sair: Notification
    │   Motivo: naturalmente assíncrono, não precisa de transação com ninguém
    │   Kafka: consome eventos de todos os contextos
    │
    ├── 3º a sair: Community
    │   Motivo: funcionalidades complexas (página customizável, loja, eventos)
    │
    └── Últimos: Identity, Social
        Motivo: são estáveis, pouca mudança, escala moderada
```

### 5.3 Preparação no monólito para extração futura

- Módulos Maven separados (já previsto na estrutura)
- Comunicação via **interfaces**, nunca acesso direto a repos de outro módulo
- **Eventos de domínio** via Spring Events (substituíveis por Kafka sem mudar o publisher)
- **Schema por contexto** no PostgreSQL (ex: `identity.users`, `content.posts`)
- Cada módulo tem suas **migrations Flyway** separadas

---

## 6. Estrutura de Diretórios

### 6.1 Monorepo completo

```
/mnt/BACKUP/BACKUP/Estudos/Aulas/
├── .git/
├── .gitignore
├── .github/
│   └── workflows/
│       ├── ci-backend.yml
│       ├── ci-frontend.yml
│       └── ci-docker.yml
│
├── kora/                            # PROJETO PRINCIPAL
│   ├── backend/                     # Java monólito modular (Maven)
│   │   ├── pom.xml
│   │   ├── kora-api/
│   │   ├── kora-common/
│   │   ├── kora-identity/
│   │   ├── kora-social/
│   │   ├── kora-content/
│   │   ├── kora-community/
│   │   ├── kora-notification/
│   │   ├── Dockerfile
│   │   └── compose.yaml             # Postgres, Redis, Kafka
│   │
│   ├── frontend/                    # Angular workspace
│   │   ├── angular.json
│   │   ├── package.json
│   │   ├── Dockerfile
│   │   └── src/
│   │
│   ├── infra/                       # DevOps
│   │   ├── k8s/                     # Manifests Kubernetes
│   │   ├── docker/                  # Dockerfiles auxiliares
│   │   └── scripts/                 # Scripts de automação
│   │
│   └── docs/                        # Documentação
│       ├── adrs/                    # Architecture Decision Records
│       ├── api/                     # OpenAPI / Swagger
│       ├── diagrams/                # C4, sequência, ER
│       └── patterns/                # Documentação de patterns aplicados
│
├── templates/                       # Templates reutilizáveis
│   ├── kafka-producer-api/          # Protótipo atual (mover para cá)
│   └── kafka-consumer-api/          # Protótipo atual (mover para cá)
│
├── java/                            # Estudos Java (manter separado)
│   ├── docs/
│   └── projects/                    # Protótipos atuais (migrar para templates/)
│
├── angular/                         # Estudos Angular
├── golang/                          # Estudos Go
├── IA/                              # Configurações de IA
│
├── KORA-PLAN.md                     # Este arquivo
└── CLAUDE.md                        # Instruções para Claude Code
```

---

## 7. Git Flow e Estratégia de Commits

### 7.1 Branches

```
main                    ← código estável, sempre funcional
  │
  ├── develop           ← integração de features, base para release
  │     │
  │     ├── feature/identity-user-crud
  │     ├── feature/content-post-feed
  │     ├── feature/kora-ui-shell
  │     └── ...
  │
  ├── release/0.1.0     ← preparação para release (bug fixes, docs)
  │
  └── hotfix/fix-auth   ← correções urgentes em main
```

### 7.2 Convenção de commits (Conventional Commits)

```
<type>(<scope>): <descrição curta>

<corpo opcional — explica o "porquê">

<footer — referências, breaking changes>
```

**Types:**

| Type | Quando usar |
|------|-------------|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `refactor` | Refatoração sem mudar comportamento |
| `arch` | Mudança arquitetural (novo type customizado) |
| `pattern` | Aplicação de design pattern (novo type customizado) |
| `docs` | Documentação |
| `test` | Adição/correção de testes |
| `infra` | Docker, CI/CD, K8s |
| `chore` | Dependências, configs, limpeza |

**Scopes:** `identity`, `social`, `content`, `community`, `notification`, `common`, `ui`, `infra`, `devops`

### 7.3 Exemplos de commits didáticos

```
feat(identity): implement User aggregate with registration use case

Aplica DDD: User como aggregate root, Email como Value Object.
Validações de negócio na entidade, não no controller.

ADR: docs/adrs/002-user-aggregate-design.md
```

```
pattern(content): apply Strategy pattern for feed generation

Feed pode ser gerado por diferentes estratégias:
- ChronologicalFeedStrategy (padrão)
- RelevanceFeedStrategy (futuro)

Facilita extensão sem modificar FeedService (OCP - SOLID).

ADR: docs/adrs/005-feed-strategy-pattern.md
```

```
arch(content): extract Content module from monolith core

Content context agora é módulo Maven independente.
Comunicação com Social via interface (DIP).
Prepara extração futura para microserviço.

ADR: docs/adrs/003-modular-monolith-structure.md
```

---

## 8. Fases de Construção

### Fase 0 — Ambiente e Ferramentas
**Objetivo:** Setup completo para começar a desenvolver.

| # | Tarefa | Detalhes |
|---|--------|----------|
| 0.1 | Migrar Spring Boot para 3.3.x | Trocar parent POM de 4.1.0-SNAPSHOT para 3.3.x estável |
| 0.2 | Mover protótipos Kafka para `templates/` | kafka-producer-api e kafka-consumer-api viram templates |
| 0.3 | Criar estrutura `kora/backend/` | Parent POM + módulos vazios |
| 0.4 | Criar `kora/frontend/` | Angular workspace |
| 0.5 | Criar `compose.yaml` funcional | PostgreSQL + Redis (Kafka vem depois) |
| 0.6 | Configurar CLAUDE.md | Skills, hooks, padrões de código |
| 0.7 | Configurar .cursorrules | Rules para Cursor seguir |
| 0.8 | Criar Git Flow (branches develop, main) | Setup inicial |
| 0.9 | Criar ADR-001: Tech Stack | Documentar decisões da seção 2 |
| 0.10 | Configurar .editorconfig + formatação | Consistência de código |

**Commit esperado:** `chore: setup kora project structure and tooling`

---

### Fase 1 — Identity Context (Backend)
**Objetivo:** Autenticação, registro, perfil básico.

| # | Tarefa | Pattern/Conceito |
|---|--------|-----------------|
| 1.1 | Modelar `User` aggregate (DDD) | Aggregate Root, Value Objects (Email, Username) |
| 1.2 | Implementar `UserRepository` (interface + JPA) | Repository Pattern, DIP |
| 1.3 | Implementar `RegisterUserUseCase` | Application Service, Command pattern (light) |
| 1.4 | Implementar `AuthService` (JWT) | — |
| 1.5 | REST endpoints: POST /auth/register, POST /auth/login, GET /users/{id} | — |
| 1.6 | Validação e exception handling global | @ControllerAdvice, Problem Details (RFC 9457) |
| 1.7 | Testes: unitários (domínio) + integração (API) | Testcontainers para Postgres |
| 1.8 | Flyway migration: schema `identity` | — |
| 1.9 | ADR-002: User Aggregate Design | Documentar decisões |
| 1.10 | Swagger/OpenAPI | springdoc-openapi |

**Design Patterns envolvidos:**
- Repository Pattern
- Factory Method (criação de User com validações)
- Value Object (Email, Username)
- DTO Pattern (separação API ↔ Domain)

**Commits esperados:**
```
feat(identity): create User aggregate with Email value object
feat(identity): implement user registration use case
feat(identity): add JWT authentication
feat(common): add global exception handler with Problem Details
test(identity): add integration tests with Testcontainers
docs(identity): add ADR-002 user aggregate design
```

---

### Fase 2 — Social Context (Backend)
**Objetivo:** Conexões entre usuários (follow/unfollow).

| # | Tarefa | Pattern/Conceito |
|---|--------|-----------------|
| 2.1 | Modelar `Connection` aggregate | — |
| 2.2 | Implementar follow/unfollow | — |
| 2.3 | Domain Event: `UserFollowed`, `UserUnfollowed` | Observer/Event-Driven (in-process) |
| 2.4 | REST endpoints: POST /connections, DELETE /connections/{id}, GET /connections | — |
| 2.5 | Paginação cursor-based | — |
| 2.6 | Testes | — |
| 2.7 | N+1: query otimizada para listar conexões com perfil | JOIN FETCH, projeções |

**Commits esperados:**
```
feat(social): create Connection aggregate and follow/unfollow
feat(social): add domain events for UserFollowed/UserUnfollowed
pattern(social): apply Observer pattern via Spring Events
fix(social): optimize connection listing to avoid N+1
```

---

### Fase 3 — Content Context (Backend)
**Objetivo:** Posts, feed, comentários.

| # | Tarefa | Pattern/Conceito |
|---|--------|-----------------|
| 3.1 | Modelar `Post` aggregate | — |
| 3.2 | Implementar criação de post | — |
| 3.3 | Implementar `FeedService` | Strategy Pattern (feed por cronologia vs relevância) |
| 3.4 | Feed: query otimizada (sem N+1) | Read model / projeção otimizada |
| 3.5 | Domain Event: `PostCreated` | — |
| 3.6 | Paginação cursor-based no feed | — |
| 3.7 | Comentários em posts | — |
| 3.8 | Testes | — |
| 3.9 | ADR: Fan-out on Write vs Fan-out on Read | Decisão de como construir o feed |

**Design Patterns envolvidos:**
- Strategy Pattern (geração de feed)
- CQRS light (separar modelo de escrita e leitura do feed)
- Observer (PostCreated → atualiza feeds)

**N+1 — Como evitar no feed:**
```
-- RUIM: N+1 (1 query para posts + N queries para autor de cada post)
SELECT * FROM posts WHERE ...;
-- Para cada post:
SELECT * FROM users WHERE id = ?;

-- BOM: 1 query com JOIN
SELECT p.*, u.username, u.avatar_url
FROM content.posts p
JOIN identity.users u ON p.author_id = u.id
WHERE p.author_id IN (SELECT target_id FROM social.connections WHERE source_id = ?)
ORDER BY p.created_at DESC
LIMIT 20;
```

---

### Fase 4 — Frontend Core (Angular)
**Objetivo:** Shell da aplicação + telas de Identity, Social, Content.

| # | Tarefa | Pattern/Conceito |
|---|--------|-----------------|
| 4.1 | Setup Angular workspace com arquitetura definida | Feature modules, Core module, Shared module |
| 4.2 | Definir padrões: state management, HTTP interceptors | — |
| 4.3 | Implementar Auth (login, register, guards) | — |
| 4.4 | Shell: layout com sidebar, header, router-outlet | — |
| 4.5 | Tela de perfil | — |
| 4.6 | Feed de posts | Infinite scroll, loading states |
| 4.7 | Criar/editar post | — |
| 4.8 | Lista de conexões | — |
| 4.9 | Responsividade | Mobile-first |
| 4.10 | ADR: Arquitetura Frontend | Documentar decisions |

**Arquitetura Angular sugerida:**
```
src/
├── app/
│   ├── core/               # Singleton services, guards, interceptors
│   │   ├── auth/
│   │   ├── http/
│   │   └── layout/
│   ├── shared/             # Componentes, pipes, directives reutilizáveis
│   │   ├── components/
│   │   ├── pipes/
│   │   └── directives/
│   ├── features/           # Feature modules (lazy loaded)
│   │   ├── identity/
│   │   │   ├── login/
│   │   │   ├── register/
│   │   │   └── profile/
│   │   ├── social/
│   │   │   └── connections/
│   │   ├── content/
│   │   │   ├── feed/
│   │   │   └── post/
│   │   └── community/
│   └── app.routes.ts
```

---

### Fase 5 — DevOps Core
**Objetivo:** CI/CD funcional para backend e frontend.

| # | Tarefa | Detalhes |
|---|--------|----------|
| 5.1 | Dockerfile multi-stage para backend | Build Maven + JRE slim |
| 5.2 | Dockerfile multi-stage para frontend | Build Angular + Nginx |
| 5.3 | Docker Compose completo | Backend + Frontend + Postgres + Redis |
| 5.4 | GitHub Actions: CI backend | mvn test, mvn verify |
| 5.5 | GitHub Actions: CI frontend | ng test, ng build |
| 5.6 | GitHub Actions: build Docker images | Build + tag |
| 5.7 | Kind/Minikube: manifests básicos | Deployment + Service + ConfigMap |
| 5.8 | ADR: DevOps Strategy | Documentar pipeline |

---

### Fase 6 — Refinar IA para padrões do projeto
**Objetivo:** Claude e Cursor entendem o Kora e seus padrões.

| # | Tarefa |
|---|--------|
| 6.1 | Atualizar CLAUDE.md com padrões de backend, frontend, DevOps |
| 6.2 | Criar skills personalizadas (escritas manualmente) |
| 6.3 | Criar hooks para validação de código |
| 6.4 | Configurar .cursorrules com padrões Angular + Java |
| 6.5 | Criar MCP server local se necessário |

---

### Fase 7 — Community + Notification (Backend + Frontend)
**Objetivo:** Comunidades customizáveis, notificações.

| # | Tarefa | Pattern/Conceito |
|---|--------|-----------------|
| 7.1 | Modelar `Community` aggregate | — |
| 7.2 | Page Builder: Composite Pattern para página customizável | Ver [Artigo 005](docs/articles/005-composite-pattern-community-page-builder.md). Árvore de componentes (Container/Leaf), JSONB no Postgres, sanitização via OWASP HTML Sanitizer |
| 7.3 | Eventos dentro de comunidades | — |
| 7.4 | Blog em comunidades | — |
| 7.5 | Sistema de notificações (in-app) | Observer, Template Method |
| 7.6 | WebSocket para notificações real-time | — |
| 7.7 | Frontend: telas de comunidade, notificações | — |
| 7.8 | Testes + ADRs | — |

---

### Fase 8 — DevOps dos novos módulos
**Objetivo:** Estender CI/CD para Community e Notification.

| # | Tarefa |
|---|--------|
| 8.1 | Atualizar Docker Compose |
| 8.2 | Atualizar pipelines CI |
| 8.3 | Health checks e probes em K8s |

---

### Fase 9 — Biblioteca interna (`kora-common`)
**Objetivo:** Consolidar código compartilhado.

| # | Tarefa | Detalhes |
|---|--------|----------|
| 9.1 | Filtros de segurança por perfil | Roles: USER, ADMIN, COMMUNITY_ADMIN |
| 9.2 | DTOs compartilhados | UserSummaryDTO, PageResponse, ErrorResponse |
| 9.3 | Autenticação centralizada | JWT filter, SecurityConfig |
| 9.4 | Auditing (created_at, updated_at, created_by) | JPA Auditing |
| 9.5 | Base exceptions + handlers | BusinessException, NotFoundException, etc. |
| 9.6 | Pagination utilities | CursorPage, OffsetPage |

---

### Fase 10 — Análise e Maturidade
**Objetivo:** Revisar tudo, identificar pontos de melhoria.

| # | Tarefa |
|---|--------|
| 10.1 | Code review completo de todos os módulos |
| 10.2 | Análise de performance (queries lentas, N+1 restantes) |
| 10.3 | Análise de segurança (OWASP checklist) |
| 10.4 | Análise de test coverage |
| 10.5 | Documentação final dos patterns aplicados |
| 10.6 | Retrospectiva: o que funcionou, o que mudaria |
| 10.7 | Definir roadmap do próximo nível de maturidade |

---

### Fases Futuras (Nível 2 de Maturidade)

| Fase | Objetivo |
|------|----------|
| **11. Kafka + Avro** | Migrar de RabbitMQ para Kafka com Schema Registry (Avro). Começar por Content (PostCreated fan-out). Ver [Artigo 012](docs/articles/012-schema-registry-avro-evolucao.md) |
| **12. Event-Driven Architecture** | Idempotent Consumer, retry com backoff, Circuit Breaker, DLQ. Usar templates do protótipo Kafka como referência |
| **13. CQRS + Go Gateway** | Read model separado para Feed (materializado via Kafka). API Gateway em Go na frente do Kora. Ver [Artigo 010](docs/articles/010-go-para-gateways-e-proxies.md) |
| **14. Microserviços** | Extrair Content e Notification como serviços independentes |
| **15. Mobile (React Native) + BFF** | App mobile, BFF pattern para adaptar API. Portfolio multi-plataforma |
| **16. Sharding e Particionamento** | Kafka partitions por userId, DB sharding por comunidade |
| **17. Search** | Elasticsearch/OpenSearch para busca de posts e comunidades |
| **18. GraalVM** | Native images para serviços que precisam de startup rápido |
| **19. Kotlin** | Reescrever um serviço em Kotlin (ex: Notification) |
| **20. Go (advanced)** | Sidecar proxy, Kubernetes Operator, CLI tools |

---

## 9. Design Patterns — Onde e Por Quê

### 9.1 Patterns que vamos aplicar por fase

| Pattern | Tipo | Onde | Fase | Por quê |
|---------|------|------|------|---------|
| **Repository** | Estrutural | Todos os módulos | 1+ | Abstrai acesso a dados, testabilidade |
| **DTO** | Estrutural | API ↔ Domain | 1+ | Desacopla representação externa do domínio |
| **Factory Method** | Criacional | User, Post, Community | 1-3 | Encapsula lógica de criação com validações |
| **Value Object** | DDD | Email, Username, Money | 1+ | Imutabilidade, validação embutida |
| **Strategy** | Comportamental | FeedService | 3 | Diferentes algoritmos de feed, OCP |
| **Observer** | Comportamental | Domain Events | 2+ | Desacopla produtores de consumidores de eventos |
| **Template Method** | Comportamental | Notification | 7 | Fluxo fixo (validar→enviar→logar), partes variáveis (canal) |
| **Builder** | Criacional | DTOs complexos, Queries | 3+ | Construção legível de objetos complexos |
| **Adapter** | Estrutural | Integrações externas | 5+ | Isola dependências externas |
| **Facade** | Estrutural | BFF endpoints | 4 | Simplifica API para o frontend |
| **Chain of Responsibility** | Comportamental | Validação de HTML/SCSS | 7 | Pipeline de sanitização extensível |
| **Circuit Breaker** | Resiliência | Chamadas externas | 11+ | Resilience4j, previne cascading failure |
| **Idempotent Consumer** | Mensageria | Kafka consumers | 11+ | At-least-once delivery sem efeitos duplicados |

### 9.2 Como documentar cada pattern aplicado

Para cada pattern, criar um arquivo em `docs/patterns/`:

```markdown
# Pattern: Strategy (Feed Generation)

## Contexto
O feed pode ser gerado por diferentes algoritmos...

## Problema
FeedService tinha um if/else para cada tipo de feed, violando OCP...

## Solução
Interface FeedStrategy + implementações concretas...

## Código
[link para arquivo]

## Trade-offs
- (+) Fácil adicionar novo algoritmo
- (+) Cada estratégia testável isoladamente
- (-) Mais classes/interfaces
- (-) Overhead mínimo de indireção

## Referências
- GoF: Strategy Pattern
- Effective Java, Item X
```

---

## 10. Anti-Patterns — O Que Evitar

| Anti-Pattern | O que é | Como evitar | Quando verificar |
|-------------|---------|-------------|-----------------|
| **N+1 Queries** | 1 query + N queries em loop | JOIN FETCH, projeções, batch fetch | Toda query de listagem |
| **Anemic Domain Model** | Entidades sem lógica, tudo no Service | Lógica de negócio na entidade | Fase 1+ |
| **God Class** | Service/Controller com 500+ linhas | SRP, extrair use cases | Code review |
| **Shared Database (entre contextos)** | Módulos acessando tabelas de outros | Schema por contexto, interfaces | Fase 1+ |
| **Circular Dependency** | A depende de B que depende de A | Eventos, DIP, extrair interface para common | Toda nova dependência |
| **Chatty Communication** | 10 chamadas REST para montar uma tela | Endpoints agregados, BFF | Frontend integration |
| **Distributed Monolith** | Microserviços que precisam deploy juntos | Começar monólito, extrair só quando necessário | Fase 14 |
| **Premature Optimization** | Cache/sharding antes de ter problema | Medir primeiro, otimizar depois | Sempre |
| **Big Ball of Mud** | Sem fronteiras entre módulos | Módulos Maven, interfaces, regras de dependência | Toda nova feature |
| **Smart Endpoints, Dumb Pipes** | Lógica de negócio no controller | Controller fino → Application Service → Domain | Fase 1+ |
| **Temporal Coupling** | Métodos que só funcionam se chamados em ordem | Fazer operações auto-contidas | Code review |
| **Feature Envy** | Serviço A manipulando dados de B diretamente | Mover lógica para onde os dados estão | Code review |

---

## 11. Observabilidade e Métricas

### 11.1 Stack

```
Aplicação (Micrometer) → Prometheus → Grafana
     │
     └── Logs (SLF4J/Logback) → stdout → (futuro: Loki)
```

### 11.2 Métricas essenciais (RED)

| Métrica | O que mede | Alerta quando |
|---------|-----------|---------------|
| **Rate** | req/s por endpoint | Cai abaixo do baseline |
| **Errors** | % de 4xx/5xx | > 1% de 5xx |
| **Duration** | Latência p50/p95/p99 | p99 > 500ms |

### 11.3 Métricas de negócio

| Métrica | Descrição |
|---------|-----------|
| `kora.users.registered` | Total de registros |
| `kora.posts.created` | Posts criados/min |
| `kora.feed.latency` | Tempo para gerar feed |
| `kora.kafka.consumer.lag` | Lag do consumer (fase Kafka) |

### 11.4 Logs estruturados

```java
log.info("Post created",
    kv("postId", post.getId()),
    kv("authorId", post.getAuthorId()),
    kv("traceId", MDC.get("traceId")));
```

---

## 12. Segurança

### 12.1 Checklist por fase

| Fase | Item | Detalhe |
|------|------|---------|
| 1 | Senha com BCrypt | Nunca MD5/SHA1 |
| 1 | JWT com expiração curta | Access 15min, Refresh 7d |
| 1 | Rate limiting no login | Prevenir brute force |
| 4 | CORS configurado | Só o domínio do frontend |
| 7 | Sanitização de HTML | Whitelist de tags, nunca regex para validar HTML |
| 7 | CSP (Content-Security-Policy) | Prevenir XSS na página customizável |
| 9 | RBAC | Roles: USER, ADMIN, COMMUNITY_OWNER, COMMUNITY_ADMIN |
| 10 | OWASP Top 10 review | Checklist completo |

### 12.2 Comunidade customizável — Sanitização

**NUNCA usar regex para validar HTML.** Usar biblioteca de sanitização:

- **Backend:** OWASP Java HTML Sanitizer ou Jsoup com whitelist
- **Frontend:** DOMPurify

```java
// RUIM: regex para validar HTML
if (html.matches("<script>")) { ... } // Bypassável de mil formas

// BOM: whitelist com biblioteca dedicada
PolicyFactory policy = new HtmlPolicyBuilder()
    .allowElements("div", "span", "p", "h1", "h2", "h3", "img", "a")
    .allowAttributes("class").matching(TAILWIND_PATTERN).globally()
    .allowAttributes("href").onElements("a")
    .allowAttributes("src").onElements("img")
    .toFactory();
String safeHtml = policy.sanitize(userInput);
```

---

## 13. DevOps e Plataforma

### 13.1 Docker Compose (desenvolvimento local)

```yaml
# kora/backend/compose.yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: kora
      POSTGRES_USER: kora
      POSTGRES_PASSWORD: kora_dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  # Kafka — adicionado na Fase 11
  # zookeeper:
  #   image: confluentinc/cp-zookeeper:7.6.0
  # kafka:
  #   image: confluentinc/cp-kafka:7.6.0

volumes:
  postgres_data:
```

### 13.2 CI/CD (GitHub Actions)

```yaml
# .github/workflows/ci-backend.yml
name: CI Backend
on:
  push:
    paths: ['kora/backend/**']
  pull_request:
    paths: ['kora/backend/**']

jobs:
  build:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: kora_test
          POSTGRES_USER: kora
          POSTGRES_PASSWORD: test
        ports: ['5432:5432']
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - run: mvn -f kora/backend/pom.xml verify
```

### 13.3 Kubernetes local (Kind)

```yaml
# kora/infra/k8s/kora-backend.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kora-backend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kora-backend
  template:
    metadata:
      labels:
        app: kora-backend
    spec:
      containers:
        - name: kora-backend
          image: kora-backend:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_DATASOURCE_URL
              value: jdbc:postgresql://postgres:5432/kora
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 10
```

---

## 14. Configuração de IA (Claude + Cursor)

### 14.1 CLAUDE.md (raiz do projeto)

```markdown
# Kora — Instruções para Claude Code

## Projeto
Rede social "Kora" — monólito modular em Java 21 + Spring Boot 3.3.x + Angular.

## Regras
- NUNCA executar git push
- NUNCA baixar skills externas
- Commits só quando o usuário pedir explicitamente
- Responder em português (BR)
- Commits em inglês (Conventional Commits)

## Stack
- Backend: Java 21, Spring Boot 3.3.x, Maven, PostgreSQL, Redis
- Frontend: Angular 18+, TypeScript
- Infra: Docker, GitHub Actions, Kind/Minikube
- Testes: JUnit 5, Testcontainers, Mockito

## Padrões de código
- DDD: Aggregate Root, Value Objects, Domain Events
- Hexagonal-inspired: api/ (adapters) → application/ (use cases) → domain/ → infrastructure/
- Controller fino: só recebe request, chama use case, retorna response
- Testes: unitários para domain, integração para API (Testcontainers)

## Estrutura de pacotes (cada módulo)
- api/ — Controllers, request/response DTOs
- application/ — Use cases, application services
- domain/ — Entities, value objects, domain services, repository interfaces
- infrastructure/ — JPA repositories, external service adapters
- event/ — Domain events

## Anti-patterns proibidos
- N+1 queries
- Lógica de negócio em controllers
- Acesso direto a repositório de outro módulo
- Circular dependencies entre módulos
```

### 14.2 Skills personalizadas (a serem escritas manualmente)

| Skill | Propósito |
|-------|-----------|
| `java` | Padrões Java do projeto (já existe, refinar) |
| `angular` | Padrões Angular do projeto (já existe, refinar) |
| `new-module` | Criar novo módulo Maven com estrutura padrão |
| `new-endpoint` | Criar endpoint REST seguindo padrão do projeto |
| `new-entity` | Criar aggregate com Value Objects |
| `adr` | Criar novo Architecture Decision Record |
| `review` | Revisar código por patterns, anti-patterns, N+1 |

### 14.3 Hooks (a serem configurados)

| Hook | Trigger | Ação |
|------|---------|------|
| `pre-commit` | Antes de cada commit | Rodar `mvn verify -pl <módulo>` |
| `post-edit` | Após editar arquivo Java | Verificar imports organizados |

### 14.4 MCP (Model Context Protocol)

Para considerar depois:
- MCP server local para acessar documentação do projeto
- MCP para consultar ADRs e patterns documentados
- MCP para consultar métricas (Prometheus/Grafana)

---

## 15. ADRs — Registro de Decisões Arquiteturais

### Template

```markdown
# ADR-XXX: Título da Decisão

## Status
Proposta | Aceita | Substituída por ADR-YYY

## Contexto
O que motivou essa decisão? Qual problema estamos resolvendo?

## Decisão
O que decidimos fazer?

## Alternativas consideradas
1. Alternativa A — prós e contras
2. Alternativa B — prós e contras

## Consequências
- Positivas: ...
- Negativas: ...
- Riscos: ...

## Referências
- Links, livros, artigos
```

### ADRs existentes (em `docs/adrs/`)

| # | Título | Status |
|---|--------|--------|
| 001 | [Tech Stack Principal](docs/adrs/001-tech-stack-principal.md) | Aceita |
| 002 | [Monólito Modular Primeiro](docs/adrs/002-monolito-modular-primeiro.md) | Aceita |
| 003 | [Evolução da Mensageria: Spring Events → RabbitMQ → Kafka](docs/adrs/003-mensageria-evolucao-events-rabbitmq-kafka.md) | Aceita |
| 004 | [Hexagonal Simplificada por Módulo](docs/adrs/004-hexagonal-simplificada-por-modulo.md) | Aceita |
| 005 | [Financeiro Pessoal e Comercial Separados](docs/adrs/005-financeiro-modulos-separados.md) | Aceita |

### ADRs planejadas

| # | Título | Fase |
|---|--------|------|
| 006 | User Aggregate Design (DDD) | 1 |
| 007 | JWT Authentication Strategy | 1 |
| 008 | Feed Generation: Fan-out on Write vs Read | 3 |
| 009 | Angular Architecture (Feature Modules) | 4 |
| 010 | Community Page Sanitization Strategy | 7 |
| 011 | RabbitMQ Integration | 7 |
| 012 | Event-Driven Migration (RabbitMQ → Kafka) | 11 |
| 013 | CQRS for Feed Read Model | 13 |
| 014 | Content Service Extraction | 14 |
| 015 | Go API Gateway | 13 |
| 016 | Mobile (React Native) + BFF | 15 |

---

## 16. Artigos e Guias de Estudo (em `docs/`)

### Artigos (`docs/articles/`)

| # | Título | Tema |
|---|--------|------|
| 001 | [Por que Java 21 + Spring Boot 3.3.x](docs/articles/001-por-que-java-21-spring-boot-3.md) | Tech stack |
| 002 | [Monólito Modular vs Microserviços](docs/articles/002-monolito-modular-vs-microservicos.md) | Arquitetura |
| 003 | [De HTTP para RabbitMQ para Kafka](docs/articles/003-rabbitmq-vs-kafka-evolucao-mensageria.md) | Mensageria |
| 004 | [Financeiro: Separar ou Unir?](docs/articles/004-financeiro-separar-ou-unir.md) | DDD / Bounded Contexts |
| 005 | [Composite Pattern para Page Builder](docs/articles/005-composite-pattern-community-page-builder.md) | Design Pattern |
| 006 | [PostgreSQL vs MongoDB](docs/articles/006-nosql-mongodb-quando-usar.md) | Persistência |
| 007 | [Micro-Frontend e Mobile](docs/articles/007-micro-frontend-e-mobile.md) | Frontend / Mobile |
| 008 | [Posts/Feed: Fan-out e Separação](docs/articles/008-posts-como-servico-separado-fanout.md) | Arquitetura |
| 009 | [Configuração de IA (Claude + Cursor)](docs/articles/009-configuracao-ia-claude-cursor.md) | Tooling / IA |
| 010 | [Go para Gateways e Proxies](docs/articles/010-go-para-gateways-e-proxies.md) | Estudo / Go |
| 011 | [Padrões de Implementação do Kora](docs/articles/011-padroes-implementacao-kora.md) | Arquitetura de camadas, checklist de review |
| 012 | [Schema Registry e Avro: Evolução](docs/articles/012-schema-registry-avro-evolucao.md) | Kafka / Serialização |
| 013 | [Schema Registry e Avro (resumo)](docs/articles/013-schema-registry-avro-kafka-cursor.md) | Kafka (Cursor) |
| 014 | [Design Patterns Roadmap](docs/articles/014-design-patterns-roadmap.md) | Patterns |

### Guias de Estudo (`docs/study-guides/`)

| Guia | Conteúdo |
|------|----------|
| [Go para Networking, Gateways e Proxies](docs/study-guides/go-networking-gateways-proxies.md) | Roadmap de estudo Go: fundamentos → networking → API gateway → ferramentas de plataforma |

---

## 17. Glossário

| Termo | Definição |
|-------|-----------|
| **Aggregate** | Cluster de entidades tratado como unidade para mudanças de dados |
| **Bounded Context** | Limite lógico onde um modelo de domínio se aplica |
| **Value Object** | Objeto imutável definido por seus atributos, sem identidade própria |
| **Domain Event** | Registro de algo que aconteceu no domínio |
| **CQRS** | Command Query Responsibility Segregation — modelos separados para escrita e leitura |
| **Fan-out on Write** | Ao criar post, materializa no feed de cada follower |
| **Fan-out on Read** | Ao abrir feed, consulta posts dos seguidos em tempo real |
| **DLQ** | Dead Letter Queue — fila para mensagens que falharam no processamento |
| **Idempotent Consumer** | Consumer que produz o mesmo resultado mesmo se processar a mensagem N vezes |
| **Circuit Breaker** | Pattern que impede chamadas a serviços instáveis |
| **BFF** | Backend For Frontend — backend especializado para um frontend específico |
| **ADR** | Architecture Decision Record — documentação de decisão arquitetural |
| **RED** | Rate, Errors, Duration — métricas essenciais de API |
| **N+1** | Anti-pattern onde 1 query dispara N queries adicionais |

---

## Acompanhamento

- [ ] Fase 0 — Ambiente e Ferramentas
- [ ] Fase 1 — Identity Context
- [ ] Fase 2 — Social Context
- [ ] Fase 3 — Content Context
- [ ] Fase 4 — Frontend Core
- [ ] Fase 5 — DevOps Core
- [ ] Fase 6 — Refinar IA
- [ ] Fase 7 — Community + Notification
- [ ] Fase 8 — DevOps dos novos módulos
- [ ] Fase 9 — Biblioteca interna
- [ ] Fase 10 — Análise e Maturidade

---

*Última atualização: 2026-03-22*
*Este documento é vivo — será refinado conforme decisões forem tomadas.*