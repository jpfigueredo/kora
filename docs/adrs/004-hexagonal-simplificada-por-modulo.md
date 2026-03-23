# ADR-004: Arquitetura Hexagonal Simplificada por Módulo

> Status: **Aceita**
> Data: 2026-03-22
> Deciders: Gustavo

## Contexto

Cada módulo Maven do Kora precisa de uma organização interna que:
1. Separe lógica de negócio de infraestrutura
2. Permita testar domain sem banco, Spring ou frameworks
3. Seja consistente entre todos os módulos
4. Não seja over-engineered para o tamanho do projeto

## Decisão

Cada módulo segue **hexagonal simplificada** (Ports & Adapters) com 5 pacotes:

```
api/              → Adapters de entrada (Controllers, Request/Response DTOs)
application/      → Use Cases (@Service, @Transactional)
domain/           → Core (Entities, VOs, Repository interfaces) — PURO JAVA
infrastructure/   → Adapters de saída (JPA, configs, clients externos)
event/            → Domain Events (records imutáveis)
```

### Regra de dependência

`api → application → domain ← infrastructure`

Domain NÃO importa Spring, JPA, ou qualquer framework.

## Alternativas Consideradas

### Hexagonal simplificada (ESCOLHIDA)

**Prós:**
- Domínio isolado e testável
- Fronteiras claras entre camadas
- JPA Entity separada da Domain Entity (flexibilidade)
- Consistente e previsível

**Contras:**
- Mais classes que uma abordagem CRUD (mapper, JPA entity + domain entity)
- Pode parecer over-engineering para CRUDs simples

### Layered Architecture (Controller → Service → Repository)

**Prós:**
- Simples, familiar
- Menos boilerplate

**Contras:**
- Domain model tende a ficar anêmico (service com toda a lógica)
- Difícil testar sem banco (JPA entity = domain entity = acoplado)
- Não prepara para extração de microserviço

### Clean Architecture completa (Uncle Bob)

**Prós:**
- Separação máxima
- Muito elegante em teoria

**Contras:**
- Excesso de interfaces e indireções para 1 dev
- Over-engineering para um projeto educacional

## Consequências

### Positivas
- Código organizado e previsível
- Domain testável sem infraestrutura
- Fácil de revisar (checklist no artigo 011)
- Preparação para entrevistas (hexagonal é tema frequente)

### Negativas
- Boilerplate de mapper Domain ↔ JPA
- Curva de aprendizado inicial (onde colocar cada coisa)

## Referências
- Alistair Cockburn — Hexagonal Architecture
- `docs/articles/011-padroes-implementacao-kora.md` (guia completo)
- Eric Evans — *Domain-Driven Design*: Entities, Value Objects, Repositories
