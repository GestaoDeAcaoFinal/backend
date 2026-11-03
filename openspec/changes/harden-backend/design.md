## Context

O backend é uma aplicação Java 17 com Spring Boot 4, Spring MVC, Spring Data JPA e H2 em memória. Resources expõem operações de corretoras, ações e negociação; serviços concentram regras de negócio; repositórios JPA persistem as entidades; clientes HTTP consultam BrasilAPI, ViaCEP, Brapi e Twelve Data.

Atualmente, DTOs e parâmetros não declaram validações, os serviços usam `RuntimeException`, clientes trabalham com `Map` cru e capturam erros de forma ampla, e uma credencial externa está em configuração versionada. A corretora é marcada como validada sem consulta real à CVM. A suíte contém apenas um teste de contexto e a persistência está configurada diretamente para H2. Consulte `proposal.md` para a motivação e `specs/*/spec.md` para os contratos observáveis.

## Goals / Non-Goals

**Goals:**

- Aplicar validação e normalização em uma fronteira anterior a banco e rede.
- Isolar falhas de domínio e de integração em tipos próprios, convertidos centralmente para o contrato HTTP especificado.
- Tornar clientes externos configuráveis, tipados, limitados por timeout e seguros em logs e respostas.
- Validar a corretora em fonte oficial da CVM sem baixar o conjunto de dados em cada cadastro.
- Preservar as regras funcionais existentes de negociação enquanto são protegidas por invariantes e testes.
- Tornar configuração, persistência, empacotamento e testes reproduzíveis sem credenciais ou rede reais.

**Non-Goals:**

- Adicionar frontend, autenticação ou autorização de usuários.
- Alterar o modelo de carteira ou introduzir custódia, taxas, impostos e liquidação.
- Fazer scraping de StatusInvest, Investidor10 ou qualquer página sem contrato público estável.
- Adotar Flyway ou manter o Hibernate como mecanismo de migração; o schema versionado será administrado pelo Liquibase.
- Revogar automaticamente a credencial já exposta; essa ação permanece responsabilidade do proprietário no provedor.

## Decisions

### 1. Validar na fronteira HTTP e normalizar antes do primeiro efeito colateral

DTOs receberão Bean Validation para formatos e obrigatoriedade. Resources usarão `@Valid` e `@Validated`; identificadores, quantidades e preços usarão restrições positivas. CNPJ, CEP e ticker serão convertidos para uma forma canônica antes de buscas de duplicidade, consultas externas ou persistência.

A alternativa de validar somente dentro dos serviços foi descartada porque permite trabalho desnecessário e distribui regras sintáticas por fluxos diferentes. Os serviços continuarão protegendo invariantes de domínio para chamadas que não venham de HTTP.

### 2. Centralizar erros conhecidos em exceções de domínio e `ProblemDetail`

Serão definidos tipos de exceção para entrada ou operação inválida, recurso ausente, conflito, dado externo inexistente, falha de integração e indisponibilidade. Um `@RestControllerAdvice` será o único responsável por convertê-los em `ProblemDetail` e acrescentar detalhes de campos inválidos.

O mapeamento adotado é `400` para entrada ou regra de operação inválida, `404` para recurso local ausente, `409` para duplicidade ou posição insuficiente, `422` para identificador bem formado ausente ou inativo na fonte autoritativa, `502` para erro HTTP ou conteúdo inválido de provedor e `503` para timeout ou indisponibilidade. Isso resolve a ambiguidade do documento original reservando `400` a falhas atribuíveis ao consumidor e `502` a respostas externas inconsistentes.

A alternativa de manter mensagens de `RuntimeException` nos resources foi descartada por misturar contrato HTTP e regra de negócio, além de produzir respostas instáveis.

### 3. Tratar cada provedor por um gateway tipado

Os clientes de BrasilAPI, ViaCEP, Brapi, Twelve Data e CVM receberão o `RestClient.Builder` configurado pela aplicação e criarão clientes com URL base própria. Tempos máximos de conexão e leitura serão definidos globalmente. Cada gateway terá tipos de resposta mínimos para os campos usados pelo domínio e converterá falhas de transporte, status e parsing para exceções de integração sem propagar credenciais, URL sensível ou payload bruto.

A alternativa de continuar usando `Map` foi descartada porque erros de nomes ou tipos de campos aparecem tarde e tendem a virar `500`. Criar um cliente HTTP inteiramente independente por integração também foi descartado para manter timeouts e observabilidade consistentes.

### 4. Carregar e indexar o cadastro oficial da CVM com política fail-closed

Um componente obterá `cad_intermed.zip`, localizará os CSVs relevantes, normalizará CNPJs de intermediários ativos e publicará um índice imutável em memória. O índice terá validade configurável, com padrão de 24 horas. Durante a validade, cadastros reutilizarão o índice; sem cache válido, qualquer falha de download ou parsing impedirá a validação e resultará em `503`.

A alternativa de consultar páginas ou fontes não oficiais foi rejeitada pela falta de contrato estável. Marcar a corretora como válida durante indisponibilidade também foi rejeitado porque tornaria `validadaNaCvm` não verificável.

### 5. Preservar a classificação de mercado e isolar sua regra

