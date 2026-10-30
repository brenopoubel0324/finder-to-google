# Finder to Google

Aplicativo Android nativo em Kotlin, interface Material 3 em português (Brasil), com tema claro/escuro. Funciona como roteador local de links: pode ocupar o papel de navegador padrão, enviar pesquisas do Finder ao Google App e encaminhar outras URLs ao navegador selecionado. Android 10+ (`minSdk 29`), compilação e destino Android 16 (`compileSdk`/`targetSdk 36`).

**APK debug gerado e assinatura verificada.** O SDK Android 16 e as ferramentas foram instalados após liberar o acesso à rede. O artefato entregue é `Finder-to-Google-debug.apk` (versão 1.0.0). Veja [o registro de validação](docs/ESTADO_DA_ENTREGA.md) para os resultados dos testes. A integração com o Finder em Samsung físico ainda precisa de validação.

## Regra de roteamento desta versão

Esta implementação inclui as regras de origem solicitadas após o handoff. Isso altera o comportamento original, que reconheceria buscas Google vindas de qualquer app:

| Origem disponível | Busca Google válida | Outros links HTTP/HTTPS |
|---|---|---|
| Samsung Finder reconhecido pelo sistema | Google App via `ACTION_WEB_SEARCH` | Navegador selecionado |
| Outro aplicativo reconhecido pelo sistema | Navegador selecionado | Navegador selecionado |
| Desconhecida, opção experimental desligada (padrão) | Navegador selecionado | Navegador selecionado |
| Desconhecida, opção experimental ligada | Google somente se `client=tablet-android-samsung-rvo1`; demais buscas no navegador | Navegador selecionado |

O pacote Finder reconhecido é `com.samsung.android.app.galaxyfinder`. `Activity.getLaunchedFromPackage()` (API 34+) e `callingPackage`, quando disponíveis, fornecem identidade do sistema. O Android frequentemente não a compartilha em aberturas comuns. `getReferrer()` é apenas uma pista visível no diagnóstico; extras do chamador podem falsificá-la. Não há promessa de identificação universal da origem.

A opção experimental é um reconhecimento de URL, não uma autenticação do Finder. Outro app pode gerar a mesma assinatura. Uma origem conhecida como outro app continua no navegador mesmo com a opção ligada. Se o firmware usar outra assinatura, será necessário verificar e atualizar a regra. O app não aceita um extra externo que force a origem Finder.

## Compilar

Requisitos: JDK **21** completo, Android SDK com plataforma **36**, Build Tools **35.0.0** (padrão do AGP 8.13) e acesso inicial à rede para dependências. Os testes Robolectric do Android 16 exigem Java 21; o bytecode do app continua com destino Java 17 e mínimo Android 10. Versões fixadas: AGP 8.13.2, Kotlin 2.2.21, Gradle 8.13. Não depende de Android Studio, mas ele facilita instalar SDK e importar o projeto.

1. No Android Studio, abra esta pasta e instale **Android SDK Platform 36** pelo SDK Manager.
2. Configure o SDK em `local.properties` (não versionado) ou defina `ANDROID_HOME`:

   ```properties
   sdk.dir=/caminho/absoluto/Android/Sdk
   ```

3. Pelo terminal, a partir desta pasta:

   ```sh
   chmod +x gradlew
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```

   No Windows, use `gradlew.bat testDebugUnitTest lintDebug assembleDebug`.

Se preferir as ferramentas de linha de comando do SDK:

```sh
sdkmanager 'platforms;android-36' 'build-tools;35.0.0' 'platform-tools'
sdkmanager --licenses
./gradlew testDebugUnitTest lintDebug assembleDebug
```

O APK debug, **quando a compilação tiver sucesso**, estará em `app/build/outputs/apk/debug/app-debug.apk`, assinado automaticamente com a chave debug local. Relatórios: `app/build/reports/tests/testDebugUnitTest/index.html` e `app/build/reports/lint-results-debug.html`. Uma versão release para distribuição exige sua própria assinatura; não há chave privada de produção no projeto.

### Primeiro uso do Gradle Wrapper / offline

