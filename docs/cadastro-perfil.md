# Cadastro de perfil com CPF e telefone

## Regra de cadastro

`POST /api/usuarios` exige `username`, `cpf`, `telefone`, `password` e `papel`. `PUT /api/usuarios/{id}` exige `username`, `cpf`, `telefone`, `papel` e `ativo`; senha continua no endpoint separado `PUT /api/usuarios/{id}/senha`. As rotas permanecem exclusivas de ADMIN.

CPF aceita 11 dígitos ou a máscara completa `000.000.000-00`, verifica os dois dígitos verificadores e rejeita sequências repetidas. CPF é único por conta, incluindo contas inativas. A normalização remove a máscara antes de consultar duplicidade e persistir.

Telefone aceita DDD brasileiro e número fixo de oito dígitos (inicial de 2 a 5) ou celular de nove dígitos (inicial 9), com ou sem a máscara usual. Armazena 10 ou 11 dígitos, sem código de país ou ramal. A lista de DDD segue a [relação de códigos nacionais da Anatel](https://www.anatel.gov.br/Portal/verificaDocumentos/documento.asp?assuntoPublicacao=null&caminhoRel=null&documentoPath=telefonia_fixa%2Fstfc%2Fcod_prestadoras%2Flista_csp_codigos_nacionais.pdf&filtro=1&numeroPublicacao=141766).

Não há serviço externo. Validar dígitos e formato não comprova identidade, situação cadastral na Receita Federal, existência da linha ou titularidade do telefone. Não há envio de SMS.

## Migração e contas antigas

Liquibase aplica `007-cpf-telefone-usuario.xml`, adicionando colunas anuláveis de 11 caracteres e a restrição `uk_usuario_cpf`. Contas anteriores mantêm identificador, username, hash, papel, status e sessões; CPF e telefone ficam vazios até a próxima edição pelo ADMIN. A próxima edição exige ambos válidos. Não são preenchidos dados pessoais fictícios.

O ADMIN inicial do perfil `dev` mantém o mecanismo de bootstrap e pode completar seu cadastro pela área administrativa. Nenhuma credencial ou novo dado pessoal foi adicionado à configuração.

## Segurança e erros

- Login continua com username e senha. CPF e telefone não entram no JWT.
- Respostas administrativas incluem CPF e telefone para preenchimento da edição; as senhas e hashes continuam ausentes dos DTOs.
- Dados ausentes ou inválidos retornam HTTP400; CPF usado por outra conta retorna HTTP409, sem expor o valor ou identificar a outra conta.
- A restrição do banco protege a corrida entre verificação e gravação. O serviço converte a violação de integridade em conflito com mensagem segura, sem propagar SQL ou conteúdo da exceção.
- As categorias `org.hibernate.orm.jdbc.error` e `org.hibernate.orm.jdbc.warn` ficam desabilitadas porque mensagens JDBC de unicidade podem conter dados pessoais. O serviço registra um aviso genérico de conflito, sem valores nem exceção SQL; o detalhe bruto do banco deixa de aparecer nesses logs.
- Alterar apenas CPF/telefone não altera permissões nem a versão da sessão. As regras de username, papel, status, senha, último ADMIN e isolamento da carteira foram preservadas.

## Verificação local — 25/11/2026

Dois testes HTTP novos falharam antes da implementação: esperavam HTTP400 e o sistema anterior retornava HTTP201 para campos ausentes ou CPF repetido inválido.

Após a implementação, 88 testes focados passaram. Depois da correção de privacidade, o `clean verify` completo passou com **376 testes, zero falhas, erros ou ignorados**. O JaCoCo mediu **868/879 linhas (98,75%) e 354/360 branches (98,33%)**, em 58 classes, mantendo os mínimos 85/85 sem exclusões novas.

A revisão independente encontrou a exposição de CPF no log automático do Hibernate antes da captura da exceção. Um teste com violação real da unicidade e saída capturada reproduziu a falha; após a correção de configuração e diagnóstico seguro, esse teste passou na suíte completa.

`CadastroPerfilMigrationTest` migra primeiro até006, insere contas antigas, aplica007 e verifica preservação dos dados, campos nulos legados e rejeição de CPF duplicado no banco H2. Testes HTTP exercitam criação normalizada, dados inválidos, obrigatoriedade em edição legada, duplicidade e preservação das proteções ADMIN/USER. Os testes não acessam provedores ou banco de desenvolvimento.

As migrações foram executadas somente nas bases de teste. Aplicações locais já abertas precisam ser reiniciadas com o backend e frontend atualizados. Publicação, sincronização do repositório conjunto e Docker são etapas separadas.

O frontend passou nos **193 testes Angular**, no **build de produção** e nos **17 testes Playwright em Chrome**, usando H2 e provedores simulados. Os fluxos novos de criação, edição, persistência e CPF duplicado foram executados em 1280px e390px; capturas foram inspecionadas visualmente e a ausência de overflow horizontal foi verificada. Os testes existentes de login/permissões, último ADMIN, senha, carteira individual e operações também passaram. O build mantém avisos de depreciação Sass/Bootstrap, sem erros.

O smoke de Docker teve suas fixtures adaptadas ao contrato, mas **não foi executado** nesta etapa. Não houve rebuild de imagem, alteração de container, publicação ou migração de uma base real.


## Telefone único — 26/11/2026

O cadastro e a edição rejeitam telefone já vinculado a outra conta, inclusive inativa, com HTTP 409 e a mensagem “Telefone já cadastrado em outra conta”. A comparação ocorre depois da normalização: `(17) 99999-8888` e `17999998888` são o mesmo número. O próprio telefone pode ser mantido na edição ou substituído por um disponível. A unicidade de CPF continua preservada.

A migração 008 adiciona `uk_usuario_telefone` sem modificar a migração 007 já aplicada. Valores nulos de contas legadas continuam permitidos até completar a edição. Se houver duplicidades anteriores, a migração interrompe sem apagar ou escolher dados automaticamente. A restrição do banco cobre também gravações concorrentes que passem pela consulta de disponibilidade; o serviço converte conflito de integridade em HTTP 409 com mensagem segura. A aplicação não comprova existência nem posse do telefone e não envia SMS.

Validação local: `clean verify` aprovou 389 testes e o limite de cobertura 85/85. Novas regressões cobrem criação/edição com máscara, conta inativa, manutenção do próprio número, troca por número disponível, proteção do banco, migração de contas legadas e interrupção segura quando já há números repetidos.
