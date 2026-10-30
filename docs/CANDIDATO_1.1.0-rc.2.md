# Candidato 1.1.0-rc.2 — assinatura do Finder no S25

## Evidência e correção

O usuário enviou a URL de uma pesquisa fictícia por `teste` no Finder do S25:

```text
https://www.google.com/search?client=ms-android-samsung-a1-terr1-rvo1aqb&hl=pt&q=teste
```

A rc.1 reconhecia somente `tablet-android-samsung-rvo1`. Com origem desconhecida, essa diferença produzia exatamente o diagnóstico observado: assinatura não reconhecida → navegador. A rc.2 inclui somente o novo valor exato confirmado, preservando o do tablet. Isso atende à condição do plano de exigir evidência e testes antes de acrescentar assinatura.

Mantêm-se opt-in de compatibilidade, origem verificável de outro app → navegador, hosts/caminho/portas validados, decodificação única e limite do termo. Não há regra por prefixo ou referrer, nem registro de URL/consulta. Google indisponível continua encaminhando a URL original inteira ao navegador escolhido.

## Instalação e validação no aparelho

VersionCode 3, versionName 1.1.0-rc.2 (debug com sufixo -debug). O APK debug pode atualizar rc.1/1.0.0 debug sem desinstalar; mantém certificado e preferências. A release mantém o certificado definitivo já criado, distinto do debug. Não houve nova chave nem instalação/desinstalação no aparelho do usuário.

Depois de instalar a atualização debug, confirme a versão rc.2 no rodapé, mantenha compatibilidade ligada e o roteador como navegador padrão. Pesquise `teste` no Finder e toque na sugestão da web. Esperado: pesquisa no Google App. No diagnóstico: assinatura de compatibilidade → Google App; solicitação enviada. Isso depende de Google instalado/habilitado e ainda precisa de confirmação física. Teste também link comum e modo de compatibilidade desligado.

## Autoauditoria da alteração

Base: commit 1a5985b (rc.1). Uma rodada própria com o procedimento `breno-review-audit:self-audit-loop`, limitada ao parser, consumidores/política, testes, texto de configuração e versão. Nenhum achado bloqueante/alto identificado no escopo revisto. A busca por assinatura permanece exata e condicionada à validade da pesquisa; outra origem verificável prevalece; o valor do tablet continua coberto pelos testes anteriores. Não é uma revisão independente.

Riscos residuais: outro aplicativo pode imitar a assinatura; o Android pode omitir identidade; outro firmware pode gerar valor diferente; o Google pode recusar ou não renderizar a pesquisa. Compilação/testes não certificam Android 17/One UI 9. A custódia durável da chave definitiva e a matriz física da release continuam pendentes antes de marcar a versão final.

## Resultado desta compilação

- 61 testes JUnit/Robolectric aprovados (cinco novos casos cobrindo S25, fronteiras da assinatura, política e encaminhamento/fallback); zero falhas/erros.
- 6 verificações estruturais offline aprovadas.
- Lint debug/release: zero erros/fatais; 21 avisos de versões/KTX, tipografia e recursos/ícone em cada variante, já avaliados no candidato anterior.
- `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`: BUILD SUCCESSFUL em 1m14s.
- `lintRelease assembleRelease`: BUILD SUCCESSFUL em 1m28s, R8/minificação e redução de recursos ativos.
- Assinaturas verificadas por apksigner; certificados iguais aos respectivos APKs rc.1. Manifest release: debuggable=false, nenhum serviço/INTERNET, somente permissão interna AndroidX de nível signature.
- Whitespace e varredura de segredos/artefatos no conteúdo versionado conferidos antes de empacotar fonte.
- APK de instrumentação rc.2 compilado, sem nova execução nativa. Os quatro testes instrumentados e smoke release da rc.1 permanecem resultados históricos, não execuções da rc.2. Não há aparelho Samsung ou runtime Android 17 disponível nesta sessão.

| Variante | Tamanho APK | SHA-256 |
|---|---:|---|
| Debug | 13.956.062 bytes | `2f7891d6d310cd68668ae5924cba1f288f024b156b821e2dd4e071f6d542f656` |
| Release | 2.419.507 bytes | `17230178a6aa1de9990beeb46f0c1113d8ff38cb5bda97b18494236897b44d20` |

Certificado debug SHA-256: `dabc6d27ddf900e0f8a137e17aa38ab4e1d4f9ef8500d11186d3c97c77d35823`.
Certificado release SHA-256: `a99bd676fb3a8603605fd94e2fd0cbc7cf36f9d313623931635fa4e1780741c8`.

[Atualização debug em ZIP no Drive privado](https://drive.google.com/file/d/14ramgsiEBjzVlC2gc6MEclC6n1H2u-An/view), upload e tamanho 4.960.997 bytes conferidos por leitura de metadados. Os ZIPs instaláveis contêm somente APK, checksum e instruções; fonte/relatórios são separados e sem keystore/credenciais. A entrega continua candidata até confirmação física.
