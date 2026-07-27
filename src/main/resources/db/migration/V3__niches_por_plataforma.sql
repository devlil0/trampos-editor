-- Nichos deixam de ser formato (SHORTS) e passam a ser plataforma (TIKTOK, REELS, YOUTUBE).
-- Quem atendia SHORTS atendia Reels e TikTok, então o valor antigo vira as duas linhas.
-- A ordem importa: inserir TIKTOK antes de converter SHORTS, senão o SELECT não acha mais nada.

INSERT INTO editor_profile_niches (editor_profile_id, niche)
SELECT editor_profile_id, 'TIKTOK'
FROM editor_profile_niches
WHERE niche = 'SHORTS'
ON CONFLICT ON CONSTRAINT pk_editor_profile_niches DO NOTHING;

UPDATE editor_profile_niches
SET niche = 'REELS'
WHERE niche = 'SHORTS';
