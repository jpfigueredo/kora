# /adr — Criar Architecture Decision Record

Crie um novo ADR seguindo o template do Kora.

## Parâmetros
- `$ARGUMENTS` — título da decisão (ex: "Feed Strategy Pattern", "JWT Authentication")

## Template

Criar em `docs/adrs/ADR-XXX-<slug>.md`:

```markdown
# ADR-XXX: TÍTULO

**Status:** Proposta | Aceita | Substituída por ADR-YYY
**Data:** YYYY-MM-DD
**Decisores:** Gustavo

## Contexto

O que motivou essa decisão? Qual problema estamos resolvendo?

## Decisão

O que decidimos fazer? Ser específico e concreto.

## Alternativas consideradas

### Opção A — (escolhida)
**Prós:** ...
**Contras:** ...

### Opção B
**Prós:** ...
**Contras:** ...

## Consequências

- **Positivas:** ...
- **Negativas:** ...
- **Riscos:** ...

## Referências

- Links para artigos, livros, docs
- Links para artigos do projeto em docs/articles/
```

## Após criar

1. Adicionar na seção "ADRs existentes" do KORA-PLAN.md
2. Se relevante, criar artigo associado em docs/articles/
3. Referenciar no commit message: `ADR: docs/adrs/ADR-XXX-slug.md`
