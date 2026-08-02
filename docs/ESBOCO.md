# TramposEditor — Esboço do Sistema Completo

> O mapa do todo: cada superfície que o produto vai ter, o que faz o sistema girar, e
> onde a estética, a ideologia e a cultura encostam no software.
> Complementa o `FUNDACAO.md` (estratégia e decisões) — aqui é a forma do sistema.
> Data: 2026-08-02 · Estado: Blocos 0–4 e 7 entregues; 5 e 6 em aberto.

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
│  /painel                    hub                             │
│  /painel/meu-perfil         editor monta sua vitrine        │
│  /painel/pedidos            cliente posta e recebe          │
│  /painel/trabalhos          editor escolhe trampo           │
│  /painel/conversas    ⬜    A CAIXA DE ENTRADA (falta)      │
│  /painel/trabalhos-fechados ⬜  histórico + review (falta)  │
└─────────────────────────────────────────────────────────────┘
                            ↓ roda em cima de
┌─ MOTOR ───────────────────────── guarda e faz cumprir ─────┐
│  User · EditorProfile · PortfolioItem                       │
│  JobRequest · JobInterest                                   │
│  Conversation · Message ⬜   Deal ⬜   Review ⬜             │
│  Notification ⬜   Boost (F2)   Escrow (F3)                 │
└─────────────────────────────────────────────────────────────┘
```

### Por que a camada pública é grande e a logada é pequena

Não é acidente e não deve ser "corrigido": o produto vive de **SEO long-tail**
(`FUNDACAO` §11.2.6). A camada pública é o funil de aquisição inteiro e precisa ser
indexável. A camada logada não é indexada de propósito (`robots: { index: false }`) —
ela não capta ninguém, ela **resolve** quem já foi captado.

O erro clássico seria inflar a camada logada com dashboards. O coração não é mais telas:
é **uma tela onde as duas pontas se falam**.

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
| `/painel` | ✅ | ambos | hub por papel | MVP |
| `/painel/meu-perfil` | ✅ | editor | virar editor, editar vitrine | MVP |
| `/painel/pedidos` | ✅ | cliente | postar, ver interessados, aceitar | 1.5 |
| `/painel/trabalhos` | ✅ | editor | feed, candidatar, acompanhar | 1.5 |
| **`/painel/conversas`** | ⬜ | ambos | **onde a relação acontece** | **MVP** |
| **trabalho fechado + review** | ⬜ | ambos | fecha o laço de reputação | **MVP** |
| notificação por e-mail | ⬜ | ambos | faz o assíncrono funcionar | **MVP** |
| páginas de tag (`/editores/tiktok`) | ⬜ | cliente | SEO long-tail dedicado | 2 |
| destaque / boost | ⬜ | editor | monetização pay-per-use | 2 |
| pagamento protegido | ⬜ | ambos | take-rate como seguro | 3 |
| vertical áudio | ⬜ | ambos | mesma máquina, outra `Vertical` | 4 |
| moderação / curadoria | ⬜ | admin | sustenta a promessa de qualidade | 2 |

---

## 3. Os quatro laços que fazem o sistema girar

Um marketplace não é um conjunto de telas, é um conjunto de **ciclos**. Se um ciclo não
fecha, o produto vaza pelo buraco.

**Laço de aquisição** — o único que já fecha
`busca no Google → perfil indexado → vitrine → ???`

**Laço da oferta** (o editor volta porque ganha)
`publica perfil → aparece → recebe contato → entrega → recebe nota → sobe na ordenação → aparece mais`

**Laço da demanda** (o cliente volta porque resolveu)
`contrata → deu certo → volta / indica`

**Laço de reputação** (o combustível de confiança)
`trabalho fechado → avaliação → nota → ordenação → visibilidade`

### O diagnóstico em uma frase

> Os quatro laços passam por **conversa** e **avaliação** — que são exatamente as duas
> peças que não existem. O sistema hoje tem entrada e saída, mas não tem circulação.

É por isso que "faltam as janelas do pós-login" está certo em espírito: o que falta não
é *mais* painel, é o pedaço que faz o ciclo voltar ao começo.

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
| 4 | **A ordenação da vitrine** — quem aparece primeiro | `/editores` | 🟡 implícita |
| 5 | **Status visível** — selo, nível, veterania | card e perfil | 🔴 não existe |
| 6 | **Estados vazios** — o que se diz quando não há nada | todas | 🟡 alguns bons |
| 7 | **A voz fora do site** — e-mail, notificação | ⬜ | 🔴 não existe |
| 8 | **Erro e limite** — 404, sem resultado, sem permissão | `not-found`, `error` | 🟡 genéricos |

### 4.1. O léxico já começou sozinho

"Trampo" entrou na interface ("Você edita? Pegue um trampo"), "vitrine" substituiu
"editores" na nav, "trabalhos abertos" em vez de "vagas". Isso **é** cultura, e é o
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

## 6. A tensão do "1%" — e como resolver sem quebrar a liquidez

Você quer que pertencer ao TramposEditor signifique ser do 1%. Existe um conflito real
entre isso e uma decisão já travada:

> `FUNDACAO` §11.9: a conta é uma só, e **publicar o perfil é o que registra a pessoa
> como editor**. Sem aprovação, sem análise. (`EditorProfileService.upsertMyProfile`
> *concede* o papel EDITOR.)

**Se todo mundo entra, ninguém é 1%.** Exclusividade sem mecanismo é slogan.

Mas fechar a porta agora seria pior: mataria a liquidez, que o próprio `FUNDACAO` §2
define como o risco nº1. Editor é o lado fácil de atrair e o estoque que faz o cliente
ficar. Não se filtra o que ainda não se tem.

### A saída: porta aberta, status conquistado

| Camada | Regra | Efeito |
|---|---|---|
| **Entrada** | livre — publicar perfil basta | preserva a liquidez |
| **Hierarquia** | nota, entregas, tempo de casa, disponibilidade | o 1% é *conquistado*, não concedido |
| **Recorte editorial** | uma seleção curada em cima da base | o 1% é *visível* |

Assim o "1%" deixa de ser porta e vira **topo**. Quem entra vê onde dá pra chegar — o
que é mais motivador que um portão, e não custa estoque.

### Os dois sentimentos são diferentes — e é bom que sejam

Um erro comum seria vender "seja 1%" para os dois lados. Não funciona: o cliente não quer
ser do 1%, ele quer o vídeo cortado.

| Lado | Sentimento | Como se produz |
|---|---|---|
| **Editor** | *"eu pertenço"* — status, ofício, orgulho | anti-leilão, hierarquia visível, léxico de guilda |
| **Cliente** | *"eu tenho acesso"* — contratei do 1% | curadoria visível, prova de qualidade |

O mesmo mecanismo — curadoria e reputação — produz os dois. É por isso que o **Bloco 6
(avaliações) não é só uma feature: é a infraestrutura da cultura.** Sem ela, "1%" não
tem como ser medido nem mostrado.

---

## 7. Consequência prática: o que construir, em ordem

Deriva direto dos laços do §3 — não é lista de desejos.

| # | O quê | Por que agora | Bloco |
|---|---|---|---|
| 1 | **Conversa, com duas portas de entrada** | fecha o `href="#"` do perfil e unifica os dois fluxos numa caixa de entrada só | 5 |
| 2 | **Notificação por e-mail** | sem ela o item 1 não gira: ninguém sabe que foi procurado | novo |
| 3 | **Esconder a nota até existir review** | hoje todo card anuncia "★ 0.0", que faz a vitrine parecer morta | — |
| 4 | **Trabalho fechado + avaliação** | fecha o laço de reputação e liga a nota de volta; é a base do "1%" (§6) | 6 |
| 5 | Paginação do feed, N+1 da vitrine | escala, não MVP | — |

Os itens 1 e 2 separam "as peças existem" de "o marketplace funciona".

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
