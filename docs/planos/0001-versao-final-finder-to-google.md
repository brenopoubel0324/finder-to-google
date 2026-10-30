# Plano de execução — Finder to Google, versão final

Versão do plano: **1.1 — APROVADO para execução por etapas, após duas rodadas de revisão**.
Revisão e veredito em `0001-versao-final-finder-to-google.revisao.md`.
Escopo desta entrega: planejar e revisar; a implementação ainda não está autorizada por este documento.

## 1. Objetivo, fatos e limites

Entregar uma versão final instalável, com configuração compreensível, preservação das preferências existentes, roteamento resiliente, interface adequada a celular/tablet, diagnóstico seguro e assinatura permanente.

Fatos confirmados:

- Projeto Kotlin em `/workspace/finder-to-google`, applicationId `br.com.findertogoogle`, minSdk 29 e target/compileSdk 36. Não há histórico Git utilizável no diretório atual.
- APK debug 1.0.0/code 1 funcionando no Samsung do usuário, conforme confirmação e captura. SHA-256: `0fafbffd722462347daca7a5550444bd80b838c56a3e0115b2e400cf11984085`.
- Nesse teste, a origem era desconhecida; o referrer indicava `com.sec.android.app.launcher`. O reconhecimento da assinatura de URL estava ligado e o Google App abriu.
- Os 38 testes Robolectric passaram no build original; Lint apresentou zero erros e 17 avisos. Os quatro testes instrumentados foram compilados, não executados. A investigação posterior reexecutou 15 testes de política/integração com sucesso.
- O catálogo atual cruza HTTP/HTTPS por Activity, e um erro ao processar um candidato pode descartar todo o resultado.
- Não há signingConfig de release nem chave definitiva selecionada. O fato de existir certificado debug não resolve essa decisão.

Pendências externas: modelo/versões exatas de Android, One UI, Google e navegador; disponibilidade de aparelho/emulador; escolha e guarda da chave definitiva; eventual necessidade de reinstalação para trocar certificado.

Fora de escopo: Play Store, publicação automática, root, Shizuku, acessibilidade, serviços/receivers residentes, rede própria, servidor, analytics, sincronização, nova lista ampla de hosts/clientes Google e identificação universal do app de origem. Não substituir XML/Material por Compose nem SharedPreferences por outro framework sem necessidade técnica.

## 2. Contratos que não podem regredir

| Condição | Destino |
|---|---|
| Finder com identidade verificável + busca Google válida | Google App por WEB_SEARCH |
| Outro app com identidade verificável | Navegador escolhido, mesmo com compatibilidade ligada |
| Origem desconhecida + compatibilidade desligada | Navegador escolhido |
| Origem desconhecida + compatibilidade ligada + assinatura exata confirmada | Google App por WEB_SEARCH |
| URL HTTP/HTTPS fora dessas regras | Navegador escolhido, URL original intacta |
| Google indisponível/bloqueado | URL original no navegador; se falhar, recuperação |
| Navegador indisponível | Recuperação sem escolher outro pacote automaticamente |
| Esquema inválido | Recusa com orientação, sem abertura externa |

Preservar hosts exatos atuais, caminho literal `/search`, ausência de userInfo, política atual de portas, `Uri.getQueryParameter("q")` sem duplo decode, q não branco e limite de 4.096 unidades UTF-16. Preservar exatamente `client=tablet-android-samsung-rvo1`; novas assinaturas só entram com evidência e testes próprios.

Referrer continua apenas informativo. Não autenticar Finder por launcher/referrer nem habilitar compatibilidade silenciosamente. Não propagar extras/flags/ClipData do chamador. Todo encaminhamento web usa componente explícito de pacote escolhido, distinto do próprio app. Não alterar ROLE_BROWSER sem ação e confirmação do usuário.

## 3. Ordem e entregáveis

| Etapa | Entrega | Dependências |
|---|---|---|
| E0 | Baseline e histórico local | Nenhuma |
| E1 | Catálogo/fallback e migração de preferências | E0 |
| E2 | Configuração e layout | E1 |
| E3 | Diagnóstico e regras de backup | E1; integração visual após E2 |
| E4 | Regressão automatizada e validação debug | E1–E3 |
| E5 | Release assinado e otimização | E4 + decisão da chave |
| E6 | Validação do artefato release e entrega | E5 + dispositivo disponível |

