# CI e APKs — auditoria de 2026-11-06

O workflow Android continua executando em push, pull_request e workflow_dispatch,
com os mesmos testes, lint, contratos estruturais e variantes. A concorrência
cancela somente execuções anteriores do mesmo workflow, evento e ref. Push e PR
continuam separados para preservar os checks de integração; a duplicação entre
eventos permanece uma oportunidade pendente. Não foram adicionados filtros de
caminho, permissões ou serviços pagos.

O job agora tem limite de 30 minutos. Novos artefatos debug/relatórios ficam por
14 dias. Os existentes conservam sua expiração; nenhum artefato ou release foi
excluído. Guarde localmente um candidato importante antes da expiração. Reverter
a retenção não recupera artefatos que já tenham expirado.

## Executar, diagnosticar e obter o APK

1. Em Actions → Android → Run workflow, escolha a branch/ref desejada. Confira
   o SHA na execução; em PR, o commit testado pode ser o merge de integração.
2. Exija sucesso de Verificar contratos estruturais e Testar, verificar e
   compilar. Em falha, abra o job build e o primeiro passo vermelho. Relatórios
   são enviados mesmo em falha, se existirem; um APK antigo/parcial não prova
   sucesso da execução inteira.
3. Baixe finder-to-google-debug-e-relatorios na execução correta. O APK está
   originalmente em app/build/outputs/apk/debug/ e os relatórios em
   app/build/reports/; o ZIP pode omitir os prefixos comuns desses caminhos. Registre
   SHA, URL/ID da execução, variante debug, versão e SHA-256 do APK extraído.
4. Verifique o arquivo com sha256sum e compare com o hash registrado na origem.
   Execute `apksigner verify --verbose --print-certs caminho/do/app-debug.apk`
   e `apkanalyzer manifest min-sdk caminho/do/app-debug.apk`, com as ferramentas
   do SDK no PATH.
   Confira minSdk 29 e applicationId br.com.findertogoogle. Compare certificado
   com a versão já instalada antes de tentar atualizar.
5. Teste o APK no aparelho alvo conforme VALIDACAO_DISPOSITIVO.md. Build do APK
   de instrumentação não executa testes físicos nem comprova o comportamento do
   Finder da One UI. Após alteração relevante, repita os mesmos checks.

Com JDK 21 e SDK preparados, o comando local equivalente é:

```sh
python3 scripts/check_project.py
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --stacktrace
```

Debug continua debug, com a assinatura já adotada. Não é release de produção;
a assinatura release e o procedimento no README permanecem separados.

## Evidência, custo e rollback

Baseline: main 3bdd2d2a9b978e16fc74a1bed5d76d1c33f3d5d3; execução
42023619413 bem-sucedida, job de 133 s, artefato de 5.761.616 bytes, retenção
observada de 90 dias. O runner ubuntu-latest padrão em repositório público tem
compute gratuito conforme a documentação oficial; nenhuma economia faturável
foi demonstrada para esta conta. Concorrência reduz consumo gratuito obsoleto;
timeout limita travamentos; retenção reduz armazenamento futuro. Billing e
budgets pessoais exigem acesso separado e não foram alterados.

Validação local desta mudança: actionlint 1.7.12, parsing YAML, comparação dos
passos/permissões/triggers com o original, git diff --check e os seis testes de
scripts/check_project.py. Build Android completo depende do resultado da CI;
não confundir essas verificações locais com um build completo.

Rollback: antes do merge, fechar o PR deixa main intacta. Depois do merge,
reverter o commit da alteração. Os comandos Gradle, SDK, assinatura e nomes dos
artefatos não mudaram.

Fontes: https://docs.github.com/en/billing/concepts/product-billing/github-actions
e https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions
