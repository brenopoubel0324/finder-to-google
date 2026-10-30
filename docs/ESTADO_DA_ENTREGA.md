# Estado atual

O projeto está no candidato **1.1.0-rc.1**. Consulte [a execução do plano](EXECUCAO_PLANO_0001.md) e [a validação do candidato](VALIDACAO_CANDIDATO.md).

O histórico abaixo descreve a entrega inicial 1.0.0 e não substitui as pendências atuais do S25/Android 17.

# Estado verificável da entrega

**APK debug gerado, assinado e verificado.** Atualizado em 29/10/2026 (horário de São Paulo).

## Produzido

- Projeto Android Kotlin com duas Activities, Material 3, Android 16 como alvo e mínimo Android 10.
- Interceptação HTTP/HTTPS elegível a navegador padrão; Google via WEB_SEARCH; navegador alternativo explícito.
- Política de origem incorporando a atualização posterior ao handoff: outro app ou desconhecido no navegador; assinatura experimental opcional e desligada por padrão.
- Recuperação de destinos ausentes, tratamento de exceções, seletor de navegador e pedido do papel com confirmação do usuário no app e no Android.
- Diagnóstico local opcional sem URL/termo, preferências locais, ausência de permissões sensíveis e serviços.
- Testes JUnit/Robolectric e instrumentados, verificações estruturais offline, workflow de CI e documentação.

## Executado neste ambiente

- O acesso à rede foi liberado e as ferramentas foram instaladas: Temurin JDK 21.0.13, SDK Platform 36, Build Tools 35.0.0, Platform Tools e Gradle 8.13. AGP 8.13.2 e Kotlin 2.2.21.
- Comando concluído com **BUILD SUCCESSFUL**: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --continue --console=plain --stacktrace`.
- **38 testes JUnit/Robolectric aprovados**, zero falhas, erros ou testes ignorados. Incluem parsing, política de origem, referrer falsificado, fallback, atualização/remoção de navegador, preservação de URL, preferências e telas nas APIs 36 e 29.
- **Android Lint: zero erros e 17 avisos**, referentes a versões mais recentes de ferramentas/dependências, sugestões KTX, string não usada, regras explícitas de extração de dados e sugestão ortográfica. O manifest declara `allowBackup=false` e não pede INTERNET nem permissões sensíveis. AndroidX adiciona sua permissão interna de assinatura, sem diálogo ao usuário.
- **APK debug gerado** em `app/build/outputs/apk/debug/app-debug.apk`; entregue também como `Finder-to-Google-debug.apk` (aproximadamente 14 MB).
- Pacote `br.com.findertogoogle`; versão 1.0.0 (código 1); minSdk 29; targetSdk/compileSdk 36. Metadados conferidos com `aapt2 dump badging`.
- **Assinatura verificada** com `apksigner verify`: APK Signature Scheme v2, certificado Android Debug, RSA 2048.
- SHA-256: `0fafbffd722462347daca7a5550444bd80b838c56a3e0115b2e400cf11984085`.
- APK de testes instrumentados compilado em `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- Cinco verificações estruturais offline aprovadas. O registro antigo `VERIFICACOES_OFFLINE.txt` foi preservado como histórico da etapa anterior à liberação de rede.
- O catálogo usa somente APIs públicas (`GET_RESOLVED_FILTER` e `IntentFilter`), com suporte genérico HTTP/HTTPS e exclusão do próprio pacote. Os testes Android 16 exigem JDK 21; README e CI foram atualizados.
- Relatórios: `app/build/reports/tests/testDebugUnitTest/index.html` e `app/build/reports/lint-results-debug.html`. Resumo em `VERIFICACAO_BUILD.txt`.

## Não executado / não comprovado

- Os quatro testes instrumentados foram compilados, mas não executados em emulador/aparelho.
- Nenhum teste em Samsung físico/One UI foi executado. Elegibilidade/concessão efetiva do papel, repasse do Finder e Google App nativo precisam de validação real.
- O workflow de CI foi criado como código; não foi publicado nem executado remotamente.
- App Links verificados e intents destinadas diretamente a outro pacote podem contornar o roteador. No modo padrão, origem desconhecida vai ao navegador; a assinatura de URL só se aplica quando o usuário liga a opção experimental.

O README fornece comandos reproduzíveis para compilar, instalar, configurar e testar. A matriz de validação separa o modo conservador, o modo experimental e os casos que dependem de identidade compartilhada pelo sistema.
