# TramposEditor — Documento de Fundação

> Marketplace para encontrar **editores de vídeo profissionais**.
> **Registro histórico.** As decisões e os retratos datados abaixo
> preservam o contexto em que foram escritos. Consulte o
> [README atual](../README.md) para executar o projeto e conferir
> os contratos vigentes.
>
> Dia 1: estudo, análise, mapeamento e planejamento. Nada de código de produto ainda.
> Data: 2026-07-21 · Stack base já existente: Spring Boot 4.1.0 + Java 17 (esqueleto limpo).

---

## 1. O que é o produto (definição em 1 frase)

Uma plataforma de **duas pontas (two-sided marketplace)** que conecta:

- **Oferta:** editores de vídeo profissionais (freelancers).
- **Demanda:** criadores de conteúdo, infoprodutores, empresas e pessoas que precisam de edição.

Posicionamento: o **"anti-Workana"** — simples, sem burocracia, sem plano/assinatura, focado em experiência. Um *life saver* dos dois lados.

Áudio (mix/master/beats) **não é deste produto**: mesma finalidade, outro SaaS, com marca e
domínio próprios. Decisão de 2026-08-04 — a TramposEditor é de vídeo, ponto.

---

## 2. O risco nº1 que define tudo: **Cold Start (liquidez)**

> Marketplace não morre por falta de features. Morre por falta de **liquidez**: cliente que chega e não acha editor bom → sai; editor que chega e não acha cliente → sai. É o problema do ovo e da galinha.

Isso precisa ser a obsessão do dia 1, acima de qualquer detalhe técnico. Duas decisões que resolvem 80% disso:

### 2.1. NICHAR (não ser "edição de vídeo em geral")
Liquidez é mais fácil num **lago pequeno** do que num oceano. Em vez de "todos os editores para todos os clientes", escolher UM nicho pra dominar primeiro. Candidatos:

| Nicho | Por que é bom pra começar | Ticket médio |
|---|---|---|
| **Short-form (Reels/TikTok/Shorts)** | Volume altíssimo, recorrente, clientes = criadores que já vivem no TikTok/IG (onde você vai marketar) | R$ 80–400 |
| Edição p/ YouTubers / infoprodutores | Tickets maiores, relação recorrente | R$ 300–1.500 |
| Edição p/ small business / social media | Demanda B2B estável | R$ 200–800 |

**Recomendação:** começar por **short-form**. É onde oferta e demanda são mais densas e onde o marketing orgânico (§4) tem alcance natural. Expande depois.

### 2.2. Semear a OFERTA primeiro, à mão (concierge)
Editor freelancer está **faminto por trabalho** → é o lado mais fácil de atrair. Recrutar manualmente os primeiros **~30–50 editores bons e curados**, montar portfólio deles, deixar a vitrine cheia ANTES de trazer cliente. Cliente que chega tem que ver qualidade de cara.

---

## 3. Monetização — a dúvida mais difícil (e onde há uma contradição a resolver)

**Meta declarada: ~R$ 2.000/mês.**

### 3.1. A contradição que precisa ficar clara
Você quer duas coisas que, na forma ingênua, **se contradizem**:

- (A) "**pego uma % pequena por serviço fechado**"
- (B) "**sem planos, assinaturas e qualquer burocracia**"

O problema: **para cobrar % de forma confiável, você PRECISA intermediar o pagamento** (escrow / split de pagamento). Se cliente e editor só se conhecem no site e pagam por fora (Pix direto), eles **fecham fora da plataforma pra fugir da taxa** — isso se chama *platform leakage/disintermediation* e é a doença crônica de todo marketplace de freela. E intermediar pagamento é **exatamente o oposto de "sem burocracia"**: exige split (Pagar.me/Asaas/Stripe Connect), KYC, retenção, resolução de disputa, nota fiscal.

Ou seja: **% forçada = mais burocracia. Sem burocracia = % não se sustenta sozinha.** Precisamos escolher conscientemente.

### 3.2. As alternativas, com a matemática do R$ 2k

| Modelo | Como funciona | Conta pra bater R$ 2k/mês | Fere o "sem burocracia"? | Risco de leakage |
|---|---|---|---|---|
| **Take-rate (comissão)** | % sobre cada job fechado, via pagamento intermediado | 10% de R$400 = R$40/job → **50 jobs/mês**. 15% de R$500 → **~27 jobs/mês** | **Sim** (exige escrow/split) | Alto se pagamento for por fora |
| **Destaque/Boost avulso** | Editor paga p/ aparecer no topo/em destaque, **pago por uso, sem assinatura** | R$25/semana × 80 boosts, ou 20 editores × R$100 = **R$ 2k** | **Não** (pay-per-use ≠ assinatura) | Nenhum |
| **Lead unlock** | Editor paga p/ desbloquear contato de um pedido de cliente | R$10/lead × 200 = **R$ 2k** | Levemente (fricção pro editor) | Médio |
| **Anúncios no site** | Ads de terceiros | Precisa de ~150–300k pageviews/mês pra R$2k | **Sim, polui** — fere o diferencial de "melhor experiência" | — |

