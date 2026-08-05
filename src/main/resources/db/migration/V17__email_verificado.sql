-- Verificação de e-mail (ESBOCO §2, fase 1.5).
--
-- Estava represada atrás da compra do domínio pelo motivo óbvio — não dá pra confirmar
-- endereço sem ter remetente. Com o domínio no ar e a Resend plugada, destrava.
--
-- O porquê não é burocracia: o e-mail é a única prova de posse que o produto tem, e é por
-- ele que os quatro laços do §3 voltam. Endereço errado quebra o ciclo em silêncio — a
-- pessoa nunca descobre que foi procurada e conclui que o site é morto. E endereço de
-- terceiro é pior: vira reclamação de spam na conta da Resend, que custa a reputação do
-- domínio inteiro.

-- Nulo = não verificado. Data em vez de booleano porque "quando" responde perguntas que
-- "se" não responde — quanto tempo a pessoa levou, e quantos nunca voltaram.
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP;

-- Quem entrou por Google ou GitHub já chega verificado: o provedor só entrega o e-mail
-- depois de confirmá-lo, e pedir de novo seria desconfiar de uma prova melhor que a nossa.
-- Isto também vale pro futuro, no SocialLoginService — não é só backfill.
UPDATE users u
SET email_verified_at = u.created_at
WHERE EXISTS (SELECT 1 FROM user_identities i WHERE i.user_id = u.id);

-- As contas por senha que já existem ficam como estão: não verificadas.
--
-- Dá vontade de carimbá-las como verificadas pra ninguém "perder acesso" — e seria errado.
-- Elas nunca provaram o endereço; marcá-las agora seria registrar no banco uma verificação
-- que não aconteceu, e o campo deixaria de significar o que diz. Como o produto ainda não
-- foi ao ar, o custo real disso é zero: são contas de teste.

CREATE TABLE email_verification_tokens (
    id         UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    -- O hash, nunca o token — mesma regra do password_reset_tokens.
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_email_verification_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_email_verification_tokens_expires ON email_verification_tokens (expires_at);
