# Composite Pattern para o Page Builder de Comunidades

> Artigo de design pattern — Kora Project
> Data: 2026-03-21

---

## O Problema

Cada comunidade no Kora pode ter sua **página customizável**, como um draw.io compartilhado:
- Todos vêem a página
- Só admins editam
- A página é composta de **componentes aninhados** (seções, colunas, textos, imagens, widgets)

Precisamos de uma estrutura de dados que represente essa **árvore de componentes**.

## O Pattern: Composite

### Classificação
- **Tipo**: Estrutural (GoF)
- **Intenção**: Compor objetos em estruturas de árvore para representar hierarquias parte-todo. O Composite permite que clientes tratem objetos individuais e composições de objetos de maneira uniforme.

### Diagrama

```
         ┌──────────────┐
         │  PageElement  │ (Component)
         │               │
         │ + render()    │
         │ + toJson()    │
         │ + validate()  │
         └──────┬───────┘
                │
       ┌────────┴────────┐
       │                 │
┌──────▼──────┐   ┌─────▼──────────┐
│   Leaf      │   │  Container     │ (Composite)
│             │   │                │
│ TextBlock   │   │ + children[]   │
│ ImageBlock  │   │ + add(element) │
│ VideoBlock  │   │ + remove(el)   │
│ ButtonBlock │   │                │
└─────────────┘   │ Section        │
                  │ Column         │
                  │ Row            │
                  │ Grid           │
                  └────────────────┘
```

### Como funciona no Kora

Uma página de comunidade é uma **árvore**:

```
CommunityPage (root)
├── Section "Header"
│   ├── ImageBlock (banner)
│   └── TextBlock (nome da comunidade)
├── Section "Content"
│   ├── Row
│   │   ├── Column (2/3)
│   │   │   ├── TextBlock (descrição)
│   │   │   └── VideoBlock (vídeo de apresentação)
│   │   └── Column (1/3)
│   │       ├── ButtonBlock (link para loja)
│   │       └── ButtonBlock (link para eventos)
│   └── Grid "Posts recentes"
│       ├── PostCard
│       ├── PostCard
│       └── PostCard
└── Section "Footer"
    └── TextBlock (copyright)
```

### Implementação em Java

```java
// Component (interface base)
public sealed interface PageElement permits Container, LeafElement {
    String getId();
    String getType();
    Map<String, Object> getProps();
    JsonNode toJson();
    ValidationResult validate();
}

// Composite (contém filhos)
public non-sealed class Container implements PageElement {
    private final String id;
    private final String type; // "section", "row", "column", "grid"
    private final Map<String, Object> props;
    private final List<PageElement> children;

    public void addChild(PageElement element) { ... }
    public void removeChild(String elementId) { ... }
    public Optional<PageElement> findById(String id) { ... }

    @Override
    public JsonNode toJson() {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", id);
        node.put("type", type);
        node.set("props", mapper.valueToTree(props));
        ArrayNode childrenNode = node.putArray("children");
        children.forEach(child -> childrenNode.add(child.toJson()));
        return node;
    }

    @Override
    public ValidationResult validate() {
        // Valida a si mesmo + delega validação para cada filho
        List<String> errors = new ArrayList<>();
        // ... validações do container ...
        children.forEach(child -> {
            ValidationResult childResult = child.validate();
            errors.addAll(childResult.getErrors());
        });
        return new ValidationResult(errors);
    }
}

// Leaf (não tem filhos)
public non-sealed class LeafElement implements PageElement {
    private final String id;
    private final String type; // "text", "image", "video", "button"
    private final Map<String, Object> props;

    @Override
    public JsonNode toJson() { ... }

    @Override
    public ValidationResult validate() {
        // Valida só a si mesmo (ex: URL de imagem válida, texto não vazio)
    }
}
```

### Persistência (no banco)

Duas opções:

**Opção A: JSON (PostgreSQL JSONB)**
```sql
CREATE TABLE community.pages (
    id UUID PRIMARY KEY,
    community_id UUID REFERENCES community.communities(id),
    content JSONB NOT NULL,  -- árvore inteira como JSON
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL,
    updated_by UUID NOT NULL
);
```

**Opção B: Tabela hierárquica (Adjacency List)**
```sql
CREATE TABLE community.page_elements (
    id UUID PRIMARY KEY,
    page_id UUID REFERENCES community.pages(id),
    parent_id UUID REFERENCES community.page_elements(id),
    type VARCHAR(50) NOT NULL,
    props JSONB,
    sort_order INT NOT NULL
);
```

**Recomendação para o Kora:** Opção A (JSONB). A página inteira é carregada e salva de uma vez. Menos queries, mais simples. A Opção B faz sentido quando você precisa consultar elementos individuais com frequência.

### No Frontend (Angular)

O frontend renderiza essa árvore recursivamente:

```typescript
@Component({
  selector: 'kora-page-renderer',
  template: `
    @for (element of elements; track element.id) {
      @switch (element.type) {
        @case ('section') { <kora-section [element]="element" /> }
        @case ('row') { <kora-row [element]="element" /> }
        @case ('text') { <kora-text-block [props]="element.props" /> }
        @case ('image') { <kora-image-block [props]="element.props" /> }
      }
    }
  `
})
export class PageRendererComponent {
  @Input() elements: PageElement[] = [];
}
```

Cada container renderiza seus filhos chamando `<kora-page-renderer>` recursivamente.

### Segurança: Sanitização

A árvore de componentes é **sanitizada no backend** antes de persistir:

1. **Whitelist de types**: só aceitar types conhecidos ("section", "row", "text", "image", etc.)
2. **Whitelist de props**: cada type tem um schema de props permitidas
3. **Sanitizar textos**: usar OWASP Java HTML Sanitizer para qualquer HTML em `TextBlock`
4. **Validar URLs**: imagens e vídeos só de domínios permitidos (ou uploads internos)
5. **Limitar profundidade**: máximo 5 níveis de aninhamento (previne abuse)
6. **Limitar tamanho**: máximo de elementos por página (ex: 200)

### Trade-offs

| Aspecto | Prós | Contras |
|---------|------|---------|
| Composite Pattern | Hierarquia natural, render recursivo simples | Complexidade ao validar/sanitizar recursivamente |
| JSONB no Postgres | 1 query para carregar, fácil versionamento | Não indexa elementos internos facilmente |
| Sealed interface | Type safety, exhaustive switch | Mais rígido (adicionar novo type = mudar interface) |

---

## Referências

- GoF — *Design Patterns*: Composite
- Joshua Bloch — *Effective Java*: Item 17 (prefer sealed hierarchies)
