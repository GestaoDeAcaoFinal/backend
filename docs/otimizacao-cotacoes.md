# Atualização de cotações — 25/11/2026

## Comportamento

O limitador da Twelve Data utiliza os créditos disponíveis imediatamente, em uma janela deslizante de 60 segundos com margem de 100 ms na configuração padrão. O orçamento de 8 créditos é compartilhado entre cotações e câmbio. Falhas também consomem uma reserva; chamadas concorrentes não multiplicam o orçamento. Continua existindo apenas uma chamada em execução por vez nesse cliente.

O intervalo artificial de 7,6 segundos entre todas as chamadas foi removido. Em teste com relógio controlado, oito créditos livres iniciam sem avanço do relógio; antes, a oitava chamada só iniciava após 53,2 segundos de espera artificial. Isso mede o agendamento, não a latência real do provedor nem um SLA de execução.

Ao acabar o orçamento, a espera curta continua limitada pelo prazo configurado do servidor. Para uma espera maior, o backend devolve HTTP503 com `codigo=AGUARDANDO_COTA`, `tentarAposSegundos` e header `Retry-After`, antes de chamar o provedor. O frontend espera com um timer cancelável e faz até duas novas tentativas apenas dessa condição local. Nenhuma cotação ou horário é persistido nessa rejeição.

HTTP429 ou limite recebido da API externa continuam produzindo cooldown e falha parcial; não são repetidos automaticamente. Não foi aumentado o número de créditos ou alterado o plano. O orçamento é por instância: outras aplicações usando a mesma chave também consomem a cota do provedor.

O Angular utiliza duas filas independentes: brasileiras e internacionais. Cada provedor continua sequencial, mas uma resposta internacional pendente não bloqueia a fila brasileira. A ordem do resultado final segue a lista original, o progresso conta respostas concluídas e erros401/403 interrompem ambas as filas. Sair da tela cancela timers e novas tentativas.

Lotes acima dos créditos disponíveis ainda podem aguardar a próxima janela e levar mais de um minuto. Agrupar símbolos em um request não eliminaria o custo de créditos por símbolo, conforme a [documentação Twelve Data](https://twelvedata.com/docs/llms/market-data/quote.md).

## Escopo preservado

Por orientação expressa do usuário, não foram alterados cadastro, validação, normalização ou roteamento de tickers, nem impostos novos filtros de país ou moeda. Também não houve mudança em autenticação, permissões, cadastro de perfil ou isolamento de carteiras. Não foram feitas consultas com chaves reais nem criados ativos no banco real para testar o cadastro.

## Validação

- Teste RED demonstrou a espera fixa anterior; outro teste RED demonstrou que uma consulta internacional pendente bloqueava a brasileira.
- `clean verify`:382 testes, zero falhas/erros/ignorados, com 887/898 linhas (98,78%) e 356/362 branches (98,34%), em 59 classes; limites 85/85 preservados.
- Angular: 206 testes aprovados, incluindo espera mínima, limite de tentativas, cancelamento, falhas externas sem repetição e interrupção por autenticação.
- Build Angular aprovado com avisos existentes de depreciação Sass/Bootstrap.
- Revisão independente sem achados concretos.
- Playwright: 18 cenários aprovados em Chrome, incluindo consulta internacional retida enquanto a brasileira conclui, cota compartilhada com câmbio, cooldown externo, cadastro, permissões e carteiras individuais. Um teste mobile existente foi sincronizado com a resposta HTTP da edição para evitar ler uma mensagem de sucesso anterior; não houve alteração na implementação do cadastro.

Os testes automatizados utilizam relógio controlado, H2 e provedores simulados. Não medem performance ou cobertura de símbolos das APIs externas reais.
