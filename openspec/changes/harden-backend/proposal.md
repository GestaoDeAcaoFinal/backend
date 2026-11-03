## Why

A API atualmente depende de validações e integrações externas sem contratos suficientemente previsíveis, ainda contém configuração sensível no código e não possui cobertura automatizada que proteja as regras centrais. O endurecimento é necessário antes de versionar e distribuir o backend, para que consumidores HTTP recebam respostas estáveis e a aplicação possa operar com segurança em diferentes bancos e ambientes.

## What Changes

- Externalizar a chave da Twelve Data e todas as credenciais de banco, mantendo somente exemplos fictícios no repositório.
- Validar e normalizar ticker, CNPJ, CEP, identificadores e quantidades antes de acessar persistência ou provedores externos.
- **BREAKING**: padronizar respostas de erro como `ProblemDetail` e alinhar códigos HTTP a erros de validação, ausência, conflito, dados externos inválidos e indisponibilidade de provedores.
- Tornar os clientes externos resilientes por meio de timeouts globais, DTOs mínimos e tradução segura de erros de transporte, status e conteúdo.
- Validar corretoras no cadastro diário oficial de participantes intermediários da CVM, com cache e política explícita de indisponibilidade.
- Fortalecer as regras de compra e venda, preservando os cálculos atuais de preço médio e lucro/prejuízo.
- **BREAKING**: renomear a tabela de ações de `produto` para `acoes` e impor unicidade de ticker e CNPJ no banco de dados.
- Versionar a evolução do schema com Liquibase, incluindo rollback explícito e preservação das chaves estrangeiras na renomeação.
- Manter H2 como perfil padrão de desenvolvimento e disponibilizar perfis PostgreSQL e MySQL configurados por ambiente.
- Adicionar empacotamento, execução local documentada e testes automatizados que não dependam de rede, Docker ou credenciais reais.

## Capabilities

### New Capabilities

- `api-input-and-errors`: validação dos contratos HTTP e respostas de erro estáveis no formato `ProblemDetail`.
- `broker-cvm-validation`: cadastro de corretoras com normalização de CNPJ/CEP, enriquecimento externo e confirmação no cadastro oficial da CVM.
- `stock-market-data`: cadastro, consulta e atualização de ações com ticker normalizado e cotação validada em provedores externos.
- `trading-operations`: regras verificáveis para compra, venda, posição disponível, preço médio, lucro/prejuízo e histórico de operações.
- `runtime-persistence-profiles`: configuração segura de segredos, clientes HTTP e perfis H2, PostgreSQL e MySQL, com restrições de integridade persistidas.

### Modified Capabilities

Nenhuma. O projeto ainda não possui especificações-base em `openspec/specs/`; as capacidades documentadas por esta mudança serão introduzidas como novas especificações delta.

## Impact

- Contratos REST de corretoras, ações e operações, especialmente validação, status e corpo dos erros.
- Resources, DTOs, serviços, repositórios, entidades e exceções de domínio.
- Clientes BrasilAPI, ViaCEP, Brapi, Twelve Data e o novo cliente do cadastro diário da CVM.
- Schema relacional, drivers e propriedades para H2, PostgreSQL e MySQL.
- Dependências Maven relacionadas a validação, bancos e testes.
- Configuração de ambiente, Dockerfile, Compose, `.gitignore`, `.env.example` e README.
- Suíte de testes unitários, MVC, parsing de dados da CVM, contexto e empacotamento Maven.
