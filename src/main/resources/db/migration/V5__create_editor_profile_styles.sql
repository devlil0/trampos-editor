-- Estilo de edição: dimensão ortogonal à plataforma (um Reels pode ser meme ou
-- cinematográfico). Mesma modelagem das outras coleções do perfil.

CREATE TABLE editor_profile_styles (
    editor_profile_id UUID        NOT NULL,
    style             VARCHAR(30) NOT NULL,
    CONSTRAINT pk_editor_profile_styles PRIMARY KEY (editor_profile_id, style),
    CONSTRAINT fk_editor_profile_styles FOREIGN KEY (editor_profile_id) REFERENCES editor_profiles (id) ON DELETE CASCADE
);
