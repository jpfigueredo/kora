# /review — Code Review do Kora

Execute este checklist ao revisar código do projeto Kora.

## Arquitetura

- [ ] Controller é fino? (recebe → delega → retorna)
- [ ] Use Case não acessa infra diretamente?
- [ ] Domain não importa Spring/JPA?
- [ ] JPA Entity separada da Domain Entity?
- [ ] Comunicação entre módulos é via interface (port) ou evento?
- [ ] Sem dependência circular entre módulos?
- [ ] Sem acesso direto a repositório de outro módulo?

## Qualidade de Código

- [ ] Value Objects são `record` e validam no construtor?
- [ ] Factory method estático em vez de construtor público?
- [ ] Exceções de domínio (não genéricas)?
- [ ] Logging com `{}` placeholder (nunca concatenação/StringBuilder)?
- [ ] Sem try/catch no controller?
- [ ] Sem ResponseEntity no service?
- [ ] Sem String.format para construir JSON?
- [ ] Sem config de infra no construtor do service?
- [ ] 1 Use Case = 1 classe (SRP)?

## Performance

- [ ] Queries de listagem não fazem N+1?
- [ ] Paginação implementada?
- [ ] Sem carregamento EAGER desnecessário?
- [ ] Sem query dentro de loop?

## Segurança

- [ ] Inputs validados com `@Valid`?
- [ ] Sem SQL dinâmico com concatenação?
- [ ] Sem exposição de stack trace para o cliente?
- [ ] Senhas com BCrypt?
- [ ] JWT com expiração curta?

## Testes

- [ ] Testes unitários para domain/ (Value Objects, Entities)?
- [ ] Testes de integração para api/ (Testcontainers)?
- [ ] Naming: `methodUnderTest_scenario_expectedBehavior`?

## Documentação

- [ ] Decisão arquitetural documentada em ADR?
- [ ] Pattern aplicado documentado no commit message?
- [ ] Artigo criado se conceito novo foi introduzido?

## Referência completa
`docs/articles/011-padroes-implementacao-kora.md`
