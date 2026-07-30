-- Instagram é atributo da CONTA, não do perfil de editor. A conta aqui é uma só (todo
-- mundo entra como cliente e vira editor ao publicar o perfil), então cliente e editor
-- leem e escrevem o mesmo campo — duplicar em editor_profiles criaria duas verdades.
--
-- Guardamos só o username, sem "@" e sem URL: o service normaliza antes de gravar.
-- 30 é o limite do próprio Instagram.
ALTER TABLE users ADD COLUMN instagram VARCHAR(30);
