# Por que Java 21 + Spring Boot 3.3.x?

> Artigo de decisão — Kora Project
> Data: 2026-03-21

---

## Contexto

O Kora é um projeto educacional com foco em aprender arquitetura, patterns e boas práticas para atuar no mercado. A escolha de Java 21 + Spring Boot 3.3.x não é óbvia — existem argumentos para usar versões mais novas (Java 24/25 + Spring Boot 4.x) ou até para ir de Quarkus.

## Alternativas Consideradas

### Opção A: Java 21 + Spring Boot 3.3.x (ESCOLHIDA)

**Prós:**
- **Java 21 é LTS** (Long-Term Support até setembro de 2028). O mercado enterprise (bancos, fintechs, consultorias como Aubay) roda em LTS. Você não vai encontrar Java 25 em produção na maioria das empresas em 2026.
- **Spring Boot 3.3.x é GA (General Availability)** — estável, testado, documentado. Testcontainers, Spring Security, Spring Data, tudo funciona sem surpresas.
- **Todas as features modernas de Java estão em 21**: Records, Sealed Classes, Pattern Matching, Virtual Threads (preview estabilizado), Text Blocks, Switch Expressions.
- **Compatibilidade de ferramentas**: IDE, plugins Maven, Testcontainers, Resilience4j, tudo suporta 3.3.x sem gambiarras.
- **Foco no aprendizado**: quando a ferramenta funciona sem atrito, você gasta energia em **arquitetura e patterns**, não em debug de incompatibilidades.

**Contras:**
- Não terá features de Java 22+ (ex: Gatherers, Stream improvements, Statements before super).
- Spring Boot 3.3.x não terá features do 4.x quando este for lançado.

### Opção B: Java 25 + Spring Boot 4.1.x-SNAPSHOT

**Prós:**
- Features mais recentes da linguagem.
- Compatibilidade futura com LangChain4j e outras libs de AI que podem exigir Java 22+.

**Contras:**
- **Java 25 nem existe no ambiente** — o mais novo disponível é JDK 24.
- **Spring Boot 4.x é SNAPSHOT** — breaking changes semanais, sem garantia de estabilidade.
- **Ferramentas quebram**: o build do projeto já falhou por causa de `UnsupportedClassVersionError` com Java 8 tentando carregar classes compiladas para 25.
- **Mercado não usa snapshots em produção.** Você aprende mais sobre problemas reais do mercado usando versões estáveis.
- **Documentação e Stack Overflow**: quando der um erro, haverá muito menos ajuda disponível para versões snapshot.

### Opção C: Quarkus + Java 21

**Prós:**
- Startup rápido, native image "out of the box".
- Crescendo no mercado.

**Contras:**
- Ecossistema menor que Spring.
- Curva de aprendizado adicional.
- O foco agora é **arquitetura**, não framework. Trocar de framework gera atrito.

## Decisão

**Java 21 + Spring Boot 3.3.x.**

O princípio que guia: **não lutar contra a ferramenta enquanto se aprende arquitetura.** A dor deve vir do design (como modelar agregados? como evitar N+1?), não da versão do framework.

## E o LangChain4j?

O LangChain4j 0.36+ roda em Java 17+. Java 21 é mais que suficiente. Quando (e se) uma versão futura exigir Java 22+, já teremos a opção de migrar — e essa migração será simples se a arquitetura estiver bem modularizada.

## Quando reconsiderar

- Quando Spring Boot 4.x for **GA** (não snapshot).
- Quando Java 25 for **LTS** (se for — o próximo LTS após 21 provavelmente será 25, previsto para setembro de 2025 → suporte em 2026+).
- Quando uma dependência crítica do Kora exigir Java 22+.

## Referências

- [Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- [Spring Boot Release Notes](https://github.com/spring-projects/spring-boot/wiki)
- Joshua Bloch — *Effective Java*, 3rd Edition: "Prefer standard, well-documented approaches"