Trabalhar sequencialmente, com alterações pequenas e revisão ao fechar cada etapa. Não modificar o APK instalado do usuário automaticamente.

## E0 — Baseline recuperável

1. Conferir hash do APK que funcionou; preservar APK, ZIP de fonte, relatórios e identificação do certificado, sem expor chave privada.
2. Iniciar histórico Git local do projeto, com baseline do código existente. Ampliar `.gitignore` antes de adicionar arquivos para excluir builds/caches, SDK/JDK, `.jks`, `.keystore`, arquivos de senhas e configuração local de assinatura. Não publicar/pushar repositório nesta etapa.
3. Confirmar JDK 21, SDK 36 e Build Tools 35.0.0. Registrar comandos reproduzíveis usando variáveis locais; não versionar caminhos/proxy do ambiente desta sessão.
4. Registrar a captura/confirmação como teste manual do fluxo principal pelo usuário, sem marcar a matriz completa como executada.

Aceite: baseline recuperável, segredo fora do controle de versão e distinção entre teste relatado pelo usuário e teste executado pelo agente.

## E1 — Catálogo, resolução explícita e recuperação

Arquivos existentes: `routing/BrowserCatalog.kt`, `routing/BrowserForwarder.kt`, `data/AppPreferences.kt`, `ui/UiSupport.kt`, `ui/SettingsActivity.kt`, `ui/LinkRouterActivity.kt` e respectivos testes.

### E1.1 — Escolha por pacote, resolução por URI

1. Listar um item por pacote que tenha handlers genéricos para HTTP e HTTPS; aceitar Activities diferentes dentro do mesmo pacote.
2. Separar app selecionável de Activity resolvida. Contrato previsto: `BrowserApp(packageName, label)` para seleção, `BrowserTarget(component, label)` para despacho; `BrowserCatalog.installedBrowsers()` retorna apps e `resolve(packageName, uri)` retorna destino elegível ou ausência.
3. Resolver a URI real apenas dentro do pacote selecionado, usando APIs públicas de PackageManager/IntentFilter. Revalidar componente exportado/habilitado, permissões e adequação à URI antes de tentar iniciar. Não usar a resolução implícita global como fallback.
4. Separar falha da consulta de falha por candidato. Metadados inválidos de um app não podem eliminar outros candidatos; rótulo indisponível pode usar nome do pacote.
5. Atualizar consumidores e testes: `BrowserSelection`, `BrowserForwarder.availableBrowsers/select/selectedTarget/open`, picker em UiSupport, status/testes de SettingsActivity e MemorySelection dos testes.

### E1.2 — Preferências e atualização

Migrar `browser_component` para uma seleção por pacote em `browser_package`, mantendo o componente legado como dica. Preservar o arquivo `router_settings`, `experimental_finder`, `diagnostics_enabled` e `last_diagnostic`; a mudança do nome visível não renomeia a chave de compatibilidade.

Contrato de armazenamento e migração:

1. `BrowserSelection` passa a expor `selectedBrowserPackage: String?`; `AppPreferences` e o fake MemorySelection precisam mudar juntos. O componente não será mais a identidade da escolha exibida no picker.
2. Um `browser_package` sintaticamente válido e diferente do próprio app tem precedência. Se ausente/inválido, tentar extrair o pacote de um `browser_component` válido. Sem um valor seguro em nenhum dos dois, considerar não configurado e pedir escolha; nunca adivinhar outro navegador.
3. A migração não consulta PackageManager: um pacote desinstalado/desativado continua sendo a escolha do usuário. A disponibilidade será verificada separadamente e resultará em recuperação. Uma indisponibilidade temporária não autoriza usar o componente legado de outro pacote.
4. A transformação é idempotente e não apaga as chaves antigas. Ao selecionar explicitamente um app, gravar no mesmo `SharedPreferences.Editor` o novo pacote e uma dica de componente do mesmo pacote validado; após despacho bem-sucedido, a dica pode ser atualizada. Ela nunca substitui a resolução para a URI atual.
5. Não reinicializar flags por faltar um estado de onboarding. Os booleanos `experimental_finder` e `diagnostics_enabled` devem manter o valor exato anterior; a chave de diagnóstico só é limpa por regra explícita de desligar/apagar diagnóstico, não pela migração do navegador.
6. Os testes precisam demonstrar precedência do pacote novo, recuperação a partir do componente legado, navegador ausente, valor inválido/próprio, migração repetida e coerência das duas chaves depois de uma escolha nova.