### 3.3. Recomendação: **sequenciar a monetização atrás da liquidez**
Não monetizar cedo demais — isso mata marketplace novo. Proposta em 3 fases:

- **Fase 1 (MVP / lançamento): GRÁTIS dos dois lados.** Objetivo único = criar liquidez e reputação. Zero taxa, zero anúncio. Site limpo.
- **Fase 2 (monetização limpa): Destaque/Boost avulso.** Editor paga *por uso* pra aparecer em destaque. **Não é assinatura, não é burocracia, não polui o cliente, não tem leakage.** É o caminho mais fiel aos seus diferenciais e o mais provável de bater os R$ 2k primeiro.
- **Fase 3 (take-rate como SERVIÇO, não imposto): Pagamento protegido opcional.** Aí sim intermediar pagamento — mas vendido como **proteção/garantia** ("seu dinheiro fica seguro até a entrega"). O cliente paga pela plataforma porque isso o *protege*; o editor aceita porque traz *confiança e mais clientes*. A % vira o preço de um seguro, não uma taxa que as pessoas querem evitar. Isso resolve o leakage de forma honesta.

> Descartar **anúncios** como fonte principal: contradiz frontalmente "melhor experiência" e só paga com tráfego que você não terá tão cedo.

---

## 4. Marketing / Go-to-Market (canais)

Regra de ouro: no começo, **conteúdo orgânico + comunidades** ganham de tráfego pago. Google Ads só faz sentido depois que houver liquidez e conversão (senão você paga caro por clique que não fecha).

| Canal | Papel | Como usar | Quando |
|---|---|---|---|
| **TikTok / Instagram (Reels/Shorts)** | 🥇 Principal | Antes/depois de edições, "editor spotlight", bastidores, dicas. É onde vivem **os dois lados** (editores E criadores-clientes). Alcance orgânico ainda é alto pra short-form. | Desde o dia 1 |
| **Discord** | 🥈 Aquisição de oferta | Comunidades de edição já existem e são enormes. **Parceria/presença**, não competir. É o hábitat natural do editor. Ótimo pra recrutar os 30–50 primeiros. | Semeadura inicial |
| **Google Ads** | 🥉 Demanda de alta intenção | Termos "contratar editor de vídeo", "editor de reels". Alta intenção, mas caro e competitivo. Queima dinheiro sem liquidez. | Só na Fase 2+ |
| Indicação / referral | Loop de crescimento | Editor traz editor; cliente satisfeito traz cliente | Após MVP |

**Tática de cold-start (resume §2.2):** recrutar oferta à mão no Discord/IG, curar portfólios, encher a vitrine, e SÓ ENTÃO empurrar demanda via short-form.

---

## 5. System Design (mapeamento técnico)

### 5.1. Duas formas de interação — e qual escolher
Referências (Workana/99Freelas) usam o modelo **"cliente posta projeto → freelancers dão lances/propostas"**. Isso é poderoso, mas é **cheio de fricção** (proposta, lance, negociação) — parece exatamente a burocracia que você quer evitar.

| Modelo | Como é | Prós | Contras |
|---|---|---|---|
| **A) Diretório / Vitrine** (browse → contato) | Cliente navega perfis de editores, vê portfólio, contata direto | Simples, rápido de construir, fiel ao "descomplicado" | Cliente com pedido muito específico tem que procurar |
| **B) Job board** (cliente posta → editores aplicam) | Estilo Workana | Bom pra pedidos específicos | Fricção, lances, "leilão" — sensação de burocracia |

**Recomendação:** **liderar pelo Modelo A (Diretório)** — é o mais alinhado ao seu diferencial e o mais barato de lançar. Adicionar depois um "**Postar um pedido rápido**" (versão leve do B, sem sistema de lances) como fluxo secundário.

### 5.2. Entidades do domínio (mapa inicial)

```
User (base)
 ├─ role: EDITOR | CLIENT  (uma pessoa pode ser os dois)
 ├─ EditorProfile
 │    ├─ bio, nichos[], ferramentas[] (Premiere, DaVinci, AfterFX, CapCut)
 │    ├─ faixa de preço, prazo médio (turnaround), disponibilidade
 │    ├─ PortfolioItem[]  (embed YouTube/Vimeo/upload — vídeo é o coração)
 │    └─ rating agregado
 └─ ClientProfile (leve)

JobRequest (opcional / Modelo B leve)
 ├─ descrição, orçamento, prazo, referências
 └─ status

Conversation / Message      (chat interno — ponto de contato)
Review (rating + comentário, pós-serviço)
Boost / FeaturedPlacement    (monetização Fase 2)
Transaction / Escrow         (monetização Fase 3 — só quando chegar lá)
```

