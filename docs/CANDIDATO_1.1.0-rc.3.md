# Candidato 1.1.0-rc.3 — calibração local

Base 6784323, versionCode 4. Implementa o fluxo aprovado pelo usuário e o plano 0002. Mantém certificados existentes, minSdk 29, compile/target 36, duas Activities e ausência de rede/serviço/permissões sensíveis.

## Entrega funcional

- Aprender URL do Finder: desafio fictício aleatório, cópia explícita, sessão em memória por cinco minutos, captura única validada e confirmação antes de salvar/ativar compatibilidade. Rotação mantém a confirmação sem disparar pesquisa; processo encerrado cancela sessão pendente.
- Gerenciar assinaturas: cadastro exato de client, ativar/desativar/excluir, até 32 regras de até 128 caracteres ASCII permitidos. Os padrões tablet/S25 são cadastrados apenas no primeiro uso; exclusões/lista vazia persistem.
- Lista ativa lida por abertura, com efeito imediato. Compatibilidade desligada e outro app verificável preservam a rota para o navegador. Sem correspondência por prefixo/referrer ou extras que controlem aprendizado.
- Somente regras/estado ativo persistidos em finder_rules. Nenhuma URL, consulta ou desafio em preferências/diagnóstico. O desafio fica temporariamente em memória e, mediante o botão explícito, no clipboard. Campos efêmeros não salvam texto no estado de View. URI original permanece no Intent, como nas versões anteriores.

## Como instalar e usar

Use a atualização debug por cima da instalação debug atual, sem desinstalar. Confirme 1.1.0-rc.3-debug no rodapé. Ative o papel de navegador se necessário, toque Aprender URL do Finder → Copiar teste e iniciar, cole a frase no Finder e toque na sugestão da web. Confirme a assinatura apresentada. Gerenciar assinaturas oferece edição da lista sem recompilar. Valores atuais do tablet/S25 já vêm cadastrados; recalibrar só é necessário para cadastrar/reativar uma regra.

Release usa o certificado definitivo já existente: atualiza releases anteriores; não atualiza debug com certificado diferente. Não houve instalação/desinstalação no dispositivo do usuário.

## Revisão e validação

Autoauditoria própria em duas rodadas, sem bloqueante/alto pendente, registrada em AUTOAUDITORIA_CALIBRACAO.md. Corrigidos salvamento automático de campos efêmeros e tratamento defensivo da URI; testes de diálogo sincronizados com o looper de teste.

84 testes JUnit/Robolectric aprovados (23 novos), zero falhas/erros; seis verificações estruturais aprovadas. Build debug/testes e Lint: BUILD SUCCESSFUL em 25s. Release com R8/minificação e redução de recursos: BUILD SUCCESSFUL em 1m07s. Lint debug/release zero erros/fatais e 23 avisos por variante (versões/KTX/tipografia/recursos já acompanhados).

Assinaturas verificadas por apksigner, certificados iguais aos respectivos candidatos anteriores. Manifest release debuggable=false, duas Activities, nenhum serviço/INTERNET, apenas a permissão interna de assinatura do AndroidX. ZIPs com lista explícita de APK/checksum/instruções e bytes internos conferidos; fonte/relatórios separados, sem chave/credenciais. Whitespace e segredos no conteúdo versionado conferidos antes de arquivar.

| Variante | APK (bytes) | SHA-256 |
|---|---:|---|
| Debug | 14.009.876 | `f735a5537b3f3dd926a52a827e7966777358b02237f2f77bd2be4b00017885ff` |
| Release | 2.443.871 | `082182fb1f70a978ada568c54e5d176d8cec8ab83203efafe171530f516e503e` |

Certificado debug SHA-256: `dabc6d27ddf900e0f8a137e17aa38ab4e1d4f9ef8500d11186d3c97c77d35823`.
Certificado release SHA-256: `a99bd676fb3a8603605fd94e2fd0cbc7cf36f9d313623931635fa4e1780741c8`.

[Atualização debug em ZIP privado](https://drive.google.com/file/d/1S2T_nfFGA_QoluOKRnRqgT47gCmOrCpE/view), tamanho 4.979.527 bytes conferido por metadados; compartilhamento público não foi habilitado.

Instrumentação desta versão foi compilada, sem execução nativa nova. Não atribuímos os testes instrumentados/smoke da rc.1 à rc.3. Não há runtime Android 17 ou Samsung físico conectado; a matriz manual segue pendente, assim como custódia durável da chave definitiva antes de distribuir como final.

Limites: assinaturas podem ser imitadas; identidade depende do Android; calibração exige client único no formato aceito e pesquisa exata do desafio. Se o Finder mudar a estrutura da URL ou omitir client, pode ser necessária outra atualização. A versão é candidata, não certificação de compatibilidade universal com One UI.
