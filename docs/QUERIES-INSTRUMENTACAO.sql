-- =============================================================================
-- Instrumentação dos laços — ESBOCO §3.1 e §3.2
--
-- "Os quatro laços têm as peças, e o furo que sobra mudou de natureza: não é
-- mecanismo, é medição. Nenhum laço registra que deu a volta."
--
-- Este arquivo é a resposta, e é de propósito um ARQUIVO, não uma tela. Dashboard
-- na camada logada é o erro que o §1 nomeia: ele infla a área que deveria só
-- resolver, e nenhuma dessas perguntas é diária.
--
-- Como usar no banco de desenvolvimento, na raiz do projeto:
--   docker compose exec -T postgres psql -X -v ON_ERROR_STOP=1 \
--     -U tramposeditor -d tramposeditor -f - < docs/QUERIES-INSTRUMENTACAO.sql
--
-- No PowerShell, use:
--   $OutputEncoding = [Text.UTF8Encoding]::new($false)
--   Get-Content -Raw -Encoding UTF8 docs/QUERIES-INSTRUMENTACAO.sql | docker compose exec -T postgres psql -X -v ON_ERROR_STOP=1 -U tramposeditor -d tramposeditor -f -
--
-- As consultas são independentes, somente leitura e sem recorte de datas.
-- Criada é toda linha em deals. Concluída exige status = 'DONE'.
-- CANCELLED também tem closed_at e fica fora da contagem de concluídas.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- LAÇO DE AQUISIÇÃO — "uma visita orgânica virou conta"
--
-- Era o único ⬜ da tabela do §3.1: nada registrava a origem do cadastro. As três
-- colunas da V16 existem só pra estas duas consultas.
-- -----------------------------------------------------------------------------

-- 1.1 De onde vieram as contas.
--
-- Referrer nulo é a maioria esperada e NÃO é falha: acesso direto, app que suprime
-- o Referer e link colado em conversa entram todos assim. O que importa é a
-- proporção entre "google" e o resto — é ela que diz se o SEO do §11.2.6 pegou.
SELECT
    COALESCE(signup_utm_source, '(sem utm)')                  AS campanha,
    COALESCE(
        -- Só o host: uma linha por domínio, não uma por URL.
        SUBSTRING(signup_referrer FROM '^https?://(?:www\.)?([^/]+)'),
        '(direto ou desconhecido)'
    )                                                          AS veio_de,
    COUNT(*)                                                   AS contas,
    MIN(created_at)::DATE                                      AS primeira,
    MAX(created_at)::DATE                                      AS ultima
FROM users
GROUP BY 1, 2
ORDER BY contas DESC;


-- 1.2 Qual PÁGINA capta.
--
-- A pergunta que o §2.3 deixou em aberto quando as tag pages entraram: elas
-- rankeiam, mas convertem? A landing é a primeira página vista, não a do cadastro
-- — quem chega por /editores/capcut e só depois clica em criar conta foi captado
-- pela tag, e é isso que esta consulta mostra.
SELECT
    COALESCE(signup_landing, '(desconhecida)') AS primeira_pagina,
    COUNT(*)                                   AS contas
FROM users
GROUP BY 1
ORDER BY contas DESC;


-- 1.3 Contas por vínculo social atual.
--
-- Uma conta pode aparecer em mais de um provedor. Sem vínculo aparece em
-- "senha". Vínculos atuais não identificam o método original de cadastro.
SELECT
    COALESCE(i.provider, 'senha') AS metodo,
    COUNT(DISTINCT u.id)          AS contas
FROM users u
LEFT JOIN user_identities i ON i.user_id = u.id
GROUP BY 1
ORDER BY contas DESC;


-- -----------------------------------------------------------------------------
-- LAÇO DA OFERTA — "o editor voltou e respondeu"
--
-- `publica perfil → aparece → recebe contato → entrega → recebe nota → sobe`
-- -----------------------------------------------------------------------------