### 5.3. Stack (ancorada no que já existe)
- **Backend:** Spring Boot 4.1.0 + Java 17 (já no repo). Spring Web, Spring Data JPA, Spring Security, Validation.
- **Banco:** PostgreSQL (padrão dos seus projetos).
- **Auth:** JWT (ou OAuth Google/social pra reduzir fricção de cadastro — combina com "descomplicado").
- **Mídia/portfólio:** começar com **embed** (YouTube/Vimeo) = zero custo de storage. Upload próprio (S3/Cloudflare R2) só se necessário.
- **Frontend:** a definir (provavelmente separado). Prioridade: mobile-first, porque o público vive no celular.
- **Deploy:** Railway/Render (padrão dos seus projetos).

---

## 6. Diferenciais — checagem de realidade
Seus diferenciais (simplificado, descomplicado, sem burocracia, life saver dos dois lados) são um **posicionamento forte e coerente**. Só um alerta já tratado: "sem burocracia" + "% por serviço" tem tensão (§3.1) — resolvido sequenciando a monetização (Destaque avulso antes de escrow). Mantidos os diferenciais, a estratégia respeita todos eles.

---

## 7. Escopo do MVP (proposta) — o mínimo pra ter liquidez

**Incluir:**
1. Cadastro/login (editor e cliente) — social login pra reduzir fricção.
2. Perfil de editor com **portfólio (embeds)**, nichos, preço, prazo.
3. **Vitrine/diretório** navegável com busca e filtro (nicho, preço, ferramenta).
4. Contato/chat básico entre cliente e editor.
5. Avaliações pós-serviço (reputação = combustível de confiança).

**NÃO incluir no MVP** (evitar over-engineering):
- Pagamento/escrow (Fase 3).
- Sistema de lances/propostas estilo Workana.
- App mobile nativo (web mobile-first basta).

---

## 8. ~~Segunda versão — Studio Session~~ (removido em 2026-08-04)

Era "mesmo motor, outra vertical: áudio em vez de vídeo". **Saiu do escopo da TramposEditor.**

Mix/master/beats vira um SaaS próprio — mesma finalidade, público diferente (mixing
engineers, beatmakers, produtores musicais), marca e domínio separados. A seção fica aqui
como registro porque o resto do documento referencia §8; não há mais nada a construir nela.

Consequência: a TramposEditor não é mais um guarda-chuva. O cuidado do §10.4 sobre a marca
ficar apertada quando áudio entrasse deixou de existir.

---

## 9. Roadmap por fases

| Fase | Foco | Entregável | Monetização |
|---|---|---|---|
| **0 — Fundação** (agora) | Estudo, mapeamento, decisões | Este documento + nicho definido | — |
| **1 — MVP** | Liquidez | Diretório + perfis + portfólio + chat + reviews. Semear 30–50 editores à mão | Grátis |
| **2 — Monetizar** | R$ 2k/mês | Destaque/Boost avulso; Google Ads ligado | Boost pay-per-use |
| **3 — Escalar** | Confiança + take-rate | Pagamento protegido (escrow opcional) | % como "seguro" |

---

## 10. Decisões travadas (2026-07-21)
1. ✅ **Nicho inicial:** **Short-form (Reels/TikTok/Shorts)**. Confiança rápida, ticket R$80–400, canal de marketing alinhado.
2. ✅ **Modelo de interação:** **Híbrido** (vitrine + "postar pedido rápido") — porém **sequenciado**, não simultâneo:
   - MVP = só **Diretório/vitrine**.
   - Fase 1.5 = adicionar **"postar pedido rápido"** leve (sem lances/leilão), só "cliente descreve → editores respondem no chat".
3. ✅ **Monetização:** **sequenciada** — Fase 1 grátis → Fase 2 Boost avulso (meta R$2k) → Fase 3 escrow como "pagamento protegido".
4. ✅ **Marca:** **TramposEditor** (definitivo). Cuidado que resta: garantir domínio/@ nas redes. O risco de guarda-chuva apertado morreu com a remoção do §8 — a marca é de vídeo e só.

### Ainda em aberto
- **Domínio:** registrar a URL (usuário fará).
- **VPS:** escolher entre KingHost e Hostinger (conferir specs/preço vigentes no site).

---

