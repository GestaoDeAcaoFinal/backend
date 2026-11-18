# Controle de consultas Twelve Data — 18/11/2026

## Problema e causa

O diagnóstico autorizado da conta informou 8 créditos/minuto e 800/dia. O lote com nove ações americanas e a consulta USD/BRL da carteira consumiam a mesma cota sem coordenação. O erro429 era genérico e a resposta JSON429 podia ser classificada como ticker inexistente. Esse comportamento já existia antes da última alteração de ticker.

## Comportamento corrigido

- `TwelveDataLimitador` é singleton no backend e atende **quote e exchange_rate**, incluindo consultas manuais, cadastro, atualização geral e consolidação. Usa relógio monotônico, lock justo e espaçamento conservador: 60.000/8 +100ms =7.600ms entre inícios de chamadas. A primeira chamada pode ser imediata.
- Cada requisição HTTP continua limitada pelos timeouts existentes (conexão2s/leitura5s). A espera para obter o lock tem limite15s; esperas por permissão maiores que o limite também são recusadas com503. Não há fila persistente, job ou endpoint novo.
- HTTP429 e JSON429 geram503 com orientação específica de limite. HTTP Retry-After numérico/data é interpretado de forma segura; sem header válido o cooldown é60s. Não há retry automático: novas consultas externas são evitadas durante o cooldown. Autenticação do investidor não é encerrada por esse503.
- O câmbio validado é reutilizado por até300s por par de moedas; leituras concorrentes compartilham uma resposta. A coordenação do cache usa lock justo, temporizado e interrompível, com o mesmo limite de espera configurado no gate. Cache vencido exige consulta nova. Falhas, taxa nula/zero/negativa e erros do provedor não entram no cache. Não há fallback para câmbio vencido nem taxa inventada.
- Cotações de ações não são cacheadas pelo novo mecanismo: o horário só avança após resposta válida e persistência, mantendo a regra existente. Falhas preservam preço/horário anteriores.
- Angular informa progresso por resposta e mantém o botão ocupado até concluir/cancelar. Dados são recarregados uma vez ao término; lotes são cancelados ao sair da tela. Durante carregamento da carteira/catalogo, o botão de lote fica bloqueado.

## Configuração

Em application.properties, com defaults e variáveis opcionais (sem segredos):

| Variável | Default | Uso |
|---|---:|---|
| TWELVE_DATA_CREDIT_LIMIT | 8 | Créditos por janela |
| TWELVE_DATA_WINDOW_MS | 60000 | Janela em milissegundos |
| TWELVE_DATA_MARGIN_MS | 100 | Margem adicional entre chamadas |
| TWELVE_DATA_MAX_WAIT_MS | 15000 | Limite de espera |
| TWELVE_DATA_FX_CACHE_SECONDS | 300 | Validade do câmbio;0 desativa cache |

O controle é **por instância/processo do backend**. Duas instâncias ou outras aplicações usando a mesma chave precisam de orçamento compartilhado externamente ou limites individuais reduzidos; este patch não inclui controle distribuído. Reinícios perdem cache/contadores/cooldown. Consumo externo pode ocasionar429 mesmo com o espaçamento: a falha então é informada e as próximas chamadas são suspensas temporariamente. Cota diária não é renovada pelo limitador; não há tentativa infinita em caso de esgotamento.

## Validação

Validação final: `mvn verify`, **187 testes aprovados**, zero falhas/erros/ignorados e gatesJaCoCo atendidos. Inclui HTTP/JSON429, orçamento compartilhado9quotes+FX, concorrência, cooldown, interrupção e espera limitada; cacheTTL/expiração/erro/concorrência; espera do cache interrompível/limitada; interpretação de Retry-After; persistência de cotação/timestamp após falha. **15 cenários E2E aprovados novamente após a correção da espera do cache**, com Angular/Spring/H2 reais e servidor HTTP que aplica8 créditos por janela reduzida (800ms). Angular164 testes e build483,67kB aprovados. A janela reduzida permite verificar a lógica sem esperar minutos ou usar chaves reais. Captura da carteira390px conferida e fluxos ADMIN/USER mantidos.

Nenhuma credencial foi gravada em novos arquivos e nenhuma chamada adicional a provedores reais é necessária para validar este patch. Não há mudança de JWT, papéis, isolamento, endpoints ou schema de dados. Antes de publicar frontend/CI, publicar backend corrigido e atualizar o pin remoto do launcher E2E.
