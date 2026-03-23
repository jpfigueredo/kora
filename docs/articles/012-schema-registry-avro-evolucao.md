# Schema Registry e Apache Avro: Evolução da Serialização

> Artigo de estudo — Kora Project
> Data: 2026-03-22

---

## O Problema

Quando dois serviços trocam mensagens via Kafka (ou qualquer MQ), ambos precisam concordar sobre o **formato** da mensagem. Sem contrato formal:

```
Producer envia: {"userId": "123", "name": "Alice"}
Consumer espera: {"user_id": "123", "full_name": "Alice"}

→ Erro silencioso. Dados perdidos.
```

Pior: quando o formato muda (adicionou um campo, removeu outro), como garantir que consumers antigos continuam funcionando?

---

## Evolução da Serialização no Kora

### Fase 1: JSON String (agora)

```java
// Producer
String payload = objectMapper.writeValueAsString(orderDTO);
producer.send(new ProducerRecord<>("orders", payload));

// Consumer
OrderDTO order = objectMapper.readValue(record.value(), OrderDTO.class);
```

**Prós:**
- Simples, legível, zero infra adicional
- Funciona com qualquer linguagem

**Contras:**
- Sem validação de schema em runtime
- Se o producer adicionar/remover campo, consumer pode quebrar
- Tamanho do payload (JSON é verboso)
- Sem evolução de schema controlada

### Fase 11+: Avro + Schema Registry (futuro)

```avro
{
  "type": "record",
  "name": "OrderCreated",
  "namespace": "com.kora.content.event",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "customerId", "type": "string"},
    {"name": "amount", "type": "long"},
    {"name": "occurredAt", "type": {"type": "long", "logicalType": "timestamp-millis"}}
  ]
}
```

---

## O que é Apache Avro?

Formato de serialização **binário** com schema embutido:

| Aspecto | JSON | Avro | Protobuf |
|---------|------|------|----------|
| Formato | Texto | Binário | Binário |
| Schema | Implícito (na classe Java) | Explícito (.avsc) | Explícito (.proto) |
| Tamanho | Grande | Pequeno | Pequeno |
| Legibilidade | Alta | Baixa (binário) | Baixa (binário) |
| Evolução de schema | Manual | Controlada (compatibility rules) | Controlada |
| Geração de código | Não | Sim (plugin Maven) | Sim (protoc) |
| Adoção em Kafka | Muito comum (JSON string) | Padrão de mercado (Confluent) | Crescendo |

### Por que Avro para Kafka?

1. **Schema Registry**: schemas versionados centralmente. Producer e consumer verificam compatibilidade.
2. **Evolução controlada**: pode adicionar campos opcionais sem quebrar consumers antigos.
3. **Compacto**: ~30-50% menor que JSON para mesmos dados.
4. **Geração de código**: schema → classe Java automaticamente (plugin Maven).
5. **Padrão de mercado**: Confluent Platform, LinkedIn, Netflix, Uber usam Avro + Schema Registry.

---

## Schema Registry: Como Funciona

```
Producer                           Consumer
   │                                  │
   │  1. Registra schema              │
   ├─────────────▶ Schema Registry    │
   │               (HTTP REST API)    │
   │  2. Recebe schema ID            │
   │◀─────────────                    │
   │                                  │
   │  3. Envia: [schema_id + bytes]   │
   ├──────────────▶ Kafka ───────────▶│
   │                                  │
   │                    4. Busca schema│
   │               Schema Registry ◀──┤
   │                              ────▶│
   │                    5. Desserializa│
```

1. Producer registra o schema no Registry antes de enviar
2. Registry valida compatibilidade com versões anteriores
3. Mensagem no Kafka contém `[magic_byte + schema_id (4 bytes) + dados Avro]`
4. Consumer busca o schema pelo ID (com cache local)
5. Consumer desserializa usando o schema

### Regras de Compatibilidade

| Tipo | O que pode fazer | Quando usar |
|------|-----------------|-------------|
| **BACKWARD** | Novo schema lê dados antigos | Consumer atualiza primeiro |
| **FORWARD** | Schema antigo lê dados novos | Producer atualiza primeiro |
| **FULL** | Ambos (backward + forward) | **Recomendado** para maioria dos casos |
| **NONE** | Sem validação | Desenvolvimento/testes |

**No Kora:** usar **FULL** compatibility. É mais restritivo, mas evita surpresas.

### O que pode e o que não pode em FULL compatibility

| Operação | Permitido? | Detalhe |
|----------|-----------|---------|
| Adicionar campo **com default** | ✅ | `{"name": "newField", "type": "string", "default": ""}` |
| Adicionar campo **sem default** | ❌ | Quebraria backward compatibility |
| Remover campo **com default** | ✅ | Consumers antigos usam o default |
| Remover campo **sem default** | ❌ | Quebraria forward compatibility |
| Renomear campo | ❌ | É como remover + adicionar |
| Mudar tipo do campo | ❌ | (com exceções: int→long, float→double) |

---

## Implementação Prática (para quando chegarmos na fase 11)

### Schema (.avsc)

```json
{
  "type": "record",
  "name": "PostCreated",
  "namespace": "com.kora.content.event",
  "fields": [
    {"name": "postId", "type": "string"},
    {"name": "authorId", "type": "string"},
    {"name": "content", "type": "string"},
    {"name": "occurredAt", "type": {"type": "long", "logicalType": "timestamp-millis"}},
    {"name": "tags", "type": {"type": "array", "items": "string"}, "default": []}
  ]
}
```

### Maven Plugin (gera classes Java a partir do .avsc)

```xml
<plugin>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>1.11.3</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals><goal>schema</goal></goals>
            <configuration>
                <sourceDirectory>${project.basedir}/src/main/avro</sourceDirectory>
                <outputDirectory>${project.build.directory}/generated-sources/avro</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Docker Compose (Schema Registry)

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

---

## Decisão para o Kora

| Fase | Serialização | Justificativa |
|------|-------------|---------------|
| 1-6 | Spring Events (in-process, sem serialização) | Monólito, tudo no mesmo JVM |
| 7-10 | JSON String via RabbitMQ | Aprender MQ sem complexidade de schema |
| 11+ | **Avro + Schema Registry via Kafka** | Evolução de schema, contrato formal, performance |

### Por que não Avro desde o início?

1. Complexidade adicional (Schema Registry é mais um serviço para operar)
2. No monólito com Spring Events, não há serialização — os objetos são passados por referência
3. Aprender JSON primeiro constrói a base para entender **por que** Avro resolve problemas que JSON não resolve

### Por que não Protobuf?

Avro é o **padrão de facto** no ecossistema Kafka (Confluent). Protobuf é excelente mas mais comum em gRPC. Como o Kora prioriza Kafka, Avro é a escolha natural. Se implementarmos gRPC para comunicação Go ↔ Java, Protobuf entra nesse contexto.

---

## Referências

- Martin Kleppmann — *Designing Data-Intensive Applications*: Cap. 4 (Encoding and Evolution)
- Confluent docs — Schema Registry, Avro serializer
- Apache Avro specification — https://avro.apache.org/docs/current/
