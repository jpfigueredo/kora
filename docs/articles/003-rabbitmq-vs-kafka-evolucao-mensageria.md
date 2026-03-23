# De HTTP para RabbitMQ para Kafka: A Evolução da Comunicação entre Serviços

> Artigo de estudo — Kora Project
> Data: 2026-03-21

---

## Por que este artigo existe

No Kora, não vamos pular direto de chamadas HTTP para Kafka. Vamos **sentir cada dor** na evolução:

```
HTTP síncrono  →  Message Queue (RabbitMQ)  →  Event Streaming (Kafka)
```

Cada passo resolve um problema do anterior — e cria novos. Entender esse caminho é fundamental para tomar decisões arquiteturais informadas.

---

## Fase 1: Tudo Síncrono (HTTP)

### Como funciona

```
Client → POST /orders → OrderService → PaymentService (HTTP) → NotificationService (HTTP)
                              │
                              ▼
                          responde 201
```

O `OrderService` chama `PaymentService` via HTTP, espera a resposta, depois chama `NotificationService`, espera a resposta, e só então responde ao cliente.

### Quando é suficiente

- Poucos serviços (ou monólito modular com chamadas internas)
- Volume baixo/médio (dezenas a milhares de req/s)
- Todas as operações **precisam** acontecer antes de responder ao usuário

### Onde dói

1. **Acoplamento temporal**: se `PaymentService` está lento ou fora do ar, `OrderService` trava.
2. **Cascading failure**: um serviço lento causa timeout em todos que dependem dele.
3. **Latência acumulada**: 3 chamadas HTTP de 100ms cada = 300ms no mínimo.
4. **Retry difícil**: se a notificação falhou mas o pagamento foi feito, o que fazer?

### Exemplo real no Kora

No monólito modular (Fase 1-3), tudo é in-process:

```java
@Transactional
public PostResponse createPost(CreatePostCommand cmd) {
    Post post = postRepository.save(Post.create(cmd));      // ~5ms
    feedService.updateFeeds(post);                           // ~20ms
    notificationService.notifyFollowers(post.getAuthorId()); // ~50ms
    return PostResponse.from(post);                          // total: ~75ms
}
```

Funciona. Mas quando `notifyFollowers` precisar enviar 10.000 notificações? O usuário espera 75ms... ou 30 segundos?

---

## Fase 2: Message Queue (RabbitMQ)

### O que é uma Message Queue?

Um **intermediário** que recebe mensagens de produtores, armazena em filas, e entrega a consumidores.

```
Producer  →  Exchange  →  Queue  →  Consumer
```

### Modelo mental: caixa de correio

- Você (producer) deixa a carta no correio (exchange).
- O correio coloca na caixa do destinatário (queue).
- O destinatário (consumer) pega quando puder.
- **Você não espera o destinatário ler a carta.**

### Por que RabbitMQ especificamente?

| Aspecto | RabbitMQ | Kafka |
|---------|----------|-------|
| **Modelo** | Message Queue (push) | Event Log (pull) |
| **Mensagem consumida** | Removida da fila | Permanece no log |
| **Roteamento** | Exchanges + bindings (flexível) | Topics + partitions |
| **Complexidade** | Menor | Maior |
| **Caso de uso ideal** | Task distribution, RPC async | Event streaming, replay |
| **Quando usar** | "Preciso que alguém faça X" | "X aconteceu, quem quiser que escute" |

### Como resolve o problema do HTTP

```java
// ANTES (síncrono — bloqueia)
public PostResponse createPost(CreatePostCommand cmd) {
    Post post = postRepository.save(Post.create(cmd));
    feedService.updateFeeds(post);                         // bloqueia
    notificationService.notifyFollowers(post.getAuthorId()); // bloqueia
    return PostResponse.from(post);
}

// DEPOIS (com MQ — desacopla)
public PostResponse createPost(CreatePostCommand cmd) {
    Post post = postRepository.save(Post.create(cmd));
    messageQueue.send("post.created", new PostCreatedEvent(post)); // ~1ms, fire-and-forget
    return PostResponse.from(post);  // responde imediatamente
}

// Em outro processo/thread:
@RabbitListener(queues = "feed-updates")
public void onPostCreated(PostCreatedEvent event) {
    feedService.updateFeeds(event);
}

@RabbitListener(queues = "notifications")
public void onPostCreated(PostCreatedEvent event) {
    notificationService.notifyFollowers(event.getAuthorId());
}
```

