CREATE TABLE deal_mediations (
    deal_id UUID PRIMARY KEY REFERENCES deals (id) ON DELETE RESTRICT,
    agreed_amount_cents BIGINT NOT NULL CHECK (agreed_amount_cents > 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'DISPUTED', 'CLOSED')),
    activated_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    CONSTRAINT ck_deal_mediations_closed_at CHECK (
        (status = 'CLOSED' AND closed_at IS NOT NULL AND closed_at >= activated_at)
        OR (status IN ('ACTIVE', 'DISPUTED') AND closed_at IS NULL)
    )
);

-- A linha representa uma mediação já ativada. Checkout e registros financeiros
-- têm ciclo próprio. O valor em centavos é o combinado na ativação.
COMMENT ON TABLE deal_mediations IS 'Mediação por contratação, preservada mesmo após encerramento.';
