## Purpose

Estabelecer contratos HTTP previsíveis para validação de entradas e comunicação segura de erros em todos os recursos da API.

## ADDED Requirements

### Requirement: Validate requests before side effects
A API MUST validar corpos, parâmetros de rota e parâmetros de consulta antes de consultar o banco de dados, chamar provedores externos ou persistir qualquer alteração. Identificadores e quantidades MUST ser positivos quando exigidos pelo contrato da operação.

#### Scenario: Invalid request has no side effects
- **WHEN** uma requisição contém campo obrigatório ausente, formato inválido, identificador não positivo ou quantidade não positiva
- **THEN** a API rejeita a requisição com `400 Bad Request` antes de acessar persistência ou rede

#### Scenario: Valid request reaches the application service
- **WHEN** uma requisição satisfaz todas as restrições sintáticas e numéricas do contrato
- **THEN** a API encaminha valores validados e normalizados para processamento

### Requirement: Return errors as ProblemDetail
A API MUST representar erros conhecidos com corpo compatível com `ProblemDetail`, incluindo status, título e detalhe estáveis. Erros de validação de campos MUST incluir uma extensão que identifique cada campo inválido e sua mensagem sem expor detalhes internos.

#### Scenario: Field validation error
- **WHEN** um corpo de requisição possui um ou mais campos inválidos
- **THEN** a API retorna `application/problem+json` com `400 Bad Request` e os erros associados aos respectivos campos

#### Scenario: Domain error
- **WHEN** uma regra de domínio conhecida impede a operação
- **THEN** a API retorna `application/problem+json` com status e detalhe correspondentes à regra violada

### Requirement: Map failures to stable HTTP statuses
A API MUST distinguir falhas do consumidor, ausência de recursos, conflitos, dados externos inexistentes e indisponibilidade de provedores por códigos HTTP estáveis.

#### Scenario: Invalid consumer input or operation rule
- **WHEN** a entrada é malformada, viola uma restrição declarada ou descreve uma operação inválida
- **THEN** a API retorna `400 Bad Request`

#### Scenario: Local resource does not exist
- **WHEN** a ação, corretora ou operação solicitada não existe na persistência local
- **THEN** a API retorna `404 Not Found`

#### Scenario: Duplicate or insufficient position
- **WHEN** o consumidor tenta cadastrar CNPJ ou ticker já existente após normalização, ou vender acima da posição disponível
- **THEN** a API retorna `409 Conflict`

#### Scenario: External registry has no matching data
- **WHEN** CNPJ, CEP ou ticker possui formato válido, mas não existe ou não está ativo na fonte autoritativa consultada
- **THEN** a API retorna `422 Unprocessable Entity`

#### Scenario: Provider returns an error or invalid content
- **WHEN** um provedor obrigatório responde com erro HTTP ou conteúdo ausente, inesperado ou inconsistente
- **THEN** a API retorna `502 Bad Gateway`

#### Scenario: Provider is unavailable
- **WHEN** ocorre timeout ou indisponibilidade de um provedor obrigatório e não existe cache válido que permita concluir a operação
- **THEN** a API retorna `503 Service Unavailable`

### Requirement: Protect sensitive integration details
As respostas e logs produzidos para falhas externas MUST identificar o provedor relevante sem revelar chaves, credenciais, URLs completas com parâmetros sensíveis ou corpos de resposta confidenciais.

#### Scenario: External call fails with a configured credential
- **WHEN** uma chamada externa que utiliza credencial falha
- **THEN** a falha registrada e o `ProblemDetail` identificam o provedor sem incluir o valor da credencial

