# Resultado da preparação — 09/11/2026

**APK release pronto para teste final; lançamento público em rascunho.** Canal escolhido pelo proprietário: GitHub. A confirmação da rc.5 no S25/Android 17 foi registrada separadamente do novo APK.

## Artefato exato

| Campo | Valor |
|---|---|
| Aplicação | `br.com.findertogoogle` |
| Versão / código | `1.1.0` / `7` |
| Commit de compilação | `0766545c12287ce06dcf0ac7a97b4c5548be3569` |
| APK | `Finder-to-Google-1.1.0-release.apk` |
| Tamanho | 2.504.519 bytes |
| SHA-256 | `588131bdfdcb275bbc4bdb523476803e02ec82896f80417276d1732d40e17f13` |
| Certificado SHA-256 | `a99bd676fb3a8603605fd94e2fd0cbc7cf36f9d313623931635fa4e1780741c8` |
| SDK mínimo / alvo | 29 / 36 |
| Depuração | Desabilitada |
| Minificação / recursos | R8 e redução de recursos habilitados |

O commit de compilação é preservado como alvo do rascunho. Os commits posteriores ajustam ferramentas/documentação e revalidam o mesmo APK, sem mudança de bytes ou atribuição fictícia de um novo build ao commit de relatório.

## Validação executada

- Gradle `testDebugUnitTest lintDebug lintRelease assembleDebug assembleDebugAndroidTest assembleRelease`: BUILD SUCCESSFUL em 2 min 53 s. 106 testes JUnit/Robolectric, zero falhas/erros/ignorados.
- Lint debug e release: zero erros/fatais, 21 avisos em cada variante, mantendo os avisos previamente avaliados.
- Após ajuste do verificador, `lintRelease assembleRelease`: sucesso incremental no commit de compilação acima.
- Seis contratos estruturais e seis testes do empacotador aprovados. Testes negativos de versão/certificado, debug, permissão extra, componentes adicionais, alias e proteções AndroidX.
- Verificador aplicado ao APK real aprovado; debug novo e release rc.5 rejeitados como entrega 1.1.0/code 7. Conferência repetida após a correção de alias, sem alterar o APK.
- `apksigner verify` aprovado com certificado definitivo; manifest compilado conferido. Além das duas Activities do app, AndroidX adiciona InitializationProvider não exportado e ProfileInstallReceiver protegido por DUMP. Não há serviço/INTERNET; a única permissão solicitada é a interna AndroidX de assinatura.
- ZIP com lista explícita de arquivos, integridade e igualdade dos bytes internos do APK conferidas. Chaves e senhas excluídas da entrega pública.
- `git diff --check` e autoauditoria em duas rodadas aprovados no escopo técnico. Nenhum achado bloqueante/alto pendente. Duas correções médias no novo verificador (componentes AndroidX e prefixo activity-alias) têm regressões.

Build de instrumentação não significa execução de seus testes; nenhum smoke de Android nativo ou nova certificação física foi realizado neste ambiente. Sem KVM/dispositivo Samsung conectado, o teste do APK release exato depende do proprietário. Não houve medição de SUS ou tempo de tarefas.

## Entrega

Release `v1.1.0` criada em rascunho, não publicada como estável. O upload de anexos retornou HTTP 401 em uploads.github.com; a API de releases e o envio Git funcionam. Foram usados arquivos na branch downloads, com igualdade de blobs Git/tamanhos verificada e links fixados pelo commit `88ee541eafda421af62f70ecf127ae92a8daa4c5`:

- [APK release](https://github.com/brenopoubel0324/finder-to-google/raw/88ee541eafda421af62f70ecf127ae92a8daa4c5/1.1.0/Finder-to-Google-1.1.0-release.apk)
- [SHA-256](https://github.com/brenopoubel0324/finder-to-google/raw/88ee541eafda421af62f70ecf127ae92a8daa4c5/1.1.0/Finder-to-Google-1.1.0-release.apk.sha256)
- [ZIP com instruções e privacidade](https://github.com/brenopoubel0324/finder-to-google/raw/88ee541eafda421af62f70ecf127ae92a8daa4c5/1.1.0/Finder-to-Google-1.1.0-release.zip)
- [Manifesto da entrega](https://github.com/brenopoubel0324/finder-to-google/raw/88ee541eafda421af62f70ecf127ae92a8daa4c5/1.1.0/release-manifest.json)

Os downloads para validação são públicos; o lançamento final permanece em rascunho. Candidatos anteriores foram preservados. Mapping do R8 e relatórios foram preservados separadamente no ambiente de manutenção, associados ao commit/hash, sem misturar com instalador ou chave privada.

O usuário autorizou entrega privada do backup de assinatura e de um arquivo separado de recuperação no Google Drive. Backup AES256 criado; conteúdo e descriptografia conferidos. Após upload, a cópia cifrada foi baixada novamente: igualdade de bytes e descriptografia aprovadas. Pasta e arquivos conferidos com `shared=false`, somente proprietário. Nenhum link/ID de chave, senha ou material privado foi incluído no repositório. O proprietário ainda deve baixar, guardar a senha fora do Drive e remover do Drive o arquivo com a senha após guardá-la com segurança.

CI remoto não foi disparado como evidência desta versão. Bloqueio de billing anteriormente registrado, controles preservados e commits `[skip ci]`; validação desta preparação é local.

## Próximo passo antes de publicar

Instalar e testar **este APK release** no S25 conforme [roteiro](LANCAMENTO_1.1.0.md), incluindo pesquisa real do Finder, link comum e retomada após reinício. Se a instalação atual for debug, a migração exige reinstalar uma vez conforme [instruções](INSTALACAO_RELEASE.md). Confirmar guarda do backup e senha. Depois registrar os resultados e publicar o rascunho a pedido do proprietário. A matriz completa de acessibilidade/falhas continua disponível para ampliar a validação sem inventar resultados.
