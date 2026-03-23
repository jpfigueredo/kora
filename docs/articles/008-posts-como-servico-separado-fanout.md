# Posts/Feed: Nascer Separado por causa do Fan-out?

> Artigo de decisão — Kora Project
> Data: 2026-03-21

---

## A Ideia

> "Acredito que a questão dos posts deve trabalhar separadamente, por conta do fanout."

A intuição está correta: o domínio de **Content (Posts + Feed)** é o que mais escala em uma rede social. Mas "separado" pode significar coisas diferentes:

## Três níveis de "separado"

### Nível 1: Módulo Maven separado dentro do monólito (RECOMENDADO AGORA)

```
kora/backend/
├── kora-api/          # assembler
├── kora-identity/     # User, Auth
├── kora-social/       # Connections
├── kora-content/      # Posts, Feed, Comments  ← módulo separado
└── kora-common/       # shared
```

- **1 JAR, 1 deploy**, mas com fronteiras claras
- `kora-content` comunica com `kora-social` via **interfaces** (nunca acesso direto)
- Eventos de domínio via Spring Events
- Schema separado no PostgreSQL (`content.*`)

**Ganho:** preparação para extração futura sem custo operacional agora.

### Nível 2: Microserviço separado com RabbitMQ (FASE 11)

```
kora-content-service (port 8082)
    ↕ RabbitMQ ↕
kora-api (port 8080)
```

- Deploy independente
- Banco próprio
- Comunicação assíncrona via MQ

**Quando:** quando o monólito estiver maduro e o fan-out de feed justificar.

### Nível 3: Microserviço com Kafka + CQRS (FASE 13)

```
kora-content-service
    → PostCreated event → Kafka
        → Feed Materializer (consumer)
            → Redis/MongoDB (feed materializado)
                ← GET /feed (leitura rápida)
```

**Quando:** quando precisar de replay, múltiplos consumers, e escala de leitura do feed.

---

## Fan-out: O que é e por que importa

Quando um usuário com 10.000 seguidores publica um post, esse post precisa aparecer no feed de 10.000 pessoas. Isso é **fan-out**.

### Fan-out on Write (push model)

Ao criar o post, **materializa** no feed de cada seguidor:

```
UserA publica post
    → Para cada seguidor de UserA:
        → INSERT INTO user_feed (user_id, post_id) VALUES (follower_id, post_id)
```

**Prós:** leitura do feed é ultra-rápida (só lê `user_feed WHERE user_id = ?`)
**Contras:** escrita lenta quando o autor tem muitos seguidores (celebrity problem)

### Fan-out on Read (pull model)

Ao abrir o feed, **consulta** posts dos seguidos em tempo real:

```
UserB abre feed
    → SELECT posts FROM posts WHERE author_id IN (SELECT followed_id FROM connections WHERE follower_id = UserB)
    → ORDER BY created_at DESC LIMIT 20
```

**Prós:** escrita rápida (só insere o post uma vez)
**Contras:** leitura lenta se o usuário segue muitas pessoas (muitos JOINs)

### Abordagem híbrida (o que Twitter/X faz)

- **Usuários normais** (< 5.000 seguidores): fan-out on write
- **Celebridades** (> 5.000 seguidores): fan-out on read + merge no momento da leitura

```
UserB abre feed:
1. Lê user_feed (posts de usuários normais, já materializados)
2. Consulta posts recentes das celebridades que segue
3. Merge + sort por timestamp
4. Retorna top 20
```

---

## Por que NÃO começar como microserviço separado

| Argumento | Contra-argumento |
|-----------|-----------------|
| "Vai precisar escalar" | Sim, mas um monólito bem modularizado com 1 réplica aguenta centenas de milhares de requests. Você está estudando, não lançando para 1M de usuários. |
| "Fan-out é pesado" | Sem fan-out por enquanto — comece com fan-out on read (query SQL). Só mude quando sentir a dor. |
| "É mais realista" | O mais realista é: (1) monólito funcional, (2) sentir dor, (3) extrair. É assim que empresas reais fazem. |
| "Quero aprender microserviços" | Vai aprender — na Fase 11-14, quando extrair Content. E vai aprender **por quê**, não só **como**. |

## O Plano Concreto

| Fase | Content Architecture | Feed Strategy |
|------|---------------------|---------------|
| 3 | Módulo Maven (`kora-content`) | Fan-out on read (SQL JOIN) |
| 7 | Mesmo módulo + Spring Events para notificações | Idem |
| 11 | RabbitMQ para PostCreated → Notification | Idem |
| 13 | Kafka + CQRS: read model materializado | Fan-out on write (via Kafka consumer) |
| 14 | Extrai `kora-content-service` como microserviço | Híbrido (write para normais, read para celebridades) |

Cada fase é um **commit didático** que mostra a evolução.

---

## Referências

- Alex Xu — *System Design Interview*: Cap. 11 (Design a News Feed System)
- Martin Kleppmann — *DDIA*: Cap. 11 (Stream Processing)
- Twitter Engineering Blog — fanout architecture
