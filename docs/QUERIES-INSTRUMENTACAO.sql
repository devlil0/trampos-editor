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
-- Como usar:
--   docker exec -i tramposeditor_postgres psql -U tramposeditor -d tramposeditor \
--     -f - < docs/QUERIES-INSTRUMENTACAO.sql
--
-- Ou cole uma de cada vez. Elas são independentes e todas somente-leitura.
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


-- 1.3 Como a pessoa entrou: senha ou provedor.
--
-- Derivável desde a V10 — está aqui porque a pergunta "o login social valeu a
-- pena?" é do mesmo fôlego das de cima, e ninguém vai lembrar de cruzar duas
-- tabelas pra respondê-la.
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
    COUNT(*) FILTER (WHERE d.fechados > 0)                          AS fecharam_trampo,
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
    SELECT COUNT(*) AS fechados FROM deals d2 WHERE d2.editor_user_id = u.id
) d ON TRUE;


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


-- 3.2 Fechar trampo faz voltar?
--
-- Cruza o retorno com o desfecho. Se quem fechou volta muito mais que quem não
-- fechou, o gatilho certo é o do fecho (§3.2, opção 1) e não o reconvite por
-- inatividade (opção 3) — que é o mais caro e o mais fácil de virar spam.
SELECT
    (d.fechados > 0)                    AS ja_fechou_algum,
    COUNT(*)                            AS clientes,
    COUNT(*) FILTER (WHERE j.pedidos > 1) AS voltaram
FROM (SELECT client_user_id, COUNT(*) AS pedidos FROM job_requests GROUP BY client_user_id) j
LEFT JOIN LATERAL (
    SELECT COUNT(*) AS fechados FROM deals d2 WHERE d2.client_user_id = j.client_user_id
) d ON TRUE
GROUP BY 1;


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


-- 4.2 Quantos trampos fechados viraram avaliação.
--
-- O e-mail de fecho existe pra isso. Proporção baixa aqui com e-mail chegando
-- significa que o pedido não convence; proporção baixa COM e-mail não chegando é
-- outro problema — cheque a 5.1 antes de mexer no texto.
SELECT
    COUNT(DISTINCT d.id)  AS trampos_fechados,
    COUNT(DISTINCT r.id)  AS avaliacoes,
    ROUND(100.0 * COUNT(DISTINCT r.id) / NULLIF(COUNT(DISTINCT d.id), 0), 1) AS pct
FROM deals d
LEFT JOIN reviews r ON r.deal_id = d.id;


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
-- Mediana alta ou muitos "nunca" apontam pro e-mail caindo em spam — o que se
-- resolve no DNS da Resend, não no texto do aviso.
SELECT
    COUNT(*) FILTER (WHERE email_verified_at IS NULL)  AS nunca_confirmaram,
    -- PERCENTILE_CONT ignora nulo sozinho, então quem nunca confirmou não entra na
    -- mediana — que é o certo: "nunca" não é um tempo, é a coluna ao lado.
    ROUND(EXTRACT(EPOCH FROM PERCENTILE_CONT(0.5) WITHIN GROUP (
        ORDER BY email_verified_at - created_at
    )) / 60)                                           AS mediana_minutos
FROM users;