Aceite mínimo: atualizar uma instalação com compatibilidade true mantém true, o pacote escolhido permanece e o papel padrão não muda. Browser ausente continua identificado como escolha indisponível, com recuperação explícita.

### E1.3 — Recuperação e reentrada

1. Preservar a etapa de recuperação na recriação de LinkRouterActivity, sem repetir automaticamente a tentativa Google. Recuperar a URI do Intent original; não adicionar URL/termo a preferências ou relatório.
2. Manter nova instância por link; dados de recuperação antigos não podem contaminar uma nova abertura.
3. Acrescentar proteção pequena contra ciclos de fallback entre roteadores: no máximo três encaminhamentos automáticos do mesmo par URI/pacote em janela de dois segundos; a quarta tentativa entra em recuperação. Usar fingerprint somente em memória, relógio monotônico, estrutura limitada e nenhum timer/serviço.
4. Uma ação manual de recuperação pode iniciar uma nova tentativa; extras externos não podem limpar/burlar a proteção. Troca de navegador muda o destino somente pela escolha do usuário.
5. Documentar que a proteção é local ao processo e a URLs repetidas; não prometer detecção universal de ciclos entre apps.

Contrato da proteção: helper em `routing/ForwardingGuard.kt`, usado antes do despacho do fallback, com clock monotônico injetável para teste. Manter no máximo 16 entradas por processo; fingerprint de URI/pacote somente em memória, sem log/preferência/clipboard. A janela começa na primeira reserva e expira em dois segundos, com limpeza por acesso e descarte da entrada mais antiga ao atingir capacidade. Reservar antes de chamar startActivity para limitar reentrada; se startActivity lançar exceção, liberar a reserva. A quarta reserva válida na janela retorna `BrowserFailure.REENTRY_LIMIT` e nenhuma Activity é aberta. Mapear esse motivo nas telas de recuperação e testes de SettingsActivity/LinkRouterActivity. O retry/reset só ocorre em callback de botão/picker local, nunca por extra da intent. Testar janela, limite, capacidade, falha de lançamento, novo destino e duas aberturas legítimas. Uma quarta abertura manual rápida pode exigir recuperação; a mensagem deve explicar repetição, sem afirmar origem maliciosa.

Aceites: atividades HTTP/HTTPS distintas funcionam; um candidato com erro não esvazia a lista; URI/query/fragmento permanecem idênticos; self nunca é destino; atualização/remoção de navegador e falha de startActivity têm recuperação; rotação não repete Google; dois toques rápidos não são bloqueados e ciclo repetido é limitado.

## E2 — Configuração clara e interface final

Arquivos: `ui/SettingsActivity.kt`, `ui/UiSupport.kt`, `res/layout/activity_settings.xml`, `res/layout/activity_recovery.xml`, `res/values/strings.xml`, temas e recursos de ícone.

