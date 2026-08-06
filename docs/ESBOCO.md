# TramposEditor — Esboço do Sistema Completo

> O mapa do todo: cada superfície que o produto vai ter, o que faz o sistema girar, e
> onde a estética, a ideologia e a cultura encostam no software.
> Complementa o `FUNDACAO.md` (estratégia e decisões) — aqui é a forma do sistema.
> Data: 2026-08-03 · Estado: Blocos 0–5 e 7 entregues; 6 em aberto.
> Pilares: **anatomia** (§1–2) e **circulação** (§3) fechados; **cultura** (§4–6) é do
> fundador e segue viva.

---

## 1. As quatro camadas

O sistema inteiro cabe em quatro camadas. Elas se distinguem por **quem entra** e
**o que a camada precisa provar**.

```
┌─ PÚBLICA ─────────────────────── prova que existe oferta ──┐
│  /                     landing                              │
│  /para-clientes        porta de quem contrata               │
│  /para-editores        porta de quem edita                  │
│  /editores             vitrine (filtro, ordenação)          │
│  /editores/{slug}      perfil ← ATIVO DE SEO                │
└─────────────────────────────────────────────────────────────┘
                            ↓ converte
┌─ ENTRADA ─────────────────────── tira o atrito do cadastro ┐
│  /criar-conta  /entrar                                      │
└─────────────────────────────────────────────────────────────┘
                            ↓ vira conta
┌─ LOGADA (o coração) ──────────── faz as pontas se acharem ─┐
│  /painel              →     307 pro feed (hub dissolvido)   │
│  /painel/meu-perfil         editor monta sua vitrine        │
│  /painel/pedidos            cliente posta e recebe          │
│  /painel/feed         ✅    FEED, classe `trampos`          │
│  (classe `editores` do feed → /editores, na camada pública) │
│  /painel/conversas    ✅    a caixa de entrada              │
│  /painel/trampos-fechados ⬜    histórico + review (falta)  │
└─────────────────────────────────────────────────────────────┘
                            ↓ roda em cima de
┌─ MOTOR ───────────────────────── guarda e faz cumprir ─────┐
│  User · EditorProfile · PortfolioItem                       │
│  JobRequest · JobInterest                                   │
│  Conversation · Message ✅   Deal ⬜   Review ⬜             │
│  Notification ⬜   Boost (F2)   Escrow (F3)                 │
└─────────────────────────────────────────────────────────────┘
```

### ~~Os dois feeds são um par~~ — a simetria era falsa (corrigido em 2026-08-04)

Este bloco dizia que a camada logada tinha **dois feeds simétricos**, um por lado do
mercado: `/painel/trampos` pra demanda e `/painel/editores` pra oferta. **Só um dos dois
se sustentava**, e o feed da oferta foi removido.

| Feed | Rota | Tem equivalente público? | Sobreviveu? |
|---|---|---|---|
| **Demanda** | `/painel/trampos` | não — não existe lista pública de trampos | ✅ superfície legítima |
| **Oferta** | ~~`/painel/editores`~~ | sim — é a vitrine | ❌ era a vitrine com um filtro a menos |

A diferença que este documento alegava (o feed mostra só quem está **disponível**) é
**valor de filtro, não superfície**: `/v1/editors` sempre aceitou `available`. O resto da
justificativa — "recorte só por categoria" — descrevia uma limitação, não um propósito.

A prova estava no próprio código: a página do feed linkava pra vitrine **duas vezes**,
dizendo que lá havia mais filtro. Tela que recomenda outra como versão melhor de si mesma
não é tela distinta.

Hoje "quem está livre agora" é um toggle da vitrine (`/editores?available=true`), e
`/painel/editores` responde 308 pra lá. O `FeedController` foi deletado.

**O que a correção não muda:** o feed da demanda continua de pé, e continua sendo o que
faz a plataforma parecer mercado em vez de catálogo. O erro foi construir um par onde só
havia uma peça — e a lição vale pra frente: antes de criar superfície nova, checar se ela
não é recorte de uma que já existe.

### O feed unificou, e a chave não recriou a duplicata (2026-08-06)

A remoção acima resolveu a duplicação e criou um problema novo: sobraram **duas listas sem
relação declarada**. Quem estava na vitrine não descobria que existiam trampos abertos, e
quem estava no feed não tinha como ir ver quem edita. As duas pontas do mercado estavam no
produto e não se anunciavam uma pra outra.

Agora é **um feed com duas classes** e uma chave visível (`FeedSwitch`): *ver trampos* e
*ver editores*. A pergunta óbvia é se isso não ressuscita o `/painel/editores` que o bloco
acima matou — e a resposta é não, por causa de onde cada classe mora:

| Classe | Rota | Acesso | Por quê |
|---|---|---|:---|
| `editores` | `/editores` | **público** | é a vitrine, o ativo de SEO — e continua sendo a única lista de editores que existe |
| `trampos` | `/painel/feed` | **logado** | pedido aberto não é conteúdo público (ver abaixo) |

