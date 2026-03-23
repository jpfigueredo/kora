# ADR-005: Financeiro Pessoal e Comercial como Módulos Separados

> Status: **Aceita**
> Data: 2026-03-22
> Deciders: Gustavo

## Contexto

O Kora tem dois domínios financeiros:
1. **Pessoal**: controle de gastos, pagamentos recorrentes, sugestões de IA
2. **Comercial (Loja da Comunidade)**: vendas, receitas, gateway de pagamento

A pergunta: são o mesmo módulo ou módulos separados?

## Decisão

**Módulos separados**: `kora-personal-finance` e `kora-commerce`.

Conceitos compartilhados (Money, Currency, formatação) ficam em `kora-common`.

## Justificativa

- São **bounded contexts diferentes** (linguagem ubíqua diverge)
- **Perfis de usuário diferentes** (pessoa física vs admin de comunidade)
- **Dependências externas diferentes** (IA vs payment gateway)
- **Requisitos de segurança diferentes** (LGPD vs compliance financeiro)

## Referências
- `docs/articles/004-financeiro-separar-ou-unir.md` (análise completa com trade-offs)
- Eric Evans — *DDD*: Bounded Contexts
