-- Cadastro confirmado por código de seis dígitos (2026-08-06).
--
-- Até aqui, POST /v1/auth/register criava a conta e devolvia sessão no mesmo passo. Um
-- script com uma lista de endereços enchia `users` sem provar posse de nenhum deles — e
-- cada linha dessas queima um username, aparece em conversa e conta como usuário numa
-- métrica que deveria significar gente.
--
-- A verificação da V17 não resolvia isso, e vale entender por quê: ela é *posterior*. A
-- conta já existia, já tinha sessão e já podia navegar; o link só destravava notificação e
-- publicação de perfil. Contra bot, verificar depois é tarde — o custo dele é criar a
-- conta, não usá-la.
--
-- Agora o cadastro nasce nesta tabela e só vira conta quando a pessoa devolve o código que
-- foi pro e-mail. Quem não lê a caixa de entrada não passa daqui, e o que fica para trás
-- expira sozinho sem nunca ter tocado em `users`.
--
-- O que isto NÃO é: verificação de e-mail. Essa continua sendo a da V17, e continua
-- valendo pra quem já tem conta. O que muda é que a conta criada por este caminho já nasce
-- com o endereço provado — o código é a prova — e por isso ela não precisa do link.

CREATE TABLE pending_signups (
    id            UUID         NOT NULL,
    email         VARCHAR(180) NOT NULL,

    -- A senha já chega cifrada, e não em claro. Esta tabela guarda um cadastro que ainda
    -- não é conta, mas a senha é a mesma que a pessoa vai usar — vazar aqui teria o mesmo
    -- custo de vazar `users`. Mesmo tamanho da coluna de lá.
    password_hash VARCHAR(100) NOT NULL,

    display_name  VARCHAR(80)  NOT NULL,
    username      VARCHAR(60)  NOT NULL,

    -- Os papéis pedidos, separados por vírgula. Texto e não tabela-filha de propósito: a
    -- linha é efêmera (vive minutos), e normalizar o que vai ser apagado é cerimônia.
    roles         VARCHAR(120) NOT NULL,

    -- O hash do código, nunca o código — a mesma regra do password_reset_tokens e do
    -- email_verification_tokens. Seis dígitos é um espaço pequeno, então o hash aqui não
    -- protege contra força bruta offline; quem faz esse trabalho é o limite de tentativas
    -- abaixo. O hash protege contra o outro caso: quem lê a tabela não consegue confirmar
    -- cadastros alheios.
    code_hash     VARCHAR(64)  NOT NULL,

    -- Sem isto, seis dígitos caem em segundos: são só um milhão de combinações. Com cinco
    -- tentativas por código, a chance de acertar chutando é 1 em 200 mil — e o cadastro
    -- morre na quinta, obrigando um código novo (que custa outro e-mail e outro minuto).
    attempts      SMALLINT     NOT NULL DEFAULT 0,

    expires_at    TIMESTAMP    NOT NULL,
    created_at    TIMESTAMP    NOT NULL,

    -- A origem da primeira visita viaja junto (V16). Ela é capturada quando o formulário é
    -- enviado, e a conta só nasce alguns minutos depois — sem carregá-la aqui, todo
    -- cadastro que passasse por confirmação perderia a atribuição, que é justamente o dado
    -- que não é derivável de nada.
    signup_referrer   VARCHAR(255),
    signup_utm_source VARCHAR(120),
    signup_landing    VARCHAR(255),

    CONSTRAINT pk_pending_signups PRIMARY KEY (id),

    -- Um pendente por e-mail. Pedir de novo substitui o anterior em vez de acumular linha
    -- e deixar dois códigos válidos ao mesmo tempo.
    CONSTRAINT uk_pending_signups_email UNIQUE (email)
);

-- O username também é único entre pendentes: sem isto, dois cadastros simultâneos pedem o
-- mesmo endereço, os dois recebem código, e o segundo a confirmar leva um erro depois de
-- ter feito tudo certo. Melhor recusar na hora do formulário, que é onde dá pra corrigir.
--
-- Não há UNIQUE cruzado com `users` (o banco não faz isso entre tabelas): quem garante é a
-- checagem no serviço, e o UNIQUE de `users` continua sendo o juiz final na hora de criar.
CREATE UNIQUE INDEX uk_pending_signups_username ON pending_signups (username);

CREATE INDEX idx_pending_signups_expires ON pending_signups (expires_at);
