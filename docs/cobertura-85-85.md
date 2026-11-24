# Cobertura JaCoCo 85/85 — 24/11/2026

A meta global (`BUNDLE`) é de pelo menos 85% das linhas e 85% dos branches. As propriedades `jacoco.minimum.line.coverage` e `jacoco.minimum.branch.coverage` permanecem em `0.85`.

## Resultado medido

| Indicador | Antes | Depois |
| --- | --- | --- |
| Testes | 187 | 260 |
| Linhas | 785/836 — 93,90% | 800/836 — 95,69% |
| Branches | 248/340 — 72,94% | 303/340 — 89,12% |
| Classes analisadas | 57 | 57 |
| `clean verify` com limite 85/85 | Falha de cobertura | Sucesso |

O denominador da medição não foi reduzido: nenhuma classe foi excluída e nenhum arquivo de produção foi alterado. Os 73 novos casos verificam decisões já implementadas.

## Casos acrescentados

- `ClientesValidacaoTest`: 51 casos com respostas HTTP simuladas para Brapi, Twelve Data, BrasilAPI e ViaCEP. Cobrem corpo ausente, campos nulos/vazios, preços e taxas não positivos, classificação de erros, preservação de mensagens seguras e recuperação do cache após falhas.
- `TwelveDataCacheTest`: quatro casos para configurações fora dos limites, cache desabilitado e expiração exatamente no limite máximo de uma hora, usando relógio controlado.
- `UsuarioServiceTest`: 15 casos para manutenção do último ADMIN ativo, presença de outras contas sem permissão/inativas, alterações permitidas quando existe outro ADMIN ativo, versões de sessão e senha de usuário inexistente. Rejeições verificam que não há persistência nem alteração do usuário.
- `ApiExceptionHandlerTest`: três casos para agrupamento de mensagens de validação, nomes de parâmetros indisponíveis e texto padrão quando uma mensagem não é fornecida.

Os quatro clientes, `UsuarioService` e `ApiExceptionHandler` atingiram 100% dos branches no relatório desta execução. Isso não significa que todo o backend ou todos os comportamentos possíveis estejam cobertos.

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