A regra que faz a diferença: **a chave navega, não duplica.** Clicar em "ver editores"
leva pra `/editores`; não existe uma segunda lista de editores renderizada dentro do
painel. Foi exatamente essa segunda lista que caiu em 2026-08-04, e a lição do bloco
anterior teria sido desperdiçada se a chave a trouxesse de volta atrás de um parâmetro.

**Por que trampos exige login e editores não.** Não é simetria quebrada por acaso, é a
assimetria real dos dois lados: o perfil do editor é publicado *pra* ser achado, e o
pedido do cliente é um briefing com orçamento e prazo — publicá-lo abriria uma lista de
demanda pronta pra raspagem e pra abordagem fora da plataforma. O `JobRequestController`
já recusava sem token desde que nasceu; a chave não afrouxou isso. Efeito colateral útil:
"ver trampos" deslogado cai no `/entrar`, o que torna a chave uma porta de conversão de
editor — o lado da oferta é o que mais custa captar.

**Trocar de classe zera filtro e página.** As dimensões quase não se sobrepõem — trampo se
filtra por nicho e orçamento, editor por ferramenta, estilo, preço e disponibilidade — e
carregar o recorte de um lado pro outro entregaria lista vazia sem a pessoa entender por
quê. `/painel/trampos` responde 308 pra `/painel/feed`, e `/painel/editores` passou a
apontar pra `/editores` sem o `?available=true`: com a chave, "ver editores" virou gesto de
navegação, e chegar já filtrado escondia parte da vitrine de quem só queria ver quem existe.

### Por que a camada pública é grande e a logada é pequena

Não é acidente e não deve ser "corrigido": o produto vive de **SEO long-tail**
(`FUNDACAO` §11.2.6). A camada pública é o funil de aquisição inteiro e precisa ser
indexável. A camada logada não é indexada de propósito (`robots: { index: false }`) —
ela não capta ninguém, ela **resolve** quem já foi captado.

O erro clássico seria inflar a camada logada com dashboards. O coração não é mais telas:
é **uma tela onde as duas pontas se falam**.

### Feed não é dashboard

Dashboard é tela que **descreve** o que já aconteceu — gráfico, contador, resumo. Feed é
tela que **oferece o próximo passo**: aqui está o que tem hoje, escolha. O que infla a
camada logada é o primeiro tipo; o segundo é circulação, que é justamente o que o §3
diagnostica como faltando.