## 11. Arquitetura — execução em blocos (2026-07-22)

A construção foi quebrada em **blocos verticais**: cada um entrega algo que roda e é testável,
em ordem de dependência. Evita produzir esqueleto vazio que se reescreve na primeira feature real.

### 11.1. Os blocos

| # | Bloco | Entrega | Depende de | Status |
|---|---|---|---|---|
| 0 | **Fundação técnica** | pom, Docker, perfis, Flyway, erro global, health | — | ✅ feito |
| 1 | **Identidade & Acesso** | `UserEntity` + roles, registro/login JWT, `/me` | 0 | ✅ feito |
| 2 | **Taxonomia** | `Vertical` (VIDEO\|AUDIO), `EditorNiche`, `EditorTool` | 0 | ✅ feito |
| 3 | **Perfil de Editor + Portfólio** | `EditorProfile`, `PortfolioItem` (embed) | 1, 2 | ✅ feito |
| 4 | **Vitrine / Diretório** | busca, filtros, paginação — coração do MVP | 3 | ✅ feito |
| 5 | **Contato / Chat** | `Conversation`, `Message` | 1 | ⬜ |
| 6 | **Reviews** | `Review` + rating agregado | 3, 5 | ⬜ |

Fora do MVP: 7 = "postar pedido rápido" (Fase 1.5), 8 = Boost (Fase 2), 9 = Escrow (Fase 3).

### 11.2. Decisões técnicas travadas (2026-07-22)

1. ✅ **Banco: PostgreSQL.** SQLite descartado — filesystem efêmero em container apaga o
   arquivo, não há dialeto Hibernate oficial e o marketplace tem escrita concorrente.
   Supabase é Postgres gerenciado e serviria, mas o valor dele (Auth, RLS, SDK) não seria
   usado com JWT próprio no Spring — e traria pooler/IPv6/pausa do free tier de brinde.
   Na VPS, Postgres em container é mais simples e barato.
2. ✅ **Deploy: VPS (KingHost ou Hostinger)**, não PaaS. Daí `Dockerfile` + `docker-compose.prod.yml`,
   app publicada só em `127.0.0.1:8080` e Nginx fazendo TLS/proxy na frente. Porta do
   Postgres **nunca** exposta na internet.
3. ✅ **Java 21** (era 17). Habilita virtual threads (`spring.threads.virtual.enabled`), que
   casa com a carga de I/O bloqueante de um marketplace.
4. ✅ **Auth: JWT próprio (HS256)** via `oauth2-resource-server`, sem biblioteca extra.
   Social login (Google) fica para depois — o modelo de `UserEntity` não precisa mudar.
5. ✅ **Migrations: Flyway** com `ddl-auto: validate`. Schema versionado em SQL desde a
   primeira tabela; divergência entre migration e entidade quebra o teste, não o deploy.
6. ✅ **Frontend: React + Tailwind, mas via Next.js (App Router)** — não SPA/Vite.
   Motivo: o produto é um **diretório público** e vive de SEO long-tail ("editor de reels").
   SPA renderizada no cliente indexa mal justamente as páginas de perfil, que são o ativo
   de aquisição orgânica. Custo aceito: mais um runtime Node no deploy.
7. ✅ **Spring Boot 4.0.7**, não 4.1.0 (nem voltar pro 3.5).
   - **Não voltar pro 3.5:** é a última minor da linha 3.x — beco sem saída. Migraríamos pro 4
     em alguns meses de qualquer forma, com muito mais código em cima. E o medo de
     "ecossistema imaturo" não se confirmou: **springdoc 3.0.3 (Swagger/OpenAPI) é GA e
     compilado contra Boot 4.0.5** — a lib que mais provavelmente entra a seguir já está pronta.
   - **4.0.7 em vez de 4.1.0:** ambos saíram em 10/06/2026, mas o 4.1.0 é o primeiro da linha
     (zero patches) e o 4.0.7 tem 7 releases de correção atrás. A troca custou **uma linha
     no pom, zero mudança de código** — o 4.0.7 gerencia o mesmo Testcontainers 2.0.5, tem
     `spring-boot-flyway` e `starter-webmvc`. Num projeto cujo risco nº1 é liquidez e não
     tecnologia, noite perdida caçando bug de framework é desperdício puro.

### 11.3. Pegadinhas do Spring Boot 4.x encontradas na prática

Registradas porque custaram tempo e vão reaparecer nos próximos blocos:

- `spring-boot-starter-web` virou **`spring-boot-starter-webmvc`**; os test starters foram
  fatiados (`spring-boot-starter-webmvc-test`, `-data-jpa-test`).