1. Apresentar configuração curta na própria tela: escolher navegador, ativar papel, escolher modo e testar no Finder. Não criar onboarding complexo ou exigir conta.
2. Renomear para “Compatibilidade com Finder (Samsung)”. Explicar em linguagem simples o reconhecimento pela URL, a possibilidade de outro app gerar a mesma assinatura e a necessidade de escolha explícita. Instalação nova mantém false; atualização preserva a preferência.
3. Mostrar separadamente papel real do Android, navegador disponível e modo de roteamento. “Pronto para o modo escolhido” não significa que o Google renderizou resultados nem que qualquer firmware é compatível.
4. Manter teste direto do Google e teste do navegador, indicando que não substituem tocar no Finder. A solicitação de papel exige navegador selecionado, explicação prévia e confirmação do sistema. Cancelar não pode exibir sucesso.
5. Usar cartões curtos e área avançada recolhível dentro de SettingsActivity para os textos técnicos/diagnóstico. Preservar as duas Activities exportadas atuais, sem adicionar uma tela exportada desnecessária.
6. Em telas largas, centralizar conteúdo com largura máxima de 640 dp e margens responsivas; em telas estreitas, ocupar a largura disponível sem corte. Manter rolagem, claro/escuro, TalkBack, alvos de toque de pelo menos 48 dp, insets e texto com fonte ampliada.
7. Criar ícone adaptativo original, mostrar versão instalada e remover strings sem uso. Não usar logotipo Google de modo a sugerir app oficial.

Aceites: sem corte em larguras 320/600/840 dp, retrato/paisagem, fonte padrão e 200%; navegação por gestos/três botões sem conteúdo atrás das barras; configurações persistem; detalhes técnicos acessíveis sem dominar a tela.

## E3 — Diagnóstico útil e privacidade

Arquivos: `ui/DiagnosticsFormatter.kt`, `data/AppPreferences.kt`, `routing/SearchLauncher.kt`, `ui/SettingsActivity.kt`, `AndroidManifest.xml`, novos XML de backup e `scripts/check_project.py`.

1. Adicionar “Copiar diagnóstico” somente por toque explícito. Copiar metadados permitidos e versão do app, sem Intent original, URL, consulta, ClipData do chamador, stack trace ou identificadores pessoais do aparelho.
2. Manter somente o último registro quando diagnóstico estiver ligado; desligar apaga o registro e impede novas gravações. Copiar não deve criar histórico adicional no app.
3. Ajustar o resultado para “Solicitação enviada ao Google App”; distinguir intent aceita de resultados nativos efetivamente vistos. Informar falhas por motivo tipado, sem salvar mensagens brutas de exceções.
4. Manter `allowBackup=false` e adicionar `fullBackupContent`/`dataExtractionRules` explícitos excluindo dados do app de backup e transferência, inclusive SharedPreferences. Validar schema e domínios disponíveis com SDK 36.
5. Ampliar o verificador estrutural para referências `@xml` e `@mipmap`, incluindo pastas qualificadas; checar exclusões de backup e manifest final. Diferenciar a permissão interna de assinatura do AndroidX de permissões sensíveis/rede.

Contrato de resultado e propagação da pesquisa:

- `SearchOpener.open(query)` deixa de retornar Boolean e passa a retornar um resultado imutável `SearchLaunchResult`: `Launched` ou `Failed(SearchFailure)`. Motivos mínimos: indisponível/sem Activity, acesso negado e requisição inválida, correspondendo às três exceções já tratadas. Não usar estado mutável “última falha”.
- Atualizar em conjunto `SearchLauncher`, `LinkRouter.route`, `RouteOutcome`, `RouteReport`, os testes/fakes de SearchOpener, o botão de teste em SettingsActivity, LinkRouterActivity e DiagnosticsFormatter. Nenhum desses consumidores pode continuar interpretando resultado tipado como Boolean.
- `RouteOutcome.BrowserOpened` e `Recovery` devem carregar `googleFailure: SearchFailure?`; o indicador `googleFailed`, se mantido para simplificar a UI, é derivado desse campo. Falha Google sempre preserva o fallback da URL original. A ausência de exceção confirma apenas que a intent foi aceita; não comprova renderização dos resultados.
- A recriação da recuperação preserva etapa, decisão e motivo tipado. A URI continua vindo do Intent; não salvar termo/URL novamente no Bundle nem em preferências. Um link novo não herda esse estado.
- Persistência/clipboard recebem somente a projeção permitida de enums e metadados saneados; jamais recebem o objeto Intent, termo, URI ou mensagem de exceção. O texto de versão identifica a versão atual do aplicativo. Testar os três motivos e a ausência de consulta/URL-canário em todos eles.

