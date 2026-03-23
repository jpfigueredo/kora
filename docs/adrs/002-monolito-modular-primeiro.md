# ADR-002: Começar como Monólito Modular

> Status: **Aceita**
> Data: 2026-03-21
> Deciders: Gustavo

## Contexto

O Kora é uma rede social com múltiplos domínios (Identity, Social, Content, Community, etc.). A tentação natural é começar com microserviços, já que o produto final provavelmente precisará escalar. No entanto, temos 1 desenvolvedor e o objetivo é aprender.

## Decisão

Começar como **monólito modular** (1 JAR, N módulos Maven com fronteiras claras) e migrar para microserviços nas fases 11-14, conforme a dor justificar.

### Estrutura

```
kora/
├── kora-api/          (assembler — junta tudo em 1 JAR)
├── kora-common/       (interfaces compartilhadas, value objects)
├── kora-identity/     (User, Auth, Profile)
├── kora-social/       (Connections, Follow)
├── kora-content/      (Posts, Comments, Feed)
├── kora-community/    (Community, Page Builder)
├── kora-notification/ (Notifications)
└── pom.xml (parent)
```

### Regras internas

- Módulos se comunicam via **interfaces em kora-common** (ports)
- Módulos se comunicam via **eventos de domínio** (Spring Events)
- **NUNCA** acesso direto a repositório de outro módulo
- Cada módulo tem schema separado no PostgreSQL (`identity.*`, `social.*`, etc.)

## Alternativas Consideradas

### Monólito Modular (ESCOLHIDA)

**Prós:**
- 1 deploy, 1 processo, zero overhead operacional
- Transações ACID entre módulos (quando necessário)
- Chamadas in-process (~0ms latência)
- Foco em arquitetura e domain model, não em infra
- Extração para microserviço é barata se as fronteiras estiverem bem definidas

**Contras:**
- Escala horizontal = réplicas inteiras
- Não demonstra operação de microserviços no portfolio (mitigação: fases 11-14)

### Microserviços desde o início

**Prós:**
- Portfolio impressionante
- Deploy independente

**Contras:**
- Complexidade operacional brutal para 1 dev (N deploys, distributed tracing, sagas)
- Risco de distributed monolith (pior dos dois mundos)
- Latência de rede desnecessária
- Custo cognitivo tira foco da arquitetura

Ver artigo: `docs/articles/002-monolito-modular-vs-microservicos.md`

## Consequências

### Positivas
- Velocidade de desenvolvimento alta nas fases iniciais
- Foco em domain model e patterns
- A migração para microserviços (fases 11-14) será um exercício didático valioso

### Negativas
- Não pratica operação de microserviços até fase 11

## Quando Reconsiderar
- Quando Content (Posts/Feed) precisar de escala independente
- Quando mais de 1 desenvolvedor trabalhar no projeto
- Quando a fase 11 do KORA-PLAN chegar

## Referências
- Sam Newman — *Building Microservices*: Cap. 1-2
- Martin Fowler — MonolithFirst
- ADR-001 (tech stack)
