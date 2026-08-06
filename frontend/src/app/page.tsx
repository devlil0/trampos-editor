import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { fetchEditors } from "@/lib/api";
import { getCurrentUser } from "@/lib/session";
import { Aurora } from "@/components/aurora";
import { EditorCard } from "@/components/editor-card";
import { HeroReel } from "@/components/hero-reel";
import { NicheIcon, ToolIcon } from "@/components/niche-tag";
import { TagMarquee, type MarqueeTag } from "@/components/tag-marquee";
import { TypedWord } from "@/components/typed-word";
import {
  coverThumbnail,
  nicheIcon,
  NICHE_OPTIONS,
  toolIcon,
  TOOL_OPTIONS,
} from "@/lib/format";
import type { EditorSummary } from "@/lib/types";

export const metadata: Metadata = {
  alternates: { canonical: "/" },
};

/**
 * Só entra na esteira quem tem logo — a regra sai dos próprios ícones, não de uma lista
 * paralela que envelheceria. Podcast (microfone desenhado) e os estilos, que não têm
 * marca, ficam de fora: numa faixa em movimento, chip sem ícone vira buraco visual.
 * Todo chip leva a um filtro real da vitrine.
 */
const TAGS: MarqueeTag[] = [
  ...NICHE_OPTIONS.filter(([value]) => nicheIcon(value) != null).map(([value, label]) => ({
    label,
    href: `/editores?niche=${value}`,
    icon: <NicheIcon niche={value} size={16} />,
  })),
  ...TOOL_OPTIONS.filter(([value]) => toolIcon(value) != null).map(([value, label]) => ({
    label,
    href: `/editores?tool=${value}`,
    icon: <ToolIcon tool={value} size={16} />,
  })),
];

const DORES = [
  "Quarenta propostas genéricas pra ler.",
  "Negociação que trava antes de começar.",
  "Taxa da plataforma em cima do combinado.",
];

/**
 * Landing — porta de entrada do site. A vitrine mora em /editores; aqui o trabalho é
 * dizer o que é, provar que existe oferta de verdade e mandar a pessoa pro lado certo.
 */