O **JAR oficial do Gradle Wrapper 8.13 está incluído** em `gradle/wrapper/gradle-wrapper.jar`, com SHA-256 conferido. O wrapper baixa a distribuição Gradle, também com SHA-256 fixado em `gradle-wrapper.properties`. Se o pequeno JAR for removido, `gradlew`/`gradlew.bat` incluem um bootstrap Java auditável que pode obtê-lo e verificar seu checksum novamente.

Para preparar o JAR manualmente em uma máquina com rede:

```sh
curl --fail --location --output gradle/wrapper/gradle-wrapper.jar \
  https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar
echo '81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f  gradle/wrapper/gradle-wrapper.jar' | sha256sum --check
```

Checksum conferido na [lista oficial gradle/actions](https://github.com/gradle/actions/blob/v4/sources/src/wrapper-validation/wrapper-checksums.json). Para um build totalmente offline, prepare também SDK e caches de Gradle/Maven/Robolectric antes de desconectar e use `./gradlew --offline ...`. Só copiar o pequeno JAR não fornece essas dependências. Android Studio pode pedir a preparação do wrapper antes de sincronizar: execute o comando acima ou `./gradlew --version` primeiro.

Ambientes com proxy podem exigir propriedades JVM de proxy/truststore para o próprio Gradle no arquivo local `~/.gradle/gradle.properties`. O bootstrap respeita `HTTPS_PROXY`; nunca desabilita a verificação TLS. Não coloque credenciais no repositório.

Há um workflow em `.github/workflows/android.yml` que executa testes, Lint e build e publica APK/relatórios como artefatos **se o projeto for colocado em um repositório GitHub e o workflow for executado**. Nenhuma execução remota foi iniciada nesta entrega.

## Instalar e configurar

1. Baixe e abra o APK entregue no aparelho. Para instalar uma compilação local via ADB:

   ```sh
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   Alternativamente, transfira o APK ao aparelho e abra-o. Autorize a instalação por essa fonte se o Android solicitar. Restrições do Samsung Auto Blocker ou de administração podem exigir ajuste manual autorizado pelo dono do dispositivo.
2. Abra **Finder to Google** e toque em **Escolher navegador**. Selecione Vivaldi, Samsung Internet, Chrome ou outro navegador compatível. Essa escolha pode ser trocada depois, sem alterar o navegador padrão do Android.
3. Use **Testar link comum** e **Testar pesquisa no Google App** para verificar os dois destinos.
4. Toque em **Ativar interceptação**, leia o efeito e confirme no diálogo do Android. Só o usuário concede o papel; a tela consulta `RoleManager.isRoleHeld()` para mostrar o estado real, inclusive após cancelamento ou mudança nas configurações.
5. Abra o Finder e toque em uma sugestão de **Pesquisas da web sugeridas**. Se for para o navegador, ligue **Diagnóstico local**, repita e consulte a origem informada.
6. Se a origem for desconhecida e o aparelho usar a assinatura confirmada no handoff, você pode ligar **Reconhecer URL do Finder (experimental)** e repetir o teste. Isso amplia a regra para URLs semelhantes de origem desconhecida.

Ao tornar este app navegador padrão, **outros links também passam por ele** e são encaminhados explicitamente ao navegador escolhido. App Links verificados e intents direcionadas a outro pacote podem seguir diretamente ao destino definido pelo sistema, sem passar pelo roteador. O app não controla `google.com`, não declara App Links verificados e não altera essa configuração.

Não requer root, Shizuku, acessibilidade, VPN, ADB após instalação, conta própria ou serviço em segundo plano. O Google App e o navegador fazem suas próprias conexões normalmente; o roteador não pede `INTERNET` nem efetua chamadas de rede.

## Falhas e restauração

- Google App removido, desativado ou bloqueado: tenta abrir a **URL original inteira** no navegador escolhido e explica a falha de forma transitória.
- Navegador removido, desativado, ausente ou com abertura bloqueada: exibe recuperação para escolher outro e continuar **o link recebido**. Não usa um `ACTION_VIEW` implícito nem escolhe outro pacote sem ação do usuário.
- Atualização que renomeie a Activity do navegador: revalida as Activities disponíveis no mesmo pacote selecionado. Não muda para um pacote diferente automaticamente.
- Sem navegador instalado compatível com HTTP e HTTPS: orienta instalar/habilitar um. Links não são consumidos silenciosamente; é possível tentar novamente ou fechar explicitamente.
- **Restaurar navegador anterior** abre, após orientação, as configurações de aplicativos padrão. Em One UI, procure **Configurações → Aplicativos → Escolher aplicativos padrão → Aplicativo de navegador**. Selecione o navegador anterior manualmente. O app não promete restaurar automaticamente nem guarda uma suposição sobre qual era o padrão.

## Parsing e privacidade

Somente esquemas HTTP/HTTPS são aceitos. Apenas os hosts exatos, sem diferenciar maiúsculas/minúsculas, `google.com`, `www.google.com`, `google.com.br` e `www.google.com.br`, no caminho literal `/search`, podem produzir busca nativa. Não aceita usuário na autoridade, porta diferente da padrão, host com sufixo falso, `/maps`, `/url`, `/aclk` ou `/search/` como busca.

O termo usa **`Uri.getQueryParameter("q")` uma única vez**; mantém acentos, emoji, espaços e `+` codificado. Parâmetro ausente, vazio, somente espaços ou com mais de 4.096 unidades UTF-16 segue no navegador. Em parâmetros `q` duplicados, vale o primeiro, exatamente como na API Android. Não há decodificação adicional. URLs encaminhadas usam o `Uri` original, inclusive parâmetros e fragmento, sem reconstrução.

Preferências em `SharedPreferences` locais, backup desabilitado, sem analytics, anúncios, logs de pesquisa ou permissões sensíveis. Não há solicitação de permissões em tempo de execução. O diagnóstico fica **desligado por padrão** e, quando ativado, guarda apenas um último registro de metadados: origem/identidade disponível, pista de referrer e decisão/resultado. Não guarda histórico, URL, termo nem texto de exceções. Desligar a opção ou tocar em **Apagar diagnóstico** apaga o registro. A URL de recuperação permanece apenas na Activity/intent recebida; não é salva nas preferências.

## Testes e estrutura

- `GoogleSearchUrlParser`: parser sem I/O; testes de hosts, caminhos, Unicode, sinais, duplicatas e limites.
- `LinkOriginResolver` e `RoutingPolicy`: identidade do sistema, referrer não confiável e opção experimental.
- `SearchLauncher`: `ACTION_WEB_SEARCH` com `SearchManager.QUERY`, pacote `com.google.android.googlequicksearchbox`; tenta `startActivity` e captura falhas, sem depender de `resolveActivity`.
- `AndroidBrowserCatalog` / `BrowserForwarder`: catálogo HTTP e HTTPS, visibilidade limitada via `<queries>`, exclusão do próprio pacote, seleção explícita, atualização e recuperação.
- `LinkRouter`: integração entre política, Google e fallback. Intents de saída são novas: não propagam extras, ClipData ou flags do chamador.
- `SettingsActivity`: configuração e estado real do papel de navegador. `LinkRouterActivity`: sem UI própria em rotas bem-sucedidas; tela somente para erro/recuperação. O sistema pode mostrar sua animação/splash ao abrir uma Activity.
- `app/src/test`: testes JUnit/Robolectric, inclusive integração e criação/recuperação de telas em API 36 e compatibilidade API 29.
- `app/src/androidTest`: testes instrumentados de parser real, filtro genérico, catálogo e status real do papel. Eles não concedem o papel nem alteram outros apps automaticamente.

Com aparelho ou emulador Android 16 conectado:

```sh
./gradlew connectedDebugAndroidTest
```

O teste definitivo continua sendo tocar no Finder real. Consulte a [matriz de validação no dispositivo](docs/VALIDACAO_DISPOSITIVO.md), que inclui outros aplicativos, ADB, Google desabilitado, navegador removido, temas, gestos e restauração.

Sem SDK é possível executar somente as verificações estruturais offline:

```sh
python3 scripts/check_project.py
```

Essas verificações validam XML, referências de recursos e contratos do manifest. Não compilam Kotlin, não executam Robolectric e não comprovam compatibilidade com um firmware One UI. A disponibilidade efetiva de `ROLE_BROWSER`, a identidade compartilhada pelo Finder e a abertura nativa do Google precisam ser confirmadas no aparelho alvo.
