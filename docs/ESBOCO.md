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
