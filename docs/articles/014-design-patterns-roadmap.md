# Design Patterns: Roadmap para o Kora

> Artigo de estudo — Kora Project
> Data: 2026-03-22

---

## O Problema

> "Não consigo pensar em como aplicar design patterns nas tarefas."

Normal. Patterns não são algo que você "escolhe antes de codar". Você **reconhece a necessidade** durante a implementação. Este guia mapeia **onde cada pattern aparece naturalmente** no Kora.

---

## Os 3 Tipos (GoF)

### Creational (Criação de objetos)

| Pattern | O que resolve | Onde no Kora |
|---------|--------------|-------------|
| **Factory Method** | Criar objetos sem expor a lógica de criação | `EventFactory.create("PostCreated", payload)` — diferentes tipos de evento |
| **Abstract Factory** | Família de objetos relacionados | `NotificationFactory` → cria `EmailNotification`, `PushNotification`, `InAppNotification` |
| **Builder** | Construir objetos complexos passo a passo | `Post.builder().content("...").author(user).tags(list).build()` |
| **Singleton** | Garantir 1 instância global | Spring `@Bean` já faz isso (scope singleton). Não precisa implementar manualmente |
| **Prototype** | Clonar objetos | Template de página de comunidade: `defaultTemplate.clone()` e customizar |

### Structural (Composição de objetos)

| Pattern | O que resolve | Onde no Kora |
|---------|--------------|-------------|
| **Composite** | Hierarquia parte-todo | **Page Builder de comunidades** — árvore de componentes (já documentado no artigo 005) |
| **Adapter** | Compatibilizar interfaces incompatíveis | `PaymentGatewayAdapter` — adapta Stripe/PagSeguro para interface interna `PaymentProcessor` |
| **Decorator** | Adicionar comportamento sem alterar classe | `LoggingRepository`, `CachingRepository` que wrappam o repository real |
| **Facade** | Interface simplificada para subsistema complexo | `FeedFacade` — esconde a complexidade de montar feed (posts + connections + sorting) |
| **Proxy** | Controlar acesso a um objeto | `CachedFeedProxy` — verifica cache antes de ir ao banco |

### Behavioral (Comunicação entre objetos)

| Pattern | O que resolve | Onde no Kora |
|---------|--------------|-------------|
| **Observer** | Notificar múltiplos interessados sobre um evento | Spring Events: `PostCreatedEvent` → `FeedListener`, `NotificationListener`, `AnalyticsListener` |
| **Strategy** | Algoritmos intercambiáveis | `FeedAlgorithm`: `ChronologicalFeed`, `RankedFeed`, `TrendingFeed` |
| **Template Method** | Esqueleto de algoritmo com passos customizáveis | `BaseEventConsumer.process()` → `validate()` → `handle()` → `acknowledge()` |
| **Command** | Encapsular request como objeto | `CreatePostCommand`, `DeletePostCommand` — CQRS commands |
| **Chain of Responsibility** | Pipeline de handlers | Middleware chain: `AuthFilter` → `RateLimitFilter` → `LoggingFilter` → `Controller` |
| **State** | Comportamento baseado em estado | `CircuitBreaker`: Closed → Open → HalfOpen (estados com transições) |
| **Mediator** | Centralizar comunicação entre objetos | `EventBus` — publica eventos e roteia para listeners corretos |

---

## Quando você vai encontrar cada um (por fase)

### Fase 3: Monólito Modular (backend)

Patterns que aparecem naturalmente:
- **Builder** — para DTOs e entidades com muitos campos
- **Observer** — Spring Events para comunicação entre módulos
- **Facade** — simplificar acesso a módulos (ex: `SocialFacade.getFeed(userId)`)
- **Repository** — abstração de acesso a dados (não é GoF, mas é fundamental)

### Fase 4: Frontend Angular

- **Observer** — RxJS Observables (o Angular inteiro é baseado nisso)
- **Strategy** — componentes que aceitam diferentes estratégias de renderização
- **Composite** — component tree do Angular é Composite por natureza

### Fase 5-6: DevOps

- **Builder** — Docker multi-stage build (builder pattern aplicado a infra)

### Fase 7: Evolução (Notification, eventos, RabbitMQ)

- **Strategy** — `NotificationStrategy`: Email, Push, InApp
- **Template Method** — `BaseEventHandler` com steps comuns
- **Factory Method** — criar o handler correto baseado no tipo de evento
- **State** — Circuit Breaker para chamadas externas

### Fase 11-14: Kafka, CQRS, Microserviços

- **Command** — CQRS commands vs queries
- **Chain of Responsibility** — pipeline de processamento de eventos Kafka
- **Adapter** — adaptar diferentes serviços externos (payment gateways, notification providers)
- **Mediator** — orquestração de Sagas

---

## Como aprender patterns (método prático)

### ❌ NÃO faça isso:
- Ler o livro GoF do início ao fim tentando memorizar 23 patterns
- Tentar encaixar patterns em código que não precisa deles
- Criar abstrações "porque é pattern"

### ✅ Faça isso:
1. **Implemente o código da forma mais simples possível**
2. **Sinta a dor**: duplicação, switch/case crescendo, acoplamento forte
3. **Pergunte**: "que pattern resolve essa dor?"
4. **Refatore** aplicando o pattern
5. **Documente** em um commit: "refactor: apply Strategy pattern to FeedAlgorithm"

### Exemplo concreto (vai acontecer no Kora):

```java
// FASE 3 — Implementação simples
public class FeedService {
    public List<PostDTO> getFeed(UUID userId) {
        List<UUID> connectionIds = socialService.getConnectionIds(userId);
        return postRepository.findByAuthorIdIn(connectionIds, PageRequest.of(0, 20));
    }
}

// FASE 7 — Precisa de diferentes algoritmos de feed
// DOR: if/else ou switch crescendo
public List<PostDTO> getFeed(UUID userId, FeedType type) {
    if (type == CHRONOLOGICAL) { ... }
    else if (type == RANKED) { ... }     // vai adicionando...
    else if (type == TRENDING) { ... }
}

// REFACTOR: Strategy Pattern
public interface FeedAlgorithm {
    List<PostDTO> generate(UUID userId, Pageable page);
}

public class ChronologicalFeed implements FeedAlgorithm { ... }
public class RankedFeed implements FeedAlgorithm { ... }
public class TrendingFeed implements FeedAlgorithm { ... }

public class FeedService {
    private final Map<FeedType, FeedAlgorithm> algorithms;

    public List<PostDTO> getFeed(UUID userId, FeedType type, Pageable page) {
        return algorithms.get(type).generate(userId, page);
    }
}
```

O commit que faz essa mudança **é didático por si só**.

---

## Livros para patterns (que você já tem)

1. **"Code Complete" (McConnell)** — Cap. 5-7: design de classes e rotinas
2. **"The Pragmatic Programmer" (Hunt & Thomas)** — princípios gerais que sustentam patterns
3. **"Effective Java" (Bloch)** — implementações específicas de patterns em Java (Builder, Factory, etc.)
4. **"Domain-Driven Design" (Evans)** — patterns táticos (Entity, Value Object, Repository, Service)

## Livros que valeria a pena adquirir

- **"Head First Design Patterns"** (Freeman & Robson) — explicações visuais, ótimo para começar
- **"Design Patterns" (GoF)** — referência canônica, mas denso. Usar como consulta, não leitura linear

---

## Resumo

Patterns não são um checklist. São **vocabulário compartilhado** para soluções que emergem da prática. No Kora, eu vou apontar quando um pattern faz sentido durante a implementação — e o commit será o registro.
