# Comparação no mesmo Samsung — trabalho e bateria

**Estado: pendente no aparelho.** Este ambiente não está conectado ao S25. Contadores Robolectric demonstram trabalho da implementação; não demonstram energia, wakelocks do firmware ou comportamento do Google App real.

## Artefatos e condições

- Use o mesmo S25, perfil de usuário, navegador, Google App e configurações. Registre modelo, Android/One UI, versões, SHA-256 e certificado dos dois APKs.
- Baseline release: 1.1.0/code 7. Candidato: 1.1.1-rc.1/code 8, mesma chave definitiva. A atualização preserva dados. Se a instalação atual for debug, pode comparar a rc.5 debug já entregue com o candidato debug de mesma chave; registre essa baseline diferente, sem chamá-la de release 1.1.0.
- Não alterne assinaturas em uma comparação: debug e release exigem reinstalação entre si. Não limpe dados/configurações do aparelho principal. Preserve os APKs originais.
- Para repetir a ordem após instalar code 8, prepare compilação do código baseline com a mesma chave e code maior; não prometa downgrade por instalação normal. `adb install -d` pode falhar e não é condição para o roteiro.
- Fixe brilho, tema, fonte, rede, sinal, modo de energia, temperatura inicial, carga e estado de carregamento. Espere tarefas de instalação/dexopt terminarem. Atualizações automáticas e diferenças no Google/navegador podem dominar o resultado.

## Rodada funcional e de trabalho

Execute primeiro na baseline; depois atualize o APK e repita a mesma sequência:

1. Abrir o app configurado. Na Ajuda, expandir/ocultar suporte 20 vezes. Retomar o app 10 vezes sem modificar configurações; conferir rolagem, foco e ausência de tela piscando.
2. Fazer 10 pesquisas fictícias reais no Finder e abrir 10 links comuns. Registrar falhas/duplicações e tempo de tarefa observado, sem atribuir o tempo de rede ao roteador. Não há contadores/logs de consultas no APK de produção; para contar IPC/renderizações no aparelho, usar um trace do Android Studio/Perfetto quando o firmware permitir, com a mesma configuração nas duas versões. A captura ADB abaixo não conta chamadas Binder individuais.
3. Mudar o navegador padrão do Android para outro app, voltar e conferir status inativo. Negar o pedido de papel, depois conceder novamente. O modo/diagnóstico não devem ligar automaticamente.
4. Com cache de apresentação já aquecido, desabilitar ou remover o navegador em um perfil de teste, se viável; tentar abrir um link antes de simplesmente confiar no texto mostrado. Deve recuperar/permitir escolher outro, sem enviar um Intent ao componente indisponível. Reabilitar/reinstalar e repetir. Google indisponível deve manter fallback original.
5. Enquanto o app está fora de tela, mudar papel e disponibilidade do navegador. Retomar deve detectar as mudanças mesmo que nenhum broadcast tenha sido observado.
6. Abrir o app, sair para a tela inicial e encerrar o processo em ambiente de teste (`adb -s SERIAL shell am kill br.com.findertogoogle`, que depende de estar em background). Abrir novamente: recuperar preferências, consultar o estado real e não restaurar autorização/cache antigos. Calibração pendente perde a sessão; captura já aguardando confirmação segue o contrato de recuperação, sem pesquisa automática.
7. O app **não possui serviço próprio** para encerrar. Não criar serviço/watchdog para esse teste. Se testar Forçar parada, registrar como cenário distinto: o Android espera abertura explícita antes de retomar o app. Voltar da tela com a navegação padrão é outro cenário.
8. Repetir com fonte 200%, TalkBack, orientação e teclado; a otimização não pode deixar uma lista com regras antigas de mesmo tamanho ou restaurar um switch obsoleto.

## Janela de ociosidade e energia

Depois do roteiro funcional, medir uma janela igual de ociosidade com tela apagada e app fora de tela em cada versão (por exemplo, 60 minutos, sem conectar/desconectar o carregador durante a janela). A espera é manual ou do equipamento de laboratório; **não é timer no app**. Evitar polling ADB/perfilador durante a janela. Fazer várias rodadas, anotando dispersão e condições; um único A/B sequencial é exploratório e não estabelece causalidade.

Com ADB autorizado, executar uma captura manual imediatamente antes/depois de cada janela, para arquivos locais fora do Git:

```sh
python3 scripts/capture_device_state.py --serial SERIAL --output /caminho/privado/baseline-inicio
# Executar cenário/janela sem consulta contínua.
python3 scripts/capture_device_state.py --serial SERIAL --output /caminho/privado/baseline-fim
# Atualizar para o candidato e repetir em pastas candidato-inicio/fim.
```

O coletor usa consultas finitas e públicas para o usuário shell do ADB, sem root, resets, desabilitação de apps, leitura de URLs ou timers. São dez consultas finitas por captura. O próprio ADB/perfilador interfere no estado do aparelho: mantenha seu uso igual nas rodadas. Os dumps são locais e devem ser revisados antes de compartilhar.

As estatísticas `batterystats --charged` são acumuladas desde o ciclo de carga/reset do Android; comparar o delta da janela para o mesmo UID, preservando o marco inicial. Reinstalação, troca de UID, carregamento ou reset invalidam uma subtração direta. `meminfo`/`gfxinfo` não medem energia. Alguns firmwares não oferecem atribuição energética confiável por UID; registrar isso como indisponível, sem converter tempo de CPU, consultas ou percentual arredondado de carga em mAh fictícios.

Para comprovar economia de energia, preferir contador energético suportado/calibrado ou medidor externo sob condições controladas, com repetições. Separar consumo do roteador do Google/navegador e também avaliar o total da tarefa, se esse for o objetivo. Ociosidade sem serviço já era a arquitetura da baseline; ausência de consultas no candidato não prova uma nova economia em repouso.

## Registro a preencher

| Métrica | Antes | Depois | Fonte / limite |
|---|---|---|---|
| Modelo, firmware, APK/hash/certificado | Pendente | Pendente | Mesmo aparelho e condições |
| Consultas/IPC e recriações | Pendente | Pendente | Trace instrumentado, não inferir de dumps |
| Pesquisas/links: sucesso e duplicações | Pendente | Pendente | Roteiro manual |
| Recuperação de papel/navegador/processo | Pendente | Pendente | Roteiro manual |
| CPU/wakelock/energia atribuídos ao UID | Pendente | Pendente | Disponibilidade do Android e deltas |
| Energia total da janela, repetições e dispersão | Pendente | Pendente | Medição física/contador confiável |

Até preencher essas evidências, a conclusão é **redução de trabalho comprovada no harness; bateria no aparelho ainda não medida**.
