-- Bloco 5 — Conversa (ESBOCO §7, item 1).
--
-- Uma conversa, duas portas de entrada: o "Falar com" da vitrine e o aceite de um
-- interesse num pedido. As duas desembocam aqui, pra que exista UMA caixa de entrada
-- em vez de dois fluxos paralelos.
--
-- Antes disso, o contato era a revelação do e-mail no aceite (FUNDACAO §11.9) — o que
-- mandava a relação pra fora da plataforma no instante em que ela começava.

CREATE TABLE conversations (
    id             UUID      NOT NULL,
    -- Sempre há um lado que contrata e um que edita: a assimetria é do domínio, e
    -- guardá-la aqui evita uma tabela de participantes que não acrescentaria nada.
    client_user_id UUID      NOT NULL,
    editor_user_id UUID      NOT NULL,
    -- Preenchido quando a conversa nasceu de um pedido; nulo quando nasceu do perfil.
    job_request_id UUID,
    -- Ordena a caixa de entrada sem precisar agregar a tabela de mensagens.
    last_message_at TIMESTAMP,
    -- Marcador de leitura por lado. Duas colunas resolvem o "tem mensagem nova?" sem
    -- recibo por mensagem, que seria caro e desnecessário nesta escala.
    client_read_at TIMESTAMP,
    editor_read_at TIMESTAMP,
    created_at     TIMESTAMP NOT NULL,
    CONSTRAINT pk_conversations PRIMARY KEY (id),
    CONSTRAINT fk_conversations_client FOREIGN KEY (client_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_editor FOREIGN KEY (editor_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_job FOREIGN KEY (job_request_id) REFERENCES job_requests (id) ON DELETE CASCADE,
    -- Ninguém conversa consigo mesmo.
    CONSTRAINT ck_conversations_partes_distintas CHECK (client_user_id <> editor_user_id)
);

-- Unicidade em duas regras, e não numa só, porque NULL não colide em UNIQUE no
-- Postgres: sem o índice parcial, o mesmo par abriria infinitas conversas diretas.
CREATE UNIQUE INDEX uk_conversations_direta
    ON conversations (client_user_id, editor_user_id)
    WHERE job_request_id IS NULL;

CREATE UNIQUE INDEX uk_conversations_por_pedido
    ON conversations (client_user_id, editor_user_id, job_request_id)
    WHERE job_request_id IS NOT NULL;

-- A caixa de entrada é "minhas conversas, mais recente primeiro" pelos dois lados.
CREATE INDEX idx_conversations_client ON conversations (client_user_id, last_message_at DESC);
CREATE INDEX idx_conversations_editor ON conversations (editor_user_id, last_message_at DESC);

CREATE TABLE messages (
    id              UUID          NOT NULL,
    conversation_id UUID          NOT NULL,
    sender_user_id  UUID          NOT NULL,
    body            VARCHAR(2000) NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at);
