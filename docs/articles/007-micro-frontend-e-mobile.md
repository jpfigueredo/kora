# Micro-Frontend e Mobile: Quando e Como?

> Artigo de decisão — Kora Project
> Data: 2026-03-21

---

## Parte 1: Micro-Frontend

### O que é?

Micro-frontend é a extensão de microserviços para o frontend. Cada equipe é dona de uma "fatia vertical" — do banco ao botão na tela.

```
┌─────────────────────────────────────────┐
│              App Shell (Host)            │
│                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐│
│  │ Identity │ │ Content  │ │Community ││
│  │ (Angular)│ │ (React)  │ │  (Vue)   ││
│  └──────────┘ └──────────┘ └──────────┘│
└─────────────────────────────────────────┘
```

**Tecnologia:** Module Federation (Webpack 5 / Vite) permite carregar módulos de diferentes builds em runtime.

### Prós

- Cada equipe escolhe seu framework
- Deploy independente de cada micro-frontend
- Escala de times (10 devs front podem trabalhar sem conflito)

### Contras

- **Complexidade brutal**: shared dependencies, versionamento, routing entre micro-frontends
- **Performance**: carregar Angular + React + Vue na mesma página = bundle enorme
- **UX inconsistente**: cada framework tem seu padrão de componentes, animações, etc.
- **1 desenvolvedor**: não há benefício. Toda a complexidade, nenhum ganho de paralelismo.

### Decisão para o Kora

**Não agora. Talvez nunca.**

O benefício de micro-frontend é para **equipes grandes** (5+ devs frontend) que precisam de autonomia. Para 1 desenvolvedor:

- Usar **Angular** para tudo (prioridade Aubay)
- Se quiser aprender React: fazer um **projeto separado** (ex: landing page, ou um app mobile companion)
- Se quiser demonstrar habilidade multi-framework: fazer o mesmo componente em Angular, React e Vue como exercício isolado, não misturado no Kora

### Quando reconsiderar

- Se o Kora tiver 3+ devs frontend
- Se uma feature específica for **significativamente** melhor em outro framework
- Se quiser usar micro-frontend como **exercício de aprendizado isolado** (aí faz um POC separado)

---

## Parte 2: Mobile (React Native)

### Por que React Native?

| Framework | Linguagem | Prós | Contras |
|-----------|-----------|------|---------|
| **React Native** | TypeScript/JavaScript | Compartilha lógica com web, comunidade enorme | Performance não-nativa em cenários pesados |
| **Flutter** | Dart | UI consistente, performance boa | Linguagem nova (Dart), menos libs |
| **Kotlin Multiplatform** | Kotlin | Compartilha lógica com backend, nativo real | Ecossistema menor, UI separada por plataforma |
| **Angular + Ionic/Capacitor** | TypeScript | Compartilha com o frontend Angular | Performance inferior, UX web-like |

### Trade-offs de adicionar mobile

**Prós de ter mobile:**
- Portfolio mais completo (web + mobile)
- Aprende React (adicional ao Angular)
- BFF pattern faz sentido real (mobile precisa de dados diferentes que web)
- Notificações push nativas

**Contras de fazer mobile agora:**
- Dobra o trabalho de frontend
- React Native é um ecossistema diferente (navigation, state, styling)
- Precisa de BFF ou GraphQL para não sobrecarregar a API
- Testes em 2 plataformas (Android + iOS — iOS precisa de Mac)

### Decisão para o Kora

**Sim, mas como Fase 15+ (nível 2 de maturidade).**

Ordem sugerida:
1. Angular web (Fases 4-7) ← prioridade
2. Mobile React Native (Fase 15+) ← depois do web estar sólido
3. BFF pattern quando tiver mobile (adaptar API para necessidades mobile)

### Quando o BFF faz sentido?

O BFF (Backend For Frontend) se justifica quando web e mobile precisam de **dados diferentes para a mesma tela**:

```
Web Feed:                          Mobile Feed:
- Post completo                    - Post resumido (140 chars)
- 10 comentários                   - 3 comentários
- Imagem full-size                 - Thumbnail
- Sidebar com sugestões            - Sem sidebar

Sem BFF:
  Web    → GET /api/feed?full=true
  Mobile → GET /api/feed?compact=true
  (lógica de formatação no backend, fica bagunçado)

Com BFF:
  Web    → GET /bff-web/feed    (retorna formato ideal para web)
  Mobile → GET /bff-mobile/feed (retorna formato ideal para mobile)
```

No monólito modular, o BFF é simplesmente **controllers diferentes**:
- `WebFeedController` (retorna dados para web)
- `MobileFeedController` (retorna dados para mobile)

Sem precisar de serviço separado.

---

## Resumo

| Decisão | Agora | Futuro |
|---------|-------|--------|
| Micro-frontend | Não | POC separado se quiser aprender Module Federation |
| Mobile (React Native) | Não | Fase 15+ (depois do web estar sólido) |
| BFF | Não | Quando tiver mobile |
| Multi-framework (Angular + React + Vue) | Não no Kora | Exercícios isolados separados |

## Referências

- Martin Fowler — [Micro Frontends](https://martinfowler.com/articles/micro-frontends.html)
- Sam Newman — *Building Microservices*: BFF Pattern
- React Native docs — para quando chegar a hora
