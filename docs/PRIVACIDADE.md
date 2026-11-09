# Privacidade — Finder to Google

Atualizada em 9 de novembro de 2026. Aplica-se à versão 1.1.0 distribuída no [repositório oficial](https://github.com/brenopoubel0324/finder-to-google).

O aplicativo recebe links quando é escolhido como navegador padrão do Android. Analisa o link no dispositivo e abre a pesquisa reconhecida no Google App ou encaminha o endereço inteiro ao navegador escolhido. Não tem servidor próprio, conta, anúncios, analytics, telemetria ou permissão INTERNET.

## Dados no aparelho

São guardados localmente o navegador escolhido, opções de compatibilidade/diagnóstico e padrões de reconhecimento. Não há histórico de navegação nem registro de termos pesquisados. A URL recebida permanece temporariamente no Intent da tela, inclusive para recuperação de falha; não é copiada para preferências. O Android administra o ciclo de vida dessas telas.

O diagnóstico é opcional e começa desligado. Quando ligado, guarda somente o último resultado e metadados de origem: pacote informado pelo sistema, tipo de identificação e pista de referrer saneada. Não inclui URL, consulta ou texto de exceções. Desligar a opção ou usar Apagar diagnóstico remove esse registro. A pista de referrer não comprova quem enviou o link.

A calibração usa uma frase fictícia por até cinco minutos em memória, sem serviço em segundo plano. A frase só é copiada para a área de transferência ao tocar no botão de cópia. A confirmação salva apenas o identificador do padrão, sem a pesquisa. Copiar diagnóstico também exige um toque explícito. A área de transferência é administrada pelo Android; outros aplicativos podem ter acesso conforme as regras do sistema.

## Destinos externos e compartilhamento

Para pesquisar, o aplicativo entrega o termo ao Google App. Para abrir uma página, entrega a URL original ao navegador escolhido. Esses aplicativos podem acessar a rede e aplicar suas próprias políticas de privacidade. O roteador não controla o tratamento posterior por Google, navegador ou páginas visitadas.

Nada é enviado automaticamente ao desenvolvedor. Se você abrir uma issue pública no GitHub ou compartilhar um diagnóstico manualmente, o conteúdo será tratado pelo serviço usado. Não publique pesquisas, URLs particulares, senhas ou capturas com dados pessoais.

## Exclusão e controle

É possível apagar o diagnóstico, desativar compatibilidade, remover padrões e trocar o navegador nas configurações do app. Limpar os dados ou desinstalar remove suas preferências locais; backup e transferência desses dados estão explicitamente excluídos pelo aplicativo. Cópias que você compartilhar ou colocar na área de transferência precisam ser removidas separadamente.

Para parar de usar o roteador, escolha outro navegador padrão nas configurações do Android. O aplicativo não usa root, acessibilidade, VPN, serviços residentes ou permissões de execução sensíveis.

## Contato

Use as [issues do repositório](https://github.com/brenopoubel0324/finder-to-google/issues) para dúvidas sobre esta política, sem incluir informações privadas. Mudanças serão registradas neste arquivo junto à versão correspondente.
