# Configuração de IA para o Kora (Claude Code + Cursor)

> Artigo de guia — Kora Project
> Data: 2026-03-22

---

## Por que configurar as IAs?

Sem configuração, cada conversa começa do zero. A IA não sabe:
- Qual é a arquitetura do projeto
- Quais padrões seguir
- Quais anti-patterns evitar
- Qual o estilo de interação (mentor vs executante)

Com configuração, a IA **já nasce sabendo** o contexto do Kora. Isso economiza tempo e evita erros.

---

## Anatomia da Configuração do Claude Code

### 1. CLAUDE.md (Instruções do Projeto)

**Local:** raiz do repositório (`/CLAUDE.md`)
**Função:** carregado automaticamente em toda conversa. Define regras, padrões, anti-patterns.

No Kora, o CLAUDE.md contém:
- Regras absolutas (nunca push, nunca baixar skills externas)
- Tech stack com justificativas
- Arquitetura de camadas (hexagonal simplificada)
- Padrões de código por camada (Controller, Use Case, Domain, Infrastructure)
- Anti-patterns proibidos
- Formato de commits
- Estilo de interação (mentor-aprendiz)

**Princípio:** o CLAUDE.md é a "constituição" do projeto. Tudo que está nele é lei.

### 2. Skills (Commands)

**Local:** `.claude/commands/` (projeto) e `~/.claude/commands/` (global)
**Função:** comandos invocáveis via `/nome-da-skill`. Automatizam tarefas repetitivas.

Skills criadas para o Kora:

| Skill | Comando | O que faz |
|-------|---------|-----------|
| Review | `/review` | Code review completo seguindo checklist do Kora |
| Commit | `/commit` | Prepara commit didático em Conventional Commits |
| Task | `/task` | Propõe próxima tarefa no estilo mentor-aprendiz (Gherkin) |
| ADR | `/adr` | Cria Architecture Decision Record |
| Article | `/article` | Escreve artigo de documentação |
| Study | `/study` | Recomenda material de estudo dos livros do usuário |

Skills globais (por linguagem):

| Skill | Comando | Contexto |
|-------|---------|----------|
| Java | `/java` | Padrões Java 21 + Spring Boot 3.3.x + hexagonal |
| Angular | `/angular` | Standalone components, Signals, inject() |
| Go | `/golang` | net/http, goroutines, interfaces implícitas |
| Kotlin | `/kotlin` | Coroutines, data classes, Spring Boot |

**Regra:** todas as skills são escritas internamente. Nunca baixar de fontes externas.

### 3. Memory (Memória Persistente)

**Local:** `~/.claude/projects/<projeto>/memory/`
**Função:** informações que persistem entre conversas. Perfil do usuário, feedback, contexto do projeto.

Tipos de memória:
- **user**: perfil, preferências, nível de conhecimento
- **feedback**: correções e confirmações de abordagem
- **project**: estado atual do projeto, decisões em andamento
- **reference**: links para recursos externos

### 4. MCP Servers

**Local:** `~/.claude/settings.json`
**Função:** conectam Claude a ferramentas externas.

MCPs configurados:
- **github**: acesso a repos, PRs, issues via `@modelcontextprotocol/server-github`
- **fetch**: busca de URLs para referência

MCPs futuros (a avaliar):
- **postgres**: consultas diretas ao banco de dev
- **docker**: gerenciamento de containers

### 5. Hooks (Automações)

**Local:** `~/.claude/settings.json` → `hooks`
**Função:** scripts que executam automaticamente em resposta a eventos (antes/depois de tool calls).

Hooks planejados para o Kora:
- **pre-commit**: rodar `mvn verify` antes de commitar
- **post-edit**: rodar linter no arquivo editado
- **pre-push**: BLOQUEAR (nunca push)

---

## Hierarquia de Configuração

```
~/.claude/settings.json          ← Global (MCPs, hooks, permissions)
~/.claude/commands/              ← Skills globais (java, angular, go, kotlin)
    java.md
    angular.md
    golang.md
    kotlin.md

/projeto/CLAUDE.md               ← Instruções do projeto (lidas automaticamente)
/projeto/.claude/commands/       ← Skills do projeto (review, commit, task, adr, article, study)
    review.md
    commit.md
    task.md
    adr.md
    article.md
    study.md

~/.claude/projects/<id>/memory/  ← Memória persistente entre conversas
    MEMORY.md (índice)
    user_profile.md
    feedback_*.md
    project_*.md
    reference_*.md
```

**Precedência:** CLAUDE.md do projeto > skills do projeto > skills globais > memória.

---

## Para o Cursor

O Cursor usa `.cursorrules` (equivalente ao CLAUDE.md) e tem seu próprio sistema de rules.

Para manter consistência entre Claude e Cursor:
1. O CLAUDE.md é a fonte primária de verdade
2. Se usar Cursor, criar `.cursorrules` baseado no CLAUDE.md
3. Skills do Cursor ficam em `.cursor/rules/` (formato diferente)

**Decisão atual:** o Claude Code é o assistente principal. Cursor é usado para edição e autocomplete.

---

## Como Evoluir a Configuração

A configuração de IA evolui junto com o projeto:

| Fase | O que muda na config |
|------|---------------------|
| 1-3 (monólito) | Padrões de camada, domain model, testes |
| 4-6 (frontend) | Skill Angular, padrões de componente, routing |
| 7-10 (RabbitMQ) | Padrões de mensageria, event handlers |
| 11+ (Kafka) | Padrões de consumer/producer, Avro, CQRS |
| DevOps | Padrões de Dockerfile, Compose, K8s manifests |

Cada fase pode adicionar ou refinar skills, atualizar o CLAUDE.md, e criar novas memórias.

---

## Referências

- [Claude Code Docs — CLAUDE.md](https://docs.anthropic.com/en/docs/claude-code)
- [Claude Code Docs — Commands](https://docs.anthropic.com/en/docs/claude-code/commands)
- [MCP Specification](https://modelcontextprotocol.io/)
