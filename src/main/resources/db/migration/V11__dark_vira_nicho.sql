-- "Canal dark" era estilo e passa a ser nicho.
--
-- A troca corrige uma classificação errada: estilo é COMO se corta (clean, dinâmica,
-- cinematográfica); nicho é ONDE/QUE TIPO de conteúdo (TikTok, YouTube, podcast). Canal
-- dark — sem rosto, narração sobre imagem de apoio — é tipo de canal, não jeito de cortar.
--
-- Consequência prática, e a razão real da mudança: nicho tem página de tag indexável
-- (ESBOCO §2.3) e estilo não. "Editor para canal dark" é busca com intenção de contratar;
-- deixá-la como estilo era desperdiçar a única dimensão de estilo com demanda de busca.
--
-- Mesma forma da V3, que já moveu SHORTS: insere o valor novo antes de apagar o velho.

INSERT INTO editor_profile_niches (editor_profile_id, niche)
SELECT editor_profile_id, 'DARK'
FROM editor_profile_styles
WHERE style = 'DARK'
ON CONFLICT ON CONSTRAINT pk_editor_profile_niches DO NOTHING;

DELETE FROM editor_profile_styles
WHERE style = 'DARK';