- **Flyway não roda** só com `flyway-core` no classpath: a autoconfiguração mora em
  **`org.springframework.boot:spring-boot-flyway`**. Sem ela, falha silenciosa —
  o erro que aparece é `Schema validation: missing table`.
- **Jackson 3**: pacotes migraram de `com.fasterxml.jackson` para `tools.jackson`.
- `AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure`.
- **Testcontainers 2.0** (gerenciado pelo parent): artefatos renomeados para
  `testcontainers-postgresql` / `testcontainers-junit-jupiter`, `PostgreSQLContainer` mudou
  para `org.testcontainers.postgresql` e **deixou de ser genérico** (`PostgreSQLContainer`, sem `<?>`).
- `ServiceConnection` → `org.springframework.boot.testcontainers.service.connection`.
- `NimbusJwtEncoder` assume **RS256** por padrão: com chave HMAC é preciso passar
  `JwsHeader.with(MacAlgorithm.HS256)` explicitamente, senão dá
  `Failed to select a JWK signing key`.

### 11.4. Dívidas conhecidas do Bloco 1

- **Sem refresh token** — o access token dura 12h e acabou. Quando o frontend existir,
  decidir entre refresh token rotativo ou sessão mais longa.
- **Sem verificação de e-mail** e sem "esqueci minha senha".
- **Sem rate limiting** no `/v1/auth/login` — necessário antes de ir a público (brute force).
- **Sem revogação de token** (JWT stateless): desativar um usuário não invalida o token que
  ele já tem, só bloqueia novos logins.

### 11.5. Decisões e dívidas dos Blocos 2–4 (2026-07-26)

Diretório de editores entregue ponta a ponta (taxonomia + perfil/portfólio + vitrine pública).

**Decisões:**
- **Taxonomia como enum, não tabela** (`Vertical`, `EditorNiche`, `EditorTool`): lista muda
  pouco e evita seed/join extra no MVP. Trocar por tabela depois é migration simples.
  Nota (2026-08-04): `Vertical` existia pra plugar áudio sem reescrever o núcleo. Com o §8
  removido, ela ficou sem propósito — hoje é uma coluna com um valor só (`VIDEO`).
- **Slug imutável** gerado do `displayName` no cadastro do perfil (`joao-silva`). URL estável =
  SEO estável (o motivo do Next.js no §11.2.6). Colisão resolve com sufixo aleatório curto.
- **Vitrine 100% pública** (`GET /v1/editors/**` no `permitAll`); a gestão do perfil mora em
  base separada `/v1/me/editor-profile` pra não vazar rota de escrita no `permitAll` por engano.
- **Provider do portfólio inferido da URL** (YouTube/Vimeo), não enviado pelo editor — menos
  fricção. Embed = zero storage (§5.3).
- **Pegadinha Postgres:** parâmetro `null` sem tipo em `lower(:q)` vira `lower(bytea)` e explode
  mesmo com `:q is null` curto-circuitando. Resolvido com `cast(:q as string)` na JPQL.

**Dívidas conhecidas:**
- **Cover do card via lazy-load** (`toSummary` acessa o 1º item do portfólio dentro da
  transação): N+1 na listagem. Irrelevante nos ~50 editores do MVP; otimizar com projeção/fetch
  quando a vitrine crescer.
- **Rating sempre 0** até o Bloco 6 (reviews) existir — a coluna já está lá.
- **Ordenação fixa** (disponível > rating > recente); sem sort configurável pelo cliente ainda.

### 11.6. Frontend MVP — vitrine (2026-07-26)

> **Retrato do MVP em julho de 2026.** Desde então entraram as telas de
> autenticação e de gestão do perfil e o Dockerfile do Next. A API ainda
> é acessada pelo servidor Next via `API_URL`. O navegador carrega
> avatares públicos e segue redirecionamentos OAuth via `PUBLIC_API_URL`.
> O [guia do frontend](../frontend/README.md) descreve essas rotas.

Front iniciado em `frontend/` (app separado, como previsto no §5.3). Visual aprovado a partir
de mockup HTML estático antes de escrever React (referências do cliente: Dolphin Anty pro
minimalismo/violeta com dark de origem, 99freelas só pela lógica de card/filtro).

**Stack:** Next.js 16.2 (App Router, Turbopack) + React 19 + **Tailwind v4** (CSS-first, `@theme`
no `globals.css`, sem `tailwind.config`) + **fonte Inter** via `next/font` (self-hosted). TypeScript.

**Decisões:**
- **SSR de verdade:** as páginas são Server Components que buscam da API server-side. É o ponto
  do Next em vez de SPA — as páginas de perfil precisam ser indexáveis (SEO long-tail, §11.2.6).
- **`API_URL` é server-only** (não `NEXT_PUBLIC_`): o browser nunca fala direto com o backend;
  o Next renderiza o HTML. Default `http://localhost:8080`.
