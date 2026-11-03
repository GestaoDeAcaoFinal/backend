# Gateways externos e CVM — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** concluir as tarefas 3.1 a 3.5 do change `harden-backend`, tipando as integrações HTTP e adicionando download, leitura e cache fail-closed do cadastro oficial de intermediários da CVM.

**Architecture:** os clientes HTTP convertem respostas externas em records internos e delegam a tradução segura de falhas a um executor comum. O cadastro CVM fica isolado em um gateway de bytes, um parser ZIP/CSV e um cache imutável com relógio injetável; nenhum serviço de negócio passa a depender da CVM antes da tarefa 4.1.

**Tech Stack:** Java 17, Spring Boot 4.0.6, Spring RestClient, Jackson, Apache Commons CSV, JUnit 5, MockRestServiceServer e Mockito.

**Spec:** `openspec/changes/harden-backend/design.md`, `openspec/changes/harden-backend/specs/external-integrations/spec.md` e `openspec/changes/harden-backend/tasks.md`.

## Restrições globais

- Não fazer chamadas reais de rede nos testes.
- Não expor URL, chave, corpo ou detalhes internos nas mensagens de erro públicas.
- Considerar ativo somente `SIT = EM FUNCIONAMENTO NORMAL`.
- Ler `cad_intermed.csv` dentro de `cad_intermed.zip`, delimitado por `;` e codificado em Windows-1252.
- Cache vencido nunca pode ser usado como fallback quando a atualização falhar.

### Task 1: Contratos tipados e política uniforme de erro HTTP

**Files:**
- Create: `src/main/java/com/av2/gestaodeacao/cliente/dto/BrasilApiCnpjResponse.java`
- Create: `src/main/java/com/av2/gestaodeacao/cliente/dto/ViaCepResponse.java`
- Create: `src/main/java/com/av2/gestaodeacao/cliente/dto/BrapiResponse.java`
- Create: `src/main/java/com/av2/gestaodeacao/cliente/dto/BrapiQuoteResponse.java`
- Create: `src/main/java/com/av2/gestaodeacao/cliente/dto/TwelveDataResponse.java`
- Create: `src/main/java/com/av2/gestaodeacao/cliente/support/ProvedorHttpExecutor.java`
- Modify: `src/main/java/com/av2/gestaodeacao/cliente/{AcaoCliente,CnpjCliente,ViaCepCliente,TwelveDataCliente}.java`
- Modify: `src/main/java/com/av2/gestaodeacao/services/{AcaoService,CorretoraService}.java`
- Modify: `src/main/resources/application.properties`
- Create: `src/test/java/com/av2/gestaodeacao/cliente/ClientesExternosTest.java`
- Modify: `src/test/java/com/av2/gestaodeacao/services/ServiceExceptionFlowTest.java`

1. Escrever testes de parsing tipado para respostas válidas, campo obrigatório ausente e tipo incompatível.
2. Escrever testes HTTP simulados para sucesso, 404, erro de status, conteúdo inválido e timeout.
3. Executar `mvn -Dtest=ClientesExternosTest,ServiceExceptionFlowTest test` e confirmar RED.
4. Criar records Jackson e `ProvedorHttpExecutor.executar(String, Supplier<T>)` que preserve exceções de domínio, converta transporte em `ProvedorIndisponivelException` e demais falhas externas em `FalhaDeProvedorException`.
5. Configurar cada cliente com seu próprio `base-url`, validar os campos necessários e devolver somente tipos concretos.
6. Adaptar os serviços aos records, sem adicionar ainda consulta CVM ao fluxo de corretoras.
7. Reexecutar os testes e marcar 3.1 e 3.2 somente após GREEN.

### Task 2: Download e parser do cadastro oficial CVM

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/av2/gestaodeacao/cliente/cvm/CadastroCvmGateway.java`
- Create: `src/main/java/com/av2/gestaodeacao/cliente/cvm/CvmCliente.java`
- Create: `src/main/java/com/av2/gestaodeacao/services/cvm/CadastroCvmParser.java`
- Create: `src/test/java/com/av2/gestaodeacao/cliente/cvm/CvmClienteTest.java`
- Create: `src/test/java/com/av2/gestaodeacao/services/cvm/CadastroCvmParserTest.java`

1. Criar fixtures ZIP em memória com `cad_intermed.csv` em Windows-1252, incluindo registros ativo, inativo, ausente e malformado.
2. Escrever testes do gateway usando `MockRestServiceServer`, sem internet.
3. Executar `mvn -Dtest=CvmClienteTest,CadastroCvmParserTest test` e confirmar RED.
4. Adicionar Apache Commons CSV e implementar `CadastroCvmGateway.baixarCadastro()` no `CvmCliente`.
5. Implementar `CadastroCvmParser.parse(byte[])`, validar ZIP, arquivo, cabeçalhos `CNPJ`/`SIT` e retornar `Set.copyOf(...)` apenas com CNPJs ativos normalizados.
6. Reexecutar os testes e marcar 3.3 e 3.4 somente após GREEN.

### Task 3: Cache imutável com TTL e verificação do bloco

**Files:**
- Create: `src/main/java/com/av2/gestaodeacao/services/cvm/CadastroCvmCache.java`
- Create: `src/test/java/com/av2/gestaodeacao/services/cvm/CadastroCvmCacheTest.java`
- Modify: `src/test/java/com/av2/gestaodeacao/HttpClientConfigurationTests.java`
- Modify: `openspec/changes/harden-backend/tasks.md`

1. Escrever testes com relógio controlado para reutilização antes do TTL, atualização no vencimento, troca atômica do conjunto e falha 503 quando não há cache válido.
2. Executar `mvn -Dtest=CadastroCvmCacheTest test` e confirmar RED.
3. Implementar snapshot imutável contendo `Set<String>` e instante de carga, TTL configurável com padrão `24h`, atualização sincronizada e política fail-closed.
4. Incluir o gateway CVM no teste de configuração compartilhada de `RestClient.Builder`.
5. Executar os testes focados do bloco e depois `mvn test`.
6. Marcar 3.5 e confirmar que 3.1–3.5 estão concluídas no OpenSpec.
