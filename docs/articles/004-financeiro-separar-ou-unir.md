# Financeiro Pessoal vs Financeiro da Comunidade: Separar ou Unir?

> Artigo de decisão — Kora Project
> Data: 2026-03-21

---

## O Problema

O Kora tem duas funcionalidades financeiras:

1. **Financeiro Pessoal**: o usuário adiciona cartões, cadastra pagamentos recorrentes, recebe sugestões de um assistente financeiro (IA), vê dashboard com gastos.
2. **Financeiro da Comunidade (Loja)**: a comunidade tem uma loja, vende produtos/serviços, precisa de relatórios de vendas, gestão de receitas.

Pergunta: **são o mesmo serviço ou serviços diferentes?**

---

## Análise de Domínio (DDD)

### Linguagem Ubíqua

| Termo | Financeiro Pessoal | Financeiro da Comunidade |
|-------|-------------------|-------------------------|
| "Cartão" | Cartão do usuário para controle de gastos | Meio de pagamento aceito pela loja |
| "Transação" | Gasto pessoal (supermercado, aluguel) | Venda de produto (ingresso, curso) |
| "Relatório" | Dashboard de gastos mensais do indivíduo | Relatório de vendas da comunidade |
| "Saldo" | Não existe (é controle, não carteira digital) | Receita da comunidade |
| "Recorrência" | Pagamentos fixos do mês (Netflix, aluguel) | Assinaturas de membros |
| "Perfil" | Pessoa física | Comunidade/empresa |

**Os termos são iguais mas os significados são diferentes.** Isso é um sinal clássico de **bounded contexts distintos** (Eric Evans — DDD).

### Regras de negócio

| Regra | Pessoal | Comunidade |
|-------|---------|------------|
| Quem vê os dados? | Só o próprio usuário | Admins da comunidade |
| Quem edita? | O usuário | Admins da comunidade |
| Integração externa | Nenhuma (input manual ou scraping futuro) | Gateway de pagamento (Stripe, PagSeguro) |
| IA/Sugestões | Sim (assistente financeiro) | Talvez (analytics de vendas) |
| Compliance | Baixo (dados pessoais) | Médio/Alto (transações financeiras reais) |
| Escala | 1 dashboard por usuário | N transações por comunidade |

---

## Opção A: Unir em 1 módulo (`kora-finance`)

```
kora-finance/
├── domain/
│   ├── personal/    # PersonalBudget, Expense, RecurringPayment
│   └── commerce/    # Sale, Revenue, CommunityWallet
├── application/
│   ├── personal/    # ManageBudgetUseCase
│   └── commerce/    # ProcessSaleUseCase
└── infrastructure/
    ├── personal/    # PersonalFinanceRepository
    └── commerce/    # SaleRepository
```

**Prós:**
- 1 módulo Maven, menos complexidade de build
- Conceitos compartilhados (Money value object, formatação de moeda, geração de PDF)
- Mais fácil para 1 desenvolvedor manter

**Contras:**
- Risco de acoplar lógicas que não deveriam se conhecer
- Se o financeiro da comunidade precisa de gateway de pagamento, puxa essa dependência para o módulo inteiro
- Difícil de extrair depois se um crescer mais que o outro
- Violação potencial de SRP: o módulo faz "duas coisas"

### Opção B: Separar em 2 módulos (RECOMENDADA)

```
kora-personal-finance/
├── domain/       # Budget, Expense, RecurringPayment, FinancialGoal
├── application/  # ManageBudgetUseCase, SuggestSavingsUseCase (IA)
└── infrastructure/

kora-commerce/
├── domain/       # Shop, Product, Order, Sale, CommunityWallet
├── application/  # ProcessOrderUseCase, GenerateRevenueReportUseCase
└── infrastructure/

kora-common/
└── finance/      # Money (value object), Currency, ReportGenerator (interface)
```

**Prós:**
- Fronteiras claras de domínio
- Dependências diferentes (IA no pessoal, payment gateway no comércio)
- Pode escalar/extrair independentemente
- Cada módulo é pequeno e focado (SRP)
- Perfis de acesso diferentes (usuário vs admin de comunidade)

**Contras:**
- Mais módulos Maven para gerenciar
- Alguma duplicação de conceitos (Money, Currency — resolve com `kora-common`)
- Se precisar de um relatório que cruza dados pessoais + vendas, precisa de um serviço agregador

---

## Decisão: Separar (Opção B)

**Por quê:**

1. **São bounded contexts diferentes** — linguagem ubíqua diverge
2. **Perfis de usuário diferentes** — pessoa física vs admin de comunidade
3. **Requisitos de segurança diferentes** — dados pessoais (LGPD) vs transações comerciais
4. **Dependências externas diferentes** — IA vs payment gateway
5. **Preparação para extração** — se o Kora crescer, commerce é candidato óbvio a microserviço (precisa de gateway, compliance, auditoria)
6. **Compartilham pouco** — Money, Currency, formatação → isso vai em `kora-common`

### Como eles se comunicam?

```
                    ┌─────────────────┐
                    │   kora-common   │
                    │ Money, Currency │
                    │ ReportGenerator │
                    └────────▲────────┘
                             │
              ┌──────────────┼──────────────┐
              │                             │
  ┌───────────┴──────────┐    ┌─────────────┴─────────┐
  │ kora-personal-finance│    │    kora-commerce       │
  │                      │    │                        │
  │ Budget, Expense      │    │ Shop, Product, Order   │
  │ RecurringPayment     │    │ Sale, CommunityWallet  │
  │ FinancialGoal        │    │                        │
  │ AI Suggestions       │    │ Payment Gateway        │
  └──────────────────────┘    └────────────────────────┘
```

- **Sem dependência direta entre eles**
- Se precisar de um relatório cruzado (ex: "quanto gastei em compras da comunidade X"), isso é uma **query de leitura** que pode agregar dados de ambos via API ou projeção

---

## Sobre relatórios financeiros

Tanto o pessoal quanto o da comunidade precisam de relatórios. Opções:

| Abordagem | Quando usar |
|-----------|-------------|
| **Query direta no PostgreSQL** | Volume baixo, relatórios simples (< 10k registros) |
| **View materializada** | Relatórios frequentes sobre dados que não mudam muito |
| **CQRS com read model** | Alto volume, relatórios complexos, dashboards real-time |
| **Export para analytics (BigQuery, DuckDB)** | Relatórios ad-hoc, data science |

**Para o Kora inicialmente:** query direta + views materializadas. CQRS quando justificar.

---

## Referências

- Eric Evans — *Domain-Driven Design*: Bounded Contexts, Context Mapping
- Vlad Khononov — *Learning Domain-Driven Design*: Cap. 4 (Bounded Contexts)
- Sam Newman — *Building Microservices*: Cap. 3 (How to Model Services)
