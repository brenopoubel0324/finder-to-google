# Validação no Android 16 / Samsung One UI

**Estado desta entrega: pendente em dispositivo.** O APK foi compilado com SDK 36, mas nenhum Samsung físico está conectado. Os testes deste arquivo são um roteiro; não são resultados observados em One UI. Anote modelo, versão do Android, versão da One UI, versão do Google App e navegador/versão usados.

## Preparação

1. Instale o APK entregue ou compile conforme o README. Abra Finder to Google.
2. Selecione um navegador instalado (repita depois com Samsung Internet e Vivaldi/Chrome, se disponíveis).
3. Toque em **Testar link comum**. Deve abrir `https://example.com/?origem=finder-to-google#teste` uma vez, no navegador escolhido.
4. Toque em **Testar pesquisa no Google App**. Deve mostrar uma pesquisa nativa por `teste Finder to Google` no Google, sem aba de navegador. Esse botão testa o destino; não testa a origem Finder.
5. Ligue **Diagnóstico local**. Deixe a opção experimental desligada inicialmente.
6. Toque em **Ativar interceptação**, leia a explicação e confirme também no diálogo do Android.
7. Volte ao app e confirme o status **Ativo como navegador padrão**. Negar/cancelar a solicitação deve manter o estado real anterior.

## Origem e Finder (teste definitivo)

1. Abra o Samsung Finder, digite `teste`, toque em uma sugestão de **Pesquisas da web sugeridas**.
2. Se o Android compartilhar o pacote `com.samsung.android.app.galaxyfinder`, deve abrir resultados nativos no Google.
3. Consulte o diagnóstico. Se a origem for desconhecida, abrir o navegador é o comportamento esperado no modo padrão, mesmo que o referrer cite o Finder.
4. Nesse caso, ligue **Reconhecer URL do Finder (experimental)**, retorne ao Finder e repita o toque. Uma busca Google com `client=tablet-android-samsung-rvo1` deve abrir no Google. Outra assinatura de firmware não é reconhecida automaticamente.
5. Desligue o modo experimental e confirme que a origem desconhecida volta ao navegador.
6. Repita após encerrar as telas do roteador e do Finder e após reiniciar o aparelho; não há serviço para iniciar.

Não considere somente um teste por ADB como confirmação de integração com o Finder. A shell não é o Finder. A origem pode ser desconhecida ou identificada como shell e seguirá as regras respectivas.

## Matriz de URLs

Para **Finder reconhecido**, os casos Google válidos abrem busca nativa. Para **outro app reconhecido**, todos vão ao navegador. Para **origem desconhecida**, todos vão ao navegador, salvo assinatura exata com o modo experimental ligado.

| URL / situação | Resultado com Finder reconhecido |
|---|---|
| `https://www.google.com/search?client=tablet-android-samsung-rvo1&hl=pt&q=teste` | Busca `teste` no Google |
| `https://google.com/search?q=ol%C3%A1%20mundo` | Busca `olá mundo` |
| `https://www.google.com/search?q=C%2B%2B+Android` | Busca `C++ Android` |
| `https://www.google.com.br/search?q=%F0%9F%98%80+%2526` | Busca `😀 %26`, sem segunda decodificação |
| `https://www.google.com/search?hl=pt` | Navegador, URL intacta |
| `https://www.google.com/search?q=` | Navegador, URL intacta |
| `https://www.google.com/maps` | Navegador, se o sistema entregar ao roteador |
| `https://www.google.com/url?q=teste` | Navegador, URL intacta |
| `https://www.google.com.evil.test/search?q=teste` | Navegador; jamais WEB_SEARCH |
| `https://example.com/p?a=1&b=2#fragmento` | Navegador, URL intacta |
| `q` decodificado com mais de 4.096 unidades UTF-16 | Navegador, URL intacta |

O host falso pode não existir; o esperado é o navegador tentar abri-lo, sem executar a busca `teste`. O erro DNS subsequente pertence ao navegador.

## Simulação por ADB

ADB é opcional e serve apenas para instalação/validação. Não é necessário durante o uso.

```sh
# Resolução do sistema (respeita navegador padrão e App Links verificados).
adb shell 'am start -a android.intent.action.VIEW -d "https://www.google.com/search?hl=pt&q=teste"'

# Entrega explícita ao roteador, para isolar a resolução do sistema.
adb shell 'am start -n br.com.findertogoogle/.ui.LinkRouterActivity -a android.intent.action.VIEW -d "https://example.com/p?a=1&b=2#fragmento"'

# Pista falsificada não deve identificar Finder no modo padrão.
adb shell 'am start -n br.com.findertogoogle/.ui.LinkRouterActivity -a android.intent.action.VIEW -d "https://google.com/search?q=teste" --es android.intent.extra.REFERRER_NAME "android-app://com.samsung.android.app.galaxyfinder"'

# Esquema indevido entregue explicitamente: tela de recusa, nenhuma abertura externa.
adb shell 'am start -n br.com.findertogoogle/.ui.LinkRouterActivity -a android.intent.action.VIEW -d "file:///sdcard/teste"'
```

Para testar a regra **outro app reconhecido** de ponta a ponta, um app de teste no Android 14+ deve chamar `startActivity(intent, ActivityOptions.makeBasic().setShareIdentityEnabled(true).toBundle())`. Uma abertura comum pode omitir a identidade; isso é esperado. Os testes Robolectric cobrem a política das duas situações sem depender do firmware.

## Falhas e recuperação

| Ação no aparelho | Resultado esperado |
|---|---|
| Desabilitar Google pelas configurações, se o aparelho permitir, e repetir uma rota Google | Abre a URL original no navegador; mensagem transitória explica a falha |
| Reabilitar Google | Busca nativa volta a funcionar na rota apropriada |
| Desinstalar/desabilitar navegador selecionado | Tela curta de recuperação; escolher outro abre o link recebido |
| Não ter navegador compatível instalado | Orientação para instalar/habilitar; nenhuma tentativa implícita ou loop |
| Atualizar navegador mantendo pacote e mudando Activity | Revalida catálogo e usa Activity atual do mesmo pacote |
| Trocar navegador nas configurações do APK | Próximo link comum usa a nova escolha; papel padrão do roteador permanece |
| Abrir vários links e usar Voltar | Cada rota tem sua própria identidade; não reaproveita a origem da anterior |
| Alterar tema/fonte grande/orientação; usar gestos e navegação de três botões | Conteúdo legível, rolável, fora das barras e recortes de tela |
| Desligar diagnóstico | Último registro é apagado; nenhuma URL/pesquisa aparece nele |
| Restaurar navegador anterior nas configurações do Android | Status do APK passa a Inativo; os links voltam ao fluxo escolhido no sistema |

Reabilite os aplicativos após os testes. App Links verificados, links forçados diretamente a outro pacote, perfis de trabalho e políticas de administrador podem impedir que o link chegue ao roteador. Registre isso separadamente de falhas no encaminhamento.
