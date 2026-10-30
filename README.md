# Finder to Google

Aplicativo Android nativo em Kotlin, interface Material 3 em português (Brasil), com tema claro/escuro. Funciona como roteador local de links: pode ocupar o papel de navegador padrão, enviar pesquisas do Finder ao Google App e encaminhar outras URLs ao navegador selecionado. Android 10+ (`minSdk 29`), compilação e destino Android 16 (`compileSdk`/`targetSdk 36`).

**Candidato 1.1.0-rc.3 implementado.** Permite aprender assinaturas pelo Finder e gerenciar regras locais com efeito imediato, sem recompilar para cada valor novo de client. Inclui as melhorias anteriores de interface, migração, recuperação e diagnóstico. A atualização debug preserva certificado e preferências; o release mantém a chave definitiva. **O fluxo foi testado automaticamente; falta a confirmação física no Samsung.** Consulte [a entrega atual](docs/CANDIDATO_1.1.0-rc.3.md).

## Regra de roteamento desta versão

Esta implementação inclui as regras de origem solicitadas após o handoff. Isso altera o comportamento original, que reconheceria buscas Google vindas de qualquer app:

| Origem disponível | Busca Google válida | Outros links HTTP/HTTPS |
|---|---|---|
| Samsung Finder reconhecido pelo sistema | Google App via `ACTION_WEB_SEARCH` | Navegador selecionado |
| Outro aplicativo reconhecido pelo sistema | Navegador selecionado | Navegador selecionado |
| Desconhecida, opção experimental desligada (padrão) | Navegador selecionado | Navegador selecionado |
| Desconhecida, opção experimental ligada | Google somente com uma assinatura exata ativa na lista local; demais buscas no navegador | Navegador selecionado |

No primeiro uso, a lista inclui `tablet-android-samsung-rvo1` (tablet) e `ms-android-samsung-a1-terr1-rvo1aqb` (S25, URL enviada pelo usuário). É possível adicionar, desativar e excluir assinaturas, inclusive essas duas. Lista vazia continua vazia ao reiniciar. Não há reconhecimento por prefixo Samsung, substring ou referrer.

O pacote Finder reconhecido é `com.samsung.android.app.galaxyfinder`. `Activity.getLaunchedFromPackage()` (API 34+) e `callingPackage`, quando disponíveis, fornecem identidade do sistema. O Android frequentemente não a compartilha em aberturas comuns. `getReferrer()` é apenas uma pista visível no diagnóstico; extras do chamador podem falsificá-la. Não há promessa de identificação universal da origem.

A opção experimental é um reconhecimento de URL, não uma autenticação do Finder. Outro app pode gerar a mesma assinatura. Uma origem conhecida como outro app continua no navegador mesmo com a opção ligada. Se mudar somente o valor de client, use a calibração ou cadastro manual; mudanças estruturais de URL podem exigir atualização do app. O app não aceita um extra externo que force origem, inicie aprendizado ou confirme uma regra.

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

O APK debug fica em `app/build/outputs/apk/debug/app-debug.apk`, assinado automaticamente com a chave debug local. Relatórios: `app/build/reports/tests/testDebugUnitTest/index.html` e `app/build/reports/lint-results-debug.html`. Uma versão release para distribuição exige sua própria assinatura; não há chave privada de produção no projeto.

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