-- 2.1 Onde o editor para de andar no funil.
--
-- Cada degrau que despenca é um laço vazando. Editor que publicou e nunca foi
-- procurado é problema de vitrine; procurado e que nunca respondeu é problema de
-- notificação (ou de e-mail não confirmado — ver 4.1).
SELECT
    COUNT(*) FILTER (WHERE e.id IS NOT NULL)                        AS publicaram_perfil,
    COUNT(*) FILTER (WHERE c.conversas > 0)                         AS foram_procurados,
    COUNT(*) FILTER (WHERE m.respostas > 0)                         AS responderam,
    COUNT(*) FILTER (WHERE d.criadas > 0)                          AS com_contratacao_criada,
    COUNT(*) FILTER (WHERE d.concluidas > 0)                       AS com_contratacao_concluida,
    COUNT(*) FILTER (WHERE e.rating_count > 0)                      AS receberam_nota
FROM users u
LEFT JOIN editor_profiles e ON e.user_id = u.id
LEFT JOIN LATERAL (
    SELECT COUNT(*) AS conversas FROM conversations c2 WHERE c2.editor_user_id = u.id
) c ON TRUE
LEFT JOIN LATERAL (
    SELECT COUNT(*) AS respostas FROM messages m2 WHERE m2.sender_user_id = u.id
) m ON TRUE
LEFT JOIN LATERAL (
    SELECT COUNT(*) AS criadas,
           COUNT(*) FILTER (WHERE d2.status = 'DONE') AS concluidas
    FROM deals d2
    WHERE d2.editor_user_id = u.id
) d ON TRUE;


-- 2.2 Contratações criadas e seu estado atual, nas duas portas de entrada.
SELECT
    COUNT(*)                                      AS contratacoes_criadas,
    COUNT(*) FILTER (WHERE status = 'OPEN')       AS abertas,
    COUNT(*) FILTER (WHERE status = 'DONE')       AS concluidas,
    COUNT(*) FILTER (WHERE status = 'CANCELLED')  AS canceladas
FROM deals;


-- -----------------------------------------------------------------------------
-- LAÇO DA DEMANDA — "o mesmo cliente publicou um segundo trampo"
--
-- O §3.2 chamou este de o buraco não mapeado: nada no plano fazia um cliente
-- satisfeito voltar. O gatilho existe desde o Bloco 6; o que faltava era medir.
-- -----------------------------------------------------------------------------

-- 3.1 Quantos clientes voltaram.
--
-- É A pergunta do §3.2. Um marketplace de serviço vive de repetição — se esta
-- coluna ficar em zero por muito tempo, o laço não está girando, e nenhuma das
-- outras métricas compensa isso.
SELECT
    COUNT(*)                                        AS clientes_que_publicaram,
    COUNT(*) FILTER (WHERE pedidos > 1)             AS voltaram,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE pedidos > 1) / NULLIF(COUNT(*), 0),
    1)                                              AS pct_retorno
FROM (
    SELECT client_user_id, COUNT(*) AS pedidos
    FROM job_requests
    GROUP BY client_user_id
) por_cliente;


-- 3.2 Repetição de pedidos por situação das contratações.
--
-- Cada cliente com pelo menos um pedido entra em um grupo. "Voltaram"
-- significa dois ou mais pedidos no histórico. A comparação não verifica
-- a ordem dos eventos nem demonstra que concluir uma contratação causou retorno.
SELECT
    CASE
        WHEN d.concluidas > 0 THEN 'com_conclusao'
        WHEN d.criadas > 0 THEN 'criada_sem_conclusao'
        ELSE 'sem_contratacao'
    END AS grupo,
    COUNT(*) AS clientes,
    COUNT(*) FILTER (WHERE j.pedidos > 1) AS voltaram
FROM (SELECT client_user_id, COUNT(*) AS pedidos FROM job_requests GROUP BY client_user_id) j
LEFT JOIN LATERAL (
    SELECT COUNT(*) AS criadas,
           COUNT(*) FILTER (WHERE d2.status = 'DONE') AS concluidas
    FROM deals d2
    WHERE d2.client_user_id = j.client_user_id
) d ON TRUE
GROUP BY 1
ORDER BY 1;


-- -----------------------------------------------------------------------------
-- LAÇO DE REPUTAÇÃO — "nota preenchida e ordenação mexeu"
--
-- O §2.1 registra que `rating_avg` era sempre 0.0, e que por isso a vitrine
-- ordenava de fato por antiguidade. Esta seção é como se descobre que isso mudou.
-- -----------------------------------------------------------------------------

