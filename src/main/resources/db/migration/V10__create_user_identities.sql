-- Login com Google e GitHub.
--
-- A regra que manda aqui é a conta única do FUNDACAO §11.9: mesmo e-mail é a mesma
-- pessoa. Entrar pelo Google numa conta criada com senha VINCULA o provedor à conta
-- existente — nunca cria uma segunda. Duas contas com o mesmo e-mail seria o começo de
-- "cadê meu perfil?", que é o pior primeiro contato possível.

-- Quem entra só por provedor não tem senha, e forçar uma senha falsa seria mentir sobre
-- o que a conta é. A partir daqui, senha é opcional — o que exige que todo caminho de
-- login por senha trate hash nulo como "essa conta não entra por senha".
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

CREATE TABLE user_identities (
    id         UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    -- 'google' | 'github' — o registrationId do Spring Security.
    provider   VARCHAR(30) NOT NULL,
    -- O id da pessoa no provedor. Nunca o e-mail: e-mail muda, o subject não.
    subject    VARCHAR(190) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT pk_user_identities PRIMARY KEY (id),
    CONSTRAINT fk_user_identities_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- Uma identidade do provedor aponta pra uma conta só.
    CONSTRAINT uk_user_identities_provider_subject UNIQUE (provider, subject),
    -- E a mesma conta não vincula o mesmo provedor duas vezes.
    CONSTRAINT uk_user_identities_user_provider UNIQUE (user_id, provider)
);

-- Código de uso único que fecha o login social.
--
-- O provedor devolve pro BACKEND, mas quem guarda a sessão é o frontend, num cookie
-- httpOnly (o browser nunca vê o JWT — ver `lib/session.ts`). Mandar o JWT na URL de
-- redirecionamento resolveria e vazaria: fica em log de servidor, em histórico e no
-- Referer. Então o backend devolve um código curto e de uso único, e o frontend troca
-- por JWT numa chamada de servidor pra servidor. É o mesmo padrão do authorization code
-- do próprio OAuth, pela mesma razão.
CREATE TABLE auth_exchange_codes (
    id         UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    code_hash  VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT pk_auth_exchange_codes PRIMARY KEY (id),
    CONSTRAINT fk_auth_exchange_codes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_auth_exchange_codes_hash UNIQUE (code_hash)
);

CREATE INDEX idx_auth_exchange_codes_expires ON auth_exchange_codes (expires_at);
