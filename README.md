# TramposEditor

Marketplace que conecta **editores de vídeo** (short-form: Reels/TikTok/Shorts) a criadores e empresas.

Contexto de produto, estratégia e roadmap: [`docs/FUNDACAO.md`](docs/FUNDACAO.md).

## Stack

Spring Boot 4.0.7 · Java 21 · PostgreSQL 16 · Flyway · Spring Security (JWT HS256) · Docker

## Rodando localmente

Requer Docker e JDK 21.

```bash
docker compose up -d          # sobe o Postgres em localhost:5432
./mvnw spring-boot:run        # perfil "dev" é o padrão
```

A API sobe em `http://localhost:8080`. Health: `GET /actuator/health`.

```bash
./mvnw test                   # testes de integração com Testcontainers (precisa do Docker)
```

## Endpoints

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| POST | `/v1/auth/register` | pública | Cadastro. `roles` aceita `EDITOR` e/ou `CLIENT` |
| POST | `/v1/auth/login` | pública | Retorna access token |
| GET | `/v1/users/me` | Bearer | Usuário autenticado |
| GET | `/v1/editors` | pública | Vitrine: busca/filtro (`niche`, `tool`, `available`, `maxPriceCents`, `q`) + paginação (`page`, `size`) |
| GET | `/v1/editors/{slug}` | pública | Perfil público do editor com portfólio |
| GET | `/v1/me/editor-profile` | Bearer | Perfil de editor do usuário logado |
| PUT | `/v1/me/editor-profile` | Bearer (EDITOR) | Cria/atualiza o próprio perfil |
| POST | `/v1/me/editor-profile/portfolio` | Bearer (EDITOR) | Adiciona item de portfólio (link YouTube/Vimeo) |
| DELETE | `/v1/me/editor-profile/portfolio/{itemId}` | Bearer (EDITOR) | Remove item de portfólio |
| GET | `/actuator/health` | pública | Health check |

```bash
curl -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"editor@exemplo.com","password":"senhasegura123","displayName":"Editor","roles":["EDITOR"]}'
```

## Deploy na VPS

```bash
cp .env.example .env          # preencher; JWT_SECRET precisa de 32+ caracteres
docker compose -f docker-compose.prod.yml up -d --build
```

A aplicação escuta apenas em `127.0.0.1:8080` e o Postgres não é exposto — quem fala com a
internet é o Nginx na frente, terminando TLS e fazendo `proxy_pass` para a porta local.
