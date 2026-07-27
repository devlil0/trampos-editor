-- "Postar pedido rápido" (FUNDACAO §10.2): o cliente descreve o serviço e os editores
-- demonstram interesse. Sem lance/leilão de propósito.

CREATE TABLE job_requests (
    id             UUID          NOT NULL,
    client_user_id UUID          NOT NULL,
    title          VARCHAR(120)  NOT NULL,
    description    VARCHAR(2000) NOT NULL,
    niche          VARCHAR(30)   NOT NULL,
    budget_cents   INTEGER,
    deadline_days  INTEGER,
    reference_url  VARCHAR(500),
    status         VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP,
    CONSTRAINT pk_job_requests PRIMARY KEY (id),
    CONSTRAINT fk_job_requests_client FOREIGN KEY (client_user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- A lista que o editor vê é sempre "abertos, mais recentes primeiro".
CREATE INDEX idx_job_requests_status_created ON job_requests (status, created_at DESC);
CREATE INDEX idx_job_requests_client ON job_requests (client_user_id);

CREATE TABLE job_interests (
    id             UUID         NOT NULL,
    job_request_id UUID         NOT NULL,
    editor_user_id UUID         NOT NULL,
    message        VARCHAR(500) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_job_interests PRIMARY KEY (id),
    CONSTRAINT fk_job_interests_request FOREIGN KEY (job_request_id) REFERENCES job_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_interests_editor FOREIGN KEY (editor_user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- Um editor se candidata uma vez por pedido.
    CONSTRAINT uk_job_interests_editor UNIQUE (job_request_id, editor_user_id)
);

CREATE INDEX idx_job_interests_editor ON job_interests (editor_user_id);
