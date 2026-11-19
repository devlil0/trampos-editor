# Erros HTTP da API

Ações do frontend podem usar o status para escolher o comportamento e ler
`error` como mensagem a exibir. Validações de campos continuam retornando
`errors`, uma lista de mensagens.

| Status | Situação | Resposta |
| --- | --- | --- |
| `400` | UUID, enum ou outro parâmetro com tipo inválido, ou corpo JSON que a API não consegue ler | `{"error":"…"}` |
| `400` | Campos de um DTO rejeitados pela validação | `{"errors":["campo: mensagem"]}` |
| `401` | Falha ou ausência de autenticação | A autenticação Bearer pode devolver apenas o status e `WWW-Authenticate`. Erros de credenciais tratados pela API podem trazer `error`. |
| `403` | Usuário autenticado sem acesso | Use `error` quando o serviço devolver a mensagem. Trate também uma resposta sem corpo. |
| `409` | Conflito de negócio ou violação de unicidade reconhecida | `{"error":"…"}` |
| `413` | Upload maior que o limite da API | `{"error":"O upload excede o tamanho permitido."}` |
| `429` | Limite de autenticação ou intervalo de envio da T05 | `{"error":"…"}`, com `Retry-After` em segundos e `Cache-Control: no-store`. |
| `503` | Falha de envio obrigatório da T01 quando o fluxo permite informar a falha | `{"error":"…"}`. A solicitação de recuperação de senha continua com a resposta genérica da T01/T05. |

Para `400`, mostre a mensagem de validação ou oriente corrigir a entrada.
Para `409`, preserve os dados do formulário e mostre a orientação recebida.
Para `413`, avise sobre o tamanho e permita escolher outro arquivo. O avatar
continua limitado a 5 MB por arquivo e 6 MB por requisição multipart da API.
Um proxy pode rejeitar o upload antes da aplicação, então a ação precisa
tratar `413` mesmo quando não há JSON.

A API verifica o SQLSTATE `23505` e o nome estruturado da restrição retornado
pelo PostgreSQL para identificar duplicidades conhecidas de conta, cadastro
pendente, candidatura, conversa, trampo aberto ou avaliação. Outras violações
de integridade retornam `500` com uma mensagem genérica. O log da API registra
o tipo da falha e o código SQLSTATE. Mensagens do driver, SQL e valores de
entrada ficam fora desse registro.

A liberação de `/error` se aplica somente ao despacho interno de erro.
Uma requisição direta segue as mesmas regras de autenticação das demais rotas
protegidas.
