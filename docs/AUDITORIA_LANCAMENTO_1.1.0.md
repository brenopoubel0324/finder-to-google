# Autoauditoria da preparação do lançamento

Base: `ab5f357` (main remoto, incluindo controles de custo do CI). Escopo: versionamento 1.1.0/code 7, documentação de distribuição/privacidade, empacotamento e validação. Sem alteração de código Kotlin, política, dados ou recursos do app. Processo self-audit-loop, sem revisor independente.

## Rodada 1 — antes da compilação

Nenhum achado bloqueante/alto na revisão dos arquivos completos e consumidores. Riscos médios tratados na implementação:

- O documento físico ainda descrevia ações de versões antigas e negava validação no Samsung de forma geral. Atualizados os caminhos atuais e registrada a confirmação específica do usuário em 09/11, separada do teste pendente da release exata.
- Uma entrega manual sem validação poderia usar debug, chave errada ou APK antigo. O empacotador recusa pacote/versão/SDK/certificado inesperados, depuração, backup habilitado e componentes/permissões adicionais. Usa lista explícita de documentos, não copia diretórios e não sobrescreve entrega anterior. Testes negativos cobrem os principais modos de falha.
- Compilação não comprova testes nativos; o manifesto da entrega registra que falta testar o APK release exato. Publicação final permanece em rascunho até esse resultado e a confirmação da guarda da chave.

Validação inicial: seis contratos estruturais e quatro testes do verificador aprovados; `git diff --check` aprovado. A revisão preserva os controles de custo existentes e evita execução remota com `[skip ci]` enquanto billing estiver bloqueado.

## Conferência e pendências

Achado médio na aplicação do verificador ao APK real: a primeira regra rejeitava todos os providers/receivers, embora AndroidX acrescente InitializationProvider e ProfileInstallReceiver. Corrigida com lista exata de nomes/tipos, authorities, exported e permissão DUMP do receptor. Não remover inicializadores da biblioteca nem dispensar a inspeção do manifest final. Teste negativo adicional exige falhar se essas proteções forem enfraquecidas. O verificador continua recusando serviços e componentes extras. Isso não altera o APK ou seus fluxos.

Conferir após build: testes, Lint das duas variantes, APKs, R8, assinatura, hashes e ZIP. Registrar evidência em `RESULTADO_LANCAMENTO_1.1.0.md`. Sem aparelho conectado/KVM neste ambiente, não declarar smoke nativo ou compatibilidade universal. SUS/TalkBack/matriz física e custódia externa da chave não foram inferidos a partir de automação.
