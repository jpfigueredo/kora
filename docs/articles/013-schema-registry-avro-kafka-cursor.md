# Schema Registry e Avro: Por que e Quando?

> Artigo de decisão — Kora Project
> Data: 2026-03-22

---

## O Problema

Kafka transporta **bytes**. Ele não sabe (nem se importa) o que é o conteúdo. Quando você manda um JSON como `String`, funciona — mas tem riscos:

```
Producer (v1): {"orderId": "123", "amount": 100.0}
Producer (v2): {"orderId": "123", "total": 100.0, "currency": "BRL"}
                                    ^^^^^^^^        ^^^^^^^^^^^^^^^
                                    renomeou        campo novo

Consumer: espera "amount" → recebe "total" → NullPointerException
```

**Sem schema enforcement, qualquer mudança no formato da mensagem pode quebrar consumers silenciosamente.**

## O que é Schema Registry?

Um **serviço centralizado** que:
1. Armazena schemas (definições de formato) das mensagens
2. Valida que producers e consumers concordam sobre o formato
3. Gerencia evolução de schema (backward/forward compatibility)

```
Producer                          Consumer
    │                                 │
    │  1. Registra schema             │
    ├─────────► Schema Registry ◄─────┤  2. Busca schema
    │           (Confluent/Apicurio)  │
    │                                 │
    │  3. Serializa com schema ID     │
    ├─────────► Kafka Topic ──────────┤  4. Deserializa com schema
    │                                 │
```

## Formatos de serialização

| Formato | Tamanho | Schema? | Human-readable? | Velocidade |
|---------|---------|---------|-----------------|------------|
| **JSON String** | Grande | Não (schema implícito) | Sim | Lenta |
| **JSON Schema** | Grande | Sim (via registry) | Sim | Lenta |
| **Avro** | Pequeno | Sim (obrigatório) | Não (binário) | Rápida |
| **Protobuf** | Pequeno | Sim (obrigatório) | Não (binário) | Muito rápida |

### Por que Avro é o mais usado com Kafka?

1. **Schema embutido**: o schema faz parte da definição do formato. Sem schema, sem serialização.
2. **Evolução de schema**: Avro tem regras claras de compatibilidade:
   - **Backward compatible**: consumer novo lê dados antigos ✓
   - **Forward compatible**: consumer antigo lê dados novos ✓
   - **Full compatible**: ambos ✓
3. **Compacto**: não repete nomes de campos em cada mensagem (diferente de JSON)
4. **Confluent adotou como padrão**: a maioria das empresas que usam Kafka + Confluent usam Avro

### Exemplo de schema Avro

```json
{
  "type": "record",
  "name": "PostCreated",
  "namespace": "org.gustalab.kora.events",
  "fields": [
    {"name": "postId", "type": "string"},
    {"name": "authorId", "type": "string"},
    {"name": "content", "type": "string"},
    {"name": "createdAt", "type": "long", "logicalType": "timestamp-millis"},
    {"name": "tags", "type": {"type": "array", "items": "string"}, "default": []}
  ]
}
```

### Evolução segura

```json
// V1
{"name": "PostCreated", "fields": [
    {"name": "postId", "type": "string"},
    {"name": "content", "type": "string"}
]}

// V2 — adiciona campo com default (backward compatible ✓)
{"name": "PostCreated", "fields": [
    {"name": "postId", "type": "string"},
    {"name": "content", "type": "string"},
    {"name": "tags", "type": {"type": "array", "items": "string"}, "default": []}
]}
```

Consumer V1 lendo mensagem V2: ignora `tags` (não conhece). ✓
Consumer V2 lendo mensagem V1: usa `default: []` para `tags`. ✓

---

## No Kora: Quando migrar de JSON String para Avro?

| Fase | Formato | Por quê |
|------|---------|---------|
| 1-6 | JSON String | Aprendizado, simplicidade, debug fácil (mensagens legíveis) |
| 7-10 | JSON String + validação manual | Adicionar DTOs de evento validados no producer |
| 11+ | **Avro + Schema Registry** | Múltiplos consumers, evolução de schema, produção real |

### Como rodar Schema Registry localmente (gratuito)

**Opção A: Confluent Schema Registry (Docker)**
```yaml
schema-registry:
  image: confluentinc/cp-schema-registry:7.6.0
  depends_on:
    - kafka
  ports:
    - "8081:8081"
  environment:
    SCHEMA_REGISTRY_HOST_NAME: schema-registry
    SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: kafka:9092
```

**Opção B: Apicurio Registry (100% open-source, sem vendor lock-in)**
```yaml
apicurio-registry:
  image: apicurio/apicurio-registry-mem:2.5.8.Final
  ports:
    - "8081:8080"
```

Ambas são gratuitas para desenvolvimento local.

---

## Big Techs usam Schema Registry?

| Empresa | Usa Schema Registry? | Formato |
|---------|---------------------|---------|
| LinkedIn (criou Kafka) | Sim | Avro |
| Netflix | Sim | Avro |
| Uber | Sim | Avro + Protobuf |
| Spotify | Sim | Protobuf |
| Nubank | Sim | Avro |
| iFood | Sim | Avro |

**Resposta: sim, toda empresa séria que usa Kafka usa Schema Registry.** É praticamente obrigatório em produção.

---

## Decisão para o Kora

- **Agora (Fases 1-10):** JSON String. Foco em aprender Kafka Client API, partitions, consumer groups.
- **Fase 11+:** Avro + Apicurio Registry (open-source). Exercício de migração: trocar JSON por Avro em um tópico, ver como consumers lidam.
- **Documentar a migração** como artigo/commit didático.

## Referências

- Apache Avro Spec: https://avro.apache.org/docs/current/specification/
- Confluent Schema Registry docs
- Martin Kleppmann — *DDIA*: Cap. 4 (Encoding and Evolution)
