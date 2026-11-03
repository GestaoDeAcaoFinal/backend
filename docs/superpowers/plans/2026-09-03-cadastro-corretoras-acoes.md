# Cadastro de corretoras e ações Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** concluir as tarefas 4.1 a 4.6, garantindo validação real de corretoras na CVM e fluxos de ações completos, positivos e atômicos.

**Architecture:** `CorretoraService` passa a depender do cache CVM já implementado e só persiste após enriquecimento e confirmação ativa. A classificação BR/EUA sai de `AcaoService` para um componente puro; o serviço calcula e valida todos os novos valores antes de mutar e salvar a entidade.

**Tech Stack:** Java 17, Spring Boot 4.0.6, Spring MVC, Spring Data JPA, JUnit 5, Mockito e MockMvc.

**Spec:** `openspec/changes/harden-backend/specs/broker-cvm-validation/spec.md`, `openspec/changes/harden-backend/specs/stock-market-data/spec.md` e `openspec/changes/harden-backend/design.md`.

## Global Constraints

- CNPJ, CEP e ticker devem ser normalizados antes de banco, rede ou persistência.
- Intermediário ausente/inativo na CVM deve produzir `422`; indisponibilidade sem cache válido deve produzir `503`.
- Ticker contendo dígito é `BR`; ticker somente com letras é `EUA`.
- Nome, moeda e cotação estritamente positiva são obrigatórios antes de persistir ações.
- Testes não podem acessar provedores reais.

---

### Task 1: Cadastro e consultas de corretoras

**Files:**
- Modify: `src/main/java/com/av2/gestaodeacao/services/CorretoraService.java`
- Create: `src/test/java/com/av2/gestaodeacao/services/CorretoraServiceTest.java`
- Create: `src/test/java/com/av2/gestaodeacao/resources/CorretoraResourceMvcTest.java`

**Interfaces:**
- Consumes: `CadastroCvmCache.intermediarioAtivo(String): boolean`
- Produces: cadastro persistido somente quando CVM retorna `true`; buscas por ID/CNPJ com `RecursoNaoEncontradoException`.

- [ ] Escrever testes de cadastro ativo, duplicado, CEP/CNPJ ausente, CVM inativa e CVM indisponível; em toda rejeição verificar `repository.save` nunca chamado.
- [ ] Executar `mvn -Dtest=CorretoraServiceTest test` e confirmar RED por ausência da dependência CVM no serviço.
- [ ] Injetar `CadastroCvmCache`, consultar o CNPJ já normalizado e lançar `DadoExternoNaoEncontradoException` quando não estiver ativo.
- [ ] Escrever e executar testes MVC para listagem, busca por ID/CNPJ e respostas `404`/`409`.
- [ ] Marcar 4.1 e 4.2 após os testes focados passarem.

### Task 2: Classificação e invariantes de ações

**Files:**
- Create: `src/main/java/com/av2/gestaodeacao/services/MercadoTickerClassifier.java`
- Modify: `src/main/java/com/av2/gestaodeacao/services/AcaoService.java`
- Modify: `src/main/java/com/av2/gestaodeacao/cliente/AcaoCliente.java`
- Modify: `src/main/java/com/av2/gestaodeacao/cliente/TwelveDataCliente.java`
- Create: `src/test/java/com/av2/gestaodeacao/services/MercadoTickerClassifierTest.java`
- Create: `src/test/java/com/av2/gestaodeacao/services/AcaoServiceTest.java`

**Interfaces:**
- Produces: `MercadoTickerClassifier.classificar(String): String`, retornando `BR` ou `EUA`.
- Consumes: `BrapiResponse`, `BrapiQuoteResponse` e `TwelveDataResponse` tipados.

- [ ] Escrever testes para `PETR4`/`B3SA3` como BR e `AAPL` como EUA, mais seleção exclusiva do gateway correspondente.
- [ ] Escrever testes de cadastro BR/EUA, corretora ausente, duplicidade, ticker inexistente e cotação zero/negativa.
- [ ] Escrever testes de atualização bem-sucedida e falhas `502`/`503` preservando cotação e timestamp, além de ação ausente.
- [ ] Executar os testes e confirmar RED por classificador ausente, cotação não positiva aceita ou estado alterado.
- [ ] Implementar o classificador, validar cotação antes de qualquer mutação e realizar um único `repository.save` no sucesso.
- [ ] Reexecutar testes e marcar 4.3, 4.4 e 4.5 após GREEN.

### Task 3: Consultas MVC de ações e verificação

**Files:**
- Create: `src/test/java/com/av2/gestaodeacao/resources/AcaoResourceMvcTest.java`
- Modify: `openspec/changes/harden-backend/tasks.md`

**Interfaces:**
- Consumes: `AcaoResource` e `AcaoService` existentes.
- Produces: cobertura MVC para lista, ID, ticker e `404`.

- [ ] Escrever testes MockMvc de listagem, busca por ID, encaminhamento do ticker e ausências `404`.
- [ ] Executar `mvn -Dtest=AcaoResourceMvcTest test` e confirmar os contratos.
- [ ] Executar todos os testes focados do bloco e depois `mvn test`.
- [ ] Executar `git diff --check`, revisar os arquivos do bloco e marcar 4.6.
