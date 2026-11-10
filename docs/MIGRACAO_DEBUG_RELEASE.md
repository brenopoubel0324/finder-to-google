# Migrar do debug para a release preservando as configurações

A versão **1.1.1-rc.2/code 9** acrescenta exportação/importação explícita por arquivo JSON. Primeiro atualize o debug; **não desinstale sua instalação atual antes de exportar e verificar**. Debug e release usam certificados diferentes e não instalam um por cima do outro.

## Passo a passo

1. Instale o novo **APK debug code 9 por cima do debug existente**. O certificado é o mesmo dos APKs debug entregues anteriormente; navegador, opções e padrões devem permanecer. Confira suas escolhas antes de continuar.
2. Entre em **Configurações → Backup e migração → Exportar configurações**. O seletor Android pedirá onde salvar `Finder-to-Google-configuracoes.json`. Escolha uma pasta compartilhada que permaneça após desinstalar, como Downloads, ou sua pasta de projeto dentro de Backups e exportações no Drive. Não guarde somente na pasta privada do app.
3. Aguarde **Backup salvo e verificado**. O app escreve, lê de volta e compara os dados. Se houver erro, cancelamento ou interrupção, **não desinstale**: repita e verifique. Confira que consegue encontrar o arquivo no gerenciador de arquivos. Para uma segunda conferência, escolha **Importar configurações**, selecione o arquivo, confira o resumo e toque **Cancelar**; nada será alterado.
4. Se quiser conservar o último diagnóstico, copie-o separadamente em Ajuda → Detalhes para suporte. Ele não é configuração e não integra o JSON.
5. Com o arquivo guardado/verificado, escolha temporariamente seu navegador habitual como padrão no Android e desinstale **somente Finder to Google debug**. Não apague o arquivo exportado.
6. Instale o **APK release code 9**, abra Configurações → Importar configurações e escolha o JSON. Confira navegador, compatibilidade, diagnóstico e contagem de padrões. Toque **Importar** para substituir as configurações.
7. Confira a lista de padrões ativos/desativados e siga o guia para conceder novamente o papel de navegador padrão no Android. Teste uma pesquisa real do Finder e um link comum. Se o navegador salvo não estiver instalado, sua escolha permanece registrada e o app orienta selecionar outro.
8. Guarde o JSON para recuperação. Ele pode revelar o pacote do navegador e os identificadores dos seus padrões; não publique esse arquivo no GitHub.

Importar substitui a lista inteira, incluindo regras removidas/desativadas. Uma lista vazia continua vazia. Cancelar o seletor/resumo não importa. O app não abre navegador/pesquisa automaticamente ao restaurar. Depois dessa migração única, futuras atualizações release com a mesma chave e código maior preservam dados sem reinstalação.

## Incluído e excluído

Inclui pacote/componente preferido do navegador, compatibilidade, opção de diagnóstico e lista completa de padrões com estado ativo/desativado. Não inclui chave de assinatura, senhas, contas Google, URL, pesquisa, último diagnóstico, sessão de calibração, resultado humano de teste ou autorização de navegador padrão. O backup da chave de assinatura entregue separadamente no Drive é outro arquivo e tem outra finalidade.

O seletor é nativo do Android e não requer permissão de armazenamento, root, Shevery/Shizuku ou serviço. O aplicativo não envia nada automaticamente para a nuvem; um provedor de documentos escolhido pelo usuário pode sincronizar o arquivo por conta própria.

## Validação e interrupção

Formato v1 identificado, limite de 16 KiB/32 regras, UTF-8 estrito, campos e tipos exatos. Duplicatas, versões desconhecidas, entradas inválidas e campos extras são rejeitados antes de modificar configurações. Apenas a confirmação explícita permite importar opções que estavam ligadas no backup.

Após confirmação, um journal privado registra a restauração antes de atualizar os dois arquivos de preferências. Uma operação interrompida é concluída na próxima entrada em AppPreferences/FinderSignatureStore, antes das leituras. Em falha de armazenamento, não declarar sucesso: liberar espaço e reabrir, sem desinstalar. O journal guarda somente configurações validadas e é removido após concluir; backup automático continua excluído.

Testes de fluxo simulam arquivo fora do app, nova instalação e restauração com confirmação, além de corrupção, cancelamento, regras vazias/mistas e recuperação do journal. Isso não equivale à desinstalação/instalação física entre certificados no S25; esse teste continua necessário com o JSON verificado preservado.
