## Purpose

Definir configuração segura e reproduzível para segredos, clientes externos, bancos de dados e execução local da aplicação.

## ADDED Requirements

### Requirement: Externalize secrets and credentials
A aplicação MUST obter a chave da Twelve Data por `TWELVE_DATA_API_KEY` e credenciais de banco por variáveis de ambiente. Arquivos versionados MUST NOT conter segredos ou credenciais reais.

#### Scenario: Application starts with configured external credential
- **WHEN** `TWELVE_DATA_API_KEY` está definida no ambiente
- **THEN** o cliente da Twelve Data utiliza o valor sem gravá-lo em código, configuração versionada ou logs

#### Scenario: Environment example is distributed
- **WHEN** o repositório fornece `.env.example`
- **THEN** o arquivo contém somente nomes de variáveis e valores fictícios, enquanto `.env` permanece ignorado pelo Git

### Requirement: Provide database runtime profiles
A aplicação MUST usar H2 como perfil padrão para desenvolvimento e testes e MUST oferecer perfis PostgreSQL e MySQL configurados por `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.

#### Scenario: Default local execution
- **WHEN** nenhum perfil externo de banco é selecionado
- **THEN** a aplicação inicia com H2 sem exigir Docker ou credenciais externas

#### Scenario: External database profile is selected
- **WHEN** o perfil PostgreSQL ou MySQL é ativado com as variáveis obrigatórias
- **THEN** a aplicação utiliza o driver e a conexão correspondentes ao perfil

### Requirement: Control schema generation by environment
A propriedade de geração de schema MUST ser configurável. O valor `update` MUST ser padrão apenas em perfis de desenvolvimento e MUST NOT ser imposto a ambientes que utilizem PostgreSQL ou MySQL fora de desenvolvimento.

#### Scenario: Development profile manages schema automatically
- **WHEN** a aplicação inicia em um perfil de desenvolvimento sem sobrescrever a configuração
- **THEN** o Hibernate utiliza `ddl-auto=update`

#### Scenario: Deployment overrides schema policy
- **WHEN** um ambiente define explicitamente a política de geração de schema
- **THEN** a aplicação respeita o valor configurado pelo ambiente

### Requirement: Enforce persistent uniqueness
O schema MUST armazenar ações na tabela `acoes` e MUST impor unicidade aos tickers de ações e aos CNPJs de corretoras, complementando a verificação realizada antes da persistência.

#### Scenario: Concurrent duplicate reaches the database
- **WHEN** duas gravações concorrentes tentam persistir o mesmo ticker normalizado ou CNPJ normalizado
- **THEN** a restrição do banco impede a duplicidade e a API comunica `409 Conflict`

#### Scenario: Action data is persisted
- **WHEN** uma ação válida é salva
- **THEN** seus dados são armazenados na tabela `acoes`

### Requirement: Apply bounded external call timeouts
Os clientes HTTP externos MUST aplicar timeouts globais configuráveis de conexão e leitura, e uma chamada que exceda esses limites MUST ser tratada como indisponibilidade do provedor.

#### Scenario: Provider exceeds read timeout
- **WHEN** um provedor obrigatório não responde dentro do limite configurado
- **THEN** a chamada é encerrada e a API retorna `503 Service Unavailable` sem expor detalhes sensíveis

### Requirement: Support reproducible local packaging
O projeto MUST documentar as variáveis, perfis, endpoints, formato de erro e fontes externas, e MUST fornecer meios de empacotar a aplicação e iniciar PostgreSQL ou MySQL opcionalmente por Compose.

#### Scenario: Developer uses only H2
- **WHEN** um desenvolvedor segue a documentação de execução padrão
- **THEN** consegue iniciar a aplicação localmente sem Docker

#### Scenario: Developer selects a containerized database
- **WHEN** um desenvolvedor ativa o serviço PostgreSQL ou MySQL documentado no Compose
- **THEN** obtém um banco compatível com o respectivo perfil da aplicação
