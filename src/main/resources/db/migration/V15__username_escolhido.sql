-- O username deixa de ser enfeite: passa a ser escolhido, verificado e trocável.
--
-- Até aqui ele só era DERIVADO — a V12 o gerou a partir do slug ou do display_name, e o
-- `UsernameService.validar()` existia sem ninguém chamar. Ninguém nunca escolheu o próprio
-- endereço, e ninguém podia trocar. Isto conserta as duas pontas.
--
-- Duas colunas, uma por pergunta que o produto passa a fazer:

-- 1. "Esta pessoa escolheu o nome dela, ou herdou um gerado?"
--
--    O cadastro por senha passa a pedir o username no formulário, mas o login social não
--    tem formulário — a conta nasce no callback do provedor. Lá o nome continua sendo
--    gerado, e esta coluna é o que marca a dívida: quem entra com `false` cai numa tela de
--    escolha antes de navegar (o guard mora em `/painel/layout.tsx`).
--
--    O default é TRUE porque toda conta que já existe fica com o nome que já tem: ele já é
--    o endereço público dela, e virar todo mundo pra tela de onboarding no primeiro login
--    depois do deploy seria cobrar do usuário antigo uma dívida que é nossa.
ALTER TABLE users ADD COLUMN username_chosen BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. "Quando foi a última troca?" — o cooldown de 14 dias sai daqui.
--
--    Nulo quer dizer "nunca trocou", e é o estado de toda conta existente e de toda conta
--    nova. A primeira escolha (no cadastro ou no onboarding) NÃO preenche este campo: ela
--    não é troca, é a estreia. Sem essa distinção, quem errasse uma letra no cadastro
--    ficaria 14 dias com o nome errado.
ALTER TABLE users ADD COLUMN username_changed_at TIMESTAMP;