- **Filtros via URL:** os controles (client) empurram query params (`?niche=`, `?q=`) e o server
  re-renderiza. Estado compartilhável por link e amigável a SEO/back-forward.
- **Tema dark como origem:** default dark no `:root`, light opt-in via `[data-theme="light"]`,
  persistido em localStorage; script inline no `<head>` evita flash.
- **Thumbnails:** capa derivada da URL do YouTube (`i.ytimg.com/vi/{id}/hqdefault.jpg`), com
  `remotePatterns` no `next.config`. Vimeo e vídeos sem ID caem em gradiente de fallback.

**Pegadinhas do Next 16 (posterior ao cutoff — lidas em `node_modules/next/dist/docs/`):**
`params`/`searchParams` viraram **Promise** (acesso síncrono foi removido de vez, tem que
`await`); Turbopack é default em dev e build. O `frontend/AGENTS.md` avisa pra ler a doc bundled
antes de codar — vale seguir.

**Estado:** vitrine (`/`) e perfil (`/editores/[slug]`) prontos e validados ponta a ponta com
6 editores semeados via API (`scratchpad/seed.sh`). `next build` limpo. Falta: telas de auth
(login/cadastro), fluxo de "criar/editar meu perfil" no front, e Dockerfile do front pro deploy.

### 11.7. Identidade visual — logo aplicada (2026-07-27)

Logo definitiva (claquete) feita na LogoFaster, entregue em três artes 675×675 em `media/`:
`icon-square-format` (fundo chapado), `icon-circle-format` e `icon-with-shadow`. Cores da
própria arte: fundo `#2F1140`, miolo `#390E57`, traço branco — coerente com o violeta que o
front já usava (`--accent: #8b5cf6`), então **nenhum token de tema mudou**.

**Decisões:**
- **Re-enquadramento obrigatório.** Na arte original a claquete ocupa só ~48% da largura; a 32px
  (nav) ou 16px (favicon) isso vira borrão. Todos os assets derivam de um recorte quadrado
  centrado no bbox do glifo (medido: x 169–495, y 199–506), com `fill` de 0.66–0.72.
- **Gerador versionado**, não binário mágico: `frontend/scripts/gen-brand-assets.mjs`
  (usa o `sharp` que já vem com o Next). Rodar `node scripts/gen-brand-assets.mjs` refaz tudo.
  A arte fonte é a `icon-square-format` — o círculo sai de máscara, e o recorte cai todo dentro
  do disco, então não há canto transparente.
- **Convenções de arquivo do Next** em vez de `<link>` na mão: `src/app/icon.png` (512),
  `apple-icon.png` (180), `favicon.ico` (16/32/48) e `opengraph-image.png` /`twitter-image.png`
  (1200×630) com seus `.alt.txt`. O Next emite as tags sozinho.
- **Ícone da aba = arte circular** (`icon-circle-format`), decisão do usuário. Detalhe que engana:
  reenquadrar a arte circular **não** preserva o círculo — o recorte apertado cai todo dentro do
  disco e volta a ser um quadrado cheio, então a máscara redonda é redesenhada no tamanho final.
  O `fill` do ícone da aba é menor (0.62 contra 0.72 do quadrado) porque a claquete é diagonal e
  o disco corta os cantos do retângulo envolvente dela.
- **`apple-icon` segue quadrado e opaco** de propósito: o iOS aplica a máscara dele por cima e
  renderiza transparência como preto — um círculo ali viraria círculo dentro de quadrado preto.
- **Nav** usa `public/logo-mark.png` a 36px com `rounded-full` (4x de densidade), substituindo o
  triângulo de play provisório. `alt=""` porque o wordmark ao lado já nomeia o link.
- **`SITE_URL` nova env** (default `http://localhost:3000`) pro `metadataBase` — `og:image`
  exige URL absoluta e o domínio ainda não existe. Trocar no deploy.

**Pegadinhas:**
- O decoder de `.ico` do Next **exige PNG embutido em RGBA**. Com `flatten()` (que tira o alpha)
  o dev server derruba a página inteira com `Format error decoding Ico: The PNG is not in RGBA
  format!` — daí o `ensureAlpha().png({ palette: false })` só no caminho do `.ico`.
- `sharp` renderiza texto de SVG nesta máquina (via librsvg), o que permitiu gerar o card de
  compartilhamento estático. A fonte é a do sistema (Segoe UI), **não Inter** — se algum dia a
  fidelidade importar, trocar por `next/og` (`opengraph-image.tsx`).

### 11.8. Taxonomia por plataforma, auth no front e área logada (2026-07-27)

