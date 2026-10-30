# Validação do candidato 1.1.0-rc.1

Data: 30/10/2026. O estado abaixo refere-se aos APKs identificados em `EXECUCAO_PLANO_0001.md`.

| Verificação | Resultado |
|---|---|
| JUnit/Robolectric Android 16/API 36 e casos API 29 | 56 testes, zero falhas/erros |
| Verificador estrutural offline | 6 testes aprovados |
| Lint debug e release | Zero erros/fatais; 21 avisos por variante avaliados |
| `assembleDebug`, `assembleDebugAndroidTest` | Compilados |
| `assembleRelease` com R8 e shrinkResources | Compilado e assinado; APK 2.419.107 bytes |
| `verifyReleaseSigning` sem configuração | Falha esperada antes de empacotar release; debug não exige segredos |
| Assinatura APK debug | Verificada, mesmo certificado da versão 1.0.0 |
| Assinatura APK release | Verificada, RSA 4096, certificado definitivo novo |
| Manifest release | debuggable=false, sem INTERNET e sem serviço; permissão interna AndroidX de nível signature identificada |
| APK/checksum em ZIP | Bytes verificados; sem APK de testes, chave ou senhas |
| Downloads Drive | Uploads privados conferidos por tamanho/metadados |
| `git diff --check` | Aprovado |
| Instrumentação debug em emulador API 36 | 4 testes aprovados, zero falhas/ignorados; Gradle BUILD SUCCESSFUL em 4m53s |
| Smoke do release exato no emulador API 36 | Instalação concluída, SettingsActivity com Status: ok, processo confirmado |
| Teste físico do release no S25 Android 17/One UI 9 | Pendente |

## Instrumentação e limites do ambiente

Foi criado emulador API 36 com Google APIs, sem aceleração KVM. As tentativas iniciais não executaram testes: primeiro aparelho offline, depois serviço package indisponível durante boot. Esses resultados são falhas do ambiente e não testes aprovados. Após boot completo e serviço package disponível, nova tentativa executou os quatro testes com sucesso (zero falhas/ignorados, BUILD SUCCESSFUL em 4m53s). Testaram parser real, filtros/papel disponível, exclusão do roteador do catálogo e estado real do papel mostrado na tela. Essa execução usa a variante debug e não altera o papel padrão. Log: instrumentation-final.log.

Depois do término bem-sucedido da instrumentação debug, o mesmo APK release cujo hash consta no relatório foi instalado no emulador. A tela SettingsActivity abriu com Status: ok e o processo foi confirmado. A tentativa inicial de desinstalação coincidiu com a limpeza do runner e retornou DELETE_FAILED_INTERNAL_ERROR; a instalação subsequente, após o término do Gradle, foi bem-sucedida. Não houve alteração em aparelho físico. Esse smoke cobre instalação/abertura da variante minificada, não a integração real com Finder/Google.

Não há execução de Android 17 neste ambiente. Emulador Android 16 não comprova comportamento do Finder/Google da Samsung. O caso confirmado pelo usuário foi no tablet, com assinatura exata `tablet-android-samsung-rvo1`; o S25 permanece em investigação. A matriz física e a assinatura do candidato precisam ser conferidas antes de promover a final.

## Comandos reproduzíveis

```sh
python3 scripts/check_project.py
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
# Com as quatro variáveis privadas FINDER_RELEASE_* configuradas:
./gradlew lintRelease assembleRelease
# Com aparelho/emulador pronto:
./gradlew connectedDebugAndroidTest
git diff --check
```

Relatórios de build/testes são entregues separadamente do fonte. Não contêm keystore nem credenciais de assinatura. Chave e backup local não substituem custódia durável pelo proprietário.
