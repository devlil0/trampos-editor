-- Foto de perfil.
--
-- Em tabela própria, e não numa coluna de `users`, por um motivo prático: `bytea` grande
-- na tabela principal faz o Postgres empurrar a linha pro TOAST e atrapalha toda consulta
-- que só quer nome e e-mail — que é quase todas. Aqui a imagem só é lida quando alguém
-- pede a imagem.
--
-- Guardar no banco é decisão de fase, não de arquitetura: o avatar é reduzido a 256px no
-- servidor (~20KB), então mil contas dão ~20MB. Evita criar conta de object storage e
-- custo mensal antes de o domínio existir. A troca por S3/R2 depois é implementar outra
-- `AvatarStorage` — nenhuma tela muda, mesmo padrão do `EmailSender`.

CREATE TABLE user_avatars (
    user_id     UUID        NOT NULL,
    -- Sempre reencodada pelo servidor, então é sempre um tipo que nós escolhemos.
    content_type VARCHAR(40) NOT NULL,
    bytes       BYTEA       NOT NULL,
    -- Muda a cada upload e entra na URL da imagem. É isso que permite `Cache-Control`
    -- longo sem a foto velha grudar no browser depois de trocada.
    version     UUID        NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    CONSTRAINT pk_user_avatars PRIMARY KEY (user_id),
    CONSTRAINT fk_user_avatars_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Ponteiro na conta pra que listar perfis saiba se existe foto sem tocar no bytea.
ALTER TABLE users ADD COLUMN avatar_version UUID;
