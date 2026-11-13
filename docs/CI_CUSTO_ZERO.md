# CI e APKs — operação atualizada em 13/11/2026

O nome histórico deste arquivo não representa garantia de fatura zero. A revisão aplica a skill ci-eficiente ao Finder to Google; não altera orçamento, plano, permissões de compartilhamento, segredos, APKs ou assinatura de produção.

## Perfis e ponto de validação completa

O workflow **Android**, job/check **build**, continua sendo a entrada estável. Não há filtro de caminhos que deixe o check de um PR pendente. A decisão ocorre dentro do job e fica registrada no resumo da execução.

| Perfil | Quando | Execução |
|---|---|---|
| Documentação (`docs`) | Somente README/CHANGELOG/LICENSE e Markdown em docs | Contratos estruturais + testes Python; sem JDK/SDK/Gradle/uploads de rotina |
| Cotidiano (`daily`) | Lista explícita de apresentação, layouts/cores/textos/desenhos e testes unitários | Contratos/Python + testDebugUnitTest, lintDebug, assembleDebug |
| Completo de CI (`complete`) | Outros caminhos, risco, base indisponível, diff vazio/desconhecido, tags v* e execução manual | Cotidiano + assembleDebugAndroidTest, lintRelease, assembleRelease/R8 sem chave e inspeção do APK compilado |

Dados/routing/system, SettingsActivity/SettingsTransferUi e outras áreas não listadas como apresentação, manifests/permissões, res/xml, assets/JSON, build/dependências/toolchain, ProGuard, nativo, scripts, instrumentação, release e o próprio CI promovem ao completo. Extensão Markdown dentro de assets não é documentação. JSON em docs também é conservadoramente completo. O gate de perfil rejeita saída ausente/inválida do seletor; falha, cancelamento, timeout ou inspeção inválida não viram sucesso.

O completo de CI é **validação de compilação/otimização sem chave**, não uma release distribuível. A etapa remove as quatro variáveis FINDER_RELEASE_* e exclui explicitamente apenas verifyReleaseSigning nessa execução sem credenciais. O build.gradle não foi alterado. Com configuração de assinatura incompleta, o AGP prepara APK unsigned. O inspetor recusa container assinado e confere pacote/versão/SDKs, depuração, permissões, componentes e backup no manifest compilado; o relatório diz signature_verified=false e production_release=false. O APK unsigned **não é enviado nos artefatos**.

**Antes de entregar uma release**, continua obrigatório compilar a variante com as quatro variáveis privadas, sem exclusão de tarefa, executar verifyReleaseSigning, apksigner e o gate do certificado definitivo em scripts/prepare_release.py, além do smoke do APK exato quando houver dispositivo. Os testes de autorização, persistência, recuperação e calibração permanecem na suíte de unidade; compile de instrumentação não executa esses testes no Android físico. Nenhuma chave foi movida ao GitHub para esta otimização.

## Eventos, diff, concorrência e cache

- Push em **main** valida integração; PR direcionado a main valida o checkout do merge; tags **v*** e workflow_dispatch sempre selecionam completo. Branches de trabalho usam PR/manual, evitando push + PR duplicados. Não existe nova publicação automática.
- Push compara **event.before até github.sha**, incluindo todos os commits do push. PR compara o base.sha do evento com o merge efetivamente em HEAD. Git --no-renames trata rename como exclusão e inclusão, sem perder o caminho original. Saída NUL não quebra nomes com espaços/quebras de linha. Não há paginação/API limitada a 300 arquivos.
- Checkout raso é mantido; busca somente o commit base quando necessário, com timeout. Base ausente/inacessível ou head divergente promove ao completo, nunca a documentação. Erro interno inesperado do seletor falha o job.
- Somente **PR obsoleto** é cancelado por concorrência. Integração, tags e manuais usam grupo por run: um push só de docs não pode cancelar um completo de código em main, nem substituir um completo ainda pendente. O diff acumulado do PR conserva o risco ao cancelar um merge obsoleto.
- Timeout de 30 minutos preservado. Cache Gradle existente reaproveitado; PR é explicitamente somente leitura. Não foi criado cache amplo do SDK. Token contents:read, checkout sem credencial persistida, sem pull_request_target e sem secrets de produção em código de PR.

## Artefatos e distribuição

Cotidiano/documentação bem-sucedidos não enviam APK/relatórios a cada push. Falhas e completos de push/PR enviam relatórios/mapping por sete dias. Manual/tag completo bem-sucedido envia um único pacote debug + relatórios/mapping por 14 dias, sem duplicar o upload de relatórios. A ausência do APK debug esperado falha a conferência; APK unsigned nunca integra os caminhos de upload. Artefatos históricos não foram apagados e conservam sua expiração original.

O debug do runner tem certificado próprio e não substitui a atualização debug assinada localmente entregue ao usuário. As chaves definitivas/debug locais continuam fora do Git. A branch **downloads é órfã e não contém este workflow**: ela não é aprovada implicitamente pelo nome da pasta. Distribuição continua exigindo manifesto com commit-fonte, versão, certificado e SHA-256, evidência dos testes correspondentes, inspeção de assinatura/manifest, igualdade de bytes no ZIP e no download público, como nas entregas registradas em AUDITORIA_BACKUP.md. Binário ou JSON no diff do código-fonte não recebe dispensa de documentação. Não foi criado um novo caminho de publicação com menos verificações.

