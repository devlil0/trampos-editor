# Frontend do TramposEditor

Interface em Next.js com App Router. Para o desenvolvimento, use Node.js 24
e npm. O backend Java e o PostgreSQL devem estar disponíveis conforme o
[guia da raiz](../README.md#rodando-localmente).

## Desenvolvimento

No PowerShell, partindo da raiz do projeto:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd run dev
```

No Linux ou macOS, use `cd frontend`, `npm ci` e `npm run dev`.
Abra `http://localhost:3000`. Para testar o cadastro por senha, use o
[capturador SMTP local](../docs/EMAIL.md#capturador-smtp-local) ligado
à API. O Next só cria a sessão após a confirmação do código.

## Variáveis de ambiente

Os valores padrão de localhost atendem quando os dois runtimes rodam na
máquina local. Para mudar uma URL, use o exemplo sanitizado em
[`.env.example`](.env.example).

| Variável | Uso |
| --- | --- |
| `API_URL` | Endereço da API que o servidor Next alcança. Usado em consultas e operações autenticadas. Padrão `http://localhost:8080`. |
| `PUBLIC_API_URL` | Endereço da API acessível pelo navegador para fotos públicas e redirecionamentos OAuth. Quando omitido, usa `API_URL`. |
| `SITE_URL` | Origem pública do Next para metadados, robots.txt e sitemap. Padrão `http://localhost:3000`. No Compose de produção, mantenha o mesmo valor no build e em execução. |

O Next age como BFF, intermediando chamadas entre o navegador e a API.
O servidor Next usa o token guardado em cookie `HttpOnly` para autenticar
essas chamadas.

- **Avatar público.** O Next produz a URL e o navegador busca a imagem
  em `GET /v1/avatars/{username}` na API pública. O upload e a remoção
  passam pelas ações autenticadas no Next.
- **OAuth.** O início passa pelo Next. O navegador segue para a origem
  pública da API e para o provedor. A API recebe o callback do provedor e
  redireciona de volta ao Next. O servidor Next troca o código de uso
  único com a API interna, usando a prova do navegador. OAuth é opcional.
  Veja o [contrato do login social](../docs/LOGIN-SOCIAL.md).

Para publicar o frontend junto com a API, siga o
[guia de deploy](../docs/DEPLOY.md).

## Assets de marca

Ícones, favicon e imagem de compartilhamento são gerados a partir das
artes originais em `../media/`. Para gerar novamente, rode
`node scripts/gen-brand-assets.mjs` dentro de `frontend`.

O script escreve os ícones e as imagens em `src/app/` e gera
`public/logo-mark.png`. O contexto dessa decisão está na seção 11.7
do [documento de fundação](../docs/FUNDACAO.md).
