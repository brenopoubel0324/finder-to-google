# Revisão do plano 0003 — UX/UI

Procedimento: `breno-review-audit:execution-plan-review`. Revisão própria, sem agente independente. Repositório `/workspace/finder-to-google`, branch master, base fd19e6d, sem mudanças anteriores ao trabalho de planejamento. Escopo comparado à rc.3 e aos contratos dos planos 0001/0002.

## Rodada 1 — versão 1.0: REVISAR

Achados registrados antes de corrigir o plano:

| ID | Gravidade | Local | Problema e mudança obrigatória |
|---|---|---|---|
| A1 | Alto | §3.3 / confirmação em LinkRouterActivity.showCalibration | Trocar o botão para Salvar padrão quando o modo está ligado pode ocultar a reativação se o modo for desligado em outra tela e a pessoa retornar. Revalidar modo/texto/efeito ao retomar e ao confirmar; alteração de estado não pode ativar compatibilidade sem informar. Acrescentar teste desse retorno. |
| M1 | Médio | §3.3 e §7 / FinderCalibration.current e estado salvo do router | Sessão pendente termina com o processo, mas uma captura já feita pode ter confirmação restaurada pelo Android, como na rc.3. Distinguir esses casos: não restaurar desafio/sessão; preservar somente candidato validado e Intent original na confirmação, sem repetir Google. |
| M2 | Médio | §2 / cálculo do índice | SUS 0–100 não é porcentagem de sucesso. Fixar questionário/escala e método de cálculo; não misturar tempo dos concluídos com exclusão de falhas da taxa de sucesso. Medição incompleta deve permanecer sem score final. |

Demais contratos conferidos: estado real do papel, compatibilidade opt-in, diagnóstico opcional, padrões excluídos não repovoados, undo preservando enabled em vez de save que reativa, limites de client, fallback explícito, extras sem controle de efeitos e ausência de permissões/serviços novos.

## Rodada 2 — versão 1.1: APROVADO para execução

Releitura integral da versão 1.1 e confronto com os arquivos citados. A1 corrigido no plano: revalidar modo/texto/efeito ao retomar e confirmar, com teste obrigatório; M1 corrigido: separar sessão pendente de candidato já capturado; M2 corrigido: cálculo SUS e denominadores fixados, sem score para questionário incompleto. Nenhum achado bloqueante/alto remanescente, nenhuma mudança obrigatória adicional identificada. O plano pode orientar a implementação.

Conferências técnicas: RoleManager/BrowserCatalog como fontes reais; navegação mantém duas Activities exportadas; nomes visuais não renomeiam prefs; candidato só restaura metadados permitidos associados ao Intent; não repetir Google por rotação; undo precisa preservar enabled e checar capacidade/conflito; manter política/fallback/guard e ausência de serviços/permissões/telemetria. A aprovação é do plano técnico, não uma confirmação do usuário nem aprovação do APK futuro.

Validação mínima para executar: estados/navegação/retorno Android; atualização mantendo preferências; desafio/consentimento/calibração; undo de regra ativa/desativada e conflitos; regressão de roteamento/privacidade; seis verificações estruturais, JUnit/Robolectric, Lint e builds; assinatura/manifest/checksum; inspeção TalkBack/fontes/teclado e tarefas no Samsung. Comandos e critérios estão no §7 do plano. Nenhum teste Kotlin ou build foi reexecutado nesta entrega de documentos; os 84 testes citados pertencem à rc.3.

Pendências externas declaradas: participantes/baseline, escolha da fonte da tradução SUS em E0, medições reais, dispositivo para QA e custódia durável da chave. Não bloqueiam o início do trabalho de protótipo/implementação, mas impedem declarar índice atingido, versão final ou compatibilidade física ainda não medida. Principal risco residual: simplificar a UI e esconder uma mudança de comportamento; os testes de estado e consentimento são obrigatórios para evitá-lo.
