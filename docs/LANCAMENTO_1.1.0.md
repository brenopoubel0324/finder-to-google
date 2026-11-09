# Preparação do lançamento 1.1.0

Canal escolhido: APK no GitHub. Versão preparada: **1.1.0, versionCode 7**, minSdk 29, target/compileSdk 36, R8/redução de recursos ativos, assinatura definitiva preservada. A versão é preparada em rascunho; sua numeração não declara QA físico concluído.

## Evidência no Samsung

Em 9 de novembro de 2026, o usuário confirmou que a **rc.5 abriu uma pesquisa real do Finder no Google App do S25 com Android 17**. Modelo/versões são informados pelo usuário; One UI 9 foi informado anteriormente. É um teste individual do fluxo principal, não a execução de toda a matriz nem a validação do novo APK release. O tablet teve confirmação anterior. Não foram medidos SUS/tempos de tarefa.

Desde a rc.5, nenhuma regra de roteamento ou fluxo de interface foi alterado nesta preparação. Mudaram versão, documentação e ferramentas de entrega. O APK release exato, com R8 e chave definitiva, ainda exige instalação e smoke.

## Processo reproduzível

1. Use uma árvore limpa no commit destinado à distribuição e JDK 21/SDK 36/Build Tools 35.0.0.
2. Disponibilize as quatro variáveis privadas `FINDER_RELEASE_*` conforme `.env.example`. Nunca passe senhas em argumentos, imprima variáveis ou envie o keystore ao GitHub.
3. Execute os checks e gere a release a partir desse commit:

   ```sh
   python3 scripts/check_project.py
   python3 -m unittest discover -s scripts -p 'test_*.py'
   ./gradlew testDebugUnitTest lintDebug lintRelease assembleDebug assembleDebugAndroidTest assembleRelease
   python3 scripts/prepare_release.py \
     --apk app/build/outputs/apk/release/app-release.apk \
     --output /caminho/fora-do-repositorio/finder-1.1.0
   ```

4. O empacotador verifica assinatura, certificado, pacote/versão/SDKs, ausência de depuração, permissões/componentes inesperados e backup. Copia somente APK, checksum, manifesto e documentos explícitos ao ZIP, validando os bytes internos. Não inclui keystores, variáveis, diretórios privados, fontes ou relatórios indiscriminadamente. A pasta de saída precisa ser nova.
5. Registre os resultados em `RESULTADO_LANCAMENTO_1.1.0.md`. Guarde `mapping.txt` do R8 e relatórios em local de manutenção separado, ligados ao commit e hash do APK.

O manifesto da entrega registra o commit da árvore limpa. Isso pressupõe compilar no mesmo commit antes de empacotar; analisar um APK isolado não prova sua correspondência com os fontes. O script não concede aprovação física e registra essa pendência.

## Teste do APK release exato — antes da publicação

Não atribuir à release o resultado obtido com debug. Use o SHA-256 do arquivo entregue.

| Teste no Samsung | Resultado a registrar |
|---|---|
| Instalar release; seguir a migração se a instalação atual for debug | Abre sem crash e mostra 1.1.0 |
| Escolher navegador; cancelar e depois conceder papel padrão | Estado real, sem sucesso após cancelamento |
| Pesquisa real do Finder e link comum de outro app | Google App / navegador escolhido, respectivamente |
| Calibrar padrão e reiniciar app/aparelho | Regra persiste; novo termo funciona sem serviço |
| Alternar modo, diagnóstico e regras; copiar/apagar diagnóstico | Opções persistem e não há URL/termo no registro |
| Navegador/Google indisponível, se viável em perfil de teste | Recuperação/fallback sem loop; reabilitar após teste |
| Temas, fonte 200%, teclado, TalkBack e Voltar | Ações alcançáveis, legibilidade e navegação |
| Atualizar uma release anterior de mesma chave, se disponível | Preferências preservadas |

Não desabilitar nem remover apps necessários do aparelho principal apenas para simular falha. Casos que não puderem ser executados devem ser registrados como pendentes. O roteiro extenso está em `VALIDACAO_DISPOSITIVO.md`.

## Chave e recuperação

- Confirmar uma cópia cifrada durável fora deste workspace e sua recuperação pelo proprietário. A cópia local do ambiente não prova custódia durável.
- Confirmar que a chave recuperada tem certificado SHA-256 `a99bd676fb3a8603605fd94e2fd0cbc7cf36f9d313623931635fa4e1780741c8` sem imprimir senhas ou compartilhar a chave.
- Toda atualização futura usa essa chave e versionCode maior que 7. Nunca publicar um APK debug como atualização release.

## GitHub e aprovação final

Preparar release **em rascunho** `v1.1.0`, ligada ao commit de compilação. Incluir APK, SHA-256, ZIP, manifesto, instruções e limitações; downloads devem apontar para arquivos imutáveis. Se o endpoint de assets estiver indisponível, arquivos na branch downloads com links pelo SHA do commit preservam uma alternativa verificável. Não sobrescrever arquivos de candidatos anteriores.

Após o smoke do APK release exato e a custódia da chave confirmados, registrar resultados e publicar a release a pedido do proprietário. Capturas promocionais devem ser reais e identificadas por versão/dispositivo; prévias Robolectric não devem ser apresentadas como capturas do Samsung.

O repositório contém registro de bloqueio por billing do GitHub Actions em 6 de novembro. Esta preparação usa validação local e commits `[skip ci]`, preservando timeout/concorrência/retenção. Não foram habilitados serviços pagos ou alterados controles financeiros; CI remoto anterior não valida este commit. Retomar checks remotos somente após resolver o bloqueio.

## Regressão após lançamento

Manter a rc.5 e os artefatos anteriores disponíveis. Se aparecer uma regressão, orientar trocar o navegador padrão para restabelecer a navegação e preparar uma correção com **code maior** e a mesma chave. Instalar uma versão antiga por cima não é um rollback garantido. Issues devem informar versão, modelo, Android/One UI e resultado esperado/observado, sem URLs pessoais.
