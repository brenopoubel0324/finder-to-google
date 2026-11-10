# Instalar o APK de lançamento

**Para migrar sem recadastrar configurações:** atualize primeiro o debug para 1.1.1-rc.2/code 9, exporte e verifique o JSON em Configurações → Backup e migração. Só depois reinstale e importe na release correspondente. Siga [o roteiro de migração](MIGRACAO_DEBUG_RELEASE.md). As instruções manuais abaixo continuam sendo uma alternativa para versões antigas sem exportação.

Use o arquivo **Finder-to-Google-1.1.0-release.apk** disponibilizado pelo proprietário na release do [repositório oficial](https://github.com/brenopoubel0324/finder-to-google/releases). APKs do GitHub Actions são debug e não substituem o instalador release. Uma release em rascunho ainda está em validação; não é um lançamento público aprovado.

## Antes de instalar

- Android 10 ou superior; Google App e um navegador que abra HTTP/HTTPS devem estar instalados e habilitados.
- Confira nome, tamanho e SHA-256 com os arquivos publicados. A assinatura definitiva tem SHA-256 `a99bd676fb3a8603605fd94e2fd0cbc7cf36f9d313623931635fa4e1780741c8`.
- Não é necessário root, ADB, acessibilidade ou app adicional durante o uso.

## Se você usa o APK debug

Debug e release têm o mesmo identificador, mas certificados diferentes. O Android não permite instalar a release por cima do debug.

1. Anote o navegador escolhido, se a compatibilidade está ligada e os padrões personalizados em Configurações → Padrões de links. Diagnóstico não é backup das configurações.
2. Escolha seu navegador habitual como padrão nas configurações do Android.
3. Desinstale o debug apenas quando estiver pronto para perder seus dados locais.
4. Instale a release e refaça as escolhas anotadas. Não há migração automática entre essas assinaturas.

Se você já usa release com a chave definitiva, instale a nova release por cima: as preferências são preservadas. Uma versão antiga com código menor não instala como atualização; rollback exige nova compilação com a mesma chave e código maior.

## Instalação e configuração

1. Baixe o APK. Se o Android solicitar, autorize instalar aplicativos pela fonte usada. Proteções Samsung ou políticas de administrador podem exigir ajuste manual pelo proprietário; o app não altera essas proteções.
2. Abra Finder to Google e siga Configurar em 3 etapas.
3. Escolha o navegador para links comuns e conceda ao Finder to Google o papel de navegador padrão no diálogo Android.
4. No passo de teste, ligue Compatibilidade com Finder se precisar reconhecer o padrão Samsung com origem desconhecida. Em instalação nova essa opção fica desligada até sua escolha.
5. Abra o Finder, pesquise `teste` e toque na sugestão da web. Confira que abriu resultados no Google App. O teste direto do Google na Ajuda verifica somente o destino.
6. Abra um link comum em outro aplicativo e confira o navegador escolhido. App Links verificados podem abrir diretamente em outros apps, conforme o Android.

Se uma pesquisa do Finder abrir no navegador, use Ajuda → Pesquisa do Finder abre no navegador. O ajuste guiado permite aprender um novo identificador de link sem recompilar. Mudanças no formato geral do link podem exigir uma atualização.

## Ajuda e remoção

Google indisponível: a URL original vai ao navegador selecionado. Navegador indisponível: uma tela de recuperação permite escolher outro. Para suporte, informe versão do app, modelo e versões do Android/One UI; diagnóstico é opcional e fica em Ajuda → Detalhes para suporte.

Para deixar de usar o roteador, escolha outro navegador em Configurações do Android → Aplicativos → Escolher aplicativos padrão → Aplicativo de navegador. Depois, se desejar, desinstale. Não é preciso desligar um serviço em segundo plano.

Consulte a [política de privacidade](PRIVACIDADE.md) e o [roteiro de validação](LANCAMENTO_1.1.0.md).