Aceites: diagnóstico não contém uma consulta/URL-canário em nenhuma rota; clipboard só muda quando o usuário toca em copiar; desligar diagnóstico remove o registro; XML é compilável e políticas cobrem backup/transferência; APK final sem INTERNET e sem serviço residente. Não prometer controle de transferências externas feitas pelo próprio usuário ou por ferramentas do fabricante fora das regras do app.

## E4 — Validação automatizada e revisão de implementação

Preservar os testes existentes e acrescentar testes de regressão úteis:

- Migração: compatibilidade true/false, navegador válido/removido, valor inválido, seleção explícita e repetição da migração.
- Catálogo: Activities diferentes por esquema, um candidato quebrado e outro válido, pacote próprio excluído e componentes protegidos/desabilitados.
- Encaminhamento: resolução específica para cada URI, troca de Activity dentro do pacote, remoção durante resolução, URI intacta, exceções e proteção de reentrada.
- Política: a captura real — origem desconhecida, hint do launcher, assinatura exata e compatibilidade true → Google; false → navegador. Hint sozinho nunca autentica Finder; outro app verificável continua no navegador.
- Recuperação: rotação/recriação após falha Google não repete pesquisa; novo link continua independente.
- Interface: estado real do papel, cancelamento da solicitação, navegador ausente, conservação das preferências, copiar/limpar/desligar diagnóstico e acessibilidade manual.

Comandos mínimos, executados antes de preparar release:

```sh
python3 scripts/check_project.py
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
git diff --check
git diff --cached --check
```

Atualizar o workflow para compilar também os testes instrumentados. Não exigir segredos de assinatura em pull requests ou em testes debug. Corrigir erros Lint e avaliar avisos relevantes sem desabilitar verificações globalmente; sugestões KTX/novas versões não exigem refatoração lateral.

Executar testes instrumentados em Android 16 quando aparelho/emulador estiver disponível. Um auxiliar de teste pode compartilhar identidade por API pública para testar “outro app conhecido”; isso fica fora do APK de produção. O teste real no Finder continua manual.

Com dispositivo Android 16 selecionado/conectado, executar `./gradlew connectedDebugAndroidTest`. Esse comando valida a variante debug; não afirmar que executa a variante release. Se não houver dispositivo, registrar instrumentação como pendente. O release exato terá teste manual próprio em E6; instrumentação contra release só poderá ser acrescentada com configuração explícita da variante alvo/assinatura, sem presumir uma task já configurada.

Fechar a etapa com autoauditoria do diff contra este plano e com resultados registrados. Não marcar teste não executado como aprovado.

## E5 — Release, chave e tamanho

1. Antes de assinar, resolver a escolha da chave: reutilizar uma chave de produção fornecida pelo usuário, ou criar uma nova chave definitiva com armazenamento e backup fora do projeto. Nunca imprimir senhas ou incluir chave em ZIP/artefatos/repositório.
2. Fornecer template de configuração sem segredos e ler valores locais/variáveis de ambiente. Build debug/testes não depende de chave release. A entrega de release deve falhar claramente se assinatura obrigatória estiver ausente; não distribuir APK unsigned nem usar debug como assinatura de produção silenciosamente.
3. Manter applicationId. Primeiro candidato: versionName `1.1.0-rc.1`, versionCode maior que 1. Cada APK entregue com mudança de conteúdo recebe novo código; final `1.1.0` usa código maior que o candidato anterior.
4. Habilitar minificação e redução de recursos no release usando consumer rules existentes e regras adicionais somente quando houver evidência de necessidade. Manter debug simples. Se R8 causar regressão, corrigir/testar ou manter a otimização desabilitada e registrar essa decisão; tamanho é objetivo secundário à estabilidade.
5. Verificar APK release com apksigner e aapt2/apkanalyzer: certificado esperado, debuggable=false, applicationId/versões corretos, min/target SDK, ausência de permissões sensíveis/rede e de serviços próprios. Registrar tamanho e SHA-256.

Critério da chave: certificado diferente do debug instalado normalmente exige reinstalação. Nenhuma desinstalação ou apagamento de preferências será feito automaticamente; explicar a migração antes de orientar o usuário a instalar.

