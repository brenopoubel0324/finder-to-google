# Autoauditoria — plano 0001

> Histórico da rc.1. A URL do S25 foi recebida posteriormente e a regra foi corrigida na rc.2; consulte [o registro atual](CANDIDATO_1.1.0-rc.2.md).

Data: 30/10/2026. Uma rodada formal sobre a implementação candidata 1.1.0-rc.1, comparada à baseline local `5d5c470` e ao plano aprovado 1.1. Revisão própria com o skill `breno-review-audit:self-audit-loop`; não é uma segunda revisão independente.

## Escopo e resultado

Foram revistos migração, catálogo e resolução, encaminhamento explícito, proteção contra reentrada, política/parser, identidade, falhas tipadas, recriação de recuperação, preferências, clipboard, UI/recursos, manifest/backup, build/assinatura, testes e documentação. A passagem de simplificação removeu recursos/import sem uso, sem refatorações laterais.

**APROVADO no escopo da implementação candidata:** nenhum achado bloqueante/alto de código identificado nesta rodada. A promoção a versão final continua pendente da validação física e do caso do S25; este veredito não certifica Android 17/One UI 9.

## Achados e limites registrados antes do fechamento

| Gravidade | Local | Evidência e tratamento |
|---|---|---|
| Médio, pendência externa | `GoogleSearchUrlParser.hasFinderSignature` / S25 | O único client confirmado é o do tablet. A captura do telefone mostra encaminhamento ao navegador. Sem URL real não é possível afirmar se o motivo é assinatura, formato de URL ou diagnóstico anterior. Solicitada pesquisa fictícia e URL; não adicionada assinatura presumida. Novo motivo tipado distingue compatibilidade desligada de assinatura desconhecida. |
| Médio, pendência de validação | Matriz em dispositivo | Falta testar release exato no Samsung, fonte 200%, TalkBack, navegação e atualização com certificado definitivo. Testes de largura em Robolectric não equivalem a validação visual completa. Não declarar versão final. |
| Médio, pendência operacional | Custódia da assinatura | Keystore e credenciais fora do projeto e backup local separado; falta guarda durável sob controle do proprietário antes da distribuição final. Nenhum segredo nos artefatos entregues. |
| Baixo, acompanhado | Lint | 21 avisos em cada variante, zero erros/fatais. Avaliados: sugestões de versões/KTX, tipografia, recursos qualificadores/ícone. Não houve supressão global. |

Durante a implementação, antes desta rodada formal, foram corrigidos: verificação de assinatura que não podia depender da existência da tarefa nativa, botões com possível truncamento de texto ampliado e exclusão explícita dos nomes de credenciais no Git. O build negativo de assinatura e a recompilação final cobrem essas correções. Não são contados como achados novos da rodada.

## Validação e varredura

56 testes JUnit/Robolectric, 6 verificações estruturais, Lint debug/release, builds debug/release/testes, assinatura/certificados, manifest final, checksums e whitespace. Quatro testes instrumentados debug também aprovados no emulador API 36. Resultado detalhado e limites da validação em `VALIDACAO_CANDIDATO.md`.

A varredura não encontrou regressão adicional no escopo revisto. Riscos residuais: origem nem sempre compartilhada pelo Android, heurística de URL imitável e dependente de firmware, proteção de ciclos local ao processo e à URL idêntica, App Links que não passam pelo roteador, variações no Google App e ausência de QA físico do candidato. Esses limites permanecem documentados.
