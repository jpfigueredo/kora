# Monólito Modular vs Microserviços: Por Onde Começar?

> Artigo de decisão — Kora Project
> Data: 2026-03-21

---

## O Dilema

"Vou construir uma rede social. Devo começar com microserviços, já que sei que vou precisar escalar?"

**Resposta curta: não.** E este artigo explica o porquê com profundidade.

## O que é um Monólito Modular?

É um **único artefato de deploy** (1 JAR, 1 container) com **fronteiras internas bem definidas** entre módulos. Cada módulo:

- Tem seu próprio pacote/namespace
- Expõe **interfaces** (não implementações) para outros módulos
- Pode ter schema de banco separado
- Se comunica via chamadas in-process ou eventos de domínio internos

```
┌──────────────────────────────────────┐
│            kora.jar                   │
│                                      │
│  ┌────────┐  ┌────────┐  ┌────────┐│
│  │Identity│──│ Social │──│Content ││
│  └────────┘  └────────┘  └────────┘│
│       │           │           │     │
│  ┌──────────────────────────────────│
│  │         PostgreSQL               ││
│  │  identity.*  social.*  content.* ││
│  └──────────────────────────────────│
└──────────────────────────────────────┘
```

## O que são Microserviços?

Cada módulo vira um **processo separado** com deploy, banco, scaling e monitoring independentes.

```
┌──────────┐    ┌──────────┐    ┌──────────┐
│ Identity │    │  Social  │    │ Content  │
│ Service  │───▶│ Service  │───▶│ Service  │
│ (port    │    │ (port    │    │ (port    │
│  8081)   │    │  8082)   │    │  8083)   │
└────┬─────┘    └────┬─────┘    └────┬─────┘
     │               │               │
┌────▼─────┐    ┌────▼─────┐    ┌────▼─────┐
│ Postgres │    │ Postgres │    │ Postgres │
│ identity │    │  social  │    │ content  │
└──────────┘    └──────────┘    └──────────┘
```

## Comparação Detalhada

### Complexidade Operacional

| Aspecto | Monólito Modular | Microserviços |
|---------|-----------------|---------------|
| Deploy | 1 container | N containers |
| Rede | Não há | Service discovery, load balancing, mTLS |
| Monitoramento | 1 processo, N métricas | N processos × N métricas |
| Debug | Stack trace local | Distributed tracing (Jaeger/Zipkin) |
| Logs | 1 lugar | Agregação necessária (ELK/Loki) |
| CI/CD | 1 pipeline | N pipelines |
| Banco | 1 instância (schemas separados) | N instâncias ou clusters |

**Custo de 1 pessoa manter microserviços:** altíssimo. Você gasta mais tempo em infra do que em lógica de negócio.

### Consistência de Dados

| Cenário | Monólito Modular | Microserviços |
|---------|-----------------|---------------|
| "Criar usuário + perfil" | 1 transação ACID | Saga ou 2PC (complexo) |
| "Deletar comunidade + posts" | 1 transação | Eventual consistency |
| Rollback | `@Transactional` | Compensating transactions |

No monólito, `@Transactional` garante que tudo funciona ou nada funciona. Em microserviços, você precisa de **Sagas** — e Sagas são difíceis de implementar e debugar.

### Performance

| Aspecto | Monólito Modular | Microserviços |
|---------|-----------------|---------------|
| Chamada entre módulos | ~0ms (in-process) | 1-50ms (rede) |
| Serialização | Nenhuma | JSON/Protobuf a cada chamada |
| N+1 entre serviços | Impossível (tudo no mesmo processo) | Muito fácil de cair |

### Escalabilidade

| Aspecto | Monólito Modular | Microserviços |
|---------|-----------------|---------------|
| Escala horizontal | Réplicas inteiras do monólito | Réplicas seletivas por serviço |
| Escala granular | Não (escala tudo junto) | Sim (só Content precisa de 10 réplicas) |

**Mas:** escalar réplicas inteiras de um monólito bem otimizado é **suficiente para milhões de usuários.** O Twitter começou como monólito Ruby. O Shopify ainda roda monólito modular em Ruby.

### Evolução

```
Monólito Modular (2 meses)
    │
    │  sentiu dor de escala no Content?
    │  Content tem 100x mais tráfego?
    │  Time cresceu e precisa deploy independente?
    │
    ▼
Extrair Content como microserviço (1-2 semanas se bem modularizado)
    │
    ▼
Extrair Notification (assíncrono por natureza)
    │
    ▼
... e assim por diante, conforme a DOR justificar
```

A chave: **a modularidade interna do monólito torna a extração barata.** Se os módulos se comunicam via interfaces e eventos, extrair é trocar:
- Chamada in-process → chamada HTTP/gRPC
- Spring Event → Kafka Event
- Schema compartilhado → banco separado

## A Armadilha do "Distributed Monolith"

Se você começa com microserviços **sem entender as fronteiras de domínio**, acaba com o pior dos dois mundos:

- N deploys (custo operacional de microserviços)
- Mas todos precisam ser deployados juntos (acoplamento de monólito)
- Consistência eventual sem necessidade
- Latência de rede sem necessidade

**Isso é o anti-pattern "Distributed Monolith"** e é extremamente comum em equipes que adotam microserviços prematuramente.

## Decisão para o Kora

**Monólito modular.** Porque:

1. **1 desenvolvedor** — não há necessidade de deploy independente
2. **Aprendizado** — entender fronteiras de domínio é mais importante que aprender Kubernetes
3. **Velocidade** — feature completa em horas, não dias
4. **Preparação real** — no mercado, você vai encontrar mais monólitos precisando ser modularizados do que greenfield microservices
5. **A migração faz parte do aprendizado** — o Kora vai migrar para microserviços nas fases 11-14, e esse processo ensina mais do que já começar com microserviços

## Leitura Recomendada

- Sam Newman — *Building Microservices*, 2nd Edition (2021): Capítulo 1 e 2
- Martin Fowler — [MonolithFirst](https://martinfowler.com/bliki/MonolithFirst.html)
- DHH — [The Majestic Monolith](https://m.signalvnoise.com/the-majestic-monolith/)
- Neal Ford, Mark Richards — *Fundamentals of Software Architecture*: Capítulo sobre modularity
- Vlad Khononov — *Learning Domain-Driven Design*: Capítulo sobre Bounded Contexts

## Glossário Rápido

- **ACID**: Atomicity, Consistency, Isolation, Durability — propriedades de transações
- **Saga**: Padrão para manter consistência entre serviços via sequência de transações locais + compensação
- **Eventual Consistency**: os dados eventualmente convergem para o estado correto, mas podem estar temporariamente inconsistentes
- **2PC**: Two-Phase Commit — protocolo de commit distribuído (pesado, evitar)
