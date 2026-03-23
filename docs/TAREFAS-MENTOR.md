# Tarefas de Mentor — Kora Project

> Formato: Gherkin-inspired. Cada tarefa é um passo concreto.
> Formato de estudo autodidata com tarefas incrementais e revisão rigorosa.

---

## FASE 0 — Ambiente e Ferramentas

### Tarefa 0.1: Migrar Spring Boot e criar estrutura Kora

**Cenário:** Preparar o projeto real do Kora

**Dado que** o protótipo Kafka já existe em `java/projects/`
**E** o KORA-PLAN.md define a estrutura alvo

**Quero que você:**

1. Crie a pasta `kora/backend/` na raiz do repositório
2. Crie um `pom.xml` parent com:
   - `groupId`: `org.gustalab`
   - `artifactId`: `kora`
   - `parent`: Spring Boot **3.3.x** (última versão estável, não snapshot)
   - `java.version`: 21
   - `packaging`: pom
   - `modules`: `kora-api`, `kora-common`, `kora-identity`
3. Crie os 3 módulos com `pom.xml` filho (por enquanto sem código, só a estrutura Maven):
   - `kora-api` (será o Spring Boot app — `spring-boot-starter-web`)
   - `kora-common` (JAR puro, sem Spring Boot starter)
   - `kora-identity` (depende de `kora-common`)
4. Em `kora-api`, crie a classe `KoraApplication.java` com `@SpringBootApplication` e um endpoint `GET /health` que retorna `"ok"`
5. `mvn clean install -DskipTests` deve passar sem erros

**Sugestões:**
- Use o Spring Initializr só para consultar as versões, mas crie o POM na mão — isso ensina mais
- Olhe o `java/projects/pom.xml` como referência de como estruturar módulos Maven
- Lembre de configurar `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` antes de rodar Maven

**Após completar:** me mande os `pom.xml` e a classe `KoraApplication.java`. Eu reviso e discutimos trade-offs.

---

### Tarefa 0.2: Docker Compose para PostgreSQL + Redis

**Cenário:** Infraestrutura local para desenvolvimento

**Dado que** o Kora precisa de PostgreSQL e Redis
**E** tudo deve rodar localmente com zero custo

**Quero que você:**

1. Crie `kora/backend/compose.yaml` com:
   - PostgreSQL 16 Alpine (porta 5432, db: `kora`, user: `kora`, password: `kora_dev`)
   - Redis 7 Alpine (porta 6379)
   - Volume nomeado para dados do Postgres
2. Suba com `docker compose up -d` e verifique:
   - `docker compose ps` mostra ambos running
   - `psql -h localhost -U kora -d kora` conecta
   - `redis-cli ping` retorna PONG

**Sugestões:**
- Olhe o `compose.yaml` que já existe em `java/projects/compose.yaml` como referência
- Não precisa de Kafka/Zookeeper agora — só PostgreSQL e Redis
- Use `healthcheck` no Postgres para garantir que ele está pronto antes de conectar

**Após completar:** me mande o `compose.yaml`. Eu reviso.

---

### Tarefa 0.3: Mover protótipos Kafka para templates

**Cenário:** Organizar o repositório

**Dado que** `kafka-producer-api` e `kafka-consumer-api` são protótipos de aprendizado
**E** o Kora terá sua própria estrutura

**Quero que você:**

1. Crie a pasta `templates/` na raiz do repositório
2. Mova `java/projects/kafka-producer-api/` → `templates/kafka-producer-api/`
3. Mova `java/projects/kafka-consumer-api/` → `templates/kafka-consumer-api/`
4. Atualize (ou remova) o `java/projects/pom.xml` raiz que referenciava esses módulos
5. Garanta que o build dos templates ainda funciona: `mvn -f templates/kafka-producer-api/pom.xml clean compile`

**Sugestões:**
- Use `git mv` para manter o histórico do Git
- O `java/projects/pom.xml` pode virar só um POM vazio ou ser removido

**Após completar:** me mostre a nova estrutura de diretórios. Eu valido.

---

### Tarefa 0.4: Criar CLAUDE.md na raiz do projeto

**Cenário:** Configurar a IA para seguir os padrões do Kora

**Dado que** o KORA-PLAN.md seção 14 já define o conteúdo do CLAUDE.md

**Quero que você:**

