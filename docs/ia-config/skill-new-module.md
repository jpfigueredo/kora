# /new-module — Criar Novo Módulo Maven (Kora)

Crie um novo módulo Maven seguindo a estrutura padrão do Kora.

## Parâmetros
- `$ARGUMENTS` — nome do módulo (ex: `kora-identity`, `kora-content`)

## Estrutura a criar

```
kora/backend/<module-name>/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/org/gustalab/kora/<context>/
    │   │   ├── api/                 # Controllers, Request/Response DTOs
    │   │   ├── application/         # Use Cases (@Service)
    │   │   ├── domain/              # Entities, Value Objects, Repository interfaces
    │   │   │   └── exception/       # Domain exceptions
    │   │   ├── infrastructure/      # JPA, adapters
    │   │   └── event/               # Domain Events
    │   └── resources/
    │       └── db/migration/        # Flyway migrations para este contexto
    └── test/
        └── java/org/gustalab/kora/<context>/
```

## pom.xml filho (template)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.gustalab</groupId>
        <artifactId>kora</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>MODULE_NAME</artifactId>
    <description>CONTEXT_NAME Context</description>

    <dependencies>
        <dependency>
            <groupId>org.gustalab</groupId>
            <artifactId>kora-common</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

## Após criar

1. Adicionar `<module>MODULE_NAME</module>` no parent pom.xml
2. Adicionar dependência no `kora-api/pom.xml` (assembler)
3. Criar pelo menos 1 classe em cada pacote para validar a estrutura
4. Rodar `mvn clean compile` para verificar
