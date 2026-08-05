-- Instrumentação dos laços (ESBOCO §3.1 e §3.2).
--
-- O diagnóstico do §3 fechou com os quatro laços tendo as peças pra girar — e nenhum
-- registrando que deu a volta. "Está funcionando?" não tinha resposta em lugar nenhum.
--
-- Quase tudo que falta já é derivável do que existe: método de cadastro sai de
-- `user_identities`, cliente que voltou sai de `job_requests`, efeito da nota na ordenação
-- sai de `reviews` + `editor_profiles.rating_avg`. Nada disso se perde esperando.
--
-- O que NÃO é derivável é a origem externa: ela existe por um instante, na primeira visita,
-- e some se ninguém gravar. Daí estas três colunas — e daí a decisão de 2026-08-04 de que
-- elas precisam existir ANTES de o site ir ao ar, não antes de existir domínio. Os domínios
-- foram comprados em 2026-08-05; esta é a migration que fecha aquela janela.
--
-- Sem tela: dashboard na camada logada é o erro que o §1 nomeia. O que sai daqui se
-- consulta pelo `docs/QUERIES-INSTRUMENTACAO.sql`.

-- De onde a pessoa veio. Nulo é resposta legítima e será a maioria: acesso direto, app que
-- suprime Referer, e todo cadastro anterior a esta migration.
ALTER TABLE users ADD COLUMN signup_referrer VARCHAR(255);

-- Só o `utm_source`. Medium e campaign ficaram de fora porque hoje não existe campanha
-- nenhuma pra medir — coluna que ninguém preenche é dívida, não instrumento. Quando houver
-- campanha, elas entram numa migration que sabe o que está medindo.
ALTER TABLE users ADD COLUMN signup_utm_source VARCHAR(120);

-- A primeira página vista, não a página do cadastro. É esta a que responde "qual conteúdo
-- capta?" — que é a pergunta do SEO long-tail do FUNDACAO §11.2.6. Quem chega por
-- /editores/capcut e só depois clica em criar conta foi captado pela tag, não pelo
-- formulário.
ALTER TABLE users ADD COLUMN signup_landing VARCHAR(255);

-- Marca se o código de troca nasceu junto com a conta.
--
-- O login social cria a conta no callback do provedor, mas quem tem o cookie de primeira
-- visita é o frontend, que só entra em cena na troca do código. Sem esta coluna, a troca
-- não sabe distinguir "conta nova" de "pessoa que já tinha conta entrando de novo" — e
-- atribuiria a origem de hoje a alguém que se cadastrou mês passado.
ALTER TABLE auth_exchange_codes ADD COLUMN new_account BOOLEAN NOT NULL DEFAULT FALSE;
