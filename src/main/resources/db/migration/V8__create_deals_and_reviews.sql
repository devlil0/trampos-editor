-- Bloco 6 — Trampo fechado e avaliação (ESBOCO §7, item 4).
--
-- Este é o marco de "acabou", e ele fecha DOIS laços de uma vez (ESBOCO §3.1 e §3.2):
--   * reputação — sem trampo fechado não há o que avaliar, e sem nota a ordenação da
--     vitrine (§2.1) fica inerte, decidindo tudo por antiguidade;
--   * demanda   — sem evento de fim, ninguém sabe que o trampo terminou, então não há
--     o que pedir, medir nem oferecer depois. O laço nem começava a dar a volta.

CREATE TABLE deals (
    id              UUID        NOT NULL,
    -- Todo trampo acontece dentro de uma conversa: é ela que guarda o combinado.
    conversation_id UUID        NOT NULL,
    -- Preenchido quando o trampo nasceu de um pedido do feed; nulo quando nasceu da
    -- vitrine. Mesma assimetria que `conversations` já carrega.
    job_request_id  UUID,
    client_user_id  UUID        NOT NULL,
    editor_user_id  UUID        NOT NULL,
    -- Do que se está falando. Copiado do pedido quando há um, digitado pelo cliente
    -- quando o trampo veio da vitrine — o histórico precisa fazer sentido sozinho,
    -- sem obrigar quem lê a abrir a conversa.
    title           VARCHAR(140) NOT NULL,
    -- OPEN -> DONE (entregue) ou CANCELLED (não vingou). Cancelado também é informação:
    -- é o que impede o histórico de virar uma lista só de sucessos.
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP   NOT NULL,
    closed_at       TIMESTAMP,
    CONSTRAINT pk_deals PRIMARY KEY (id),
    CONSTRAINT fk_deals_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_deals_job FOREIGN KEY (job_request_id) REFERENCES job_requests (id) ON DELETE SET NULL,
    CONSTRAINT fk_deals_client FOREIGN KEY (client_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_deals_editor FOREIGN KEY (editor_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_deals_partes_distintas CHECK (client_user_id <> editor_user_id),
    CONSTRAINT ck_deals_status CHECK (status IN ('OPEN', 'DONE', 'CANCELLED')),
    -- Fechado tem data de fechamento; aberto não tem. Sem isto o "quando acabou" do
    -- histórico depende de todo mundo lembrar de preencher.
    CONSTRAINT ck_deals_closed_at CHECK (
        (status = 'OPEN' AND closed_at IS NULL) OR (status <> 'OPEN' AND closed_at IS NOT NULL)
    )
);

-- Um trampo aberto por vez em cada conversa. Índice parcial de novo: fechados podem se
-- repetir à vontade (é exatamente a repetição de fornecedor que o §3.2 quer premiar),
-- mas dois abertos no mesmo par seria ambiguidade sobre qual está sendo avaliado.
CREATE UNIQUE INDEX uk_deals_um_aberto_por_conversa
    ON deals (conversation_id)
    WHERE status = 'OPEN';

-- O histórico é "meus trampos fechados, mais recente primeiro", pelos dois lados.
CREATE INDEX idx_deals_client ON deals (client_user_id, created_at DESC);
CREATE INDEX idx_deals_editor ON deals (editor_user_id, created_at DESC);

CREATE TABLE reviews (
    id             UUID         NOT NULL,
    deal_id        UUID         NOT NULL,
    -- Quem escreveu e sobre quem. Os dois lados avaliam: o ethos do §6 vale para o
    -- editor e para o cliente, e uma avaliação de mão única seria hierarquia, não cultura.
    author_user_id UUID         NOT NULL,
    target_user_id UUID         NOT NULL,
    rating         SMALLINT     NOT NULL,
    comment        VARCHAR(1000),
    created_at     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT fk_reviews_deal FOREIGN KEY (deal_id) REFERENCES deals (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_author FOREIGN KEY (author_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_target FOREIGN KEY (target_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_nao_se_autoavalia CHECK (author_user_id <> target_user_id),
    -- Uma avaliação por pessoa por trampo. Sem isto a nota vira urna aberta.
    CONSTRAINT uk_reviews_autor_por_trampo UNIQUE (deal_id, author_user_id)
);

-- A média de um editor é lida a cada review nova, pra recalcular o cache do perfil.
CREATE INDEX idx_reviews_target ON reviews (target_user_id);
