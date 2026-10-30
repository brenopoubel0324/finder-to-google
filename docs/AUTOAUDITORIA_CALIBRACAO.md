# Autoauditoria — calibração local (rc.3)

Base: 6784323. Escopo: plano 0002 e fluxo aprovado pelo usuário. Revisão própria com `breno-review-audit:self-audit-loop`, não revisão independente.

## Rodada 1 — achados antes das correções

- Médio — `dialog_signature.xml` e `activity_settings.xml`: o campo de cadastro manual e o texto selecionável do desafio usam salvamento padrão de View. Desabilitar explicitamente esse salvamento para evitar que um endereço colado por engano, ou o desafio efêmero, entre em estado adicional da interface. Testar campo sem saveEnabled e ausência de dados pessoais em persistência.
- Baixo — `FinderCalibration.inspect`: acrescentar proteção contra falha de leitura dos parâmetros da URI não confiável, mantendo o resultado Unsupported após consumir um teste correspondente.
- Sem achado bloqueante/alto nos arquivos revistos. Correspondência exata, limites, confirmação, identidade verificável e ausência de mudanças no manifest estão preservados.

Validação intermediária: testes de modelo/persistência passaram; dois testes novos de diálogo falharam porque callbacks postados no looper de teste não haviam sido executados. Ajustar a sincronização do teste, sem mudar o comportamento dos botões no app. Rodada final e resultados serão registrados após correções e nova execução.

## Rodada 2 — reavaliação

As duas correções foram aplicadas: salvamento automático desabilitado nos campos efêmeros e falha de leitura da URI tratada como teste não suportado. Os testes de diálogo agora aguardam callbacks do looper, e o teste manual verifica saveEnabled=false. A suíte completa passou: 84 testes, zero falhas/erros, incluindo aprender → confirmar → novo link → desativar com efeito imediato. As seis verificações estruturais também passaram.

Revisados os arquivos completos de store, calibração, parser, política/router, Activities, gerenciador de diálogo, recursos e consumidores. Nenhum achado bloqueante/alto remanescente; nenhum achado adicional na varredura. APROVADO no escopo da implementação candidata: builds debug/release e Lint concluídos, certificados/manifest conferidos no registro do candidato; não é aprovação física no Samsung.

Pendências: QA físico do fluxo de calibração, acessibilidade e release minificada; assinatura de URL imitável e dependente de estrutura/client; sessão termina se o processo morrer; formatos sem client válido requerem outra solução. A guarda durável da chave mantém a pendência anterior. Estes limites estão no plano e no registro do candidato. Lint/build/certificados e entrega são finalizados em CANDIDATO_1.1.0-rc.3.md.
