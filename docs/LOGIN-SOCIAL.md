# Login social na T04

Um vínculo já registrado usa o identificador estável do provedor. Se o endereço
mudar ou faltar prova de e-mail, a conta vinculada permanece a mesma. Contas
inativas continuam sem acesso.

Para criar uma conta ou fazer um vínculo novo por e-mail, o backend exige um
endereço verificado pelo provedor. O Google fornece essa indicação no ID token
validado. O GitHub precisa do escopo `user:email` para consultar `/user/emails`,
inclusive quando o perfil exibe um endereço público.

O Google pode ter verificado um e-mail de terceiro no passado sem controlar a
caixa de entrada hoje. Para vincular automaticamente uma conta já existente,
aceitamos Gmail ou um domínio que corresponda ao `hd` do ID token do Workspace.
Para os demais domínios, a pessoa pode entrar pelo método que já usa. Não há um
novo fluxo de confirmação nesta entrega.

## Contrato com o frontend

1. O botão abre `GET /entrar/iniciar?provider=google|github` no Next. A rota cria
   uma prova aleatória de 32 bytes em hexadecimal e envia apenas seu SHA-256
   em `browser_challenge` ao endpoint de autorização do backend. A prova fica
   no cookie `te_oauth_<hash>`, limitado ao host e a `/entrar/callback`, com
   `HttpOnly`, `SameSite=Lax`, `Secure` no build de produção e validade de 5 minutos.
2. O backend guarda o hash junto à solicitação OAuth na sessão e o recupera
   somente quando o Spring valida o `state`. O provedor não recebe esse hash.
   Após autenticar, o backend redireciona para `/entrar/callback?code=…&attempt=…`.
   `attempt` é o hash, usado pelo Next para localizar o cookie da tentativa.
3. `POST /v1/auth/social/exchange` exige `code` e `browserProof`, com `origin`
   opcional. A prova é enviada no corpo pelo servidor do Next. A resposta de
   sucesso permanece igual, com o JWT salvo no cookie de sessão já existente.
   Ausência ou formato inválido da prova retorna `400`. A combinação incorreta,
   expirada ou usada retorna `401`. Uma prova errada não consome o código.
4. O código vence no primeiro dos dois prazos: 2 minutos após sua emissão ou
   5 minutos após a tentativa registrada no backend. A troca mantém a trava
   no banco até o commit. O hash composto usa a coluna existente, sem migração.
   Códigos emitidos antes da mudança exigem reiniciar o login.

O Next apaga o cookie da tentativa ao concluir o callback, inclusive em
cancelamento ou falha com tentativa identificada. Sem um `state` válido, o
backend volta direto para `/entrar` e o cookie residual vence por prazo.
A sessão OAuth do Spring mantém as regras atuais de uma autorização pendente
por sessão. Se outra aba iniciar um login, a tentativa substituída pode precisar
ser reiniciada.

Os redirecionamentos usam `oauth_error` com códigos conhecidos como `cancelled`,
`expired`, `email_unverified`, `link_not_allowed`, `account_inactive`,
`unavailable` ou `failed`. A tela traduz o código para uma mensagem fixa.
Exceções, detalhes do provedor e o valor da prova ficam fora das URLs e dos logs.
O Next encaminha mensagens desconhecidas para o caso genérico.

API e frontend precisam ser publicados juntos para manter o login social.
O login por e-mail e senha permanece no fluxo atual.

## Validação

Os testes locais usam PostgreSQL descartável e provedores simulados. O teste
com apps OAuth reais de Google e GitHub continua pendente e deve ocorrer
em ambiente autorizado, com URLs de retorno específicas e sem credenciais
de produção.