Configuração proposta: `FINDER_RELEASE_KEYSTORE`, `FINDER_RELEASE_KEY_ALIAS`, `FINDER_RELEASE_STORE_PASSWORD` e `FINDER_RELEASE_KEY_PASSWORD`, com senhas fornecidas somente no ambiente/configuração local ignorada. Registrar somente o fingerprint público do certificado esperado. Não presumir que a chave debug seja uma chave de produção fornecida pelo usuário.

Com assinatura configurada, executar e registrar:

```sh
./gradlew lintRelease assembleRelease
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
"$ANDROID_HOME/build-tools/35.0.0/aapt2" dump badging app/build/outputs/apk/release/app-release.apk
apkanalyzer manifest debuggable app/build/outputs/apk/release/app-release.apk
```

O último comando deve informar false. O caminho/nome do APK deve ser conferido nos metadados de saída se a configuração mudar; `*-unsigned.apk` nunca satisfaz o aceite. Não imprimir valores secretos ao diagnosticar falta de assinatura.

## E6 — Candidato real, matriz manual e entrega

1. Testar o APK release que será entregue, inclusive a versão minificada; testes apenas do debug não aprovam o release.
2. No Samsung, registrar versão/modelo e testar: Finder em compatibilidade, link comum e URL parecida de outro app; Google indisponível; navegador ausente/trocado; reinício; rotação em recuperação; remover papel padrão; atualização de candidato para candidato com mesma chave; preservar preferências. Reabilitar apps usados nos testes.
3. App Links que não chegam ao roteador e origem não revelada são limitações a documentar, sem tratá-las como prova de bug ou de origem confiável.
4. Atualizar README, estado da entrega e matriz de validação. Descrever claramente modo necessário no Samsung do usuário, permissões reais, política de assinatura, migração e limites conhecidos.
5. Entregar APK assinado, checksum, fonte sem segredos, relatório de testes e instruções curtas. A disponibilização deve usar canal acessível; o link interno `/workspace` já falhou para APK. Preferir ZIP com APK/checksum/instruções no canal privado já utilizado, verificando o upload antes de fornecer o link. Não tornar público nem alterar compartilhamento existente.

Empacotar por lista explícita: um APK release aprovado, seu checksum e instruções; fonte e relatórios em artefatos separados. Excluir APK androidTest, unsigned, debug antigo, keystores, senhas, arquivos locais, SDK/JDK e caches. Registrar SHA-256 e certificado do candidato submetido ao teste manual, conferir que o pacote final contém os mesmos bytes e verificar tamanho/metadados após disponibilizar o arquivo. Se houver rebuild ou mudança no R8/assinatura após a validação, o artefato novo precisa de nova verificação e smoke test.

Aceite de versão final: todas as etapas técnicas concluídas, nenhum achado bloqueante/alto de implementação pendente, assinatura verificada e matriz mínima do Samsung preenchida para o APK release exato. Se faltar aparelho/chave, entregar o que estiver validado como candidato e declarar a pendência; não chamar de final.

## 4. Retorno a uma versão estável

- Preservar baseline antes das alterações e evitar mutações destrutivas de preferências.
- Antes de instalar, registrar apenas as configurações necessárias (pacote do navegador, modo e diagnóstico), nunca a pesquisa/URL recebida.
- Após instalar uma versão mais nova, não prometer instalar o APK antigo por cima: o Android pode rejeitar versionCode menor ou certificado diferente.
- Para voltar sem apagar dados, produzir uma nova compilação do código estável com o mesmo certificado da versão instalada e versionCode maior, preservando compatibilidade das preferências.
- Se a troca de certificado exigir reinstalação, a alternativa e sua perda de preferências precisam ser explicadas e aceitas pelo usuário. Nenhum downgrade, desinstalação ou reset automático.

## 5. Condições para começar e para concluir

Pode começar a implementação de E0–E4 sem decidir a chave, preservando o APK funcional atual. E5 depende da chave e da estratégia de migração; E6 depende de teste real e canal de entrega acessível. A aprovação deste plano permite organizar a execução futura, sem afirmar que esses requisitos externos já estão atendidos. O comando de executar a implementação é uma etapa posterior a esta solicitação de planejamento/revisão.
