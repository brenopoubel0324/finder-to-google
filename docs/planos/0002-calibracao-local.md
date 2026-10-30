# Calibração local — escopo autorizado

O usuário aprovou o fluxo de aprender e confirmar assinaturas, além de adicionar, desativar e excluir regras locais, para evitar recompilar quando mudar o parâmetro client. Base: 6784323 (rc.2). Entrega: 1.1.0-rc.3, versionCode 4.

## Contratos

- Preservar configurações/certificados. Sem root, rede, permissões novas, serviço, receiver, timer ou nova Activity exportada.
- Regras exatas do valor client, 1–128 caracteres ASCII alfanuméricos/ponto/hífen/sublinhado, começando por letra/número. Até 32 regras, inclusive desativadas. Lista vazia persiste; padrões tablet/S25 só no primeiro acesso. Nenhuma regra por prefixo, regex ou referrer.
- Aplicar lista ativa somente a pesquisa Google já validada, origem desconhecida e compatibilidade ligada. Outro app verificável continua no navegador; Finder verificável mantém a rota existente.
- Calibração iniciada na UI com papel de navegador já concedido. Pesquisa fictícia com desafio aleatório, copiada somente no botão explícito. Sessão em memória, cinco minutos por relógio monotônico; expira por acesso e termina se o processo morrer.
- Capturar apenas a pesquisa com desafio exato, ACTION_VIEW, origem não verificável como outro app e um único client válido. Uma captura por sessão. Links diferentes continuam normalmente.
- Antes de salvar, mostrar client e pedir confirmação explícita de salvar/ativar compatibilidade. Somente a confirmação altera regra/modo. Cancelar/continuar segue política normal; rotação restaura confirmação sem iniciar pesquisa. Não copiar URL ou pesquisa para preferências, diagnóstico ou estado salvo adicional; somente o marcador confirmado é persistido.
- Regras têm efeito imediato. Permitir cadastro manual, ativar/desativar e excluir. Preservar encaminhamento da URI inteira e recuperação se Google/navegador falharem.

## Validação e entrega

Cobrir validação/persistência/remoção completa/limite/corrupção de regras; calibração sem sessão, desafio incorreto, outros apps, URL inválida, duplicatas, prazo, cancelamento e perda de processo; UI de início/cópia/confirmar/cancelar/recriar e cadastro manual; roteamento com nova regra sem build. Executar JUnit/Robolectric, verificador estrutural, Lint, build debug/testes e release assinado. Fazer autoauditoria contra este escopo, verificar certificados, metadados e checksums. Entregar ZIPs privados no canal já autorizado, com fonte/relatórios separados e sem segredos.

Limites: client ausente/inválido ou mudança estrutural de URL pode exigir atualização; assinatura não autentica Finder; outro app pode imitar URL/desafio. Não afirmar QA físico/Android 17 nem execução nativa que não tenha ocorrido. Assinatura definitiva e QA final mantêm as pendências anteriores documentadas.
