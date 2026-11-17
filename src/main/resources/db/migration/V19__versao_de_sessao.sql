-- T03: numeração provisória. Confirmar V19 com o time antes do merge.
ALTER TABLE users ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;
