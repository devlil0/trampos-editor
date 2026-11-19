# TramposEditor

Marketplace que conecta **editores de vídeo** a criadores e empresas.

## Rodando localmente

Você precisa de Docker com Compose, JDK 21, Node.js 24 e npm. Use um clone
limpo, sem copiar arquivos privados de outra máquina. O Compose de
desenvolvimento sobe apenas o PostgreSQL 16. A API Spring Boot e o frontend
Next.js rodam em terminais próprios.

No PowerShell, partindo da raiz do projeto:

1. Suba o banco com `docker compose up -d --wait postgres`. O Compose
   espera o banco ficar saudável. A porta 5432 fica disponível só na máquina local.
2. Em outro terminal, rode na raiz `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'`. A API
   usa o perfil de desenvolvimento e sobe em `http://localhost:8080`.
3. Em outro terminal, rode os comandos abaixo na raiz e abra
   `http://localhost:3000`.

   ```powershell
   Set-Location frontend
   npm.cmd ci
   npm.cmd run dev
   ```

No Linux ou macOS, use `bash ./mvnw spring-boot:run` para a API e
`cd frontend`, `npm ci` e `npm run dev` para o frontend. Com a API e o
Next nessas portas, os valores padrão atendem ao desenvolvimento.

A saúde da API pode ser conferida com
`Invoke-RestMethod http://localhost:8080/actuator/health` no PowerShell
ou `curl http://localhost:8080/actuator/health` no Linux ou macOS. Para
testar o backend com um PostgreSQL descartável, veja os comandos no
[guia de CI](docs/CI.md#backend).

## Cadastro por senha em duas etapas

Para ler o código de teste, configure o [capturador SMTP local](docs/EMAIL.md#capturador-smtp-local)
e reinicie apenas a API usando o perfil de desenvolvimento conforme o guia.
Mantenha o banco e o Next rodando. O modo `log` simula o envio e registra
metadados operacionais. O código fica disponível no Mailpit quando se usa
o capturador.

Pelo site, abra `http://localhost:3000/criar-conta`, preencha o cadastro
com um endereço fictício e confirme o código de seis dígitos exibido
na caixa local do Mailpit. A sessão só é criada na confirmação.

Para conferir as mesmas duas chamadas diretamente na API em PowerShell,
use outro endereço e nome de usuário fictícios. O primeiro pedido
retorna `202` com `email` e `expiresIn` em segundos. Ele guarda uma
pendência e solicita o envio do código.

```powershell
$cadastro = @{
    email = 'cliente-teste@example.test'
    password = 'senha-local-ficticia'
    displayName = 'Cliente Teste'
    username = 'cliente-teste'
    roles = @('CLIENT')
} | ConvertTo-Json -Compress
$cadastro | curl.exe -sS -w "`nHTTP %{http_code}`n" -H 'Content-Type: application/json' --data-binary '@-' http://localhost:8080/v1/auth/register
```

Leia o código do pedido no Mailpit. O segundo POST deve retornar `201`,
criar a conta com e-mail confirmado e devolver o usuário e o token de
acesso. O exemplo descarta o corpo para evitar exibir o token no terminal.

```powershell
$codigo = Read-Host 'Código de seis dígitos do Mailpit'
$confirmacao = @{ email = 'cliente-teste@example.test'; code = $codigo } | ConvertTo-Json -Compress
$confirmacao | curl.exe -sS -o NUL -w "HTTP %{http_code}`n" -H 'Content-Type: application/json' --data-binary '@-' http://localhost:8080/v1/auth/register/confirm
```

No site, o formulário envia apenas `CLIENT`. Publicar o perfil de editor
com o e-mail confirmado acrescenta `EDITOR` à mesma conta. Quem integra
diretamente com a API pode enviar `CLIENT`, `EDITOR` ou ambos em
`roles`. O cadastro público nunca concede `ADMIN`. Ter o papel `EDITOR`
por si só não publica o perfil.

## Rotas principais da API

| Método | Rota | Acesso e uso |
| --- | --- | --- |
| POST | `/v1/auth/register` | Público. Inicia o cadastro e retorna `202`. |
| POST | `/v1/auth/register/confirm` | Público. Confirma o código e retorna `201` com a sessão. |
| POST | `/v1/auth/register/resend` | Público. Solicita novo código. |
| POST | `/v1/auth/login` | Público. Devolve o token de acesso para conta habilitada. |
| GET, PATCH | `/v1/users/me` | Bearer. Consulta a conta ou edita o Instagram. |
| PUT | `/v1/users/me/display-name`, `/v1/users/me/username` | Bearer. Editam nome e username em rotas separadas. |
| POST | `/v1/users/me/password` | Bearer. Troca de senha com confirmação da atual. |
| GET | `/v1/editors`, `/v1/editors/{slug}` | Público. Vitrine filtrada e detalhe com portfólio. |
| GET, PUT | `/v1/me/editor-profile` | Bearer. Consulta ou publica o perfil. A publicação exige e-mail confirmado. |
| POST, DELETE | `/v1/me/editor-profile/portfolio`, `/v1/me/editor-profile/portfolio/{itemId}` | Bearer. Adiciona ou remove item do perfil do titular. |
| POST, GET | `/v1/me/job-requests` | Bearer. Publica ou lista os pedidos do cliente. |
| POST | `/v1/me/job-requests/{jobId}/close` | Bearer. Fecha o pedido do cliente. |
| POST | `/v1/me/job-requests/{jobId}/interests/{interestId}/accept` | Bearer. Aceita candidato, abre a conversa e registra a contratação. |
| GET | `/v1/me/job-interests`, `/v1/job-requests` | Bearer. Lista as candidaturas do titular ou o feed de pedidos. |
| POST | `/v1/job-requests/{jobId}/interests` | Bearer com perfil publicado. Demonstra interesse em um pedido. |
| GET | `/actuator/health` | Público. Saúde da API. |

Em chamadas pela interface, o Next intermedeia as operações autenticadas
e mantém o token em cookie `HttpOnly`. A leitura pública do avatar carrega
a imagem direto da API. Upload e remoção passam pelo Next. No OAuth, o
navegador segue os redirecionamentos entre Next, API e provedor. A troca
do código por sessão é feita pelo servidor Next. Veja as URLs e os
limites em [frontend/README.md](frontend/README.md).

## Operação e contexto

- [Deploy, proxy, backup e rollback](docs/DEPLOY.md).
- [E-mail, códigos de resposta e teste local](docs/EMAIL.md) e
  [login social](docs/LOGIN-SOCIAL.md). A entrega real por SMTP e o
  login completo com provedores reais aguardam validação autorizada.
- [Consultas de instrumentação](docs/QUERIES-INSTRUMENTACAO.sql).
- O upgrade afetado pela V12 permanece bloqueado e os avisos de
  vulnerabilidades aguardam tratamento. Veja a [limitação da V12](docs/CI.md)
  e o [inventário de vulnerabilidades](docs/T10-VULNERABILIDADES.md).
- [Fundação](docs/FUNDACAO.md) e [esboço](docs/ESBOCO.md) registram
  decisões e retratos históricos. As decisões de retenção e moderação
  da T12 e as validações na infraestrutura de destino seguem pendentes.