1. Para preservar as configurações atuais, use a **atualização debug**, assinada com o mesmo certificado da versão 1.0.0, e instale por cima. O **release** usa a chave definitiva nova: Android rejeita atualização direta sobre o debug; a reinstalação exige configurar novamente. Não há desinstalação automática. Para instalar uma compilação local via ADB:

   ```sh
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   Alternativamente, transfira o APK ao aparelho e abra-o. Autorize a instalação por essa fonte se o Android solicitar. Restrições do Samsung Auto Blocker ou de administração podem exigir ajuste manual autorizado pelo dono do dispositivo.
2. Abra **Finder to Google** e toque em **Escolher navegador**. Selecione Vivaldi, Samsung Internet, Chrome ou outro navegador compatível. Essa escolha pode ser trocada depois, sem alterar o navegador padrão do Android.
3. Use **Testar link comum** e **Testar pesquisa no Google App** para verificar os dois destinos.
4. Toque em **Ativar interceptação**, leia o efeito e confirme no diálogo do Android. Só o usuário concede o papel; a tela consulta `RoleManager.isRoleHeld()` para mostrar o estado real, inclusive após cancelamento ou mudança nas configurações.
5. Abra o Finder e toque em uma sugestão de **Pesquisas da web sugeridas**. Se for para o navegador, abra **Diagnóstico e opções avançadas**, ligue **Diagnóstico local**, repita e consulte a origem informada.
6. Se a origem for desconhecida e o aparelho usar a assinatura confirmada no handoff, você pode ligar **Compatibilidade com Finder (Samsung)** e repetir o teste. Isso amplia a regra para URLs semelhantes de origem desconhecida.

Ao tornar este app navegador padrão, **outros links também passam por ele** e são encaminhados explicitamente ao navegador escolhido. App Links verificados e intents direcionadas a outro pacote podem seguir diretamente ao destino definido pelo sistema, sem passar pelo roteador. O app não controla `google.com`, não declara App Links verificados e não altera essa configuração.

### Aprender e gerenciar assinaturas sem recompilar

1. Com a interceptação já ativa, toque em **Aprender URL do Finder** e depois **Copiar teste e iniciar**.
2. Abra o Finder, cole a frase fictícia gerada e toque na sugestão da web. O desafio deve coincidir exatamente. A sessão vale cinco minutos, somente em memória; se o Android encerrar o processo, inicie novamente.
3. Confira o valor de client apresentado e toque em **Salvar e ativar compatibilidade**. Só essa confirmação salva a assinatura e liga a opção. A pesquisa segue então a política normal. **Continuar sem salvar** preserva regras/modo.
4. Em **Gerenciar assinaturas**, adicione somente o valor de client, ative/desative ou exclua. Mudanças valem no próximo link, sem reiniciar ou gerar APK. Salvar pelo cadastro manual não liga a compatibilidade automaticamente.

As regras ficam no arquivo local `finder_rules`, até 32 registros (inclusive desativados), sem URL/pesquisa. Um client tem de 1 a 128 caracteres ASCII: letras, números, ponto, hífen ou sublinhado, começando por letra/número. A comparação é exata e diferencia maiúsculas/minúsculas. Calibração recusa client ausente/inválido/duplicado; outros links não são capturados e outro app com identidade verificável mantém a rota ao navegador. O teste só altera o clipboard após o botão que informa a cópia. Não há serviço, timer ou tarefa residente para esperar o link.

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

Preferências em `SharedPreferences` locais, backup desabilitado, sem analytics, anúncios, logs de pesquisa ou permissões sensíveis. Não há solicitação de permissões em tempo de execução. O diagnóstico fica **desligado por padrão** e, quando ativado, guarda apenas um último registro de metadados: origem/identidade disponível, pista de referrer e decisão/resultado. Não guarda histórico, URL, termo nem texto de exceções. Desligar a opção ou tocar em **Apagar diagnóstico** apaga o registro. A URL de recuperação permanece apenas na Activity/intent recebida; não é salva nas preferências nem copiada novamente ao estado salvo. O botão Copiar diagnóstico exige ação explícita, e a cópia é marcada como sensível no Android 13+.


## Release e atualização

A versão debug e a release usam o mesmo applicationId e versionCode 4, mas certificados diferentes. A release não deve ser instalada como atualização do debug. A chave definitiva foi criada a pedido do usuário; keystore, credenciais e cópia de segurança local ficam fora do projeto. Nunca inclua esses arquivos no fonte/instalador. A custódia e o backup durável da chave precisam ser mantidos separadamente para não perder a capacidade de atualizar o app.

Para compilar release, configure as quatro variáveis descritas em `.env.example`: `FINDER_RELEASE_KEYSTORE`, `FINDER_RELEASE_KEY_ALIAS`, `FINDER_RELEASE_STORE_PASSWORD` e `FINDER_RELEASE_KEY_PASSWORD`. Use armazenamento local privado ou um gerenciador de segredos; não coloque senhas em comandos de build, commits ou relatórios. Debug e testes não dependem dessas variáveis. `verifyReleaseSigning` impede preparar release sem elas.

```sh
./gradlew lintRelease assembleRelease
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
apkanalyzer manifest debuggable app/build/outputs/apk/release/app-release.apk
```

O último comando deve informar `false`. Preserve certificado e aumente versionCode em cada nova entrega. Para retornar a código estável, faça um novo build com o mesmo certificado e código maior; não prometa instalar um APK antigo por cima.

## Compatibilidade observada

O usuário confirmou funcionamento em um **tablet**, com origem desconhecida e assinatura `tablet-android-samsung-rvo1`. Depois informou **Samsung S25, Android 17, One UI 9**, onde a captura da rc.1 mostra assinatura desconhecida e encaminhamento ao Samsung Browser. O usuário então enviou a URL de teste com `client=ms-android-samsung-a1-terr1-rvo1aqb`. A rc.2 reconhece essa assinatura exata, com regressão automatizada de política, encaminhamento WEB_SEARCH e fallback. O diagnóstico continua sem guardar URL/termo. Não generalizamos para qualquer URL Samsung nem confiamos no referrer do launcher. Falta confirmar o resultado no aparelho após atualizar.

O target SDK continua 36 e não significa instalação restrita ao Android 16. A execução no Android 17 não foi certificada por este build. A validação final depende do S25 real e do APK candidato exato.

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
