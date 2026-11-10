# Revisão de trabalho e consumo — 10/11/2026

Base: `9e82f23`, app 1.1.0/code 7. Pedido: reduzir trabalho repetido sem sacrificar confiabilidade, com eventos e cache de apresentação, sem polling ou timers que acordem o aparelho.

## Achados antes de alterar

- Médio — `SettingsActivity.snapshot/render`: cada ação local consulta RoleManager e varre navegadores, além de recriar Views sem comparar dados. `onCreate` e o primeiro `onResume` repetem a mesma operação. Usar estado de apresentação em memória, invalidação por retomada/evento e chave de renderização completa.
- Médio — `BrowserForwarder.selectedTarget`: procura o pacote escolhido enumerando todos os navegadores. Fazer a consulta restrita ao pacote conhecido, preservando os dois esquemas e os critérios de elegibilidade; o seletor continua consultando a lista atual quando aberto.
- Baixo — `render` lê as regras duas vezes para contagem/contexto. Reutilizar a mesma lista na passagem de renderização, incluindo seus valores/estados na chave para não confundir listas de mesmo tamanho.

Não há root, Shizuku, consultas privilegiadas shell nem autorização de acessibilidade. As consultas são APIs públicas PackageManager/RoleManager, que podem envolver IPC. Não há serviço próprio, AlarmManager, WorkManager, polling, timer residente ou wakelock do aplicativo a remover. Componentes AndroidX do manifest final são descritos no resultado de lançamento; não representam um serviço próprio de roteamento.

## Implementação e limites

Cache restrito à apresentação de SettingsActivity, sem persistência, TTL, sessão global ou reutilização para abrir links. Revalidar ao retomar, ao concluir seleção/pedido de papel e antes de ações que dependem desses requisitos. Receber mudanças do pacote selecionado somente enquanto a tela estiver iniciada, sem receiver novo no manifest. Pacotes não selecionados não exigem atualizar a apresentação; abrir o seletor sempre descobre os navegadores atuais.

Antes de enviar qualquer link: manter `resolve` para a URI real, filtragem HTTP/HTTPS, estado enabled/exported/permissão, pacote/componente explícitos, tratamento de exceções e proteção contra ciclos. Não usar cache para assumir que o Google ou navegador aceitam a próxima abertura. Não substituir erro por estado otimista.

Comparar trabalho no mesmo harness Robolectric/API 36 antes/depois com cenários fixos e contadores de consultas, identificando que não é um aparelho nem energia. Preparar APK baseline e candidato com a mesma assinatura e códigos crescentes para comparação no mesmo Samsung. Não atribuir percentuais de bateria a uma redução de consultas/Views.

## Recuperação necessária

Cobrir retirada do papel enquanto a tela estava fora, mudança/desativação do navegador com apresentação aquecida, perda de processo e expiração da calibração. Não existe serviço próprio para encerrar: testar encerramento/recriação do processo; não acrescentar serviço para cumprir um cenário que não se aplica. Forçar parada pelo Android é um estado diferente de perda de processo, e exigir reinício explícito após ela é esperado.

Este ambiente não tem acesso ao S25; a comparação física e a medição energética ficam pendentes até execução no mesmo aparelho. Testes de tempo/energia não serão inventados a partir dos resultados da JVM.

## Comparação executada no harness

Mesmo ambiente, JDK 21, Robolectric/API 36, mesmo cenário `SettingsWorkProbeTest`, sombras com contadores de chamadas reais às APIs modeladas. A baseline usa o código de aplicação em `9e82f23`, com somente a instrumentação de teste acrescentada; depois usa a implementação candidata. Contagem inclui apenas queryIntentActivities de ACTION_VIEW/BROWSABLE e isRoleHeld/isRoleAvailable, não todos os IPCs do processo.

