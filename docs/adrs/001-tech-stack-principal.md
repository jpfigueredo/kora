# ADR-001: Tech Stack Principal do Kora

> Status: **Aceita**
> Data: 2026-03-21
> Deciders: Gustavo

## Contexto

O Kora é um projeto educacional de rede social que precisa de uma stack que:
1. Seja relevante para o mercado (foco em Aubay, big techs, consultorias)
2. Permita aprender arquitetura, patterns e plataforma
3. Seja open-source e zero custo
4. Suporte evolução de monólito modular para microserviços

## Decisão

| Camada | Tecnologia | Versão |
| ------ | ---------- | ------ |
| Backend | Java + Spring Boot | 21 LTS + 3.3.x |
| Frontend | Angular | 18+ |
| Banco de dados | PostgreSQL | 16 |
| Cache | Redis | 7 |
| Build | Maven | 3.9+ |
| Containers | Docker + Compose | Latest |
| CI/CD | GitHub Actions | - |
| K8s local | Kind ou Minikube | - |
| Observabilidade | Micrometer + Prometheus + Grafana | - |
| Testes | JUnit 5 + Testcontainers + Mockito + AssertJ | - |
| Mensageria (fase 7) | RabbitMQ | 3.13+ |
| Mensageria (fase 11+) | Apache Kafka + Avro + Schema Registry | - |

## Alternativas Consideradas

### Java 21 vs Java 24/25

**Java 21 (ESCOLHIDA)**
- LTS até setembro 2028
- Mercado enterprise roda em LTS
- Ferramentas e libs estáveis
- Virtual Threads estabilizado

**Java 24/25**
- Features mais novas, mas sem LTS
- Ferramentas com suporte incompleto
- Spring Boot 4.x (snapshot) seria necessário

Ver artigo: `docs/articles/001-por-que-java-21-spring-boot-3.md`

### Angular vs React

**Angular (ESCOLHIDA)**
- Prioridade para Aubay (framework enterprise)
- Full-featured (routing, forms, HTTP, DI built-in)
- TypeScript obrigatório (disciplina)

**React**
- Entrará depois como React Native para mobile (fase 15+)

### PostgreSQL vs MongoDB

**PostgreSQL (ESCOLHIDA)**
- Relacional robusto com JSONB quando precisar de flexibilidade
- 1 banco para operar, menos complexidade
- ACID completo

**MongoDB**
- Pode entrar depois para feed materializado ou analytics
- Não necessário nas fases 1-10

Ver artigo: `docs/articles/006-nosql-mongodb-quando-usar.md`

## Consequências

### Positivas
- Stack madura, bem documentada, amplamente adotada
- Ecossistema Java + Spring é o maior do mercado enterprise
- Zero custo (tudo open-source)
- Preparação direta para o mercado de trabalho

### Negativas
- Spring Boot tem curva de aprendizado para configuração avançada
- Java é verboso comparado a Kotlin/Go (mitigado por Records e Lombok)

### Riscos
- Spring Boot 3.3.x pode ficar outdated durante o projeto (mitigação: migrar para 3.4.x quando GA)

## Quando Reconsiderar
- Quando Spring Boot 4.x for GA e estável
- Quando Java 25 for LTS
- Se uma vaga específica exigir stack diferente