-- 4.1 A ordenação está viva?
--
-- Enquanto `com_nota` for 0, os critérios 2 e 3 do ORDER BY não fazem nada e a
-- vitrine é uma fila de chegada. É o teste mais direto de que o §2.1 saiu do papel.
SELECT
    COUNT(*)                                   AS perfis,
    COUNT(*) FILTER (WHERE rating_count > 0)   AS com_nota,
    ROUND(AVG(rating_avg) FILTER (WHERE rating_count > 0), 2) AS media_das_notas
FROM editor_profiles;


-- 4.2 Cobertura de avaliações entre contratações concluídas.
--
-- Cada contratação DONE conta uma vez na base. Com pelo menos uma nota,
-- conta uma vez na cobertura, mesmo quando os dois lados avaliam. O total
-- de avaliações é exibido à parte. Sem DONE, o percentual é NULL.
WITH avaliacoes_por_contratacao AS (
    SELECT deal_id, COUNT(*) AS avaliacoes
    FROM reviews
    GROUP BY deal_id
)
SELECT
    COUNT(*) AS contratacoes_concluidas,
    COUNT(*) FILTER (WHERE r.avaliacoes > 0) AS concluidas_com_avaliacao,
    COALESCE(SUM(r.avaliacoes), 0) AS avaliacoes,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE r.avaliacoes > 0) / NULLIF(COUNT(*), 0),
        1
    ) AS pct_concluidas_com_avaliacao
FROM deals d
LEFT JOIN avaliacoes_por_contratacao r ON r.deal_id = d.id
WHERE d.status = 'DONE';


-- -----------------------------------------------------------------------------
-- O CANAL — pré-requisito de três dos quatro laços
--
-- Aviso automático só sai pra endereço confirmado (V17). Se esta seção estiver
-- ruim, nenhuma das de cima significa o que parece: o laço não está vazando por
-- falta de gatilho, está vazando porque o gatilho não sai.
-- -----------------------------------------------------------------------------

-- 5.1 Quantas contas podem receber aviso.
SELECT
    COUNT(*)                                          AS contas,
    COUNT(*) FILTER (WHERE email_verified_at IS NOT NULL) AS confirmadas,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE email_verified_at IS NOT NULL)
        / NULLIF(COUNT(*), 0),
    1)                                                AS pct
FROM users;


-- 5.2 Quanto tempo levam pra confirmar.
--
-- Mediana alta ou muitas contas sem confirmação pedem investigação.
-- A métrica não mede entrega, pasta de spam ou a causa da demora.
SELECT
    COUNT(*) FILTER (WHERE email_verified_at IS NULL)  AS nunca_confirmaram,
    -- PERCENTILE_CONT ignora nulo sozinho, então quem nunca confirmou não entra na
    -- mediana — que é o certo: "nunca" não é um tempo, é a coluna ao lado.
    ROUND(EXTRACT(EPOCH FROM PERCENTILE_CONT(0.5) WITHIN GROUP (
        ORDER BY email_verified_at - created_at
    )) / 60)                                           AS mediana_minutos
FROM users;

-- -----------------------------------------------------------------------------
-- Referência para validar em PostgreSQL descartável com dados sintéticos.
--
-- Quatro clientes publicaram ao menos um pedido:
--   A publicou 2, tem 2 DONE com o mesmo editor e um deles recebeu duas notas.
--   B publicou 1 e tem 1 OPEN com outro editor.
--   C publicou 2 e tem 2 CANCELLED com um terceiro editor.
--   D publicou 1 e não tem contratação.
--
-- Esperado em 2.1 e 2.2: 3 editores com contratação, 1 com conclusão;
-- 5 criadas, 1 OPEN, 2 DONE e 2 CANCELLED.
-- Esperado em 3.2: 1 de 1 voltou em com_conclusao, 1 de 2 em
-- criada_sem_conclusao e 0 de 1 em sem_contratacao.
-- Esperado em 4.2: 2 DONE, 1 com pelo menos uma nota, 2 notas, 50,0%.
-- Sem DONE: 4.2 retorna 0 concluídas, 0 concluídas avaliadas, 0 notas
-- contabilizadas e percentual NULL. O grupo com_conclusao não aparece em 3.2.
-- -----------------------------------------------------------------------------