1. Crie `/mnt/BACKUP/BACKUP/Estudos/Aulas/CLAUDE.md` com as regras definidas no plano (seção 14.1)
2. Adapte se achar necessário — é o SEU projeto, as regras devem fazer sentido para você

**Sugestões:**
- Leia a seção 14.1 do KORA-PLAN.md
- Pense se quer adicionar algo que não está lá

---

### Tarefa 0.5: Configurar Git Flow

**Cenário:** Branches no modelo corporativo

**Dado que** o plano define Git Flow na seção 7

**Quero que você:**

1. Crie a branch `develop` a partir de `main`
2. Crie uma branch `feature/fase-0-setup` a partir de `develop`
3. Faça o trabalho das tarefas 0.1-0.4 nessa feature branch
4. Quando tudo estiver pronto, faremos merge de volta para `develop`

**Sugestões:**
- `git checkout -b develop`
- `git checkout -b feature/fase-0-setup`
- Commits seguindo Conventional Commits (seção 7.2 do plano)
- NUNCA faça push

---

## FASE 1 — Identity Context (Backend)

> **Pré-requisito:** Fase 0 completa

### Tarefa 1.1: Modelar User Aggregate (DDD)

**Cenário:** Criar a base do sistema de identidade

**Dado que** o Kora precisa de usuários com registro e login
**E** queremos aplicar DDD desde o início

**Quero que você:**

1. No módulo `kora-identity`, crie a estrutura de pacotes:
   ```
   org.gustalab.kora.identity/
   ├── api/
   ├── application/
   ├── domain/
   ├── infrastructure/
   └── event/
   ```
2. Em `domain/`, crie:
   - `User` (Aggregate Root): `id` (UUID), `email`, `username`, `passwordHash`, `createdAt`
   - `Email` (Value Object): valida formato, imutável
   - `Username` (Value Object): valida tamanho e caracteres, imutável
3. Em `domain/`, crie a interface `UserRepository` (só a interface, não a implementação JPA)

**Perguntas para pensar antes de implementar:**
- Por que `Email` e `Username` são Value Objects e não `String`?
- Onde deve ficar a validação: no construtor de `User`, no Value Object, ou no Service?
- `User` deve ter um construtor público ou um Factory Method estático?

**Sugestões:**
- Use Java Records para Value Objects (são naturalmente imutáveis)
- Para `User`, talvez uma classe normal seja melhor (tem estado mutável como `passwordHash`)
- Leia o Item 1 do Effective Java: "Consider static factory methods instead of constructors"

**Após completar:** me mande as classes `User`, `Email`, `Username`, e `UserRepository`. Eu reviso aplicando DDD + SOLID.

---

### Tarefa 1.2: Implementar RegisterUserUseCase

**Cenário:** Caso de uso para registrar novo usuário

**Dado que** já temos o `User` aggregate e `UserRepository`

**Quero que você:**

1. Em `application/`, crie `RegisterUserUseCase` (ou `RegisterUserService`)
2. Ele deve:
   - Receber um command/DTO com email, username e senha
   - Verificar se email/username já existem
   - Criar `User` com senha hasheada (BCrypt)
   - Salvar via `UserRepository`
   - Publicar evento `UserRegistered` via Spring Events
3. Em `event/`, crie `UserRegistered` (domain event)

**Perguntas para pensar:**
- Quem faz o hash da senha? O `User`? O `UseCase`? Um `PasswordEncoder` injetado?
- O que retornar: o `User`, um DTO, ou `void`?
- `RegisterUserUseCase` deve ser `@Service` ou `@Component`? Importa?

**Sugestões:**
- Use `@Transactional` no use case (não no repository)
- Para password hashing: `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`
- O evento de domínio não precisa ser complexo: `record UserRegistered(UUID userId, String email, Instant occurredAt)`

**Após completar:** me mande o código. Eu reviso e discutimos SOLID, Clean Architecture, e onde cada responsabilidade mora.

---

## Regras do Mentor

1. **Tente implementar sozinho primeiro.** Se travar, me peça dicas (não a resposta).
2. **Eu nunca vou dar `tab` para completar.** Vou dar a direção e você implementa.
3. **Errar é parte do processo.** Prefiro ver código "errado" para corrigir do que código copiado.
4. **Todo código revisado vira um commit didático.** O commit message explica o padrão aplicado.
5. **A prioridade é entender o porquê, não só o como.**

---

*Última atualização: 2026-03-22*
