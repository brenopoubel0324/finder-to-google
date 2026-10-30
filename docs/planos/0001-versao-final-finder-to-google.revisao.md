# Revisão do plano — Finder to Google, versão final

Veredito atual: **APROVADO para execução por etapas — plano 1.1**. Conclusão do release continua condicionada à chave definitiva e à validação do candidato real no aparelho.

Método: `execution-plan-review`, confrontando o plano com o código, os consumidores das interfaces, os testes, os relatórios e o resultado relatado pelo usuário. Revisão do snapshot local; não existe branch/histórico Git utilizável neste momento. Nenhum código de aplicativo ou APK foi alterado nesta entrega.

## Rodada 1 — plano 1.0

Veredito: **REVISAR**.

### R1 — Alto: migração de preferências sem precedência/atomicidade definida

- Seção afetada: E1.2; `AppPreferences`, interface `BrowserSelection`, picker e status.
- Problema: o plano introduz `browser_package`, mas não define a precedência entre ele e `browser_component`, como tratar valores inválidos, nem como manter ambos coerentes após uma escolha nova. Migrar somente quando o navegador está instalado poderia apagar a escolha justamente no cenário de recuperação; renomear/resetar a preferência de compatibilidade quebraria o fluxo confirmado.
- Correção obrigatória: explicitar nomes das interfaces/keys, migração idempotente sem consulta a PackageManager, preservação de navegador ausente, tratamento de valor inválido e gravação conjunta da escolha por pacote e dica de componente. Incluir os casos de regressão.

### R2 — Alto: diagnóstico tipado sem contrato de retorno e consumidores

- Seção afetada: E3; `SearchLauncher`, `SearchOpener`, `LinkRouter`, `RouteOutcome`, `LinkRouterActivity`, `SettingsActivity` e `DiagnosticsFormatter`.
- Problema: atualmente `SearchOpener.open` retorna Boolean. A proposta de informar motivo tipado não pode ser cumprida apenas mudando o texto; faltam o resultado de domínio, o mapeamento de exceções e a propagação até fallback, recuperação, clipboard e testes. Um estado mutável “última falha” introduziria associação incorreta entre aberturas.
- Correção obrigatória: definir resultado imutável de sucesso/falha tipada, atualizar todos os consumidores e testes e manter falhas sem mensagens brutas de exceção. Persistir na recuperação somente o estado necessário, sem URL adicional.

### R3 — Médio: proteção de reentrada precisa de limite e semântica de falha

- Seção afetada: E1.3.
- Problema: a janela/limite de despachos está definida, mas não o tamanho máximo da estrutura nem se uma tentativa que falha ao iniciar consome a cota. A ação manual de recuperação também precisa ser distinguida de um extra externo falsificado.
- Correção: definir capacidade, expiração por acesso, clock injetável, reserva/liberação de tentativa e reset somente por callback local. Testar repetição legítima, ciclo, expiração e falha de startActivity. Documentar o limite local ao processo.

### R4 — Médio: comandos finais e conteúdo dos artefatos precisam ser explícitos

- Seções afetadas: E4–E6.
- Problema: há comandos debug, mas o procedimento de release está descrito sem comandos equivalentes de Lint/assemble/verificação. A entrega também precisa evitar incluir APK de teste, unsigned ou secrets ao empacotar diretórios de saída.
- Correção: incluir comandos reais de instrumentação debug, build/Lint release e verificação de assinatura/metadados; não presumir que testes debug validam o release minificado. Criar lista explícita dos arquivos entregues, conferindo certificado e hash do APK exato submetido ao teste manual.

## Pendências externas já corretamente identificadas

- Chave definitiva, guarda e possível reinstalação por mudança de certificado.
- Aparelho/emulador para instrumentação e matriz Samsung do candidato final.
- A assinatura de URL foi confirmada para o fluxo relatado, sem identidade confiável do chamador; não existe base para ampliar a lista nem confiar no referrer.

As pendências não impedem planejar/implementar E0–E4; impedem declarar um release final concluído sem resolver os critérios correspondentes.

## Rodada 2 — plano 1.1

O plano inteiro foi relido após os ajustes e novamente confrontado com as interfaces/callers atuais.

| Achado | Resolução no plano | Estado |
|---|---|---|
| R1 — alto | E1.2 define precedência, migração idempotente, ausência de consulta a PM, flags preservadas e gravação conjunta das chaves; E4 inclui regressões | Resolvido no documento |
| R2 — alto | E3 define SearchLaunchResult/SearchFailure imutáveis, mapeamento de exceções, consumidores, fallback e estado de recuperação; E4 cobre privacidade | Resolvido no documento |
| R3 — médio | E1.3 define janela, capacidade 16, reserva/liberação, clock de teste, enum de recuperação e reset local | Resolvido no documento |
| R4 — médio | E4/E5 fornecem comandos reais e distinguem instrumentação debug de release; E6 exige lista de artefatos e igualdade do hash testado/entregue | Resolvido no documento |

Veredito: **APROVADO**. Nenhum achado bloqueante/alto permanece no plano. Não há alteração obrigatória adicional antes de iniciar E0–E4; a aprovação não significa que as funções já foram implementadas, os novos testes passaram ou a assinatura está pronta.

## Validações mínimas para executar

- Baseline do APK funcional, histórico local e exclusão de segredos antes do primeiro commit de implementação.
- Regressão de preferências/compatibilidade, resolução por pacote/URI, falha de candidato isolada, recuperação/reentrada e diagnóstico sem dados de busca.
- Testes unitários, verificações estruturais, Lint, build debug/test APK e revisão do diff. Executar instrumentação com dispositivo e registrar qualquer pendência real.
- Build e Lint release com chave configurada, apksigner, metadados, debuggable=false, fingerprint/hash e smoke test do APK release exato antes da entrega.

## Riscos residuais e limites da aprovação

- A escolha/guarda da chave é externa ao código atual. Certificado diferente pode exigir reinstalação; a aprovação do plano não autoriza apagar preferências ou o APK instalado.
- A matriz Samsung do release ainda depende do aparelho/usuário. A captura existente valida o fluxo relatado do debug, sem provar o release futuro ou todos os firmwares.
- A proteção de reentrada é limitada ao processo, à janela e à mesma URI; não autentica apps nem resolve ciclos externos arbitrários. A quarta abertura legítima muito rápida pode exigir retry manual.
- O fallback continuará explícito e sem troca automática de pacote. A heurística permanecerá opcional e limitada à assinatura confirmada.
- Não foi executado build/teste nesta etapa documental. As evidências anteriores foram consultadas, sem reapresentá-las como teste do código futuro.
- O hash do APK debug foi reconferido e permaneceu `0fafbffd722462347daca7a5550444bd80b838c56a3e0115b2e400cf11984085`.

Implementação, geração de chave, alteração do APK e publicação continuam fora desta entrega de planejamento/revisão.
