# PostgreSQL vs MongoDB: Quando Usar Cada Um?

> Artigo de decisão — Kora Project
> Data: 2026-03-21

---

## Contexto

O Kora tem múltiplos domínios com necessidades diferentes de persistência. A pergunta é: **precisamos de MongoDB (ou outro NoSQL) além do PostgreSQL?**

## Comparação Fundamental

| Aspecto | PostgreSQL | MongoDB |
|---------|-----------|---------|
| **Modelo** | Relacional (tabelas, linhas, colunas) | Documentos (JSON/BSON) |
| **Schema** | Rígido (DDL define colunas) | Flexível (cada documento pode ter forma diferente) |
| **Relacionamentos** | JOINs nativos, FK, integridade referencial | Embedding ou referências manuais (sem FK nativo) |
| **Transações** | ACID completo, multi-tabela | ACID em documento único; multi-document desde 4.0 (mas com custo) |
| **Consultas** | SQL (extremamente poderoso) | MQL (MongoDB Query Language) |
| **Escala horizontal** | Difícil (read replicas ok, sharding manual) | Nativo (sharding built-in) |
| **JSONB** | Sim! PostgreSQL tem JSONB nativo | O banco inteiro é JSON |

## O Segredo: PostgreSQL tem JSONB

Muitos projetos adotam MongoDB por "precisar de documentos JSON". Mas o PostgreSQL tem **JSONB** nativo desde a versão 9.4 (2014):

```sql
-- Criar tabela com coluna JSONB
CREATE TABLE community.pages (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    content JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Inserir
INSERT INTO community.pages (id, community_id, content)
VALUES (gen_random_uuid(), '...', '{
    "type": "section",
    "children": [
        {"type": "text", "props": {"content": "Hello Kora!"}}
    ]
}');

-- Consultar dentro do JSON
SELECT content->'children'->0->>'type' FROM community.pages;

-- Indexar campos do JSON
CREATE INDEX idx_pages_type ON community.pages USING GIN (content jsonb_path_ops);
```

**Isso significa:** para a maioria dos casos onde "precisamos de flexibilidade de schema", PostgreSQL JSONB resolve sem adicionar outro banco.

## Quando MongoDB Faz Sentido (de verdade)

| Cenário | Por quê MongoDB | PostgreSQL resolve? |
|---------|----------------|-------------------|
| **Escala de escrita massiva** | Sharding nativo distribui escritas | Não facilmente |
| **Documentos muito grandes e variáveis** | Modelo nativo de documentos | JSONB funciona, mas JOINs ficam desajeitados |
| **Time series data** (logs, métricas) | MongoDB Time Series Collections | PostgreSQL TimescaleDB extension |
| **Prototipagem ultra-rápida** | Sem DDL, schema-on-read | Precisa de migrations |
| **Catálogo de produtos heterogêneo** | Cada produto tem atributos diferentes | JSONB resolve |
| **Cache/sessão** | Rápido para R/W simples | Redis é melhor para isso |

## No Kora: Onde cada banco faz sentido?

| Domínio | Banco | Por quê |
|---------|-------|---------|
| **Identity** (User, Profile) | PostgreSQL | Relacional clássico, integridade referencial |
| **Social** (Connections) | PostgreSQL | JOINs para feed, contagem de seguidores |
| **Content** (Posts, Comments) | PostgreSQL | JOINs com User, paginação SQL, full-text search |
| **Community** (Pages) | PostgreSQL (JSONB) | Página é um documento JSON, mas vive dentro de uma comunidade relacional |
| **Commerce** (Orders, Products) | PostgreSQL | Transações ACID obrigatórias para financeiro |
| **Personal Finance** | PostgreSQL | Dados estruturados, relatórios com GROUP BY |
| **Notification** | PostgreSQL (ou Redis) | Read/write simples, TTL |
| **Feed materializado** (futuro) | Redis ou MongoDB | Cache de feed gerado, acesso por key |
| **Search** (futuro) | Elasticsearch/OpenSearch | Full-text search de posts e comunidades |
| **Analytics/Relatórios** (futuro) | DuckDB ou ClickHouse | OLAP, agregações pesadas |

## Decisão para o Kora

**PostgreSQL como banco principal.** MongoDB **pode** entrar depois como:
- Cache de feed materializado (alternativa ao Redis)
- Armazenamento de logs/eventos para analytics

Mas **não é necessário agora**. PostgreSQL com JSONB cobre todos os casos das Fases 1-10.

### Quando reconsiderar

- Se o feed materializado precisar de escala de escrita que PostgreSQL não aguenta
- Se introduzirmos catálogo de produtos com schema ultra-variável
- Se precisarmos de sharding horizontal (muito improvável para projeto educacional)

## Trade-offs resumidos

```
PostgreSQL only
  (+) 1 banco para operar, 1 backup, 1 expertise
  (+) ACID completo, JOINs poderosos
  (+) JSONB quando precisar de flexibilidade
  (-) Escala de escrita tem limite
  (-) Sharding não é nativo

PostgreSQL + MongoDB
  (+) Cada banco no que faz melhor
  (-) 2 bancos para operar, 2 backups
  (-) Consistência entre bancos (eventual consistency)
  (-) Mais complexidade de infra
  (-) Curva de aprendizado de 2 tecnologias
```

## Referências

- Martin Kleppmann — *Designing Data-Intensive Applications*: Cap. 2 (Data Models and Query Languages)
- PostgreSQL docs — [JSON Types](https://www.postgresql.org/docs/current/datatype-json.html)
- MongoDB vs PostgreSQL — análise objetiva depende do workload, não de hype
