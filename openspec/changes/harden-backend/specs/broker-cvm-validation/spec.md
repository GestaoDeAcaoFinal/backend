## Purpose

Definir o cadastro e a consulta de corretoras com dados normalizados, enriquecidos e comprovadamente validados nas fontes oficiais exigidas.

## ADDED Requirements

### Requirement: Normalize broker identifiers
O sistema MUST aceitar CNPJ e CEP com ou sem pontuação permitida, remover caracteres de formatação e validar respectivamente 14 e 8 dígitos antes de consultar duplicidade ou provedores externos.

#### Scenario: Formatted broker data is accepted
- **WHEN** o consumidor envia CNPJ e CEP válidos com pontuação
- **THEN** o sistema usa os valores normalizados nas consultas e na persistência

#### Scenario: Broker data has an invalid length
- **WHEN** CNPJ ou CEP normalizado não contém a quantidade exigida de dígitos
- **THEN** o cadastro é rejeitado com `400 Bad Request` sem acessar banco ou rede

### Requirement: Reject duplicate brokers
O sistema MUST impedir o cadastro de mais de uma corretora com o mesmo CNPJ normalizado.

#### Scenario: Normalized CNPJ already exists
- **WHEN** uma corretora persistida possui o mesmo CNPJ normalizado da solicitação
- **THEN** o novo cadastro é rejeitado com `409 Conflict`

### Requirement: Enrich broker data from authoritative services
O sistema MUST consultar CEP e dados cadastrais do CNPJ para preencher endereço, razão social, nome fantasia, contato e situação cadastral antes de persistir a corretora.

#### Scenario: CEP and CNPJ are found
- **WHEN** ViaCEP e BrasilAPI retornam dados válidos para os identificadores normalizados
- **THEN** o cadastro incorpora os campos retornados que fazem parte do modelo da corretora

#### Scenario: CEP or CNPJ does not exist
- **WHEN** uma das fontes autoritativas informa que o CEP ou CNPJ não existe
- **THEN** o cadastro é rejeitado com `422 Unprocessable Entity`

### Requirement: Verify active CVM participation
O sistema MUST marcar `validadaNaCvm=true` somente quando o CNPJ normalizado constar como intermediário ativo no cadastro diário oficial de Participantes Intermediários da CVM. Uma corretora não verificada MUST NOT ser persistida como validada.

#### Scenario: Active intermediary is confirmed
- **WHEN** o CNPJ consta como intermediário ativo no cadastro oficial da CVM
- **THEN** a corretora é persistida com `validadaNaCvm=true` e data de cadastro

#### Scenario: Intermediary is absent or inactive
- **WHEN** o CNPJ não consta no cadastro da CVM ou consta como inativo
- **THEN** o cadastro é rejeitado com `422 Unprocessable Entity` e nenhuma corretora validada é persistida

### Requirement: Cache the CVM registry safely
O sistema MUST manter um índice em memória dos CNPJs ativos extraídos do conjunto oficial `cad_intermed.zip`, com validade configurável e padrão de 24 horas.

#### Scenario: Valid CVM cache exists
- **WHEN** uma validação é solicitada durante a validade do índice em memória
- **THEN** o sistema consulta o índice sem baixar novamente o conjunto oficial

#### Scenario: CVM is unavailable without valid cache
- **WHEN** o índice expirou ou não existe e o conjunto oficial da CVM não pode ser obtido ou processado
- **THEN** o cadastro falha com `503 Service Unavailable` e o sistema não presume validação

### Requirement: Retrieve registered brokers
A API MUST permitir listar corretoras e buscar uma corretora por identificador local ou por CNPJ normalizado.

#### Scenario: Broker is found by CNPJ
- **WHEN** o consumidor busca um CNPJ correspondente a uma corretora persistida
- **THEN** a API retorna a corretora correspondente

#### Scenario: Broker is not found
- **WHEN** o identificador local ou CNPJ normalizado não corresponde a uma corretora persistida
- **THEN** a API retorna `404 Not Found`

