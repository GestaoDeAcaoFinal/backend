# Tickers com separadores — 16/11/2026

A validação do DTO e do serviço rejeitava `BRK.A` e `BRK.B` antes da chamada ao provedor. A rota de busca por ticker também tinha uma validação própria restritiva, identificada durante o E2E. `TickerNormalizer.FORMATO_TICKER` agora é compartilhado pelo DTO, normalizador e rota de busca: 1–12 caracteres, ao menos uma letra ASCII, blocos alfanuméricos separados por ponto. Trim e uppercase com Locale.ROOT permanecem. Pontos nas extremidades, repetidos, espaços internos, slash, hífen e caracteres de URL não são permitidos.

O [contrato Twelve Data](https://twelvedata.com/docs/llms/fundamentals/profile.md) identifica BRK.A/BRK.B com ponto. O cliente `/quote` existente preserva o símbolo em `queryParam`; os testes verificam as URLs exatas com chave exclusivamente sintética. Esses símbolos são classificados como EUA pelo classificador existente e consultados apenas na Twelve Data.

Unicidade continua por ticker normalizado e pela restrição única do banco: `brk.b` conflita com `BRK.B`, enquanto `BRK.A` é distinto. Busca e atualização preservam o ponto. Não há migração, endpoint novo ou mudança de autenticação/regra financeira.

Validação local final: `mvn verify` executou 157 testes, zero falhas/erros/ignorados, e aprovou os limites JaCoCo. Cobertura adicionada em DTO, normalizador, rota MVC, serviço e cliente HTTP. O launcher exclusivo de testes reconhece as duas classes em `/quote` para permitir E2E com Angular e H2 sem acessar APIs reais. O cenário de cadastro, busca, atualização e duplicidade de ambas as classes passou com o Angular real.

Disponibilidade e permissões da chave real do provedor não foram verificadas; formato válido não implica que todo símbolo exista ou esteja disponível no plano contratado. Símbolos de outros instrumentos e aliases de outros provedores não foram liberados automaticamente.
