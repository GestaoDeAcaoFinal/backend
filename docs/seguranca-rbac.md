# Segurança e Controle de Acesso — RBAC

## 1. Introdução

O projeto **GestaoDeAcao** utiliza Spring Security para implementar autenticação e autorização, adotando tokens JWT (JSON Web Token) e controle de acesso baseado em papéis (Role-Based Access Control — RBAC).

O objetivo é proteger as funcionalidades administrativas e permitir que usuários comuns realizem operações de acordo com suas responsabilidades.

A autenticação é realizada por meio do endpoint `POST /api/auth/login`, que valida as credenciais e emite um token JWT. As demais requisições protegidas utilizam esse token no cabeçalho `Authorization: Bearer`.

## 2. Papéis do Sistema

Foram definidos dois papéis na enumeração `Papel`:

### ADMIN — Administrador

Responsável pelo gerenciamento administrativo da aplicação, incluindo:

- Cadastro de ações e corretoras.
- Atualização de cotações.
- Cadastro, listagem e atualização de usuários.
- Alteração de papéis e ativação ou desativação de contas.
- Redefinição de senhas.
- Consulta de dados e utilização das funcionalidades de investimentos.

### USER — Investidor

Responsável pela utilização das funcionalidades operacionais:

- Consulta de ações e corretoras cadastradas.
- Consulta das operações financeiras.
- Registro de compras e vendas de ações.
- Consulta das informações da carteira disponibilizadas pela aplicação.

Usuários com papel `USER` não possuem autorização para administrar cadastros de ações, corretoras ou usuários.

## 3. Matriz de Controle de Acesso

A matriz abaixo define as permissões implementadas no sistema.

| Funcionalidade | ADMIN | USER |
|---|---|---|
| Autenticação (`POST /api/auth/login`) | Permitido | Permitido |
| Consultar ações (`GET /acoes`) | Permitido | Permitido |
| Consultar ação por ID | Permitido | Permitido |
| Cadastrar ações (`POST /acoes`) | Permitido | Negado |
| Atualizar cotação de ação | Permitido | Negado |
| Consultar corretoras | Permitido | Permitido |
| Cadastrar corretoras | Permitido | Negado |
| Consultar operações | Permitido | Permitido |
| Comprar ações | Permitido | Permitido |
| Vender ações | Permitido | Permitido |
| Consultar carteira | Permitido | Permitido |
| Listar e consultar usuários | Permitido | Negado |
| Cadastrar usuários | Permitido | Negado |
| Alterar papel ou situação de usuários | Permitido | Negado |
| Redefinir senha de usuários | Permitido | Negado |

As operações financeiras de compra e venda são permitidas ao papel `USER`, pois representam a funcionalidade principal do sistema.

Por outro lado, as operações de escrita relacionadas à administração dos cadastros são restritas ao papel `ADMIN`.

Essa separação foi definida considerando as necessidades específicas do domínio financeiro.

## 4. Implementação da Segurança

### 4.1 Autenticação JWT

A aplicação utiliza autenticação baseada em JWT, com sessões configuradas como *stateless*.

Após validar as credenciais, o backend emite um token contendo informações de autenticação e permissões do usuário.

O token é enviado nas requisições protegidas utilizando:

`Authorization: Bearer <token>`

A expiração é configurável por meio da propriedade `JWT_EXPIRATION`, sendo utilizado o valor de 60 minutos no ambiente de desenvolvimento.

As senhas dos usuários são armazenadas utilizando BCrypt, por meio do `PasswordEncoder`.

### 4.2 Segurança nos Métodos

A classe `SecurityConfig` utiliza a anotação:

`@EnableMethodSecurity`

Essa configuração habilita as regras de autorização declaradas com `@PreAuthorize`.

A aplicação exige autenticação para os endpoints protegidos, enquanto as permissões administrativas são verificadas nos métodos e recursos correspondentes.

## 5. Justificativa das Anotações @PreAuthorize

### 5.1 AcaoResource

**Cadastro de ações — `POST /acoes`**

`@PreAuthorize("hasRole('ADMIN')")`

O cadastro de novos ativos altera o catálogo utilizado pelos investidores. Por esse motivo, somente administradores podem executar essa operação.

