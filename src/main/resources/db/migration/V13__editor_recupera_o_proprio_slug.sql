-- Conserta o desempate da V12.
--
-- Lá, o passo final resolvia colisão de username pela idade da conta (`created_at`). O
-- critério está errado: quem tem perfil de editor tem uma URL **já publicada e indexada**
-- em /editores/{slug}, e essa URL é o ativo de SEO do produto. Perder o endereço pra um
-- homônimo sem perfil quebra link que já circula — e o dono nem fica sabendo.
--
-- A regra certa: entre dois candidatos ao mesmo nome, o editor ganha. O outro recebe o
-- sufixo. Vai em duas passadas porque o UNIQUE não deixa dois donos do mesmo valor nem
-- por um instante — o ocupante sai pra um nome temporário antes de o editor entrar.

-- 1. Quem não é editor e está ocupando o slug de um editor sai pra um nome temporário.
--    O prefixo 'tmp-' não colide com nada: a V12 só gera 'u-' ou nome derivado.
UPDATE users u
SET username = 'tmp-' || LEFT(u.id::TEXT, 12)
WHERE NOT EXISTS (SELECT 1 FROM editor_profiles me WHERE me.user_id = u.id)
  AND EXISTS (
      SELECT 1 FROM editor_profiles e
      WHERE e.slug = u.username
        AND e.user_id <> u.id
  );

-- 2. O editor recupera o próprio slug, agora que o lugar está livre.
UPDATE users u
SET username = e.slug
FROM editor_profiles e
WHERE e.user_id = u.id
  AND u.username <> e.slug
  AND LENGTH(e.slug) <= 30
  AND NOT EXISTS (SELECT 1 FROM users o WHERE o.username = e.slug AND o.id <> u.id);

-- 3. Quem saiu no passo 1 volta a ter nome legível, derivado do próprio display_name com
--    sufixo curto — mesma regra da V12. Não dá pra recuperar o nome anterior a partir do
--    'tmp-', e nem interessa: o anterior era justamente o que pertencia ao editor.
UPDATE users
SET username = LEFT(
        TRIM(BOTH '-' FROM REGEXP_REPLACE(
            LOWER(TRANSLATE(display_name,
                            'áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ',
                            'aaaaaeeeeiiiiooooouuuucnAAAAAEEEEIIIIOOOOOUUUUCN')),
            '[^a-z0-9]+', '-', 'g')), 21)
        || '-' || LEFT(id::TEXT, 8)
WHERE username LIKE 'tmp-%';
