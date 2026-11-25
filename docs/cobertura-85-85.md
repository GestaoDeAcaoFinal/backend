# Cobertura JaCoCo 85/85 — 25/11/2026

A meta global (`BUNDLE`) é de pelo menos 85% das linhas e 85% dos branches. As propriedades `jacoco.minimum.line.coverage` e `jacoco.minimum.branch.coverage` permanecem em `0.85`.

## Resultado medido

| Indicador | Antes | Primeira ampliação | Atual |
| --- | --- | --- | --- |
| Testes | 187 | 260 | 334 |
| Linhas | 785/836 — 93,90% | 800/836 — 95,69% | 825/836 — 98,68% |
| Branches | 248/340 — 72,94% | 303/340 — 89,12% | 334/340 — 98,24% |
| Classes analisadas | 57 | 57 | 57 |
| `clean verify` com limite 85/85 | Falha de cobertura | Sucesso | Sucesso |

O denominador da medição não foi reduzido: nenhuma classe foi excluída e nenhum arquivo de produção foi alterado. A primeira ampliação acrescentou 73 casos; o complemento de config acrescentou 26, o de cliente.support acrescentou três, o de operações acrescentou 22, o de CVM acrescentou 18 e o de UsuarioDetailsService acrescentou cinco, verificando decisões já implementadas.

## Casos acrescentados

- `ClientesValidacaoTest`: 51 casos com respostas HTTP simuladas para Brapi, Twelve Data, BrasilAPI e ViaCEP. Cobrem corpo ausente, campos nulos/vazios, preços e taxas não positivos, classificação de erros, preservação de mensagens seguras e recuperação do cache após falhas.
- `TwelveDataCacheTest`: quatro casos para configurações fora dos limites, cache desabilitado e expiração exatamente no limite máximo de uma hora, usando relógio controlado.
- `UsuarioServiceTest`: 15 casos para manutenção do último ADMIN ativo, presença de outras contas sem permissão/inativas, alterações permitidas quando existe outro ADMIN ativo, versões de sessão e senha de usuário inexistente. Rejeições verificam que não há persistência nem alteração do usuário.
- `ApiExceptionHandlerTest`: três casos para agrupamento de mensagens de validação, nomes de parâmetros indisponíveis e texto padrão quando uma mensagem não é fornecida.

Os quatro clientes, `UsuarioService` e `ApiExceptionHandler` atingiram 100% dos branches no relatório desta execução. Isso não significa que todo o backend ou todos os comportamentos possíveis estejam cobertos.

## Complemento do pacote config

O pacote passou de 57/75 linhas (76%) e 14/24 branches (58,33%) para **75/75 linhas e 24/24 branches, ambos 100%**. As três classes também atingiram 100% de instruções:

| Classe | Linhas | Branches |
| --- | --- | --- |
| `AdminInitializer` | 15/15 | 2/2 |
| `JwtProperties` | 8/8 | 14/14 |
| `SecurityConfig` | 52/52 | 8/8 |

- `AdminInitializerTest`: seis casos para criação com hash BCrypt, preservação de quatro combinações de papel/status de contas existentes e idempotência. Usa repositório simulado e não cria usuários no banco de desenvolvimento.
- `JwtPropertiesTest`: 14 casos para segredo nulo/curto, limite de 32 bytes UTF-8, duração nula/zero/negativa/positiva e emissor nulo/vazio/em branco.
- `AuthResourceIntegrationTest`: seis novos casos HTTP com JWTs assinados pelo encoder real de teste. Claims ausentes, vazios, múltiplos, duplicados ou apenas com scope são rejeitados; um único papel ADMIN válido com scope adicional é aceito. Os claims de identidade e versão permanecem válidos para isolar a validação das permissões.

`AdminInitializer` é exclusivo do perfil `dev`, enquanto a suíte Spring utiliza `test`; sua lógica é exercitada diretamente nos novos testes unitários. Nenhum perfil de produção foi ativado na suíte. Os 40 testes focados de config/autenticação passaram antes da execução limpa completa com 286 testes.

## Complemento do pacote cliente.support — 25/11/2026

O pacote passou de 37/40 branches para **40/40 branches (100%)**. As 47 linhas e as 320 instruções também ficaram totalmente cobertas em `TwelveDataLimitador` e `ProvedorHttpExecutor`.

- `TwelveDataLimitadorTest`: uma consulta mantém a trava enquanto uma segunda expira sem executar seu fornecedor. O teste sincroniza os trabalhadores com latches, usa futures com prazo e confirma que o limitador pode ser reutilizado depois da liberação. Não usa `Thread.sleep` nem consultas externas.
- `ProvedorHttpExecutorTest`: dois casos de HTTP429 sem `Retry-After`, com cabeçalhos nulos ou existentes. Verificam a espera padrão de 60 segundos, a classificação como limite, a mensagem sem corpo sensível e a ausência de repetição automática da chamada.

Os 16 testes focados passaram, assim como o `clean verify` completo com 289 testes. A revisão independente não identificou problemas. Não houve alterações na política de consultas nem na implementação dos clientes.

## Complemento de OperacaoAcaoService — 25/11/2026

