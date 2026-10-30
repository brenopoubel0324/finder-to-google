# Execução do plano 0001 — candidato 1.1.0-rc.1

> Histórico da rc.1. A URL do S25 foi recebida posteriormente e a regra foi corrigida na rc.2; consulte [o registro atual](CANDIDATO_1.1.0-rc.2.md).

Data: 30/10/2026 (São Paulo). Base Git local: `5d5c470`. Nenhum push/publicação pública foi realizado.

## Entregas implementadas

- E0: baseline 1.0.0 preservada fora do projeto e histórico Git inicializado; segredos excluídos do controle de versão.
- E1: escolha de navegador por pacote; resolução por URI/Activity; isolamento de candidato com metadados defeituosos; migração local/idempotente de `browser_component` para `browser_package`, preservando flags e escolhas ausentes; estado de recuperação preservado sem repetir Google; proteção efêmera de fallback (3 aberturas/2 segundos, até 16 fingerprints).
- E2: configuração em cartões, modo de compatibilidade visível, papel e prontidão separados, conteúdo centralizado com máximo 640 dp, área avançada recolhível, ícone adaptativo e versão. Botões permitem múltiplas linhas para fontes ampliadas.
- E3: copiar diagnóstico mediante ação, limpeza ao desligar, metadados saneados, falhas Google tipadas e texto que informa envio da solicitação; backup/transferência explicitamente excluídos.
- E4: 56 testes JUnit/Robolectric aprovados e 6 verificações estruturais aprovadas. Lint debug/release sem erros; há avisos de sugestões KTX, versões mais recentes, tipografia e ícone. APK de instrumentação compilado e quatro testes instrumentados debug aprovados em emulador API 36; detalhes em `VALIDACAO_CANDIDATO.md`.
- E5: chave definitiva nova criada a pedido do usuário, RSA 4096; assinatura release verificada, debuggable=false; R8/minificação e redução de recursos ativos. Chave e credenciais fora do projeto, com backup local separado; não constam nos instaladores, fonte ou relatórios públicos. A custódia de longo prazo da chave permanece responsabilidade a formalizar antes da distribuição final.
- E6: pacotes candidatos privados preparados e enviados ao Drive, com hash e instruções. O release exato foi instalado e aberto em emulador API 36. A validação física da release e o ajuste da assinatura do S25 permanecem pendentes.

## Correção do registro de dispositivos

O resultado inicialmente confirmado foi no tablet, não no S25. O usuário esclareceu que o celular é S25/Android 17/One UI 9. A captura do celular mostra origem desconhecida, pista do launcher, compatibilidade ligada e decisão para navegador. Pode haver assinatura diferente ou registro anterior à troca da opção; é necessário repetir uma pesquisa fictícia por `teste` e obter o parâmetro `client`/URL. Não foi inserida uma lista presumida de assinaturas de celular.

Foi acrescentada a decisão `BROWSER_UNKNOWN_SIGNATURE` para distinguir esse caso de compatibilidade desligada sem registrar URL, consulta ou valor livre de parâmetro.

## Artefatos candidatos verificados

| Variante | APK (bytes) | SHA-256 APK |
|---|---:|---|
| Debug, atualização preservando certificado | 15.108.690 | `52d5cac0aefab6a4c76d9a14fbe200c8d6556456054e6c8ac335db9719b44cc5` |
| Release, chave definitiva nova | 2.419.107 | `2946caf1942752ffacdd6abc04ddaba9aad53e3e7b5b9b9b6bb833603ebb1050` |

Certificado debug SHA-256: `dabc6d27ddf900e0f8a137e17aa38ab4e1d4f9ef8500d11186d3c97c77d35823` (igual ao instalado anteriormente).
Certificado release SHA-256: `a99bd676fb3a8603605fd94e2fd0cbc7cf36f9d313623931635fa4e1780741c8`.

Ambas as variantes têm applicationId `br.com.findertogoogle`, versionCode 2, minSdk 29 e targetSdk 36. A assinatura definitiva é diferente da debug; exige reinstalação para migrar entre elas. Não houve instalação/desinstalação no aparelho do usuário.

Downloads privados já verificados por readback de metadados (sem compartilhamento público):

- Atualização debug: https://drive.google.com/file/d/1q59xOEIPwh_4LitNZVWbLZkx8Wx8WKzx/view?usp=drivesdk
- Release candidato: https://drive.google.com/file/d/1xE31r7fZKKNKjJtodUSLVRk-5MoCZATv/view?usp=drivesdk

## Critério de encerramento

Este estado é **candidato**, não final aprovado no S25. Falta confirmar a assinatura exata do celular, tratar o caso se a regra não corresponder e validar o release exato em aparelho, incluindo falhas/reinstalação. A versão funcional original permanece preservada. O resultado de instrumentação deve distinguir indisponibilidade do ambiente de testes aprovados.