**Atualização de cotação — `PUT /acoes/{id}/atualizar-cotacao`**

`@PreAuthorize("hasRole('ADMIN')")`

A atualização das informações de cotação modifica dados compartilhados entre os usuários. A restrição evita alterações administrativas realizadas por investidores comuns.

### 5.2 CorretoraResource

**Cadastro de corretoras — `POST /corretoras`**

`@PreAuthorize("hasRole('ADMIN')")`

O cadastro de corretoras envolve informações institucionais obtidas por integrações externas. A autorização administrativa reduz o risco de cadastros indevidos.

### 5.3 UsuarioResource

**Gerenciamento de usuários — `/api/usuarios`**

`@PreAuthorize("hasRole('ADMIN')")`

A anotação foi aplicada diretamente à classe `UsuarioResource`, pois todos os seus endpoints são administrativos.

Isso protege as seguintes operações:

- `GET /api/usuarios`
- `GET /api/usuarios/{id}`
- `POST /api/usuarios`
- `PUT /api/usuarios/{id}`
- `PUT /api/usuarios/{id}/senha`

A utilização da anotação no nível da classe evita repetição e garante uma regra uniforme de autorização.

### 5.4 Endpoints sem Restrição Administrativa

As operações de consulta de ações e corretoras permanecem acessíveis aos usuários autenticados.

As operações de compra e venda também não exigem o papel `ADMIN`, pois fazem parte das responsabilidades do investidor.

Esses endpoints continuam sujeitos à autenticação definida no `SecurityConfig`.

## 6. Gerenciamento Seguro de Usuários

O gerenciamento administrativo foi implementado por meio de `UsuarioService` e `UsuarioResource`.

O sistema oferece funcionalidades para:

- Cadastrar usuários.
- Consultar usuários por ID.
- Listar usuários.
- Atualizar nome, papel e situação da conta.
- Alterar senhas.
- Impedir nomes de usuário duplicados.

### Proteção das Senhas

As senhas recebidas no cadastro e na alteração de senha são processadas com BCrypt antes do armazenamento.

As respostas HTTP utilizam o DTO `UsuarioResponse`, que contém somente:

- `id`
- `username`
- `papel`
- `ativo`

O campo `senha` não é retornado pela API, impedindo que a senha ou seu hash sejam expostos nas respostas administrativas.

### Desativação de Contas

O sistema permite alterar o campo `ativo` para `false`.

Uma conta desativada não consegue realizar uma nova autenticação, retornando `401 Unauthorized` ao tentar efetuar login.

A desativação é utilizada no lugar da exclusão definitiva de usuários, preservando os registros existentes.

## 7. Autorização Horizontal

A autorização baseada em papéis (RBAC) define quais funcionalidades cada categoria de usuário pode acessar.

Entretanto, ela não determina, isoladamente, quais registros pertencem a cada usuário.

A autorização horizontal controla o acesso entre usuários que possuem o mesmo papel.

### Exemplo no Domínio Financeiro

Considere dois investidores:

**Investidor 01**
- Papel: `USER`
- Possui operações de compra de PETR4.

**Investidor 02**
- Papel: `USER`
- Possui operações de compra de VALE3.

Embora ambos possuam o mesmo papel, o Investidor 01 não deve conseguir consultar ou movimentar a carteira do Investidor 02.

### Regra Proposta

Cada operação financeira deverá estar vinculada ao usuário que a realizou.

As consultas e movimentações deverão considerar a identidade do usuário autenticado.

Dessa forma:

- Um investidor poderá visualizar somente suas operações.
- Um investidor não poderá vender ações pertencentes a outro usuário.
- A carteira será calculada individualmente.
- O backend deverá validar a propriedade dos registros, independentemente dos parâmetros enviados pelo cliente.

**Situação atual:** essa regra está documentada como evolução do projeto. O controle de acesso por papéis foi implementado, mas o isolamento das carteiras por proprietário ainda não foi concluído.

## 8. Testes de Autenticação e Autorização

A segurança foi validada por testes manuais no Postman e testes automatizados com JUnit, Spring Security Test e MockMvc.

