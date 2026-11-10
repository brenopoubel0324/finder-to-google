# Autoauditoria — migração por arquivo

Base `170fa1a`, candidato 1.1.1-rc.2/code 9. Escopo: migração debug → release com configurações, sem mudar política de roteamento/autorizações. Sem API privilegiada ou novo serviço. Nenhuma ação de organização/envio ao Google Drive foi executada; o usuário escolhe o destino pelo seletor Android.

## Primeira passagem

- Alto, prevenido: aplicar JSON parcialmente ou importar campos arbitrários poderia alterar opt-ins/regras sem confirmação. Parser limitado e estrito, validação integral, resumo e confirmação antes de escrever; não aceitar extras de autorização, consultas ou comandos.
- Alto, prevenido: gravação independente dos dois arquivos de preferências deixaria migração incompleta após interrupção. Journal de configuração validada, gravado antes das alterações e recuperado nas entradas das duas stores. O journal só é removido depois de concluir. Falha de armazenamento não produz sucesso fictício.
- Médio, prevenido: uma exportação parcialmente gravada poderia induzir o usuário a desinstalar. Reabrir o arquivo pelo ContentResolver e comparar o snapshot antes da mensagem de sucesso. Erro mantém orientação explícita de não desinstalar.
- Médio, prevenido: importar padrões via `save` reativaria regras desativadas ou repovoaria defaults excluídos. Restauração completa de registered/disabled, incluindo lista vazia.

Os trabalhos de leitura/escrita do arquivo escolhido ocorrem numa tarefa finita após ação do usuário, sem polling ou serviço. Aplicação das preferências ocorre em confirmação no thread principal, com commits limitados aos pequenos dados validados; o seletor e a transferência não iniciam ao abrir a tela. Ao perder processo antes da confirmação, reabrir o arquivo; só um journal de uma confirmação já efetuada permite completar a restauração.

Sem achado bloqueante/alto aberto nesta passagem. Validar testes de codec, fluxo de arquivo, opt-ins, cancelamento e journal, suíte completa, Lint das variantes, certificados/versões/ZIPs. Registrar os resultados da entrega após os checks. Teste físico entre assinaturas permanece distinto da simulação.

## Segunda passagem

Conferidos codec, journal, consumidores de preferências e resultado do seletor. Testes de UI precisaram aguardar o Handler do botão de confirmação e consumir os Intents esperados do seletor antes de verificar ausência de lançamento externo; isso ajusta a instrumentação, não o aplicativo. Botão de importação curto e resumo com quebras explícitas melhoram leitura em fonte ampliada. Componentes exigem pacote correspondente e nome de classe válido; desconhecidos, permissões, URLs/diagnóstico e tipos coercíveis não são aceitos no JSON.

Nenhuma API privilegiada, permissão ou componente de manifest foi acrescentado. Sem mudança de política/parser/fallback e com cache de apresentação invalidado após importação. Importação confirmada cancela calibração em curso; autorização do Android não é importada. A migração real depende de o usuário manter o arquivo verificado fora dos dados apagados na desinstalação. Não apresentar os testes como execução física no S25.

## Fechamento da entrega — 10/11/2026

**APROVADO como candidato de migração.** Fonte da compilação: `c487427957d6c347fd9970c9a7b0dba8d67dd2ea`. 124 testes JUnit/Robolectric passaram (9 novos sobre os 115), sem falhas/erros/ignorados; seis contratos estruturais e seis testes Python do gate aprovados. Build completo de testes/Lint/APKs debug e instrumentação/release R8: BUILD SUCCESSFUL em 2 min 18 s. Lint debug/release: zero erros, 23 avisos por variante; dois novos UseKtx foram avaliados e mantêm commits explícitos com verificação do retorno para durabilidade do journal. O aviso de plural do resumo foi corrigido com rótulos independentes.

Certificados, package/versionCode 9, manifest e ZIPs explícitos verificados. Arquivos publicados para teste na branch downloads, commit `096482ef317b8b5125037058e600423ee284a2a5`: oito blobs/tamanhos conferidos; os dois APKs foram baixados pelos links públicos e comparados por SHA-256. Nenhuma chave ou configuração real do usuário foi publicada. Mapping R8 e relatórios preservados separadamente para manutenção. Pré-release v1.1.1-rc.2 permanece em rascunho; a simulação de instalação nova não é certificação física entre assinaturas.

| APK | Bytes | SHA-256 |
|---|---:|---|
| Debug | 15.332.477 | `7a9577e8e12264bd89f845ca21862d8d9e5c6dac2865f7dd0f5a5bf9ddd4b382` |
| Release | 2.525.267 | `0d83d9aa160164b6190ce502606a640ea5cf3c68f1e9fd9d6529a11a05aeece3` |

As três verificações de UI usam o seletor real modelado pelo Robolectric e streams do ContentResolver; não são simples comparação de strings. Codec e journal têm seis testes de bordas/recuperação. Campos opacos/URLs, chave de assinatura e último diagnóstico não passam pelo formato. Cancelar a confirmação preserva opt-ins e dados. Ao exportar sem conseguir ler de volta, a UI não declara sucesso.

CI remoto não foi usado como evidência; commits [skip ci] preservam controles de custo enquanto o bloqueio de billing anteriormente registrado não for resolvido. Não houve instalação/desinstalação no S25 neste ambiente. Seguir o [guia de migração](MIGRACAO_DEBUG_RELEASE.md) e verificar o JSON antes de remover o debug. Sem bloqueante/alto aberto na revisão final; teste físico de seletor/provedor e migração entre assinaturas permanece pendente.
