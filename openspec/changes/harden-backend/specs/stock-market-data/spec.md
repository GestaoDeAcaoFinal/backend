## Purpose

Definir o cadastro, a consulta e a atualização de ações com identificação de mercado e cotações obtidas de provedores externos confiáveis.

## ADDED Requirements

### Requirement: Normalize and validate stock registration
O sistema MUST normalizar o ticker para a forma canônica em maiúsculas, validar seu formato e exigir um identificador positivo de corretora existente antes de consultar cotação ou persistir a ação.

#### Scenario: Valid stock registration
- **WHEN** o consumidor envia ticker válido e identificador de uma corretora existente
- **THEN** o sistema processa o ticker em maiúsculas e associa a nova ação à corretora informada

#### Scenario: Broker does not exist
- **WHEN** o identificador informado não corresponde a uma corretora persistida
- **THEN** o cadastro da ação falha com `404 Not Found` sem persistir a ação

### Requirement: Reject duplicate tickers
O sistema MUST impedir o cadastro de mais de uma ação com o mesmo ticker normalizado.

#### Scenario: Normalized ticker already exists
- **WHEN** uma ação persistida possui o mesmo ticker normalizado da solicitação
- **THEN** o novo cadastro é rejeitado com `409 Conflict`

### Requirement: Enrich stocks with market data
O sistema MUST consultar o provedor adequado ao mercado identificado e persistir somente uma resposta que contenha nome da empresa, cotação positiva e moeda. A ação MUST registrar o mercado e o instante em que a cotação foi obtida.

#### Scenario: Brazilian ticker is valid
- **WHEN** um ticker classificado no mercado brasileiro possui dados válidos no provedor correspondente
- **THEN** a ação é persistida com mercado `BR`, nome, moeda, cotação positiva e data/hora da cotação

#### Scenario: United States ticker is valid
- **WHEN** um ticker classificado no mercado dos Estados Unidos possui dados válidos no provedor correspondente
- **THEN** a ação é persistida com mercado `EUA`, nome, moeda, cotação positiva e data/hora da cotação

#### Scenario: Ticker does not exist
- **WHEN** o provedor autoritativo informa que o ticker de formato válido não existe
- **THEN** o cadastro é rejeitado com `422 Unprocessable Entity`

### Requirement: Update quotations atomically
O sistema MUST atualizar a cotação e seu instante de referência somente depois de obter do provedor do mercado uma cotação válida e positiva. Uma falha externa MUST NOT substituir a última cotação persistida.

#### Scenario: Quotation update succeeds
- **WHEN** o consumidor solicita atualização de uma ação existente e o provedor retorna cotação válida e positiva
- **THEN** o sistema persiste a nova cotação e uma nova data/hora de referência

#### Scenario: Quotation provider fails
- **WHEN** o provedor falha, excede o timeout ou retorna conteúdo inválido durante a atualização
- **THEN** a API retorna o erro HTTP correspondente e mantém inalterada a cotação persistida

### Requirement: Retrieve registered stocks
A API MUST permitir listar ações e buscar uma ação por identificador local ou ticker normalizado.

#### Scenario: Stock is found by normalized ticker
- **WHEN** o consumidor busca um ticker correspondente a uma ação persistida
- **THEN** a API retorna a ação e seus dados de mercado atuais

#### Scenario: Stock is not found
- **WHEN** o identificador local ou ticker normalizado não corresponde a uma ação persistida
- **THEN** a API retorna `404 Not Found`