### 8.1 Testes Manuais — Postman

| Cenário | HTTP obtido |
|---|---|
| USER consulta ações | 200 OK |
| USER consulta corretoras | 200 OK |
| USER tenta cadastrar ações | 403 Forbidden |
| USER tenta cadastrar corretoras | 403 Forbidden |
| USER tenta acessar gerenciamento de usuários | 403 Forbidden |
| ADMIN consulta usuários | 200 OK |
| ADMIN cadastra ações | Sucesso |
| ADMIN altera situação de usuário | 200 OK |
| Usuário desativado tenta login | 401 Unauthorized |
| ADMIN altera senha | 204 No Content |
| Login com senha anterior | 401 Unauthorized |
| Login com nova senha | 200 OK |

Esses testes demonstram a diferença entre autenticação e autorização:

- **200 OK:** requisição autorizada e processada.
- **401 Unauthorized:** autenticação ausente, inválida ou recusada.
- **403 Forbidden:** usuário autenticado sem a permissão necessária.

### 8.2 Testes Automatizados

A classe `UsuarioResourceIntegrationTest` verifica:

1. Criação de usuários por administradores.
2. Armazenamento de senhas com BCrypt.
3. Ausência de senhas e hashes nas respostas.
4. Listagem administrativa.
5. Alteração de papel e situação.
6. Alteração segura de senha.
7. Bloqueio de acesso administrativo para o papel `USER`.

A autenticação é simulada com recursos do Spring Security Test, enquanto os testes de autenticação da Aula 13 verificam o funcionamento do JWT real.

### Resultado da Suíte Completa

Comando utilizado:

`.\mvnw.cmd test`

Resultado registrado em 05/11/2026:

```text
Tests run: 107, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

O resultado confirma que os testes automatizados existentes e os novos testes administrativos foram executados com sucesso.

## 9. Limitações e Melhorias de Segurança

### 9.1 Proteção do Último Administrador

Na implementação atual, administradores podem alterar seu próprio cadastro e o de outros administradores.

Uma melhoria futura deverá impedir a desativação ou o rebaixamento do último administrador ativo, evitando a perda do acesso administrativo à aplicação.

### 9.2 Revogação de JWT

A desativação de uma conta impede novos logins, mas tokens JWT emitidos anteriormente podem continuar válidos até sua expiração.

Uma futura implementação poderá utilizar consulta do estado atual do usuário, revogação de tokens ou períodos menores de validade.

### 9.3 Auditoria

O sistema poderá implementar registros de auditoria para identificar:

- Quem cadastrou determinado usuário.
- Quem alterou papéis e permissões.
- Quem ativou ou desativou contas.
- Quem realizou redefinições de senha.
- Quando cada operação administrativa ocorreu.

Esses registros não deverão armazenar senhas, hashes ou tokens JWT completos.

### 9.4 Proteção Contra Tentativas de Acesso Indevidas

Para utilização em produção, são recomendadas:

- Limitação da frequência de requisições ao endpoint de login (*rate limiting*).
- Monitoramento de tentativas suspeitas.
- Comunicação exclusivamente por HTTPS.
- Armazenamento seguro das chaves e variáveis de ambiente.
- Configuração de respostas de erro sem exposição de informações internas.

## 10. Conclusão

A implementação de RBAC no projeto **GestaoDeAcao** estabeleceu uma separação entre as responsabilidades administrativas e operacionais.

O papel `ADMIN` possui acesso ao gerenciamento de usuários e aos cadastros administrativos, enquanto o papel `USER` pode consultar os dados disponibilizados e realizar operações financeiras.

A utilização de Spring Security, JWT, BCrypt e `@PreAuthorize` oferece uma estrutura de autenticação e autorização adequada ao escopo didático da aplicação.

Os testes manuais e automatizados demonstraram o funcionamento das permissões, incluindo respostas HTTP `200`, `401` e `403`.

Como evolução, destacam-se o isolamento das carteiras por usuário, a proteção do último administrador ativo, a revogação de tokens e a auditoria das operações administrativas.

Essas melhorias são importantes para ampliar a segurança da aplicação em um cenário de utilização real.