> A tabela que antes defendia aqui a existência do feed de editores caiu junto com ele —
> ver o bloco acima. O argumento que ela usava ("a vitrine é pra decidir, o feed é pra
> voltar") não sobrevivia ao fato de o feed ter **menos** filtro que a vitrine: quem volta
> também escolhe.

A "disponibilidade honesta" do §6 continua tendo dente, e agora num lugar melhor: marcar-se
ocupado tira o editor do recorte `available=true`, que é o padrão de quem procura alguém
pra hoje, mas nunca da vitrine. O gesto custa visibilidade do dia, não a existência do
perfil.

### A vitrine é a única lista de editores

Consequência direta da remoção acima, e vale fixar pra não renascer: **existe uma lista de
editores só.** Toda pergunta sobre editor — quem é o certo, quem está livre, quem usa
CapCut, quem cobra até R$ 250 — é um recorte de `/editores`, com controle visível na
própria tela.

Isso obrigou a corrigir um segundo problema que estava escondido atrás do feed: `tool`,
`style`, `available` e `maxPriceCents` eram aceitos por `/v1/editors` e **não tinham
controle nenhum na interface**. A vitrine parecia mais pobre do que era, e foi isso que fez
o feed parecer necessário. Hoje ferramenta aparece como círculo de marca (a leitura é pelo
logo, como nos marketplaces grandes), estilo e preço como chips, e disponibilidade como
toggle.

---

## 2. Inventário de superfícies

| Superfície | Existe | Público | Papel | Fase |
|---|:---:|---|---|---|
| `/` landing | ✅ | ambos | define, prova oferta, roteia | MVP |
| `/para-clientes` | ✅ | cliente | converte quem quer contratar | MVP |
| `/para-editores` | ✅ | editor | converte quem quer trabalhar | MVP |
| `/editores` vitrine | ✅ | cliente | busca, filtro, comparação | MVP |
| `/editores/{slug}` perfil | ✅ | cliente | **decisão de contratar** | MVP |
| `/criar-conta` `/entrar` | ✅ | ambos | conta única | MVP |
| `/esqueci-senha` `/redefinir-senha` | ✅ | ambos | conta perdida = usuário perdido | MVP |
| login com Google e GitHub | ✅ | ambos | tira o atrito do cadastro; vincula por e-mail | MVP |
| verificar e-mail | ✅ | ambos | e-mail confiável — pré-requisito da notificação | 1.5 |
| cadastro confirmado por código | ✅ | ambos | conta só nasce com o código de 6 dígitos devolvido (V18) — barra bot | 1.5 |
| `/painel` | ✅ | ambos | hub por papel | MVP |
| `/painel/meu-perfil` | ✅ | editor | virar editor, editar vitrine | MVP |
| `/painel/pedidos` | ✅ | cliente | postar, ver interessados, aceitar | 1.5 |
| `/painel/feed` feed, classe `trampos` | ✅ | editor | categoria, paginado, candidatar; era `/painel/trampos` | **MVP** |
| ~~`/painel/editores`~~ feed da oferta | ❌ | — | removido: era a vitrine com menos filtro (§1) | — |
| chave do feed (trampos ↔ editores) | ✅ | ambos | liga as duas pontas sem duplicar lista (§1) | MVP |
| `/painel/conversas` | ✅ | ambos | onde a relação acontece | MVP |
| `/painel/trampos-fechados` | ✅ | ambos | histórico, avaliação e **chamar de novo** (§3.2) | MVP |
| notificação por e-mail | 🟡 | ambos | faz o assíncrono funcionar — código pronto, falta config de deploy (§7) | MVP |
| páginas de tag (`/editores/tiktok`) | ✅ | cliente | SEO long-tail dedicado — 9 tags, indexa só com estoque (§2.3) | 2 |
| destaque / boost | ⬜ | editor | monetização pay-per-use | 2 |
| pagamento protegido | ⬜ | ambos | take-rate como seguro | 3 |
| `/admin` moderação | ⬜ | admin | sustenta a promessa de qualidade — ver §2.2 | 2 |

### 2.1. A ordenação da vitrine, escrita

O §4 lista a ordenação como ponto de contato de cultura e a chama de "implícita". Ela não
é implícita — ela existe, é uma linha de JPQL no `IEditorProfileRepository`, e ninguém
que lê este documento saberia disso. Escrever aqui é o que a torna uma decisão em vez de
um efeito colateral:

```
order by e.available desc, e.ratingAvg desc, e.ratingCount desc, e.createdAt desc
```

Lida em português: **primeiro quem está livre, depois quem é bem avaliado, depois quem
tem mais avaliações, e por último quem chegou primeiro.** Três coisas decorrem disso:

- ela já premia comportamento, não pagamento — é o §6 ("a ordenação é uma declaração")
  cumprido antes de ser dito;
- `ratingAvg` ainda é sempre `0.0`, então hoje o segundo e o terceiro critério não fazem
  nada e o desempate real é `createdAt` — ou seja, **a vitrine hoje ordena por antiguidade**;
- é aqui que o Boost da Fase 2 vai bater (§6, a tensão). A regra que estiver escrita neste
  bloco é a que ele vai ter que furar ou respeitar.

### 2.2. Moderação: quem tira alguém da vitrine

Era uma linha solta de fase 2. Para um produto cuja tese é "aqui não é commodity", quem
sai e por quê é anatomia, não detalhe — a promessa de qualidade sem mecanismo é só texto.

O mínimo que precisa existir antes de virar problema:

| Peça | O que faz | Por quê |
|---|---|---|
| **Denúncia** | qualquer conta reporta perfil, portfólio ou mensagem | sem entrada, moderação depende de sorte |
| **Estado do perfil** | `PUBLISHED` / `SUSPENDED` em vez de só existir | suspender não pode ser apagar — o histórico fica |
| **Portfólio roubado** | o caso concreto mais provável aqui | é vídeo de terceiro; a defesa do editor honesto é a plataforma agir |
| **Registro da decisão** | quem suspendeu, quando, por quê | a recusa do §4.2 só vale se ela for auditável |

Isso continua Fase 2 — mas fica nomeado, e não é mais um traço na tabela.

### 2.3. Páginas de tag: por que não é a vitrine de novo (2026-08-04)

`/editores?niche=TIKTOK` e `/editores/tiktok` mostram a mesma lista, e a diferença não é
cosmética: **querystring não rankeia.** Sem URL, título, H1 e texto próprios não há o que
o Google indexe. Vale o mesmo par do §1 um nível abaixo — o filtro serve quem já está
dentro, a tag page serve quem ainda está no Google. Uma retém, a outra capta.

São nove: quatro de nicho (`tiktok`, `reels`, `youtube`, `podcast`) e cinco de ferramenta
(`premiere`, `capcut`, `after-effects`, `davinci-resolve`, `final-cut-pro`). Estilo ficou
de fora: é a dimensão com menos estoque e a de menor intenção de compra na busca.

Três decisões que valem ficar escritas:

- **Indexação por estoque.** Página de tag magra não rankeia, parece produto morto e, uma
  vez indexada, não sai mais. Abaixo de 3 editores ela existe e é navegável, mas sai com
  `noindex`. Hoje isso indexa 5 e segura 4 — o limiar mora em `MIN_EDITORES_INDEXAVEL`.
- **Slug reservado.** Perfil e tag dividem `/editores/{slug}`, então um editor chamado
  "CapCut" tomaria a página da ferramenta. `EditorProfileService.RESERVED_SLUGS` recusa, e
  a lista espelha `frontend/src/lib/tags.ts`.
- **Texto à mão, um por tag.** Nove páginas com o mesmo parágrafo trocando uma palavra é
  *doorway page*: o Google pune. Os textos atuais são rascunho e esperam a voz do fundador
  (§9) — é trabalho de cultura, não de código.

Entrou junto o que faltava pra isso funcionar: `sitemap.xml` (que exclui as tags sob
`noindex`, pra não pedir indexação do que manda não indexar) e `robots.txt`.

---

## 3. Os quatro laços que fazem o sistema girar

Um marketplace não é um conjunto de telas, é um conjunto de **ciclos**. Se um ciclo não
fecha, o produto vaza pelo buraco.

**Laço de aquisição** (o desconhecido vira conta)
`busca no Google → perfil indexado → vitrine → perfil → conversa → conta`

**Laço da oferta** (o editor volta porque ganha)
`publica perfil → aparece → recebe contato → entrega → recebe nota → sobe na ordenação → aparece mais`

**Laço da demanda** (o cliente volta porque resolveu)
`posta trampo ou acha no feed → escolhe → deu certo → volta / indica`

**Laço de reputação** (o combustível de confiança)
`trampo fechado → avaliação → nota → ordenação → visibilidade`

### 3.1. O que faz cada laço dar a volta

Nomear o ciclo não faz ele girar. Cada laço precisa de duas coisas que este documento não
tinha: um **gatilho de retorno** — o que traz a pessoa de volta sem ela ter decidido
voltar — e um **evento que prova que fechou**, senão "o marketplace funciona" é opinião,
não fato verificável.

| Laço | Gatilho de retorno | Prova de que fechou | Existe? |
|---|---|---|:---:|
| **Aquisição** | não precisa — é de mão única, o Google é o gatilho | uma visita orgânica virou conta | ⬜ nada registra a origem do cadastro |
| **Oferta** | e-mail: "procuraram você", "seu interesse foi aceito" | o editor voltou e respondeu | ✅ |
| **Demanda** | e-mail no fecho + "chamar de novo" no histórico | o mesmo cliente publicou um segundo trampo | ✅ o gatilho existe; falta medir |
| **Reputação** | e-mail pedindo a avaliação quando o trampo fecha | nota preenchida e ordenação mexeu | ✅ |

### 3.2. O buraco que não estava mapeado: o cliente não tem por que voltar

Os laços da oferta e da reputação estão só esperando peças já previstas — notificação e
avaliação. O **laço da demanda não está esperando nada**, porque nada no plano faz um
cliente satisfeito voltar.

O desenho hoje termina no aceite. Depois disso o produto não tem evento de "acabou": não
existe `Deal`, então ninguém sabe que o trampo terminou, e sem esse marco não há como
pedir avaliação, não há como dizer "quer publicar outro?", e não há como contar quantos
clientes voltaram. O laço não vaza — ele nem começa a dar a volta.

Isso reordena o §7 sem inventar item novo: **o `Deal` do Bloco 6 não é só a base da
avaliação, é o gatilho do laço da demanda.** É o mesmo trabalho servindo dois laços, o
que o torna a peça de maior alavancagem que sobrou.

Três gatilhos possíveis, do mais barato ao mais caro, para decidir junto com o Bloco 6:

1. **E-mail quando o trampo fecha** — pede a nota e oferece publicar outro. Reusa a mesma
   infra de notificação do item 3 do §7; custo quase zero se ela existir.
2. **Histórico com repetição** — `/painel/trampos-fechados` mostra com quem já deu certo e
   deixa chamar de novo em um clique. É a repetição de fornecedor, que é o que mais barato
   traz cliente de volta num marketplace de serviço.
3. **Reconvite por inatividade** — cliente que não posta há N semanas recebe o que há de
   novo na categoria dele. É o mais caro e o mais fácil de virar spam; só depois dos dois
   primeiros, e coerente com a recusa do §4.2 (nada de pressão artificial).

### O diagnóstico em uma frase

> Os quatro laços passam por **conversa** e **avaliação** — que eram exatamente as duas
> peças que não existiam. O sistema tinha entrada e saída, mas não tinha circulação.

É por isso que "faltam as janelas do pós-login" está certo em espírito: o que falta não
é *mais* painel, é o pedaço que faz o ciclo voltar ao começo.

**Onde o diagnóstico está hoje (2026-08-03).** A conversa existe (Bloco 5) e os dois
feeds existem — então as duas pontas já se acham e já se falam. O que sobra tem um nome
só: **o ciclo não tem fim registrado nem voz fora da aba.**

- **notificação** (§7 item 3) — sem ela o que foi construído gira só enquanto a pessoa
  está com a aba aberta. É o furo mais barato de tapar e o que mais custa deixar aberto;
- **`Deal` + avaliação** (§7 item 5) — sem o marco de "acabou" nada fecha: nem o laço de
  reputação (§3.1) nem o da demanda (§3.2).

Os dois se cruzam: a notificação é o **canal**, o `Deal` é o **motivo** de usá-lo. Um sem
o outro entrega metade do ciclo.

**Atualização do fim do dia.** Os dois saíram, junto do resto do §7. Com isso os quatro
laços têm as peças, e o furo que sobra **mudou de natureza: não é mecanismo, é medição.**
Nenhum laço registra que deu a volta — não se sabe de onde veio um cadastro, quantos
clientes voltaram, nem se a ordenação mexeu depois da primeira nota. O produto passou a
girar sem instrumento, o que é bem melhor que não girar, mas deixa a próxima pergunta
("está funcionando?") sem resposta em lugar nenhum.

Isso é trabalho de outra natureza — evento, origem de cadastro, contagem — e por isso não
entrou no §7: aquela lista é do que faz o sistema andar, não do que o observa.

**Quando fazer (decisão de 2026-08-04): depois da aquisição do domínio.**

A ordem importa porque quase tudo aqui já é derivável do banco e pode esperar — método de
cadastro sai de `user_identities`, cliente que voltou sai de `job_requests`, e o efeito da
nota na ordenação sai de `reviews` + `editor_profiles.rating_avg`. Nada disso se perde.

O que **não** é derivável é a origem externa do cadastro (referrer e `utm_*`): ela só existe
no instante da primeira visita e some se ninguém gravar. Daí o risco aparente de adiar — e
daí também por que adiar é seguro neste caso: **sem domínio não há tráfego real**, então não
há cadastro de origem verdadeira pra perder. O que se perderia seria a origem de acessos de
teste, que não interessa a ninguém.

A janela que não pode ser furada é outra: a captura precisa existir **antes de o domínio ir
ao ar**, não antes de existir domínio. Se o site abrir ao público sem ela, aí sim os
primeiros cadastros — os únicos que provam se o SEO funciona — entram cegos e não voltam.

Escopo previsto, quando chegar a hora: uma migration adicionando `signup_referrer`,
`signup_utm_source` e `signup_landing` em `users`, captura no cadastro por senha e no
callback social (via cookie de primeira visita, já que o referrer se perde na ida ao
provedor), e um arquivo de queries versionado. **Sem tela** — dashboard na camada logada é o
erro que o §1 nomeia.

---

## 4. Onde a cultura encosta no software

Esta é a seção que o resto do documento serve. **Cultura não é uma página "sobre nós"** —
ela vive em lugares específicos do produto. Estes são os pontos de contato, do mais forte
pro mais fraco:

| # | Superfície de cultura | Onde fica | Estado |
|---|---|---|---|
| 1 | **O léxico** — como as coisas se chamam | tudo | 🟡 nascendo |
| 2 | **A recusa** — o que o produto se nega a fazer | FAQ, landing | 🟢 forte |
| 3 | **O ritual de entrada** — virar editor | `/painel/meu-perfil` | 🔴 é um formulário |
| 4 | **A ordenação da vitrine** — quem aparece primeiro | `/editores`, escrita no §2.1 | 🟢 explícita, mas inerte sem nota |
| 5 | **Status visível** — selo, nível, veterania | card e perfil | 🔴 não existe |
| 6 | **Estados vazios** — o que se diz quando não há nada | todas | 🟡 alguns bons |
| 7 | **A voz fora do site** — e-mail, notificação | ⬜ | 🔴 não existe |
| 8 | **Erro e limite** — 404, sem resultado, sem permissão | `not-found`, `error` | 🟡 genéricos |

### 4.1. O léxico já começou sozinho

"Trampo" entrou na interface ("Você edita? Pegue um trampo"), "vitrine" substituiu
"editores" na nav, "trampos abertos" em vez de "vagas". Isso **é** cultura, e é o
vetor mais barato que existe: nenhuma linha de layout muda.

Vale fixar um glossário curto e obedecê-lo em todo lugar — inclusive nos e-mails e nos
erros. Palavra que oscila (às vezes "pedido", às vezes "projeto", às vezes "job") lê como
produto montado por comitê.

### 4.2. A recusa é o seu ativo cultural mais forte

O que o TramposEditor **se nega a fazer** já está escrito e é coerente:

- não tem leilão de proposta;
- não tem comissão sobre o trabalho;
- não tem assinatura pra existir na plataforma;
- não tem pedágio pra conversar;
- não promete o que ainda não construiu (o texto sobre pagamento protegido admite que
  ele não existe).

Ideologia aparece mais no que se recusa do que no que se oferece. Este é o material
bruto da cultura — e ele já está pronto, só não está nomeado num lugar só.

### 4.3. O ritual de entrada é o buraco

Hoje virar editor é preencher um formulário e clicar em salvar. Funciona, mas é o
momento de maior carga emocional do produto inteiro — a pessoa está dizendo *"eu sou
editor"* — e a interface trata isso como cadastro de CEP.

É aqui que o sentimento de pertencer nasce ou não nasce. Sem tocar no backend, esse
momento pode virar um rito: o que se diz antes, a confirmação, o que a pessoa vê no
segundo seguinte, o primeiro e-mail que recebe.

---

## 5. A ideologia que já está escrita no código

Isto não é proposta — é o que dá pra **extrair** do que você já construiu. Cinco
princípios, todos com evidência na interface:

1. **O trabalho fala, não o discurso.** Portfólio no lugar de currículo, capa de vídeo
   no lugar de foto de perfil. *("Nada de currículo: o corte fala por ela.")*
2. **Preço e prazo à mostra.** O oposto do "solicite um orçamento". A pessoa decide
   sabendo, antes de falar com alguém.
3. **Ninguém no meio.** A plataforma apresenta e sai. Não intermedeia pagamento, não
   cobra percentual, não retém contato.
4. **Sem leilão.** Editor não disputa trabalho derrubando preço. *Este é o mais forte* —
   é o que separa o produto do Fiverr/Workana e é o que dá dignidade ao ofício.
5. **Honestidade sobre o que não existe.** O produto admite lacunas em vez de prometer.

O item 4 é a semente do "1%". A razão pela qual um editor sentiria orgulho de pertencer
não é um selo bonito: é **"aqui eu não sou commodity leiloada, sou escolhido pelo
trabalho"**. Isso já é verdade na arquitetura — nunca foi dito em voz alta.

---

## 6. "Ser 1%" — o que é e o que não é

> **Definição do fundador (2026-08-02):** ser 1% não é sobre restringir quantidade de
> usuários nem sobre acesso fechado. É **ser diferente da manada, ser único, fora da
> curva, a melhor versão de si mesmo — fazer o que os 99% não fazem.**

Isso muda a natureza do conceito: **1% é um ethos, não um andar de prédio.** Não é
quem passa na porta; é quem a pessoa decide ser depois de entrar.

### A consequência imediata: não existe tensão com a porta aberta

A decisão do `FUNDACAO` §11.9 — conta única, publicar o perfil já registra como editor,
sem aprovação — **não conflita com o 1%.** Ao contrário: a porta aberta é *necessária*.
Um ethos de superação exige que qualquer pessoa possa entrar; o que ele cobra vem depois
da entrada, não antes.

| Não é | É |
|---|---|
| filtrar quem entra | **mostrar o que separa** |
| selo de elite concedido | **padrão de conduta visível** |
| escassez | **exigência** |
| status sobre os outros | **superação de si** |

### O ethos vale para os dois lados — e é uma cultura só

Aqui cai uma suposição anterior deste documento, que dizia que editor e cliente
precisariam sentir coisas diferentes. Com o significado correto, não precisam: o mesmo
padrão se aplica aos dois, e é isso que torna a cultura coerente em vez de segmentada.

| Quem | O que é fazer o que os 99% não fazem |
|---|---|
| **Editor** | portfólio real em vez de promessa; disponibilidade honesta; responder rápido; entregar no prazo; **não rebaixar o próprio preço pra ganhar no grito** |
| **Cliente** | briefar direito em vez de "faz aí bonito"; respeitar o preço publicado; pagar em dia; dar retorno |

### A plataforma também tem que ser 1% — senão a mensagem é oca

Esta é a parte mais exigente. Um produto que prega "faça o que os 99% não fazem" e se
comporta como os 99% dos marketplaces — comissão sobre o trabalho alheio, leilão de
proposta, assinatura pra existir, promessa do que não entrega — **mata a própria
mensagem no primeiro contato.**

Com isso, a **recusa** do §4.2 deixa de ser posicionamento comercial e vira coerência:

| O que os 99% dos marketplaces fazem | O que aqui não se faz |
|---|---|
| cobram % do trabalho fechado | nenhuma comissão |
| leilão de propostas, corrida pro fundo | sem lance |
| assinatura pra aparecer | sem mensalidade |
| prometem garantia que não têm | admite o que ainda não existe |

Isso já está no produto e é o ativo mais forte que existe. Só faltava perceber que a
recusa **é** o 1% aplicado à própria plataforma.

### Onde o ethos vira software

1. **O anti-leilão já é isso**, escrito na arquitetura desde o §5.1 do `FUNDACAO`. É a
   prova de que o conceito não é slogan: o produto foi construído sem o mecanismo que
   rebaixa o ofício.
2. **Disponibilidade honesta.** Marcar-se como ocupado quando a agenda encheu custa
   visibilidade no curto prazo — e é exatamente o gesto que os 99% não fazem. A vitrine
   deve premiar isso, não punir.
3. **A ordenação é uma declaração.** Se ela premia comportamento (entrega, prazo,
   honestidade de agenda), o ethos é real. Se premia quem pagou, é publicidade.
4. **O ritual de entrada** (§4.3) ganha outro peso: publicar o perfil deixa de ser
   cadastro e passa a ser **assumir um padrão**. É o momento exato de dizer o que se
   espera de quem está entrando.
5. **Avaliação continua sendo infraestrutura de cultura** — não pra criar hierarquia de
   status, mas porque sem ela não há como *mostrar* quem está fazendo o que os outros
   não fazem.

### ⚠️ A tensão que aparece agora: o Boost da Fase 2

Com o 1% definido como ethos, o **destaque pago** (`FUNDACAO` §3.3, Fase 2) entra em
conflito direto: vender posição é deixar alguém **comprar** a aparência de destaque em
vez de merecê-la — que é literalmente o que os 99% fazem.

Não é fatal, mas exige desenho consciente. Três saídas, em ordem de fidelidade ao ethos:

1. **Boost com piso de conduta:** só pode destacar quem já cumpre o padrão (entregas,
   prazo, avaliação). Dinheiro amplifica quem merece, não substitui o mérito.
2. **Boost dentro do recorte, não acima dele:** o pago rotaciona posição entre os que já
   estão bem colocados, sem furar fila de quem tem melhor histórico.
3. **Assumir a tensão e dizer em voz alta** que o destaque é anúncio — coerente com a
   honestidade do §5.5, mas gasta um pouco do capital cultural.

Decidir isso antes de construir a Fase 2, não depois.

---

## 6.1. A estética faz parte do produto

O fundador registrou que a estética atual **é parte do SaaS**, não enfeite. Os canais
usados como referência na vitrine não são conteúdo aleatório de teste — são o mundo do
qual o produto fala: disciplina, superação, não-mediano, alta intensidade.

Isso dá direção concreta ao visual, e o que já existe está alinhado:

- **Dark como origem**, não como opção — já é a decisão do `FUNDACAO` §11.6.
- **Alto contraste e peso tipográfico forte** (extrabold, tracking fechado) — o oposto do
  SaaS pastel e arredondado que todo mundo faz.
- **Mono para rótulo e dado** — leitura de painel, de instrumento, não de brochura.
- **Sem firula:** nada de ilustração genérica; o hero mostra editores reais.
- **O léxico** (§4.1) é parte da estética tanto quanto a cor: "trampo" faz um trabalho
  que "projeto" e "job" não fazem.

> Regra prática pra qualquer decisão visual daqui pra frente: **se a escolha pareceria
> natural num SaaS qualquer, ela não é 1%.**

---

## 7. Consequência prática: o que construir, em ordem

Deriva direto dos laços do §3 — não é lista de desejos.

| # | O quê | Por que agora | Bloco | Estado |
|---|---|---|---|:---:|
| 1 | **Conversa, com duas portas de entrada** | fecha o `href="#"` do perfil e unifica os dois fluxos numa caixa de entrada só | 5 | ✅ |
| 2 | **Os dois feeds, com categoria e paginação** | os dois lados precisam de uma tela de "o que tem hoje" — e o feed sem limite não escala | 5.5 | ✅ |
| 3 | **Notificação por e-mail** | sem ela os itens 1 e 2 não giram: ninguém sabe que foi procurado. É o **canal** | novo | 🟡 |
| 4 | **`Deal` + avaliação + histórico** | o marco de "acabou": fecha reputação (§3.1) **e** demanda (§3.2). É o **motivo** | 6 | ✅ |
| 5 | **Esconder a nota até existir review** | hoje todo card anuncia "★ 0.0", que faz a vitrine parecer morta | — | ✅ |
| 6 | **Esqueci a senha** | conta perdida é usuário perdido, e não custa quase nada | novo | ✅ |
| 7 | N+1 da vitrine | escala, não MVP — mas dói primeiro na página indexada | — | ✅ |
| 8 | Login com Google e GitHub | tira o atrito do cadastro; era Fase 2 e subiu a pedido do fundador | — | ✅ |

Os itens 1 a 4 separam "as peças existem" de "o marketplace funciona". **3 e 4 são um par**:
o canal sem o motivo manda e-mail de nada; o motivo sem o canal fica esperando a aba abrir.

### Item 3 — o software acabou; o que falta é do dia do deploy (2026-08-06)

O domínio foi adquirido (`.com` e `.com.br`) e a dívida de código foi paga: os seis e-mails
existem e têm call site real (mensagem, interesse recebido, interesse aceito, trampo fechado,
confirmar e-mail, redefinir senha), saem em texto e HTML no mesmo envio, e a **verificação de
endereço** do §2 — que estava represada atrás da compra — entrou junto. Aviso automático agora
só sai pra endereço provado (`NotificationService.quietly`), o que protege a entregabilidade
do domínio antes de existir tráfego pra perder.

O que sobrou **não é feature, é configuração**, e por isso fica parado aqui até o deploy:

1. **`docker-compose.prod.yml` não passa nenhuma variável de e-mail pro container.** Só passa
   DB, JWT e CORS. Como `application-prod.yaml` liga `transport: smtp` por default, o app
   sobe mandando por SMTP com `MAIL_HOST` vazio — **todo e-mail falha em silêncio**, porque o
   `SmtpEmailSender` engole a exceção de propósito (a ação do usuário nunca cai por e-mail).
   Faltam `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `NOTIFICATIONS_FROM` e
   `FRONTEND_BASE_URL`.
2. **`FRONTEND_BASE_URL` não existe em lugar nenhum além do default.** Sem ele, todo link de
   todo e-mail — confirmar conta, redefinir senha, abrir a conversa — aponta pra
   `http://localhost:3000`, que na VPS é a própria VPS. É a falha mais silenciosa das três:
   o e-mail chega bonito e nenhum botão funciona.
3. **`.env.example` não documenta nada disso**, então quem preencher o `.env` na VPS não tem
   como saber que faltou.

Fora do repositório: criar a conta na **Resend**, verificar o domínio no DNS (SPF/DKIM) e
gerar a chave. A Resend fala SMTP — `smtp.resend.com:587`, usuário `resend`, senha = a API
key —, então o `SmtpEmailSender` atual serve **sem alterar uma linha de Java**. Foi por isso
que o adaptador nasceu falando SMTP em vez de HTTP: troca de provedor vira troca de variável.

**Decisão pendente:** o remetente tem que ser o domínio verificado na Resend, e hoje o repo
aponta pros dois lados — `CORS_ALLOWED_ORIGINS` do `.env.example` usa `tramposeditor.com.br`
e o default de `NOTIFICATIONS_FROM` usa `nao-responda@tramposeditor.com`. Escolher um antes
de verificar o DNS.

**A prioridade disto subiu em 2026-08-06, e muito.** O cadastro passou a exigir um código de
seis dígitos mandado por e-mail (V18), então o canal deixou de ser "o que faz o produto girar
depois que a pessoa entra" e virou **a porta de entrada**. Enquanto o e-mail não sair de
verdade, ninguém cria conta em produção — nem o fundador. Os três itens acima deixaram de ser
dívida de deploy e viraram pré-requisito de lançamento.

O item 5 mudou de posição na ordem: era item 3, e desceu porque é cosmético perto de 3 e 4
— mas continua sendo o de menor esforço do lote, e é o único que melhora a vitrine sem
tocar no motor.

### Por que o feed virou MVP (decisão de 2026-08-03)

Este documento tratava paginação como "escala, não MVP". Estava olhando pro problema
errado: o que faltava não era aguentar volume, era **existir a tela**. Um marketplace
onde cada lado não tem onde olhar o que o outro lado está oferecendo hoje não fecha o
laço da demanda nem o da oferta (§3) — a conversa existe, mas não há de onde ela partir.

A paginação entrou junto porque é a mesma consulta: o feed nasceu paginado em vez de
nascer devendo. O N+1 da vitrine ficou de fora — é otimização de verdade, e a vitrine
já pagina.

### Sobre o custo do chat (dúvida levantada em 2026-08-02)

Descartado como caro por engano. O chat caro é o de tempo real (WebSocket com presença,
ou Stream/Sendbird a US$ 99–500/mês). O necessário aqui são **duas tabelas no Postgres
que já roda**: `conversation` e `message`. Custo incremental de infra: zero. O front já
tem o padrão pronto — Server Actions + revalidação, igual ao "Aceitar" de
`/painel/pedidos`; não precisa de WebSocket nenhum.

O que custa de verdade: **anexo de vídeo** (resolver como o portfólio — só link, zero
storage), moderação, e a expectativa de resposta que o chat cria (por isso o item 2 vem
junto).

---

## 8. O que este documento deliberadamente não decide

- **A estética.** Tokens, tipografia e layout são trabalho seu; aqui só está *onde* eles
  encostam (§4).
- **O glossário final.** O §4.1 diz que ele precisa existir e por quê, não quais palavras.
- **O nome dos níveis do 1%.** O §6 define o mecanismo, não o rótulo.
- **Monetização.** Já resolvida no `FUNDACAO` §3 e não muda com nada daqui.
- **Qual gatilho de retorno da demanda entra primeiro.** O §3.2 lista os três e os ordena
  por custo; escolher é decisão de produto, e ela depende de quanto e-mail você acha que
  este produto tem o direito de mandar.

---

## 9. Estado dos três pilares (2026-08-03)

O esboço se sustenta em três pilares, e eles não amadurecem no mesmo ritmo:

| Pilar | Onde mora | Estado |
|---|---|---|
| **Anatomia** — que superfícies existem | §1, §2 | ✅ fechado: toda superfície tem nome, papel e fase |
| **Circulação** — o que faz o sistema girar | §3 | ✅ fechado: todo laço tem gatilho de retorno e prova de fechamento |
| **Cultura** — onde a ideologia encosta | §4, §5, §6, §6.1 | 🟡 mapeado, e por decisão do fundador é o pilar que fica com ele |

"Fechado" aqui quer dizer **mapeado**, não construído — é o que um esboço pode terminar.
O que ainda não existe em código está no §7, em ordem, com o porquê de cada posição.

Sobre o terceiro: cultura foi deliberadamente deixada com o fundador, e a divisão útil é
esta — dos 8 pontos de contato do §4, quatro são **mecânicos** (a ordenação do §2.1, os
estados vazios, as telas de erro e esconder o `★ 0.0`) e podem ser executados por
qualquer um; os outros quatro são **voz** (léxico, ritual de entrada, status visível e o
tom fora do site) e só ficam de pé se saírem de quem tem a convicção. Não é uma
lacuna do documento: é uma fronteira dele.
