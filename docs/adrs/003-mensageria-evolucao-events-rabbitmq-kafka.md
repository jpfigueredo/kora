# ADR-003: Evolução da Mensageria — Spring Events → RabbitMQ → Kafka

> Status: **Aceita**
> Data: 2026-03-21
> Deciders: Gustavo

## Contexto

O Kora precisa de comunicação assíncrona entre módulos/serviços. A escolha entre Spring Events, RabbitMQ e Kafka não é binária — cada um resolve problemas de uma fase diferente.

A decisão de não pular direto para Kafka é **pedagógica**: sentir a dor de cada fase para entender por que a próxima solução existe.

## Decisão

Evolução em 3 fases:

| Fase | Tecnologia | Motivo |
| ---- | ---------- | ------ |
| 1-6 | Spring Events (in-process) | Monólito, tudo no mesmo JVM, zero infra |
| 7-10 | RabbitMQ | Desacoplamento real, aprender MQ, Notification separado |
| 11+ | Kafka + Avro + Schema Registry | Fan-out de posts, replay, CQRS, event streaming |

## Alternativas Consideradas

### Evolução gradual (ESCOLHIDA)

**Prós:**
- Cada fase ensina por que a próxima existe
- Complexidade adicionada apenas quando justificada
- Commits didáticos mostrando a evolução

**Contras:**
- Refatoração ao migrar de uma tecnologia para outra
- Mais tempo total até chegar no Kafka

### Kafka desde o início

**Prós:**
- Já aprende Kafka cedo
- Sem refatoração de mensageria

**Contras:**
- Complexidade prematura (Schema Registry, partitions, consumer groups para 1 módulo)
- Perde o aprendizado da evolução
- Kafka para Spring Events internos é overkill

### Só RabbitMQ (nunca Kafka)

**Prós:**
- Mais simples que Kafka

**Contras:**
- Sem replay de eventos
- Sem consumer groups nativos
- Throughput limitado para fan-out de feed
- Não aprende a tecnologia mais demandada do mercado para streaming

## Consequências

### Positivas
- Aprendizado profundo de **por que** cada tecnologia existe
- Portfolio mostra evolução arquitetural (valor em entrevistas)
- Cada commit documenta a dor e a solução

### Negativas
- Trabalho de migração entre fases

## Quando Reconsiderar
- Se o projeto pular fases (ex: ir direto para microserviços)
- Se surgir necessidade de event streaming antes da fase 11

## Referências
- `docs/articles/003-rabbitmq-vs-kafka-evolucao-mensageria.md`
- `docs/articles/012-schema-registry-avro-evolucao.md`
- Martin Kleppmann — *DDIA*: Cap. 11 (Stream Processing)