A classe passou de 41/48 branches e 148/149 linhas para **48/48 branches e 149/149 linhas (100%)**. Suas 729 instruções também ficaram totalmente cobertas.

Os 22 novos casos de `OperacaoAcaoServiceTest` verificam:

- Ausência de autenticação, sessão não autenticada e usuário ausente/inativo, tanto na consulta quanto na compra. Confirmam a rejeição antes de acessar operações ou persistir dados e o uso da busca com bloqueio na escrita.
- Quantidades nulas/negativas e preços nulos, negativos, zero ou não finitos, com mensagens específicas e ausência de persistência.
- Compra sem cotação disponível: rejeição quando nenhum preço é informado e aceitação de preço explícito válido, preservando proprietário, quantidade e totais.
- Resumo que ignora referências incompletas retornadas pelo repositório e mantém os totais da posição válida. São fixtures defensivas em memória, não registros inseridos no banco ou aceitos pela API.
- Moeda vazia/em branco e BRL sem distinção de maiúsculas/minúsculas, sem consumo do provedor de câmbio e com valores financeiros preservados.

Os 40 testes focados passaram. O `clean verify` completo passou com 311 testes, zero falhas, erros ou testes ignorados, mantendo os limites globais 85/85. A revisão independente não identificou problemas. Nenhuma regra de operação, autenticação ou isolamento foi modificada.

## Complemento do pacote services.cvm — 25/11/2026

O pacote passou de 73/79 linhas e 29/40 branches para **78/79 linhas (98,73%) e 39/40 branches (97,50%)**.

| Classe | Linhas | Branches |
| --- | --- | --- |
| `CadastroCvmParser` | 51/52 — 98,08% | 25/26 — 96,15% |
| `CadastroCvmCache` | 26/26 — 100% | 14/14 — 100% |
| `CadastroCvmCache.Snapshot` | 1/1 — 100% | Não possui branches |

- `CadastroCvmParserTest`: 11 novos casos para ZIP nulo/vazio, diretórios e arquivos irrelevantes antes do CSV em subpasta, BOM UTF-8, ausência de CNPJ no cabeçalho, CNPJ ativo com quantidade incorreta de dígitos, aspas sem fechamento, cadastro vazio, deduplicação e normalização da situação. Usam ZIPs em memória e o parser real; as falhas têm mensagem pública sem conteúdo do arquivo.
- `CadastroCvmCacheTest`: sete novos casos para TTL nulo/zero/negativo, reutilização até um nanossegundo antes da expiração e recarga no limite exato, recuperação após falha inicial, falha de parsing após expiração sem devolver dados vencidos e dois leitores concorrentes compartilhando um único download e snapshot imutável. A concorrência tem prazos e encerramento das threads, sem `Thread.sleep` ou acesso a métodos privados.

O único ramo e a única linha restantes no parser são o tratamento de uma string iniciada por `U+FEFF`. A decodificação fixa Windows-1252 não produz esse caractere; um BOM UTF-8 real é decodificado como `ï»¿`, cujo tratamento foi exercitado. Não foram alteradas a codificação, a implementação ou as exclusões do JaCoCo para forçar 100%.

Os 24 testes focados de CVM passaram, assim como o `clean verify` completo com 329 testes, zero falhas, erros ou testes ignorados. A revisão independente não identificou problemas. Nenhuma consulta à CVM ou ao banco de desenvolvimento foi realizada.

## Complemento de UsuarioDetailsService — 25/11/2026

A classe atingiu **11/11 linhas, 2/2 branches e 44/44 instruções (100%)**.

`UsuarioDetailsServiceTest` acrescenta cinco casos: usuário inexistente lança `UsernameNotFoundException` com mensagem sem o identificador consultado; as quatro combinações ADMIN/USER e ativo/inativo preservam username, hash e autoridade exata e retornam o estado `enabled` correspondente. Também verificam que carregar os detalhes não altera os campos da entidade nem sua versão de sessão e não persiste dados.

Conta inativa é retornada como `UserDetails` desabilitado; a recusa de autenticação continua sob responsabilidade do Spring Security. O teste usa serviço real, repositório simulado e hash fictício, sem consultar contas reais.

Os cinco testes focados e o `clean verify` completo com 334 testes passaram, sem falhas, erros ou testes ignorados. A revisão independente não identificou problemas. Código de produção, regras de autenticação e limites de cobertura permaneceram inalterados.

## Como reproduzir

No PowerShell, na raiz do backend:

```powershell
.\mvnw.cmd clean verify
```

Em Linux/macOS:

```shell
./mvnw clean verify
```

Abra `target/site/jacoco/index.html` após a execução e atualize a página caso já estivesse aberta. O XML de medição fica em `target/site/jacoco/jacoco.xml`.

Os testes usam H2 e HTTP simulado; não consultam provedores reais de cotação nem o banco de desenvolvimento. A execução completa registrada teve zero falhas, erros ou testes ignorados. A revisão independente não apontou problemas.

Esta validação é local. A publicação e a sincronização do repositório conjunto são etapas posteriores; os números acima não representam uma nova execução remota do GitHub Actions.
