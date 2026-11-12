# Verificações de entrega

O workflow `.github/workflows/ci.yml` executa em pushes, pull requests e disparo manual. Usa Java 17, cache Maven e ações oficiais fixadas em revisão completa. As permissões são apenas de leitura; não publica imagens ou versões e não faz deploy.

Etapas: `mvn --batch-mode --no-transfer-progress -Dspring.test.context.cache.maxSize=2 verify`, limites de cobertura já definidos no `pom.xml` e build do Dockerfile. Relatórios de testes/cobertura ficam como artefatos por sete dias. Não requer segredos de banco ou de provedores: os testes usam H2 e configurações sintéticas.

O frontend `GestaoDeAcaoFinal/frontend` mantém uma revisão deste backend em `.ci/backend-ref.txt` e executa os testes ponta a ponta contra ela. Antes de publicar um frontend com novo pin, publique a branch correspondente deste backend. O commit indicado precisa estar acessível no GitHub.

O launcher `src/test/java/com/av2/gestaodeacao/e2e/E2eApplication.java` é exclusivo de teste. Ele inicia H2 em memória e um provedor HTTP simulado, e não integra a imagem/JAR de produção.

Validação local da consolidação: 140 testes e `verify`/cobertura aprovados; imagem atual construída. O smoke test pelo Nginx é mantido no frontend (`scripts/smoke-docker.mjs`) e usa as duas imagens em ambiente descartável, sem volumes ou banco real. O backend fica numa rede interna; apenas o Nginx recebe uma porta efêmera em `127.0.0.1`.

Workflows preparados e analisados localmente não representam uma execução no GitHub Actions. Essa evidência só existe depois da publicação autorizada dos commits e da conclusão do workflow remoto.
