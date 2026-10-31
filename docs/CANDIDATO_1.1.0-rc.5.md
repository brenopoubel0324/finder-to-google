# Candidato 1.1.0-rc.5 — UX/UI guiada

Execução do plano 0003, base 7c24bf2 (código anterior fd19e6d). Android Kotlin/XML/Material 3, applicationId inalterado, minSdk 29/target 36, duas Activities e mesmos certificados. Code 6: a rc.4/code 5 foi disponibilizada como candidato intermediário; a rc.5 acrescenta rolagem do formulário e proteção contra checked obsoleto/IDs repetidos de Views. Atualiza debug anterior sem desinstalar; release atualiza release, com certificado distinto do debug.

## O que mudou

- Início: estado real do Android, próxima ação e destinos dos links. Configurações/Ajuda acessíveis no cabeçalho; usuários já configurados não refazem onboarding.
- Guia: navegador → padrão no Android → teste dentro do Finder. Cancelamento/retorno recalculam requisitos; confirmação humana não vira garantia persistida e é invalidada se o contexto de roteamento mudar.
- Calibração: preparar/copiar → testar no Finder → confirmar, com estado da sessão, opção de recomeçar e explicação/consentimento atualizados quando o modo muda. Candidato capturado é restaurável, sessão pendente não é persistida.
- Padrões de links: controles diretos, cadastro em formulário rolável, excluir/Desfazer preservando enabled e evitando conflito/capacidade. Nenhum repovoamento de regras removidas.
- Interface responsiva, claro/escuro, alvos de toque, rolagem, teclado e foco. Valores client/referrer ficam nas áreas onde são úteis. Rotação/estado legado de Views não pode alterar preferências ou aplicar um checked a várias regras.
- Recuperação: ação principal conforme o motivo e alternativa secundária, com o mesmo link no Intent, sem repetir Google ao recriar.

Roteamento/parser/guard e esquema de preferências preservados. Nenhum root, INTERNET, serviço, telemetria, nova permissão ou Activity exportada. Somente metadados permitidos são salvos; a UI não lê clipboard para inferir intenção.

## Execução e medição

E0: rc.3, fontes e relatórios preservados em baseline local com hashes. Inspeção estrutural e protótipo mapeados no plano; não houve participantes, aparelho físico ou aplicação de SUS. A fonte da tradução SUS deve ser selecionada antes da pesquisa de campo, que não foi aplicada. Tempos, taxa de sucesso e metas de score permanecem sem resultado medido.

E1–E4: implementação dos fluxos e validação em Views Android com Robolectric/native graphics. Foram inspecionadas prévias claras/escuras; a matriz automatizada percorre páginas com larguras 320/360/412/600/840 e fontes 100/130/200%, além de tablet em paisagem e formulário em viewport reduzido. Isso não é captura do S25 nem substitui TalkBack/teclado/gestos reais.

E5: testes/builds/assinaturas/checksums e entregas são registrados abaixo. Teste físico, usabilidade com pessoas, QA da release exata e guarda durável da chave ainda são pendências antes da promoção a final. Não foi instalado/desinstalado nada no aparelho do usuário.

## Validação e artefatos

- 106 testes JUnit/Robolectric aprovados, zero falhas/erros; seis verificações estruturais aprovadas.
- Testes/Lint/build debug/APK de instrumentação: BUILD SUCCESSFUL em 57s. Release com R8 e redução de recursos: BUILD SUCCESSFUL em 58s.
- Lint das duas variantes: zero erros/fatais; 21 avisos avaliados, sem supressão global.
- Autoauditoria em três rodadas, sem bloqueante/alto pendente; um achado alto sobre restauração de opções foi corrigido e coberto por regressões.
- Assinaturas verificadas: certificados iguais aos candidatos anteriores. Manifest release debuggable=false, duas Activities, nenhum serviço/INTERNET e somente permissão interna AndroidX de assinatura.
- Prévias de Views/native graphics e seus testes estão nos relatórios, sem apresentar isso como medição de usuário ou dispositivo físico. Oito pares da paleta principal atingem contraste entre 6,44:1 e 16,24:1; contraste real de todos os elementos/TalkBack depende da inspeção física.
- APK instrumentado compilado; sem nova execução em runtime Android. Não atribuir resultados instrumentados/smoke de candidatos anteriores a esta versão.

| Variante | APK (bytes) | SHA-256 |
|---|---:|---|
| Debug | 14.194.933 | `d1a41c969d7e198024b8438bbcb491447904cb2c820c0d39137b2d697ce20cca` |
| Release | 2.504.535 | `6974728719b98588d8028158967c787566d6dd716f59ef7cfedb1bdee738e78e` |

Certificado debug SHA-256: `dabc6d27ddf900e0f8a137e17aa38ab4e1d4f9ef8500d11186d3c97c77d35823`.
Certificado release SHA-256: `a99bd676fb3a8603605fd94e2fd0cbc7cf36f9d313623931635fa4e1780741c8`.

[Atualização debug final em ZIP privado](https://drive.google.com/file/d/1DYNxZmU3sRi07k3AAslrArJmf03FxpLR/view), tamanho 5.011.120 bytes conferido por leitura de metadados; sem tornar público. ZIP instalável tem somente APK, checksum e instruções, com igualdade dos bytes internos verificada. Fonte e relatórios são separados; nenhuma chave/credencial é incluída. Whitespace e segredos no conteúdo versionado conferidos antes de arquivar.
