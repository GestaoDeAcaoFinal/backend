## 1. Segurança e configuração base

- [x] 1.1 Revogar no provedor a chave da Twelve Data exposta, provisionar uma substituta fora do repositório e verificar que a chave antiga não autentica mais.
- [x] 1.2 Substituir a configuração versionada pela variável `TWELVE_DATA_API_KEY`, adicionar `.env.example` fictício e ignorar `.env`; verificar por busca no repositório que nenhum segredo real permanece em arquivos rastreáveis.
- [x] 1.3 Separar propriedades para os perfis H2, PostgreSQL e MySQL usando `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, adicionar o driver runtime de MySQL e verificar que cada perfil carrega a configuração esperada em teste de contexto.
- [x] 1.4 Tornar `ddl-auto` configurável e manter `update` somente como padrão de desenvolvimento; verificar por teste de propriedades que perfis externos aceitam sobrescrita explícita.
- [x] 1.5 Configurar `RestClient.Builder` com timeouts globais de conexão e leitura e verificar por teste de configuração que todos os gateways recebem os valores definidos.


## 2. Contrato HTTP, validação e erros

- [x] 2.1 Criar exceções de domínio para entrada/operação inválida, recurso ausente, conflito, dado externo inexistente, falha de provedor e indisponibilidade; verificar por testes unitários que cada fluxo lança o tipo específico.
- [x] 2.2 Implementar o `@RestControllerAdvice` que produz `ProblemDetail` e detalhes de campos inválidos; verificar com testes MVC o content type e os campos estáveis do corpo.
- [x] 2.3 Adicionar Bean Validation aos DTOs de ação e corretora para ticker, corretora, CNPJ e CEP; verificar que payloads inválidos retornam `400` sem invocar serviços.
- [x] 2.4 Adicionar `@Validated` e restrições positivas aos parâmetros de ações e operações; verificar que ids, quantidades e preços não positivos retornam `400` antes de persistência ou rede.
- [x] 2.5 Implementar normalizadores reutilizáveis de ticker, CNPJ e CEP e verificar por testes unitários os formatos com e sem pontuação, espaços, caixa e comprimentos inválidos.
- [x] 2.6 Cobrir a matriz de status `400`, `404`, `409`, `422`, `502` e `503` em testes MVC, incluindo verificação de que chaves, credenciais, URLs sensíveis e payloads externos não aparecem na resposta.

## 3. Gateways externos e CVM

- [x] 3.1 Criar tipos mínimos de resposta para BrasilAPI, ViaCEP, Brapi e Twelve Data e remover `Map` cru dos serviços; verificar que testes de parsing cobrem campos obrigatórios ausentes e tipos inválidos.
- [x] 3.2 Refatorar os quatro clientes existentes para usar o `RestClient.Builder` injetado, URL base própria e tradução uniforme de erros; verificar com servidor HTTP simulado os casos de sucesso, erro de status, conteúdo inválido e timeout sem acesso à internet.
- [x] 3.3 Implementar o gateway que obtém o conjunto oficial `cad_intermed.zip` da CVM e verificar com fixture local que o ZIP e os CSVs são lidos sem rede.
- [x] 3.4 Implementar o parser que normaliza e indexa apenas CNPJs de intermediários ativos, validando colunas e codificação; verificar fixtures com participante ativo, inativo, ausente e arquivo malformado.
- [x] 3.5 Implementar cache imutável da CVM com TTL configurável de 24 horas e política fail-closed; verificar por teste com relógio controlado reutilização do cache, expiração, atualização e `503` sem cache válido.

## 4. Cadastro de corretoras e ações

- [x] 4.1 Atualizar o cadastro de corretora para normalizar antes da duplicidade, enriquecer por CEP/CNPJ e validar na CVM antes de persistir; verificar por teste unitário que cada rejeição evita gravação parcial.
- [x] 4.2 Atualizar buscas de corretora para usar CNPJ normalizado e exceção de recurso ausente; verificar os cenários de busca, duplicidade `409` e ausência `404` em testes de serviço e MVC.
- [x] 4.3 Isolar e testar a regra atual de classificação de mercado por ticker, mantendo dígito como `BR` e somente letras como `EUA`; verificar a seleção do gateway correto para ambos os mercados.
- [x] 4.4 Atualizar o cadastro de ação para validar corretora, normalizar ticker, rejeitar duplicidade e persistir somente dados de mercado completos com cotação positiva; verificar sucessos BR/EUA e ticker inexistente `422` em testes unitários.
- [x] 4.5 Tornar a atualização de cotação atômica, preservando cotação e timestamp anteriores quando o provedor falha; verificar sucesso, `502`, `503` e ausência `404` sem chamadas externas reais.
- [x] 4.6 Cobrir listagem e buscas de ações por id e ticker normalizado em testes MVC, verificando resposta de sucesso e `404`.

## 5. Operações de compra e venda

- [x] 5.1 Validar ação existente, quantidade positiva e cotação positiva antes de comprar; verificar que compra válida registra preço unitário, valor total, tipo e timestamp e que entradas inválidas não são persistidas.
- [x] 5.2 Preservar o cálculo de preço médio ponderado das compras e verificar por testes a primeira compra, compras adicionais e o fato de vendas não alterarem o custo médio.
- [x] 5.3 Validar preço, quantidade, existência de compra e posição disponível antes de vender; verificar `400` sem posição e `409` quando a venda excede o saldo.
- [x] 5.4 Preservar o cálculo de lucro/prejuízo com arredondamento em duas casas e verificar por testes vendas com ganho, perda e valor nulo.
- [x] 5.5 Cobrir listagem total, histórico por ação, filtros de compra/venda e busca por id, verificando resultados e `404` para ação ou operação ausente.

## 6. Persistência e migração de schema

- [x] 6.1 Renomear o mapeamento da entidade de ação para `acoes` e adicionar, por changeset Liquibase com rollback, restrições únicas para ticker e CNPJ; verificar no schema H2 gerado os nomes de tabela, constraints e chaves estrangeiras.
- [x] 6.2 Traduzir violações concorrentes de unicidade do banco para `409 Conflict`; verificar com teste de persistência que a segunda gravação duplicada falha de forma estável.
- [x] 6.3 Documentar comandos de backup, aplicação da renomeação `produto` → `acoes`, validação de contagem/chaves e rollback para PostgreSQL e MySQL; verificar o procedimento em bancos descartáveis iniciados pelo Compose da tarefa 7.2.

## 7. Empacotamento e documentação operacional

- [x] 7.1 Criar Dockerfile multiestágio para empacotar e executar a aplicação sem incluir segredos; verificar que a imagem é construída e inicia com configuração fictícia apropriada.
- [x] 7.2 Criar Compose com PostgreSQL e MySQL como serviços/perfis opcionais e verificar healthchecks e conexão da aplicação com cada perfil separadamente.
- [x] 7.3 Atualizar o README com requisitos, perfis, variáveis, execução H2 sem Docker, execução com Compose, endpoints de corretoras/ações/operações, matriz `ProblemDetail`, fontes externas e procedimento de migração; verificar todos os comandos documentados em ambiente limpo.

## 8. Verificação final

- [x] 8.1 Atualizar o teste de contexto para H2 e credencial fictícia, bloquear chamadas reais e verificar que a suíte completa executa sem internet, Docker ou segredos.
- [x] 8.2 Executar `./mvnw test` e verificar que todos os testes unitários, MVC, persistência, clientes e parser CVM passam.
- [x] 8.3 Executar `./mvnw package` e verificar que o artefato executável é gerado com sucesso.
- [x] 8.4 Executar varredura final por padrões de segredo e conferir `git diff`; verificar que somente alterações previstas estão presentes e que nenhum valor sensível foi introduzido.
