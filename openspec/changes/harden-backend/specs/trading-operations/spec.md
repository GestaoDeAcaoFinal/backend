## Purpose

Definir regras determinísticas para registrar compras e vendas, controlar posição e consultar o histórico de operações de cada ação.

## ADDED Requirements

### Requirement: Register purchases at the current quotation
O sistema MUST registrar uma compra somente para ação existente, quantidade positiva e cotação atual positiva. A operação MUST armazenar tipo `COMPRA`, quantidade, preço unitário, valor total e data/hora.

#### Scenario: Valid purchase
- **WHEN** o consumidor compra uma quantidade positiva de ação existente que possui cotação atual positiva
- **THEN** o sistema persiste a compra usando a cotação atual como preço unitário e o produto entre preço e quantidade como valor total

#### Scenario: Purchase has invalid quantity or quotation
- **WHEN** a quantidade solicitada ou a cotação atual da ação não é positiva
- **THEN** a compra é rejeitada com `400 Bad Request` sem persistir a operação

### Requirement: Maintain weighted average purchase price
Cada compra MUST registrar o preço médio ponderado considerando as compras anteriores da mesma ação e a nova compra. As vendas realizadas MUST NOT alterar esse preço médio de aquisição.

#### Scenario: Additional purchase changes average price
- **WHEN** existe uma ou mais compras anteriores e uma nova compra é registrada
- **THEN** o preço médio armazenado corresponde à soma dos valores das compras dividida pela soma de suas quantidades

#### Scenario: First purchase establishes average price
- **WHEN** a primeira compra de uma ação é registrada
- **THEN** o preço médio armazenado é igual ao preço unitário da compra

### Requirement: Prevent sales beyond available position
O sistema MUST registrar uma venda somente para ação existente, quantidade positiva, preço de venda positivo, ao menos uma compra anterior e posição disponível suficiente. A posição disponível MUST corresponder à quantidade total comprada menos a quantidade total vendida.

#### Scenario: Valid sale
- **WHEN** a quantidade solicitada é positiva e não excede a posição disponível da ação
- **THEN** o sistema persiste uma operação `VENDA` com quantidade, preço unitário, valor total, preço médio de aquisição e data/hora

#### Scenario: No purchased position exists
- **WHEN** o consumidor tenta vender uma ação sem compra anterior
- **THEN** a operação é rejeitada com `400 Bad Request`

#### Scenario: Sale exceeds available position
- **WHEN** a quantidade solicitada excede a posição disponível
- **THEN** a operação é rejeitada com `409 Conflict` sem alterar o histórico

### Requirement: Calculate realized profit or loss
O sistema MUST calcular o lucro ou prejuízo realizado da venda como `(preço de venda - preço médio de aquisição) × quantidade vendida`, arredondado para duas casas decimais.

#### Scenario: Sale above average price
- **WHEN** o preço de venda é maior que o preço médio de aquisição
- **THEN** a operação registra um valor positivo de lucro/prejuízo arredondado para duas casas decimais

#### Scenario: Sale below average price
- **WHEN** o preço de venda é menor que o preço médio de aquisição
- **THEN** a operação registra um valor negativo de lucro/prejuízo arredondado para duas casas decimais

### Requirement: Retrieve operation history
A API MUST permitir listar todas as operações, listar o histórico de uma ação, filtrar compras, filtrar vendas e buscar uma operação por identificador.

#### Scenario: History is requested for a stock
- **WHEN** o consumidor solicita o histórico de uma ação existente
- **THEN** a API retorna as operações associadas à ação

#### Scenario: Operation is not found
- **WHEN** o identificador solicitado não corresponde a uma operação persistida
- **THEN** a API retorna `404 Not Found`