## Evidência desta revisão e pendências

- Base local/remota inicial: 44f2339, árvore limpa, sem AGENTS.md aplicável encontrado. Workflow completo, scripts, contratos e plano de UX/release consultados. Nenhum arquivo de app, versão, dependência ou chave foi alterado.
- Reconsultados três runs recentes: último sucesso 42023619413; duas recusas 42219405906/42219460309 por billing em 06/11. Anotação de 42219460309 lida novamente em 13/11: "The job was not started because your account is locked due to a billing issue." É evidência desse run antigo, não prova nova sobre o estado financeiro atual. Nenhum novo run foi disparado como diagnóstico.
- Proteção da branch main: consulta retornou **HTTP 403 Resource not accessible by integration**. Nome Android/build foi preservado; nenhuma proteção foi alterada nem declarada dispensável.
- Verificações locais: 17 testes Python aprovados (seletor com commits Git reais/CLI, checkout raso, PR/merge, push com vários commits, rename/exclusão, base ausente, manual/tag, formatos inválidos e gates de artefato); seis contratos estruturais; parsing YAML; actionlint 1.7.12 com checksum conferido no release oficial; diff sem erros.
- Gradle **dry-run** confirmou a cadeia R8/packageRelease sem verifyReleaseSigning/validateSigningRelease quando as variáveis privadas estão ausentes. Não compilou o app. O inspetor foi exercitado sobre fixture ZIP unsigned derivada de APK anterior e recusou o APK assinado original. Isso valida os contratos do inspetor, **não executa um novo R8 nem comprova o job em Actions**. Gate de produção preservado e testes de certificado errado continuam falhando como esperado.
- O gate de produção refatorado também foi aplicado ao APK release 1.1.1-rc.2 já existente, com apksigner/aapt2: assinatura definitiva e manifest aprovados. Não houve novo APK ou alegação de compilação para esta revisão.
- Não foram refeitos os testes Android, emuladores ou screenshots por esta alteração de orquestração. Os APKs 1.1.1-rc.2 e seus resultados anteriores permanecem intactos.

Após confirmar a regularização do bloqueio financeiro sem habilitar gastos adicionais, executar **uma execução manual Android completa na revisão publicada** e verificar logs, inspeção unsigned, relatórios e debug. Não repetir em loop nem chamar os checks locais de CI remoto aprovado. Para entrega assinada, executar também o gate de produção descrito acima. Orçamentos/parada por limite e fatura pessoais não foram acessados ou modificados. Timeout/concorrência não são teto financeiro.

Redução demonstrada na configuração: docs dispensam toda a cadeia Android, cotidiano dispensa instrumentação e uploads de rotina, branches com PR deixam de duplicar push/PR, e completo se concentra em riscos/entregas. O completo acrescenta cobertura R8 que não existia no antigo CI. **Minutos remotos e economia na fatura não foram medidos.** Reverter o commit desta revisão restaura o workflow anterior; não recupera artefatos já expirados e não modifica os APKs entregues.

## Registro histórico — auditoria de 06/11/2026

O texto abaixo preserva a evidência anterior e não descreve a configuração atual.

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
scripts/check_project.py. As execuções 42219405906 (push) e 42219460309 (PR),
do commit 651ac7628b7be01e738e77e773fdf2c0db18a097, foram recusadas antes
de iniciar o build: "The job was not started because your account is locked
due to a billing issue." Isso confirma o bloqueio da conta, sem identificar
produto ou dívida. O PR permanece em rascunho; build desta revisão está
BLOQUEADO. Não há justificativa para repetir tentativas antes de tratar billing.

Foi possível baixar o artefato anterior 13412478389 da execução 42023619413.
O SHA-256 do ZIP coincide com o digest publicado pelo GitHub:
3ddd04bc9fe8de3cdab4ac09f9c1c31635e14f1a84eec185c0c70ec85380a443.
O APK extraído, outputs/apk/debug/app-debug.apk, tem 14.089.737 bytes e SHA-256
e1d88bbbd8e812a6440791a364e3db182059b66acf5344bb466ce0a3add29177.
Integridade ZIP de ambos aprovada. Assinatura e compatibilidade no dispositivo
não foram revalidadas. Esse artefato pertence ao baseline, não à nova revisão.

Esta atualização documental usa [skip ci] para evitar mais tentativas enquanto
a conta está bloqueada. Após verificar os controles financeiros sem habilitar
despesas, executar o workflow na ref da revisão; criar commit revisado sem
marcador se necessário para os checks de PR. Não integrar sem validar o build.

Rollback: antes do merge, fechar o PR deixa main intacta. Depois do merge,
reverter o commit da alteração. Os comandos Gradle, SDK, assinatura e nomes dos
artefatos não mudaram.

Fontes: https://docs.github.com/en/billing/concepts/product-billing/github-actions
e https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions
