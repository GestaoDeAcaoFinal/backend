# Correções de carteira e autorização — 12/11/2026

## Regras aplicadas

- O cadastro de ações/corretoras continua sendo um catálogo compartilhado. Quantidades, preços de aquisição, resultados e operações pertencem exclusivamente ao investidor autenticado, inclusive quando ele é ADMIN.
- Compras e vendas atribuem o proprietário no servidor. Listagem, histórico, filtros, consulta por ID e resumo usam o proprietário na consulta SQL. Consultar uma operação de outra conta retorna 404.
- A escrita de operações bloqueia a linha do investidor durante a transação para serializar alterações da posição.
- Não é permitido rebaixar ou desativar o último ADMIN ativo. A verificação ocorre numa transação com bloqueio das contas, para coordenar atualizações concorrentes.
- Cada requisição autenticada verifica a existência, o status, o ID e o papel atual da conta. JWT agora inclui `uid`, além de `sub`. Tokens anteriores a essa versão exigem novo login. Alterar papel ou desativar a conta impede o uso do token incompatível na próxima requisição.
- Na consolidação seguinte, o changelog `006-versao-sessao.xml` adicionou a versão de sessão da conta. JWT inclui `sv`; senha, username, papel ou status alterados incrementam a versão. Assim tokens anteriores são rejeitados mesmo após reativação. Não há refresh token automático.

## Dados anteriores à propriedade

O changelog `005-proprietario-operacao.xml` adiciona a referência ao usuário e um índice. Ele não adivinha quem criou os registros antigos. Operações sem proprietário ficam invisíveis nas consultas da aplicação até uma atribuição explícita.

No ambiente local, o histórico foi atribuído aos proprietários confirmados pelo responsável. A cópia CSV anterior à migração e o script transacional de atribuição ficam fora do repositório. O script confere contas, quantidade de registros e características das operações antes de atualizar; interrompe se houver divergência. Identificadores de contas e operações desse ambiente não fazem parte desta documentação pública.

Outros ambientes precisam de atribuição própria, conferida com seu responsável. Não reutilizar os IDs locais automaticamente.

## Validação

Os testes integrados usam H2 exclusivo de teste e JWT assinados pelo próprio contexto. Cobrem bloqueio do último ADMIN, perda de acesso por rebaixamento/desativação, reutilização de username e separação de histórico/carteira/vendas entre duas contas.

As alterações permanecem locais, sem commit ou publicação.