export default async function LandingPage() {
  // Quem já entrou não precisa da landing: ela é peça de aquisição, e quem tem conta abre
  // o produto pra ver o que tem hoje. O SEO não sofre — robô de busca chega deslogado e
  // continua recebendo esta tela.
  //
  // Vai por redirecionamento, e não renderizando o feed aqui, porque o feed já tem
  // endereço: renderizá-lo também em `/` criaria a mesma lista em duas URLs, que é
  // exatamente o que a remoção do feed da oferta corrigiu (ESBOCO §1).
  if (await getCurrentUser()) redirect("/editores");

  // A API já ordena disponível > nota > recente. O hero fica com os primeiros (ele
  // mesmo recorta em três) e a pilha de capas usa o resto.
  //
  // A segunda chamada existe só pelo `totalElements` de quem está livre: contar o
  // `available` da primeira página daria o número da página, não o da vitrine. `size=1`
  // porque o conteúdo não interessa — só o total.
  const [destaque, disponiveis] = await Promise.all([
    fetchEditors({ page: "0" }),
    fetchEditors({ available: "true", size: "1" }),
  ]);
  const editores = destaque.content;
  const livres = disponiveis.totalElements;

  // Prefere quem tem capa de verdade: a seção "escolher olhando" perde o argumento se
  // o card de exemplo abrir com o gradiente de fallback em vez de um vídeo.
  const vitrine = editores.find((e) => coverThumbnail(e.coverVideoUrl) != null) ?? editores[0];

  return (
    <>
      {/* ---------- Hero ---------- */}
      <section className="relative overflow-hidden border-b border-line">
        <Aurora />
        <div className="relative mx-auto max-w-6xl px-6 pt-16 pb-20 sm:pt-24">
          <div className="grid items-center gap-12 lg:grid-cols-[1fr_auto]">
            <div>
              <p className="font-mono text-xs font-semibold tracking-[0.18em] text-accent">
                REELS · TIKTOK · SHORTS
              </p>

              <h1 className="mt-4 max-w-[15ch] text-balance text-5xl font-extrabold leading-[0.98] tracking-[-0.03em] sm:text-6xl">
                O editor certo pro seu{" "}
                <TypedWord words={["vídeo.", "TikTok.", "Short.", "corte."]} />
              </h1>

              {/* "Nenhuma comissão" e não "sem taxa pra conversar": a taxa que assusta
                  quem contrata é a que incide no trampo fechado, e é justamente a que
                  não existe aqui (Fase 1 do FUNDACAO). Dizer o menos verdadeiro joga
                  fora o argumento mais forte contra Fiverr e Workana. */}
              <p className="mt-5 max-w-[46ch] text-lg text-muted">
                Veja o portfólio, prazo e preço antes fechar algum um trampo. Sem burocracia,
                taxas ou qualquer tipo de estresse. #TramposEditor
              </p>

              <div className="mt-8 flex flex-wrap gap-3">
                <Link
                  href="/editores"
                  className="btn btn-primary btn-lg"
                >
                  Ver editores
                  <Seta />
                </Link>
                {/* Levava pra /painel/pedidos/novo, que é o formulário de POSTAR um
                    pedido — fluxo de cliente. Quem clicava "sou editor" caía na tela
                    de quem contrata. Agora vai pra porta do próprio público. */}
                <Link
                  href="/para-editores"
                  className="btn btn-ghost btn-lg"
                >
                  Sou editor, quero aparecer
                </Link>
              </div>

              {/* O caminho do editor vem como texto, e não como um terceiro botão, pra
                  não dividir a atenção do CTA principal.

                  Aponta pro feed de trampos, e não pra /para-editores: a linha promete
                  escolher um trampo, então mandar pra uma página de explicação quebraria
                  a promessa. Deslogado cai no login e volta — e quem chega lá sem perfil
                  publicado encontra o convite pra montar um, já tratado na própria tela. */}
              <p className="mt-4 text-sm text-muted">
                <Link href="/painel/feed" className="font-semibold text-fg hover:text-accent">
                  Você edita? Pegue um trampo.
                </Link>
              </p>
            </div>

            <div className="lg:justify-self-end">
              <HeroReel editors={editores} />
              {/* "9 editores" é um número; "6 livres agora" é uma promessa de que
                  alguém responde hoje. O segundo é o que tira a pessoa da inércia, então
                  ele aparece destacado e só quando existe de fato. */}
              {editores.length > 0 && (
                <p className="mt-3.5 font-mono text-sm tracking-wide text-muted">
                  {destaque.totalElements}{" "}
                  {destaque.totalElements === 1 ? "editor" : "editores"} na vitrine
                  {livres > 0 && (
                    <>
                      {" · "}
                      <span className="font-semibold text-good">{livres} livres agora</span>
                    </>
                  )}
                </p>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* ---------- Esteira de tags ---------- */}
      <section className="border-b border-line">
        <div className="mx-auto max-w-6xl px-6 py-7">
          <p className="mb-3.5 text-sm text-muted">Procure pela plataforma ou pelo programa:</p>
          <TagMarquee tags={TAGS} />
        </div>
      </section>

      {/* ---------- O que é ----------
          Quem cai aqui por link ou anúncio ainda não sabe o que o site é, e o hero vende
          antes de explicar. Uma frase define, a seguinte diz o que a pessoa faz aqui.

          Os formatos aparecem como texto corrido, e não como chips com logo: a esteira
          logo acima já os mostra clicáveis, e duas fileiras de plataforma coladas uma na
          outra leriam como repetição. Aqui eles são parte da definição; ali, filtro. */}
      <section className="border-b border-line">
        <div className="mx-auto max-w-6xl px-6 py-16">
          {/* A pergunta é o assunto da seção, então ela é o título — antes vinha como
              rótulo miúdo em cima de um título maior, o que invertia a hierarquia.
              Escala logo abaixo da do hero: é o segundo maior texto da página, e não
              disputa com ele. O nome da marca sai em degradê, o único da landing: é o
              lugar onde a marca se apresenta, e o resto da página usa roxo chapado. */}
          <h2 className="max-w-[16ch] text-balance text-4xl font-extrabold leading-[1.02] tracking-[-0.03em] sm:text-5xl">
            O que é a{" "}
            <span className="bg-gradient-to-r from-accent to-accent-strong bg-clip-text text-transparent">
              TramposEditor
            </span>
            ?
          </h2>

          <p className="mt-6 max-w-[24ch] text-balance text-2xl font-bold leading-snug tracking-tight sm:text-3xl">
            Um marketplace de editores de vídeo.
          </p>

          <p className="mt-5 max-w-[52ch] text-lg">
            Você precisa de alguém pra cortar seu vídeo. A gente reúne os editores num lugar só,
            com portfólio, preço e prazo à mostra. Você olha, escolhe e chama.
          </p>

          <p className="mt-4 max-w-[52ch] text-muted">
            Sem burocracia, sem leilão de proposta e sem ninguém no meio. Feito pros formatos que
            você posta: TikTok, Reels, YouTube e cortes de podcast.
          </p>
        </div>
      </section>

      {/* ---------- A dor ---------- */}
      <section className="border-b border-line bg-card">
        <div className="mx-auto max-w-6xl px-6 py-16">
          <div className="grid gap-10 md:grid-cols-2">
            <div>
              <h2 className="max-w-[16ch] text-balance text-3xl font-extrabold leading-[1.06] tracking-tight sm:text-4xl">
                Contratar editor virou um projeto à parte
              </h2>
              <p className="mt-4 max-w-[38ch] text-muted">
                Entre procurar alguém e receber o vídeo pronto, some uma semana.
              </p>
            </div>

            <ul className="flex flex-col justify-center gap-5">
              {DORES.map((dor) => (
                <li key={dor} className="flex items-center gap-3.5">
                  <span className="grid h-9 w-9 flex-none place-items-center rounded-full border border-busy/35 text-busy">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
                      <path d="M18 6 6 18M6 6l12 12" />
                    </svg>
                  </span>
                  <span className="font-semibold">{dor}</span>
                </li>
              ))}
            </ul>
          </div>

          <p className="mt-12 rounded-[18px] border border-accent/45 bg-accent-soft px-6 py-8 text-center text-xl font-bold tracking-tight text-accent-strong sm:text-2xl">
            Aqui você abre o portfólio, vê prazo e preço, e chama quem gostou.
          </p>
        </div>
      </section>

      {/* ---------- Objeções ----------
          Vem logo depois da dor de propósito: aquela seção levanta o problema, esta
          responde as dúvidas que travam a pessoa antes de clicar. Texto aberto, sem
          acordeão — o que está dobrado não é lido nem indexado, e são justamente estas
          respostas que o buscador precisa ver. */}
      <section className="border-b border-line">
        <div className="mx-auto max-w-6xl px-6 py-16">
          <h2 className="max-w-[20ch] text-balance text-3xl font-extrabold leading-[1.08] tracking-tight sm:text-4xl">
            Antes de você perguntar
          </h2>

          <div className="mt-9 grid gap-x-10 gap-y-8 md:grid-cols-2">
            <Duvida pergunta="Quanto custa contratar?">
              Cada editor publica a faixa de preço dele. Você vê antes de falar com qualquer um
              — não precisa pedir orçamento pra três pessoas só pra descobrir se cabe no seu
              bolso.
            </Duvida>

            <Duvida pergunta="Quanto tempo demora?">
              O prazo médio de entrega fica no perfil, do lado do preço. E quem está com a
              agenda livre aparece marcado como disponível, então você já sabe quem consegue
              pegar seu vídeo agora.
            </Duvida>

            <Duvida pergunta="Como eu pago?">
              Direto com o editor, do jeito que vocês combinarem. A plataforma não entra no
              meio do pagamento e não fica com percentual nenhum do seu trabalho.
            </Duvida>

            <Duvida pergunta="É grátis mesmo? Qual é a pegadinha?">
              É grátis dos dois lados, sem mensalidade. Um dia vai existir um destaque avulso
              — o editor paga por uso se quiser aparecer no topo. Nunca vai existir pedágio
              pra conversar com alguém.
            </Duvida>

            {/* Resposta deliberadamente sem garantia: pagamento protegido é Fase 3 do
                FUNDACAO e não existe no produto. Prometer o que não há custaria mais caro
                que a objeção. */}
            <Duvida pergunta="E se eu não gostar do resultado?">
              Você escolhe vendo trabalho pronto, não promessa — o portfólio está lá antes de
              você chamar. E como o combinado é direto, dá pra dividir a entrega em partes e
              alinhar no caminho. Pagamento protegido pela plataforma ainda não existe; quando
              existir, vai ser opcional.
            </Duvida>

            <Duvida pergunta="Preciso criar conta pra usar?">
              Só pra falar com alguém ou publicar um pedido. Ver a vitrine, abrir perfis e
              assistir aos portfólios não pede cadastro nenhum.
            </Duvida>
          </div>
        </div>
      </section>

      {/* ---------- Como é aqui ----------
          A seção mostra em vez de contar: o título promete "olhando", então o que vem
          abaixo é o card de verdade da vitrine, não um parágrafo sobre ele. É o único
          lugar da landing onde a pessoa vê o produto antes de clicar.

          As outras duas mensagens que moravam aqui (é de graça / vocês combinam direto)
          saíram porque a seção de objeções já as responde, quase com as mesmas palavras.
          Repetir gasta a atenção de quem lê e não acrescenta argumento. */}
      {vitrine && (
        <section className="border-b border-line">
          <div className="mx-auto max-w-6xl px-6 py-16">
            <h2 className="max-w-[24ch] text-balance text-3xl font-extrabold leading-[1.08] tracking-tight sm:text-4xl">
              Do jeito que você já escolhe vídeo — <span className="text-accent">olhando</span>.
            </h2>
            <p className="mt-4 max-w-[46ch] text-muted">
              Cada perfil abre com os vídeos que a pessoa já entregou. Nada de currículo: o corte
              fala por ela.
            </p>

            <AnatomiaDoCard editor={vitrine} />
          </div>
        </section>
      )}

      {/* ---------- Comece por aqui ---------- */}
      <section>
        <div className="mx-auto max-w-6xl px-6 py-16">
          <h2 className="text-center text-3xl font-extrabold tracking-tight sm:text-4xl">
            Comece por <span className="text-accent">aqui</span>
          </h2>

          <div className="mt-10 grid gap-x-10 gap-y-9 sm:grid-cols-2">
            <Acao
              titulo="Ver editores"
              texto="Navegue por plataforma, ferramenta, preço e prazo."
              href="/editores"
              label="Abrir a vitrine"
            />
            <Acao
              titulo="Postar um pedido"
              texto="Descreva o que você precisa e deixe os editores virem até você."
              href="/painel/pedidos/novo"
              label="Descrever o trampo"
            />
            <Acao
              titulo="Publicar meu perfil"
              texto="Seu portfólio na frente de quem já está procurando editor."
              href="/criar-conta"
              label="Criar meu perfil"
            />
            <Acao
              titulo="Entender o fluxo"
              texto="O passo a passo de quem contrata, sem letra miúda."
              href="/para-clientes"
              label="Como funciona"
            />
          </div>
        </div>
      </section>
    </>
  );
}

function Seta() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M5 12h14M13 6l6 6-6 6" />
    </svg>
  );
}

