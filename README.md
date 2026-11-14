# Finder to Google

Open supported Samsung Finder web searches in the **Google app** and send other links to **the browser you choose**. Finder to Google handles the handoff locally as Android's default browser.

**[Get the public release candidate (v1.1.0-rc.5)](https://github.com/brenopoubel0324/finder-to-google/releases/tag/v1.1.0-rc.5)** · APKs, SHA-256 checksums, and test notes are on the release page.

<a name="compatibilidade-observada"></a>

A Finder search opened in the Google app on a Galaxy S25 running Android 17 with rc.5. Results depend on how the device hands off links. The final v1.1.0 build is still in validation.

<a name="instalar-e-configurar"></a>

## Set up on your phone or tablet

Requires **Android 10 or later**, the Google app, and a browser that can open HTTP and HTTPS links. The app's interface is in Brazilian Portuguese.

1. Download the APK from the release page. If you're updating, choose the build signed with the same certificate as your installation. The debug and release APKs use different certificates. Switching between them requires a reinstall, which removes local settings. For rc.5, write down your current browser choice, compatibility setting, and custom link patterns before uninstalling.
2. Open the app and choose **Configurar em 3 etapas** (Set up in 3 steps). Select the browser you want for ordinary links, then grant Finder to Google the default-browser role in Android's prompt.
3. Search for a test term in Finder and tap the web suggestion. Then tap an ordinary link in another app to check that it still opens in your chosen browser.
<a name="aprender-e-gerenciar-padrões-sem-recompilar"></a>

If a Finder search opens in the browser, use **Ajuda → Pesquisa do Finder abre no navegador** (Help → Finder search opens in the browser). The guided setup can save a supported link pattern on devices where Android doesn't identify the sender.

<a name="regra-de-roteamento-desta-versão"></a>

## Where links go

| What Android hands to the app | Destination |
| --- | --- |
| Supported Google search from recognized Samsung Finder | Google app |
| Supported search with an unknown sender and an exact saved pattern | Google app if experimental compatibility is enabled |
| Other browser links, including searches from known non-Finder apps | Your chosen browser |

Experimental compatibility starts off. An exact URL pattern can help when Android omits the sender, and another app can produce the same pattern. Verified Android App Links and links directed to another app can bypass the router.

<a name="falhas-e-restauração"></a>

If the Google app is unavailable, Finder to Google forwards the original URL to your chosen browser. If that browser is unavailable, you can pick another one and continue with the same link. To stop using the router, choose a different default browser in Android settings before uninstalling.

<a name="parsing-e-privacidade"></a>

## Privacy

Routing and link patterns stay on your device. The router has no internet permission, account, ads, analytics, or search history. Optional diagnostics are off by default and record only the last routing decision and source metadata, without the search term or URL. The Google app and your browser handle their own network requests. See the [privacy policy](docs/PRIVACIDADE.md).

<a name="release-e-atualização"></a>

## Release and migration notes

- **Public release candidate v1.1.0-rc.5.** The release page distinguishes the debug update from the separately signed release APK. Verify the SHA-256 for the version you choose.
- **v1.1.1-rc.2 development candidate.** This adds explicit settings export and import for a debug-to-release migration. It requires the matching builds. See the [migration steps](docs/MIGRACAO_DEBUG_RELEASE.md) before attempting a reinstall.
- [Changelog](CHANGELOG.md) · [Final release installation guide](docs/INSTALACAO_RELEASE.md) · [v1.1.0 release criteria](docs/LANCAMENTO_1.1.0.md) · [Device validation](docs/VALIDACAO_DISPOSITIVO.md)

<a name="compilar"></a>

## Development

Native Android app in Kotlin with Material 3. The core separates URL parsing, source resolution, and routing policy. The router validates the chosen browser at launch and forwards the original URL.

Use JDK 21 for Gradle and Robolectric, Android SDK 36 and Build Tools 35.0.0. App bytecode targets Java 17. Set `ANDROID_HOME` or point `sdk.dir` in a local, untracked `local.properties` at the SDK.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
python3 scripts/check_project.py
```

On Windows, use `gradlew.bat` for the Gradle command. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The Gradle wrapper pins the distribution checksum.

<a name="primeiro-uso-do-gradle-wrapper--offline"></a>

The official wrapper JAR is included. For offline builds, provision the SDK and Gradle, Maven, and Robolectric caches ahead of time. Then use `./gradlew --offline ...`.

<a name="testes-e-estrutura"></a>

See the [CI guide](docs/CI_CUSTO_ZERO.md) for validation profiles and the [optimization notes](docs/REVISAO_CONSUMO.md) for the development candidate.

## Support

[Report a problem](https://github.com/brenopoubel0324/finder-to-google/issues) with your app version, Galaxy model, and Android or One UI version. Include what opened when you tapped a Finder web suggestion and when you tried an ordinary link.
