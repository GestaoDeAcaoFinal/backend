# Integração Brapi — diagnóstico de 12/11/2026

O erro de BBAS3 e do cadastro de ROXO34, TAEE11 e AUAU3 ocorreu na execução local iniciada sem o token Brapi. A credencial estava configurada no IntelliJ; iniciar Maven fora da IDE não herda automaticamente as variáveis da configuração de execução.

Foi autorizada a reutilização dessa credencial apenas na memória do processo do backend, sem exibir o valor nem copiá-lo para `.env` ou para o repositório. A inicialização local precisa fornecer `BRAPI_TOKEN` ao processo; uma propriedade `integracoes.brapi.token` vazia em `SPRING_APPLICATION_JSON` tem precedência e não deve sobrescrever a configuração válida.

## Evidência de diagnóstico

- Consulta direta sem token: PETR4 respondeu 200; BBAS3 respondeu 401.
- Consulta direta com a credencial existente autorizada: BBAS3, ROXO34, TAEE11 e AUAU3 responderam 200, com nome, moeda e cotação positiva.
- HAE11, informado inicialmente e depois corrigido para TAEE11, respondeu 404.

As consultas de diagnóstico não cadastram ativos ou executam operações financeiras.

## Tratamento de erros

O executor dos provedores agora identifica respostas HTTP 401 e 403 e orienta conferir a chave de integração e as permissões do plano. A API da aplicação continua respondendo 502 nesses casos: uma recusa de autenticação do provedor não deve encerrar a sessão JWT do investidor. Nenhum corpo de erro do provedor é copiado para a mensagem pública.

404 continua sendo tratado como dado não encontrado; demais respostas inválidas mantêm o tratamento existente. Os testes parametrizados cobrem 401 e 403 e verificam que detalhes sensíveis simulados do provedor não aparecem na mensagem pública.
