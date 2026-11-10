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