Para não ampliar o contrato de cadastro neste ciclo, a identificação atual será preservada: ticker contendo dígito será tratado como mercado brasileiro e ticker formado somente por letras como mercado dos Estados Unidos. A regra será isolada e coberta por testes para permitir substituição futura sem afetar o restante do serviço.

A alternativa de exigir um novo campo de mercado no DTO foi adiada porque alteraria o payload documentado e exigiria uma decisão de produto além do endurecimento. A limitação da heurística fica registrada nos riscos.

### 6. Reforçar invariantes de negociação sem alterar as fórmulas atuais

Compra exigirá ação existente, quantidade positiva e cotação positiva. Venda exigirá preço e quantidade positivos, ao menos uma compra anterior e saldo suficiente calculado por compras menos vendas. O preço médio continuará sendo a média ponderada das compras; o lucro/prejuízo realizado continuará sendo `(preço de venda - preço médio) × quantidade`, arredondado para duas casas.

A alternativa de introduzir lotes, FIFO ou recálculo de custo após vendas foi descartada por alterar o modelo de carteira fora do escopo.

### 7. Separar configuração por perfil e reforçar integridade no banco

O perfil padrão `h2` continuará autossuficiente para desenvolvimento e testes. Perfis `postgresql` e `mysql` obterão URL, usuário e senha do ambiente e terão drivers runtime explícitos. `ddl-auto` será externalizável e terá `update` como padrão somente em desenvolvimento.

A entidade de ação passará a usar a tabela `acoes`; ticker e CNPJ terão restrições únicas no schema. As verificações de serviço permanecerão para respostas amigáveis, enquanto as restrições impedirão duplicidades sob concorrência.

A alternativa de confiar apenas na consulta prévia do serviço foi descartada por condição de corrida. O Liquibase aplicará a renomeação de `produto` para `acoes`, recriará as chaves estrangeiras com nomes coerentes e adicionará as restrições únicas. As operações terão rollback explícito; backup e validação de dados continuam obrigatórios em bancos persistentes.

### 8. Testar sem rede e verificar o artefato empacotado

Serviços serão testados unitariamente com repositórios e gateways simulados. Testes MVC cobrirão validação, status e `ProblemDetail`. O parser da CVM usará um ZIP pequeno produzido pelo próprio teste. O teste de contexto usará H2 e credencial fictícia. A verificação final executará a suíte e o empacotamento pelo Maven Wrapper.

Testes rotineiros contra APIs reais foram descartados por serem lentos, instáveis e dependentes de credenciais. Integrações reais poderão ser exercitadas manualmente por meio das instruções operacionais.

## Risks / Trade-offs

- [A CVM pode alterar nomes, codificação ou estrutura dos CSVs] → Isolar o parser, validar colunas obrigatórias, cobrir ZIPs representativos e converter formato inesperado em falha de integração.
- [O cache da CVM pode ficar desatualizado durante sua validade] → Tornar o TTL configurável, usar 24 horas como padrão coerente com a publicação diária e nunca estender silenciosamente cache expirado.
- [A heurística baseada em dígitos pode classificar tickers incomuns de forma incorreta] → Isolar e testar a regra atual; uma futura mudança poderá adicionar mercado explícito ou resolução por provedor.
- [A renomeação de `produto` para `acoes` pode ocultar dados existentes se o Hibernate apenas criar uma nova tabela] → Exigir backup e renomeação explícita em bancos persistentes antes do rollout; H2 em memória pode ser recriado.
- [`ddl-auto=update` pode divergir do schema versionado] → Usar Liquibase como fonte do schema e `ddl-auto=validate` fora do desenvolvimento; manter `update` apenas no perfil local quando explicitamente necessário.
- [Dependência de múltiplos provedores aumenta a chance de falha no cadastro] → Aplicar timeouts, evitar persistência parcial, distinguir `502` de `503` e reutilizar somente o cache CVM ainda válido.
- [A padronização de erros pode quebrar consumidores que dependem de mensagens ou status atuais] → Documentar a alteração como breaking, publicar a matriz de status e cobri-la por testes MVC.

## Migration Plan

1. Revogar a chave externa exposta e provisionar um novo valor por `TWELVE_DATA_API_KEY` antes de executar qualquer ambiente compartilhado.
2. Introduzir configuração externalizada, perfis de banco, exceções de domínio, validação e o handler de `ProblemDetail`, mantendo H2 como caminho de desenvolvimento.
3. Migrar clientes externos para gateways tipados com timeouts e implementar o carregador/cache da CVM.
4. Aplicar as invariantes de corretoras, ações e operações e adicionar as restrições únicas no schema.
5. Em H2 descartável, recriar o schema. Em PostgreSQL ou MySQL com dados, realizar backup, executar o changeset Liquibase que renomeia `produto` para `acoes` e validar contagem, constraints e chaves após a mudança.
6. Adicionar testes, Dockerfile, Compose, `.env.example` e documentação operacional; executar `mvnw test` e `mvnw package` sem rede.
7. Para rollback, restaurar a versão anterior da aplicação e, se houver banco persistente, reverter a renomeação da tabela a partir do backup ou de operação SQL controlada. A credencial revogada não deve ser restaurada.
