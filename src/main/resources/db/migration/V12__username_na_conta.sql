-- Username: a identidade da pessoa no produto, e o endereço dela em `/{username}`.
--
-- Antes só o editor tinha endereço próprio (`editor_profiles.slug`, em /editores/{slug}),
-- e quem só contrata não existia como página. Com o username na CONTA, os dois papéis
-- ganham identidade — e o produto passa a ter um lugar só pra "quem é essa pessoa", em
-- vez de um pra editor e outro pra ninguém.
--
-- O slug do editor continua na tabela dele por ora; quem manda no endereço passa a ser
-- este campo, e o slug vira histórico pro redirecionamento 301 das URLs antigas.

ALTER TABLE users ADD COLUMN username VARCHAR(30);

-- Backfill em três passadas, da melhor fonte pra pior. A ordem importa: cada passada só
-- preenche quem ainda está nulo.

-- 1. Quem é editor herda o próprio slug — é o endereço que já está publicado e indexado,
--    e mudá-lo agora quebraria link que já circula.
UPDATE users u
SET username = e.slug
FROM editor_profiles e
WHERE e.user_id = u.id
  AND u.username IS NULL
  AND LENGTH(e.slug) <= 30;

-- 2. Quem não é editor deriva do nome de exibição, com a mesma regra do slugify do Java:
--    minúsculas, sem acento, não-alfanumérico vira hífen, sem hífen nas pontas.
--
--    O acento sai por TRANSLATE e não por `unaccent()`: aquela função vem de extensão, e
--    migração que depende de extensão instalada quebra em banco novo — inclusive no
--    Testcontainers da suíte.
UPDATE users
SET username = LEFT(
        TRIM(BOTH '-' FROM REGEXP_REPLACE(
            LOWER(TRANSLATE(display_name,
                            'áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ',
                            'aaaaaeeeeiiiiooooouuuucnAAAAAEEEEIIIIOOOOOUUUUCN')),
            '[^a-z0-9]+', '-', 'g')),
        30)
WHERE username IS NULL;

-- 3. Sobra de segurança: nome que virou string vazia (só símbolo, por exemplo) e colisão
--    entre homônimos. `u-` + os 8 primeiros do UUID é feio, mas é único e a pessoa pode
--    trocar depois — melhor que barrar a migração.
UPDATE users
SET username = 'u-' || LEFT(id::TEXT, 8)
WHERE username IS NULL OR username = '';

UPDATE users u
SET username = LEFT(u.username, 21) || '-' || LEFT(u.id::TEXT, 8)
WHERE EXISTS (
    SELECT 1 FROM users o
    WHERE o.username = u.username
      AND o.id <> u.id
      AND o.created_at < u.created_at
);

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);

-- A busca por username é o caminho de toda página de perfil — o índice vem do UNIQUE.
