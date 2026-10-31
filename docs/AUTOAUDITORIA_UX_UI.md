# Autoauditoria — plano 0003, UX/UI

Base: 7c24bf2 (código rc.3 em fd19e6d). Procedimento próprio self-audit-loop, sem agente independente.

## Rodada 1 — achados antes de corrigir

- Médio — SettingsActivity / confirmação humana do teste: o resultado da sessão continuaria visível se navegador, papel, compatibilidade ou regras mudassem. Invalidar essa confirmação quando o contexto de roteamento mudar; não persistir sucesso. Testar retorno após perda de papel/mudança de modo.
- Médio — SettingsActivity.onResume / rolagem: a nova renderização restauraria a última posição salva em navegação, sem capturar a posição antes de sair para outro app. Salvar posição em onPause, sem salvar texto, URL ou desafio. Testar recriação e retorno.
- Baixo — onSaveInstanceState: alinhar nome outState ao método Android para remover aviso de parâmetro nomeado.

Os 84 testes anteriores passaram após adaptar apenas as referências da UI aos novos locais. Ainda faltam os novos testes, inspeção de renderizações, Lint/builds e verificação dos artefatos; nenhum resultado de usuário/SUS foi medido. Não foi encontrado achado bloqueante/alto nesta passagem.

## Rodada 2 — após 102 testes e inspeção das prévias

Correções da rodada 1 aprovadas pelos testes. Nova suíte: 102 testes sem falhas, seis verificações estruturais; renderizações reais de Views via Robolectric/native graphics nos dois temas inspecionadas. Não equivalem a teste físico no Android 17.

- Médio — openPage: alternar Configurações/Ajuda acrescenta repetidamente páginas à pilha. Ao voltar para uma página já na pilha, remover intermediárias em vez de duplicar; testar Voltar para o início após alternâncias.
- Médio — renderização de controles: preservar foco de teclado/acessibilidade no mesmo controle após atualizar uma página, identificando também a regra pelo client. Switches personalizados precisam de descrição que distinga padrões. Não forçar foco quando não existia nem quando a ação desapareceu.
- Baixo — limpar recursos que ficaram sem consumidor, usar carregamento compatível de drawable e formatar os XML gerados; remover pintura redundante de fundo, mantendo o fundo da janela na prévia. O teste do diálogo Android precisou consumir a fila de Intent esperada antes de afirmar que nenhuma pesquisa foi aberta; isso corrigiu o teste, não o app.

Nenhum achado bloqueante/alto. Corrigir estes pontos e repetir os checks finais antes do veredito de entrega. Acessibilidade com TalkBack real, teclado/gestos do fabricante, tempos de tarefa e SUS continuam externos e sem resultado medido.

Complemento da rodada 2: o formulário de cadastro manual ainda usava a raiz LinearLayout da rc.3. Com texto ampliado/teclado, precisa de rolagem (médio). Envolver em ScrollView e verificar viewport reduzido. Encurtar as instruções mantendo o efeito/limitação de reconhecimento explícitos. Como o APK rc.4/code 5 já foi disponibilizado como candidato intermediário, o pacote final desta execução será rc.5/code 6; não substituir silenciosamente seus bytes. Nenhuma mudança no roteamento ou dados persistidos.

Achado alto adicional antes de fechar a rodada: switches derivados de preferências não podem aceitar o estado automático de Views. Uma tela antiga pode restaurar checked obsoleto e disparar listeners; na lista, IDs de switches repetidos podem aplicar o último checked a regras diferentes. Bloquear save e restore automático pelo pai nesses controles, manter preferências como fonte única e testar Bundle legado/estado misto de regras. A correção não renomeia dados nem muda o opt-in; evita uma mutação silenciosa.


## Rodada 3 — fechamento

APROVADO no escopo da implementação candidata rc.5/code 6. O achado alto de estado automático foi corrigido com save/restore pelo pai desabilitados nos switches derivados de dados. Testes simulam Bundle legado e regras mistas, preservando preferências. Histórico sem páginas duplicadas, foco por controle/regra e formulário rolável também foram corrigidos. Nenhum bloqueante/alto remanescente na revisão final dos arquivos completos e consumidores.

106 testes JUnit/Robolectric passaram (22 novos sobre os 84 anteriores), zero falhas/erros; seis verificações estruturais passaram. O verificador de recursos passou a distinguir R do app de R de dependências, após encontrar Snackbar do Material no teste de Desfazer. Lint debug/release: zero erros/fatais, 21 avisos avaliados (versões/KTX/qualificadores/ícone/ortografia), sem supressão global. Builds debug/testes e release concluídos; certificados, manifest, versões e APKs no ZIP verificados. Campos/chaves de preferências e política/fallback/guard preservados.

Prévias geradas por View.draw com native graphics foram inspecionadas nos dois temas; o capturador espera o segundo layout do ActionMenu antes de fotografar. Paleta principal verificada por contraste (6,44:1 ou mais nos oito pares de texto medidos). Matriz de fontes/larguras, tablet/paisagem e formulário em viewport reduzido passou. Não são capturas do S25 nem prova de TalkBack/teclado do fabricante.

Varredura final sem nova regressão identificada no escopo. Pendências médias/externas: QA físico/Android 17 e release minificada em dispositivo, avaliação de tarefas/SUS com pessoas e guarda durável da chave. Não há tempos, índice SUS ou compatibilidade universal alegados. Resultados e artefatos exatos em CANDIDATO_1.1.0-rc.5.md.
