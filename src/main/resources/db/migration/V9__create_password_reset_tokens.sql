-- Esqueci a senha (ESBOCO §7, item 6).
--
-- Sem isto, conta perdida é usuário perdido: quem esquece a senha não tem saída nenhuma
-- e some. O inventário §2 listava a tela como MVP e ela não existia.

CREATE TABLE password_reset_tokens (
    id         UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    -- Guarda o HASH do token, nunca o token. Vazamento de banco não pode virar
    -- vazamento de contas: o que vai no e-mail existe só no e-mail.
    token_hash VARCHAR(64)  NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    -- Uso único. Preenchido no momento em que a senha troca.
    used_at    TIMESTAMP,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash)
);

-- A confirmação busca pelo hash; a limpeza de expirados varre por data.
CREATE INDEX idx_password_reset_tokens_expires ON password_reset_tokens (expires_at);