### Ganhos

1. **Desacoplamento temporal**: o producer não espera o consumer.
2. **Resiliência**: se o consumer cai, as mensagens ficam na fila até ele voltar.
3. **Escala**: pode ter N consumers para a mesma fila (competing consumers).
4. **Latência para o usuário**: só o `save` + `send` (~10ms).

### Onde RabbitMQ dói (e Kafka resolve)

1. **Mensagem consumida é removida**: se você precisa re-processar eventos (replay), perdeu.
2. **Sem ordenação garantida** em filas com múltiplos consumers (tem workaround, mas é limitado).
3. **Sem histórico**: novos consumers não vêem mensagens passadas.
4. **Throughput**: RabbitMQ aguenta milhares/msg/s. Kafka aguenta **milhões/msg/s**.
5. **Sem consumer groups nativos**: se dois serviços precisam do mesmo evento, precisa de exchanges + múltiplas filas.

---

## Fase 3: Event Streaming (Kafka)

### O que é Kafka?

Um **log distribuído** de eventos. Diferente de uma fila, mensagens **não são removidas** após consumo — ficam no log por um período configurável (7 dias, 30 dias, infinito).

```
Producer → Topic (partitions) ← Consumer Group A
                               ← Consumer Group B
                               ← Consumer Group C
```

### Modelo mental: jornal

- Kafka é um jornal. Cada edição (mensagem) fica na biblioteca (log).
- Você (consumer) lê na página (offset) onde parou.
- Outro leitor pode ler o mesmo jornal, na velocidade dele.
- Pode voltar e re-ler edições antigas (replay).

### Conceitos fundamentais

| Conceito | O que é |
|----------|---------|
| **Topic** | Canal lógico de eventos (ex: `post-events`) |
| **Partition** | Subdivisão do topic para paralelismo |
| **Offset** | Posição do consumer no log |
| **Consumer Group** | Conjunto de consumers que dividem as partitions entre si |
| **Producer** | Quem publica eventos |
| **Broker** | Servidor Kafka que armazena o log |

### Quando usar Kafka em vez de RabbitMQ?

| Critério | RabbitMQ | Kafka |
|----------|----------|-------|
| Múltiplos consumers para o mesmo evento | Possível (com fanout exchange) | Nativo (consumer groups) |
| Replay de eventos | Não | Sim |
| Event sourcing | Não é o foco | Natural |
| Throughput muito alto | Milhares/s | Milhões/s |
| Ordenação por chave | Difícil | Garantida por partition |
| Complexidade operacional | Moderada | Alta |

### No Kora: quando Kafka entra?

```
Fase 1-3:  Spring Events (in-process, monólito)
Fase 7:    RabbitMQ (quando precisar desacoplar Notification)
Fase 11:   Kafka (quando precisar de fan-out de posts, replay, CQRS)
```

---

## Trade-offs Resumidos

```
HTTP Síncrono
  (+) Simples, transacional, fácil de debugar
  (-) Acoplado, lento, cascading failure
     │
     ▼
RabbitMQ (Message Queue)
  (+) Desacoplado, resiliente, simples
  (-) Sem replay, sem histórico, throughput limitado
     │
     ▼
Kafka (Event Streaming)
  (+) Replay, múltiplos consumers, throughput altíssimo, CQRS
  (-) Complexo de operar, eventual consistency, curva de aprendizado
```

## O que estudar

Para entender profundamente:
1. **RabbitMQ**: exchanges (direct, fanout, topic), bindings, acks, dead-letter queues
2. **Kafka**: partitions, offsets, consumer groups, rebalancing, exactly-once semantics
3. **Patterns**: Competing Consumers, Pub/Sub, Event Sourcing, CQRS
4. **Livros**:
   - *Designing Data-Intensive Applications* (Martin Kleppmann) — Cap. 11: Stream Processing
   - *Building Microservices* (Sam Newman) — Cap. 4: Integration
   - *Enterprise Integration Patterns* (Hohpe, Woolf) — referência para patterns de mensageria

---

## Resumo para o Kora

| Fase | Comunicação | Por quê |
|------|------------|---------|
| 1-6 | Spring Events (in-process) | Monólito modular, sem necessidade de MQ |
| 7 | RabbitMQ | Notification desacoplado, aprender MQ simples |
| 11+ | Kafka | Fan-out de posts, CQRS, event streaming |

A ideia é **sentir a dor de cada fase** para entender por que a próxima solução existe.