> **Retrato anterior ao cadastro por código.** Consulte o
> [fluxo atual em duas etapas](../README.md#cadastro-por-senha-em-duas-etapas).

**Nichos deixaram de ser formato e viraram plataforma.** `EditorNiche` era
`SHORTS | YOUTUBE | PODCAST`; agora é `TIKTOK | REELS | YOUTUBE | PODCAST`. Motivo: o cliente
chega procurando "editor de TikTok", não "editor de short-form" — e cada plataforma tem marca
própria, o que dá tag com ícone reconhecível. Migration `V3__niches_por_plataforma.sql` converte
`SHORTS` em duas linhas (REELS + TIKTOK, porque quem atendia short-form atendia as duas); a ordem
importa (inserir TIKTOK antes do UPDATE, senão o SELECT não acha mais nada). Feito agora porque
não há produção: com base real isso viraria migração de dados de verdade.

**Ícones das plataformas** (`media/enterprises/`) entram pelo mesmo gerador dos assets de marca,
normalizados num quadrado transparente de 64px (as artes vinham 4–5k quadradas e o YouTube
640×443). O chip de nicho ficou **neutro** de propósito: quem carrega cor é a marca, e o chip
violeta brigava com o vermelho do YouTube e o gradiente do Reels. Podcast não é plataforma —
cai num microfone desenhado.

**Auth no front** (§11.4 continua valendo pro backend):
- **Server Actions**, não route handlers: o formulário posta direto pra função no servidor
  (`src/app/actions/auth.ts`), sem hop extra e funcionando sem JS no cliente.
- **Sessão em cookie httpOnly** (`te_session`), `maxAge` = `expiresIn` do backend (12h).
  O browser nunca vê o JWT — combina com `API_URL` server-only (§11.6) e tira o token do XSS.
- **Quem é o usuário logado sai do backend**, não do JWT decodificado: `getCurrentUser()`
  chama `/v1/users/me` a cada render. Token expirado devolve `null` e a UI trata como deslogado.
- Ler a sessão no layout raiz **torna todas as rotas dinâmicas** — aceitável porque as páginas
  já eram `no-store` e o HTML pro SEO continua vindo do servidor.
- **Login com Google entra sem refatorar**: a action nova troca o código do provedor por um
  `AuthResponse` do nosso backend e chama o mesmo `createSession`.

**Landing vs. área logada:** `/` continua sendo a vitrine pública e indexável (é o ativo de
aquisição). O pós-login mora em `/painel`, com conteúdo por papel — é pra onde login e cadastro
redirecionam. A primeira versão mostra estado da conta, o perfil público do editor (ou o convite
pra criar) e o atalho da vitrine pro cliente; **o coração do SaaS ainda vai ser definido**.

**Novas telas:** `/entrar`, `/criar-conta` (papel via checkbox — uma pessoa pode ser editor e
cliente, §5.2), `/como-funciona` (dois públicos, âncora `#para-editores` usada pela nav) e
`/painel`. As telas de sessão levam `robots: { index: false }`.

**Dívida fechada de quebra:** o toggle de tema da nav chamava `setState` dentro de `useEffect`
(erro de lint que o `next build` não pega, porque não roda eslint). Agora lê o `data-theme` do
`<html>` via `useSyncExternalStore` — a fonte da verdade é o DOM que o script inline já escreveu.

### 11.9. Conta única e o coração do painel (2026-07-27)

> **Decisão do produto e retrato da interface naquela data.** O site ainda
> começa com `CLIENT` e a publicação do perfil acrescenta `EDITOR`.
> O contrato da API aceita um ou ambos os papéis no cadastro por senha.
> Veja [papéis e a confirmação do endereço](../README.md#cadastro-por-senha-em-duas-etapas).

**Decisão de produto do usuário: a conta é uma só.** Não existe "cadastro de editor" separado —
todo mundo entra podendo contratar, e **publicar o perfil de editor é o que registra a pessoa
como editor**. Consequências:
- `EditorProfileService.upsertMyProfile` **concede** `UserRole.EDITOR` em vez de exigi-lo (antes
  devolvia 403 pra cliente puro). O teste virou o oposto: `clienteViraEditorAoCriarOPerfil`.
- O cadastro do front não pergunta mais o papel; manda sempre `["CLIENT"]`.
- `/painel/meu-perfil` não tem trava de papel — a tela é justamente o caminho pra virar editor.
- O papel EDITOR no JWT antigo fica defasado até o próximo login, e **isso não importa**: nenhuma
  rota autoriza por esse papel; o que manda é ter perfil publicado (consultado no banco).

**Blocos 7 antecipado — "postar pedido rápido".** O usuário pediu as duas metades do painel
juntas (editor monta perfil, cliente posta o serviço), então a Fase 1.5 do §10.2 veio antes do
chat. Entregue com `JobRequestEntity` + `JobInterestEntity` (migration `V4`), fiel ao §5.1:
**sem lance e sem leilão** — o editor manda uma mensagem dizendo que topa, não uma proposta com
preço.

**A troca de contato é o aceite.** Como o chat (Bloco 5) não existe, era preciso resolver como as
pontas se falam sem virar raspagem de e-mail. Regra implementada e coberta por teste: o feed
**nunca** traz o e-mail do cliente; quando o cliente aceita um interesse, aí sim cada lado vê o
e-mail do outro (`JobInterestDTO.editorEmail` e `JobRequestFeedDTO.clientEmail` saem de null).
Quando o Bloco 5 chegar, o aceite passa a abrir uma conversa em vez de revelar e-mail.

**Outras regras** (todas testadas em `JobRequestTest`): feed exige login (pedido aberto não é
conteúdo público); só quem tem perfil publicado se candidata; ninguém se candidata ao próprio
pedido; um interesse por editor por pedido (constraint no banco); pedido fechado some do feed e
recusa candidatura; só o dono fecha/aceita.

**Rotas novas:** `/v1/me/job-requests` (POST, GET), `/v1/me/job-requests/{id}/close`,
`/v1/me/job-requests/{jobId}/interests/{interestId}/accept`, `/v1/me/job-interests`,
`/v1/job-requests` (feed, GET) e `/v1/job-requests/{id}/interests` (POST).
No front: `/painel/pedidos`, `/painel/pedidos/novo` e `/painel/trabalhos`.

**Pegadinha evitada:** o filtro opcional de plataforma no feed virou **dois métodos derivados**
(`findByStatus...` / `findByStatusAndNiche...`) em vez de um JPQL com parâmetro nulo — o custo
disso já está registrado no §11.5.

**Vitrine virou lista, não grade** (pedido do usuário, referência: lista de "próximos vídeos" do
YouTube): um editor embaixo do outro, capa à esquerda e informação à direita, **sem perder nada
do card** (disponibilidade e preço seguem sobre a capa; nome, nota, chamada, tags e prazo à
direita). Em telas estreitas empilha — capa em cima, texto embaixo.

### 11.10. Landing separada e estilo de edição (2026-07-27)

**A raiz virou landing; a vitrine mudou pra `/editores`.** A URL casa com a do perfil
(`/editores/{slug}`) e o SEO não perde nada — a lista continua SSR e indexável, com título e
canonical próprios. A landing usa a referência Kameleo pela **estrutura**, não pela cor: hero com
a palavra final se digitando, seção de dor (manchete + dores + caixa com a virada), benefícios em
linhas largas e grade 2×2 de ações. Sem ilustração: o hero mostra **editores reais da API** em
quadros 9:16, porque o que um marketplace precisa provar na primeira dobra é que tem oferta.

**Terceira dimensão de taxonomia: `EditorStyle`** (migration V5, tabela `editor_profile_styles`)
— DINAMICA, LEGENDADA, CLEAN, CINEMATOGRAFICA, MOTION_GRAPHICS, STORYTELLING, MEME e **DARK**
(canal sem rosto; o rótulo é "Dark (sem rosto)" pra não confundir com tema escuro). É ortogonal à
plataforma: um Reels pode ser meme ou cinematográfico, e o cliente costuma pedir pelo estilo.
Campo **opcional** no upsert — perfil criado antes da dimensão continua válido (tem teste).

**Decisão de escopo:** foi avaliada uma taxonomia de 11 categorias com ~80 sub-itens (vlog,
unboxing, casamento, imobiliária…) e **recusada**. Com ~50 editores, cada tag divide o mesmo
estoque e a maioria dos filtros voltaria vazia — que é exatamente o que faz marketplace novo
parecer morto (§2). O longo rabo fica na bio e é achado pela busca livre (`q`), virando tag só
quando houver editor que sustente a tag.

**Esteira de tags** na landing: CSS puro, sem JS — a faixa tem a lista duas vezes e a animação
desloca 50%, então o laço é invisível. A cópia repetida sai do fluxo de leitores de tela
(`aria-hidden` + `tabIndex={-1}`), a rolagem **pausa no hover** (chip que foge do cursor é
impossível de clicar) e `motion-reduce` desliga a animação. Todo chip leva a um filtro real
(`niche`, `style` ou `tool`) — nenhuma tag decorativa que abre a vitrine vazia.

**Ícones de ferramenta** (Premiere, After Effects, DaVinci, Final Cut, CapCut) entram pelo mesmo
gerador, agora lendo tudo de `media/enterprises/`.
