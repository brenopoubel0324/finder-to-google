# Validação no Samsung — Android 16/17 e One UI

**Atualização em 09/11/2026:** o usuário confirmou que a rc.5 abriu pesquisa real do Finder no Google App no S25/Android 17 (One UI 9 informado anteriormente). O teste do novo APK release 1.1.0/code 7 e o restante da matriz continuam pendentes. Nenhum Samsung está conectado a este ambiente. Os passos abaixo são roteiro, não resultados presumidos. Consulte [a preparação do lançamento](LANCAMENTO_1.1.0.md).

## Preparação

1. Instale o APK entregue ou compile conforme o README. Abra Finder to Google.
2. Selecione um navegador instalado (repita depois com Samsung Internet e Vivaldi/Chrome, se disponíveis).
3. Na **Ajuda**, use a verificação do navegador escolhido. Deve abrir `https://example.com/?origem=finder-to-google#teste` uma vez, no navegador escolhido.
4. Na **Ajuda**, use a verificação do Google App. Deve mostrar uma pesquisa nativa por `teste Finder to Google` no Google, sem aba de navegador. Esse botão testa o destino; não testa a origem Finder.
5. Em **Ajuda → Detalhes para suporte**, ligue **Diagnóstico local** se desejar observar os resultados. Deixe a compatibilidade desligada inicialmente em um perfil de teste.
6. No Início, siga **Configurar em 3 etapas**; escolha navegador e conceda o papel no diálogo Android. Na atualização de uma instalação configurada, não refaça nem apague suas escolhas.
7. Volte ao app e confirme o status **Ativo como navegador padrão**. Negar/cancelar a solicitação deve manter o estado real anterior.

## Origem e Finder (teste definitivo)

1. Abra o Samsung Finder, digite `teste`, toque em uma sugestão de **Pesquisas da web sugeridas**.
2. Se o Android compartilhar o pacote `com.samsung.android.app.galaxyfinder`, deve abrir resultados nativos no Google.
3. Consulte o diagnóstico. Se a origem for desconhecida, abrir o navegador é o comportamento esperado no modo padrão, mesmo que o referrer cite o Finder.
4. Nesse caso, ligue **Compatibilidade com Finder (Samsung)**, retorne ao Finder e repita o toque. Uma busca Google com `client=tablet-android-samsung-rvo1` (tablet) ou `client=ms-android-samsung-a1-terr1-rvo1aqb` (S25) deve abrir no Google. Outra assinatura de firmware não é reconhecida automaticamente.
5. Desligue o modo experimental e confirme que a origem desconhecida volta ao navegador.
6. Repita após encerrar as telas do roteador e do Finder e após reiniciar o aparelho; não há serviço para iniciar.

Não considere somente um teste por ADB como confirmação de integração com o Finder. A shell não é o Finder. A origem pode ser desconhecida ou identificada como shell e seguirá as regras respectivas.

## Matriz de URLs

Para **Finder reconhecido**, os casos Google válidos abrem busca nativa. Para **outro app reconhecido**, todos vão ao navegador. Para **origem desconhecida**, todos vão ao navegador, salvo assinatura exata com o modo experimental ligado.

| URL / situação | Resultado com Finder reconhecido |
|---|---|
| `https://www.google.com/search?client=tablet-android-samsung-rvo1&hl=pt&q=teste` | Busca `teste` no Google |
| `https://www.google.com/search?client=ms-android-samsung-a1-terr1-rvo1aqb&hl=pt&q=teste` | Busca `teste` no Google; com origem desconhecida, exige compatibilidade ligada |
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


## Registro do candidato e encerramento

| Dispositivo/artefato | Evidência atual |
|---|---|
| Tablet, APK debug original 1.0.0 | Usuário confirmou abertura no Google com compatibilidade ligada; modelo/versões não confirmados |
| S25, Android 17, One UI 9 (informados pelo usuário), debug original | Captura anterior mostra link recebido e encaminhado ao Samsung Browser; URL do S25 recebida posteriormente e incorporada na rc.2 |
| 1.1.0-rc.1 debug, versionCode 2 | Atualização com mesmo certificado compilada; validação física pendente |
| 1.1.0-rc.1 release, versionCode 2 | Nova chave, R8 ativo e assinatura verificada; validação física pendente |
| 1.1.0-rc.2 debug/release, versionCode 3 | Nova assinatura exata do S25; instalação e abertura nativa no aparelho a confirmar |
| 1.1.0-rc.3 debug/release, versionCode 4 | Calibração local e gerenciador; fluxo em aparelho a confirmar |
| 1.1.0-rc.5, versionCode 6 | Em 09/11/2026, usuário confirmou pesquisa real do Finder no Google App no S25/Android 17; não confirma matriz completa/release exata |
| 1.1.0 release, versionCode 7 | Preparação do lançamento; registrar hash e smoke do APK exato antes de publicar |

Antes de marcar final, registrar o SHA-256 do APK release testado conforme `CANDIDATO_1.1.0-rc.5.md` e concluir o roteiro acima. Repetir com fonte 200%, TalkBack, retrato/paisagem, gestos/três botões e temas claro/escuro. Conferir cópia/apagamento do diagnóstico e proteção na quarta abertura automática repetida (as três primeiras devem funcionar).

Na atualização debug, registrar navegador e modo antes/depois; compatibilidade já ligada deve permanecer ligada. Para release com chave nova, a reinstalação única exige refazer configuração. Em futuras atualizações release, manter a mesma chave e aumentar versionCode; não desinstalar automaticamente nem assumir migração entre certificados diferentes. Validar também cancelamento da concessão de papel, remoção do papel e retorno às configurações.

## Calibração e regras locais — versão atual

1. Confirme versão atual no rodapé, navegador padrão ativo e configurações preservadas. Em instalação nova, a lista começa com tablet/S25 ativos; atualização não deve repovoar regras removidas.
2. Em Configurações → Padrões de links, desative o padrão do aparelho em um perfil de teste. Com origem desconhecida, repetir a pesquisa no Finder deve encaminhar ao navegador.
3. Use Ajuda → Pesquisa do Finder abre no navegador para chegar ao ajuste guiado. Se a compatibilidade estiver desligada, o fluxo primeiro orienta sua ativação explícita. Copie a pesquisa de teste, abra o Finder, cole exatamente a frase e toque na sugestão da web. Não deve salvar regra antes de confirmar.
4. Na captura, confira o client. Gire a tela: deve continuar aguardando confirmação, sem abrir Google automaticamente. Escolha Salvar e ativar compatibilidade: assinatura deve ficar ativa e a pesquisa ir ao Google App. Repita outro termo para comprovar efeito sem novo APK.
5. Repita escolhendo Continuar sem salvar; regra/modo não devem mudar. Cancelar aprendizado, esperar cinco minutos ou reiniciar o processo deve encerrar a sessão. Uma pesquisa diferente não deve ser capturada.
6. Cadastre um marcador manual válido, desative/reative/exclua e reinicie o app: lista deve persistir. Não apagar os padrões sem querer; se excluir tudo intencionalmente, não devem reaparecer ao reiniciar. A reinstalação, por remover dados, inicia lista nova.
7. Cole uma URL/valor inválido no cadastro: deve haver erro, sem salvar. A lista limita a 32 regras. Teste também copiar diagnóstico: não pode conter URL, consulta ou desafio de calibração.
8. Confira botão de aprendizado sem papel padrão, assinatura ausente/duplicada e Google/navegador indisponível; não considerar uma abertura ADB como comprovação de origem Finder. Outros apps verificáveis continuam no navegador.


## UX/UI — rc.5, roteiro físico ainda pendente

- Instale a atualização por cima e confira que navegador, modo, diagnóstico e padrões ativos/desativados continuam iguais. O início deve orientar a próxima ação sem forçar onboarding.
- Navegue Início → Configurações → Ajuda → Voltar; a pilha não deve acumular páginas duplicadas. Abra o guia, cancele o pedido Android e confira que não houve sucesso fictício.
- Teste dentro do Finder; confirmação do usuário e teste direto de destino são ações distintas. Mudar modo/navegador/padrões/papel invalida a confirmação humana da sessão.
- Teste copiar/colar o desafio, sessão expirada, cancelamento, captura, rotação e retorno após mudar compatibilidade em outra tela. Nenhuma confirmação deve ocorrer automaticamente.
- Misture regras ativas/desativadas, gire a tela e reinicie o app: o estado deve permanecer. Exclua uma desativada e use Desfazer: ela deve continuar desativada. Confirme capacidade e conflito de undo sem sobrescrita.
- Com fonte 200% e teclado aberto, o cadastro manual deve rolar até campo/erro/botões. Verifique TalkBack, gestos/três botões, temas e paisagem no S25 e tablet. As prévias de teste não substituem esse roteiro.
- Para medir as metas, siga o protocolo no plano 0003 com versão SUS brasileira validada/fonte registrada antes de aplicar, participantes e tempos reais. Nada foi medido automaticamente no app.