| Cenário | Antes | Depois |
|---|---|---|
| Inicialização com navegador escolhido | 4 queries PM globais; 2 held + 2 available | 2 queries PM restritas ao pacote; 1 held + 1 available |
| 20 alternâncias locais de suporte | 40 queries PM globais; 20 held + 20 available; 20 reconstruções | 0 queries PM/RoleManager; 20 reconstruções necessárias, porque o conteúdo muda |
| 10 retomadas, dados iguais | 20 queries PM globais; 10 held + 10 available; 10 reconstruções | 20 queries PM restritas; 10 held + 10 available; 0 reconstruções |
| Activity parada, uma hora de relógio simulada | 0 queries PM/RoleManager | 0 queries PM/RoleManager |

Evidências brutas: [antes](evidencias/consumo-antes.json) e [depois](evidencias/consumo-depois.json). `page_rebuilds=-1` na inicialização significa não contado naquele ponto, não uma redução negativa. Nenhum contador/telemetria foi adicionado ao APK de produção. O teste de relógio simulado não é observação física de Doze/energia/CPU no Android.

**Redução de trabalho comprovada no harness:** consultas redundantes de ações locais e inicialização, enumeração geral para apresentação e recriação de página sem mudança. **Sem redução no total de consultas de retomada:** elas continuam necessárias para detectar papel/navegador alterados; mudaram o escopo da consulta e a reconstrução da UI. **Economia de bateria medida: nenhuma.** A baseline já não mantinha trabalho de polling/serviço em repouso.

## Confiabilidade e autoauditoria

- Cache não atravessa Activity/processo, não é persistido e não participa da autorização/encaminhamento. URI real, pacote/componente, falhas Google/navegador e guard continuam ao vivo; não houve mudança de política, manifest ou permissões.
- Consultas de papel não são substituídas por listener privilegiado: atualização em retomada, resultado do diálogo e antes de ações. Chamadas de APIs públicas continuam tratando indisponibilidade/negação.
- O receiver dinâmico observa somente broadcasts protegidos PACKAGE_ADDED/REMOVED/CHANGED/REPLACED e só trabalha se o pacote selecionado mudou. Está registrado de onStart a onStop, nunca no manifest. Usar RECEIVER_EXPORTED é adequado aos eventos protegidos do sistema e evita depender da permissão de compatibilidade para NOT_EXPORTED em APIs antigas; o conteúdo do evento apenas invalida apresentação e os dados são lidos novamente. Negação da observação não derruba a tela: retomada/ações continuam revalidando.
- Chave de apresentação inclui página/etapa, suporte, diagnóstico, calibração, resposta e conteúdo/estado de todas as regras. Não usar só contagem nem aplicar checked salvo por Views. Callback de scroll antigo não pode atuar sobre uma renderização nova.
- Validação: 115 testes JUnit/Robolectric aprovados (9 novos sobre 106), seis contratos estruturais e seis testes do gate Python. Build debug/instrumentação e Lint debug aprovados. Testes instrumentados reais não foram executados. Detalhes de release/entrega serão anexados após a assinatura.
- Regressões cobrem ações locais sem consultas, página igual preservada, perda do papel, pacote removido com cache aquecido, evento de outro pacote ignorado, observador parado em background, mudanças perdidas recuperadas na retomada, ação de abrir link sem confiar no cache, substituição de regras com mesma contagem e Activity nova sem estado antigo. A remoção de Activity modela perda de disponibilidade no harness; desabilitação real por configurações da One UI permanece no roteiro físico.

Revisão em duas passagens: nenhum bloqueante/alto identificado. Correção média durante validação: registro NOT_EXPORTED exigia permissão interna em API 29 no harness; a versão final observa os eventos de sistema protegidos com tratamento de negação. Não foi acrescentado serviço para o cenário de encerramento. [Roteiro e coletor manual](COMPARACAO_NO_APARELHO.md) separam recuperação, CPU/IPC e energia.

Limites locais concretos: `/dev/kvm` não está disponível; a tentativa `adb devices -l` falhou ao criar `/home/agent/.android` em filesystem somente leitura. Não há acesso ao S25 neste ambiente. Alterar permissões do host não conectaria o aparelho do usuário; nenhuma comparação física ou de energia foi declarada. O coletor pode ser executado no computador conectado ao Samsung, sem mudar o ambiente do app.