/**
 * O que cada seta embaixo do card aponta. A ordem segue a leitura do card da esquerda
 * pra direita, porque é isso que alinha cada legenda com o pedaço que ela explica.
 */
const ANOTACOES = [
  {
    titulo: "O trabalho primeiro",
    texto: "A capa é um vídeo que ela já entregou — e o selo diz se ela está livre agora.",
  },
  { titulo: "O preço, sem pedir", texto: "A partir de quanto ela pega o trampo." },
  { titulo: "O prazo, declarado", texto: "Quanto tempo até o vídeo estar na sua mão." },
];

/**
 * O card real da vitrine, com legendas apontando pro que a pessoa usa pra decidir.
 *
 * É um card de verdade, clicável, de um editor de verdade: a seção defende que dá pra
 * escolher olhando, então uma maquete desenhada derrubaria o próprio argumento.
 *
 * As setas ficam numa grade de três colunas ABAIXO do card, e não sobrepostas nele.
 * Marcador posicionado por cima depende de onde cada pedaço do card cai, o que muda com
 * a largura da tela e quebraria calado no primeiro ajuste do EditorCard.
 */
function AnatomiaDoCard({ editor }: { editor: EditorSummary }) {
  return (
    <div className="mt-9 max-w-[560px]">
      <EditorCard editor={editor} />

      {/* Some no celular: com o card empilhado (capa em cima, texto embaixo), as
          colunas deixam de corresponder ao que apontam e a seta vira ruído. */}
      <div className="mt-3 hidden grid-cols-3 gap-4 sm:grid">
        {ANOTACOES.map((anotacao) => (
          <div key={anotacao.titulo}>
            <span aria-hidden="true" className="block text-sm leading-none text-accent">
              ↑
            </span>
            <h3 className="mt-1.5 text-sm font-bold tracking-tight">{anotacao.titulo}</h3>
            <p className="mt-0.5 text-xs text-muted">{anotacao.texto}</p>
          </div>
        ))}
      </div>

      {/* No celular a mesma informação vira lista corrida, sem prometer alinhamento. */}
      <ul className="mt-4 flex flex-col gap-2 sm:hidden">
        {ANOTACOES.map((anotacao) => (
          <li key={anotacao.titulo} className="text-sm">
            <b className="font-bold tracking-tight">{anotacao.titulo}</b>{" "}
            <span className="text-muted">{anotacao.texto}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

/** Pergunta que trava a decisão, com a resposta logo abaixo. */
function Duvida({ pergunta, children }: { pergunta: string; children: React.ReactNode }) {
  return (
    <div className="border-l border-line pl-6">
      <h3 className="font-bold tracking-tight">{pergunta}</h3>
      <p className="mt-1.5 max-w-[46ch] text-sm text-muted">{children}</p>
    </div>
  );
}

/** Item da grade final: título, texto e um CTA próprio, com fio à esquerda. */
function Acao({
  titulo,
  texto,
  href,
  label,
}: {
  titulo: string;
  texto: string;
  href: string;
  label: string;
}) {
  return (
    <div className="border-l border-line pl-6">
      <h3 className="text-xl font-bold tracking-tight">{titulo}</h3>
      <p className="mt-1.5 max-w-[40ch] text-sm text-muted">{texto}</p>
      <Link
        href={href}
        className="mt-4 btn btn-primary btn-sm"
      >
        {label}
        <Seta />
      </Link>
    </div>
  );
}
