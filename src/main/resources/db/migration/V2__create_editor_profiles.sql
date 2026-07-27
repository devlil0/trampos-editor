CREATE TABLE editor_profiles (
    id              UUID         NOT NULL,
    user_id         UUID         NOT NULL,
    slug            VARCHAR(140) NOT NULL,
    vertical        VARCHAR(20)  NOT NULL,
    headline        VARCHAR(120),
    bio             VARCHAR(2000),
    price_min_cents INTEGER,
    price_max_cents INTEGER,
    turnaround_days INTEGER,
    available       BOOLEAN      NOT NULL DEFAULT TRUE,
    rating_avg      NUMERIC(2, 1) NOT NULL DEFAULT 0.0,
    rating_count    INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP,
    CONSTRAINT pk_editor_profiles PRIMARY KEY (id),
    CONSTRAINT uk_editor_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_editor_profiles_slug UNIQUE (slug),
    CONSTRAINT fk_editor_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_editor_profiles_vertical ON editor_profiles (vertical);

CREATE TABLE editor_profile_niches (
    editor_profile_id UUID        NOT NULL,
    niche             VARCHAR(30) NOT NULL,
    CONSTRAINT pk_editor_profile_niches PRIMARY KEY (editor_profile_id, niche),
    CONSTRAINT fk_editor_profile_niches FOREIGN KEY (editor_profile_id) REFERENCES editor_profiles (id) ON DELETE CASCADE
);

CREATE TABLE editor_profile_tools (
    editor_profile_id UUID        NOT NULL,
    tool              VARCHAR(30) NOT NULL,
    CONSTRAINT pk_editor_profile_tools PRIMARY KEY (editor_profile_id, tool),
    CONSTRAINT fk_editor_profile_tools FOREIGN KEY (editor_profile_id) REFERENCES editor_profiles (id) ON DELETE CASCADE
);

CREATE TABLE portfolio_items (
    id                UUID         NOT NULL,
    editor_profile_id UUID         NOT NULL,
    title             VARCHAR(120) NOT NULL,
    provider          VARCHAR(20)  NOT NULL,
    video_url         VARCHAR(500) NOT NULL,
    position          INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL,
    CONSTRAINT pk_portfolio_items PRIMARY KEY (id),
    CONSTRAINT fk_portfolio_items_profile FOREIGN KEY (editor_profile_id) REFERENCES editor_profiles (id) ON DELETE CASCADE
);

CREATE INDEX idx_portfolio_items_profile ON portfolio_items (editor_profile_id);
