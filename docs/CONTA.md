# Configurações da conta na T08

## Contrato para a T17

As duas rotas exigem o Bearer de uma conta ativa. O backend identifica o
titular pelo token.

| Rota | Corpo JSON | Sucesso |
| --- | --- | --- |
| `PUT /v1/users/me/display-name` | `{"displayName":"Nome atualizado"}` | `200` com os dados atualizados da conta. |
| `POST /v1/users/me/password` | `{"currentPassword":"…","newPassword":"…"}` | `204` sem corpo. Todas as sessões anteriores perdem acesso, inclusive a usada no pedido. |

O nome é aparado nas pontas e deve ter de 1 a 80 caracteres. O perfil
público e a vitrine usam o novo nome. O endereço do perfil não muda. O
`PATCH /v1/users/me` mantém a edição de Instagram, inclusive a remoção
com `instagram: null`. A rota `PUT /v1/users/me/username` mantém suas
próprias regras. O e-mail continua fora da edição de conta.

Na troca de senha, envie as duas senhas exatamente como digitadas. A nova
senha deve ter pelo menos 8 caracteres, com teto de 72 bytes em UTF-8.
Um caractere pode ocupar mais de um byte. A API exige a senha atual,
inclusive para contas vinculadas a um provedor que já tenham senha.

Após o `204`, apague o cookie de sessão no Next e encaminhe para novo
login. A rota não devolve JWT novo. Se a troca falhar com `403`, `409`
ou `429`, a sessão atual continua válida, salvo expiração ou revogação
por outra operação.

| Status de erro | Tratamento |
| --- | --- |
| `400` | Mostrar erros dos campos em `errors`, ou `error` para JSON inválido. |
| `401` | Orientar novo login quando a sessão estiver ausente, expirada ou revogada. |
| `403` | Informar que a senha atual está incorreta. |
| `409` | Para conta social sem senha, oferecer o fluxo existente de recuperação de senha para definir a primeira. |
| `429` | Ler `Retry-After` em segundos e informar o tempo de espera. |

## Limites e transação

A troca de senha tem cotas próprias de **5 por conta** e **30 por origem**
a cada **15 minutos**, sem intervalo adicional entre tentativas. As cotas
são configuráveis em `tramposeditor.auth-limits.password-change`. A contagem
usa o ID do titular e a mesma regra de origem confiável da T05. Cada pedido
válido que chega ao serviço ocupa uma tentativa. O login por senha, o
cadastro e a recuperação mantêm suas cotas atuais. O armazenamento em
memória e o limite global de entradas são compartilhados pelo limitador
da T05. Os contadores reiniciam com o processo e cada instância guarda os
seus próprios contadores.

A troca valida a versão da sessão após bloquear a conta. Alterar o hash,
incrementar a versão e remover os links pendentes de redefinição ocorre em
uma única transação. Tanto a troca quanto o reset bloqueiam primeiro a
conta, depois tratam os tokens. A troca autenticada não envia e-mail.
