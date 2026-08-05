import Image from "next/image";
import Link from "next/link";
import { coverThumbnail, formatPrice } from "@/lib/format";
import { gradientFor } from "./video-thumb";
import { NicheIcon } from "./niche-tag";
import type { EditorSummary } from "@/lib/types";

/**
 * Inclinação fixa por posição — sem random, pra não mudar entre servidor e cliente.
 * Vai como variável CSS, não como `rotate` inline: estilo inline vence classe, e o
 * `hover:rotate-0` que endireita o quadro sob o cursor nunca chegaria a valer.
 */
const TILT = ["-4deg", "2deg", "-1.5deg"];

/**
 * Atraso do ciclo de ênfase por posição, em voltas (ver @keyframes reel-focus).
 * O quadro do meio começa grande (-1 = ciclo inteiro, ou seja, no início dele); o da
 * esquerda cresce depois de um salto e o da direita depois de dois. Mudar estes
 * números dessincroniza a ênfase dos saltos — eles saem da conta do reel-hop.
 */
const FOCUS_DELAY = [-1 / 3, -1, -2 / 3];

/** Três quadros por cópia, duas cópias: é o que fecha a volta sem emenda visível. */
const FRAMES_PER_COPY = 3;

/**
 * Prova de oferta na primeira dobra: editores de verdade, vindos da API, em quadros
 * 9:16 — o formato do produto. Um marketplace vazio não convence ninguém, então o hero
 * mostra quem já está na vitrine em vez de ilustração.
 *
 * Os três quadros pulam pra esquerda de tempos em tempos, cada um caindo no lugar do
 * vizinho, e quem chega ao meio cresce. Sob o cursor tudo congela e o quadro apontado
 * cresce mais ainda — é o "este aqui" da interação. Toda a mecânica é CSS (reel-hop e
 * reel-focus no globals.css): é decoração do hero, não pode custar JS na primeira dobra.
 */
export function HeroReel({ editors }: { editors: EditorSummary[] }) {
  const trio = editors.slice(0, FRAMES_PER_COPY);
  if (trio.length === 0) return null;

  // Os saltos do reel-hop são frações de um trilho de seis quadros. Com menos de três
  // editores a conta do passo não fecha e a fita andaria torto, então o hero fica
  // parado até a vitrine encher — vale mais que um marketplace novo sem prova nenhuma.
  if (trio.length < FRAMES_PER_COPY) {
    return (
      <ul className="flex items-center gap-3 py-4 sm:gap-4">
        {trio.map((editor, i) => (
          <li key={editor.id}>
            <Frame editor={editor} tilt={TILT[i % TILT.length]} />
          </li>
        ))}
      </ul>
    );
  }

  return (
    // Largura de três quadros: a fita precisa caber em 375px sem criar rolagem
    // horizontal na página. As pontas somem no gradiente em vez de cortar seco.
    //
    // Recorte só na horizontal: `overflow-hidden` cortaria reto o topo do quadro do
    // meio, que é maior que a caixa (1.16, mais o arco do salto e o crescer do hover).
    // `clip` na horizontal permite `visible` na vertical — com `hidden` nos dois eixos
    // o navegador promoveria o outro a `auto` e voltaria a cortar.
    //
    // O `py-14 -my-8` não é respiro, é espaço pra máscara: `mask-clip` vale border-box,
    // então a máscara apara tudo que sai da caixa e cortaria o quadro ampliado de volta,
    // por mais que o overflow o deixe passar. O padding empurra essa borda pra longe e a
    // margem negativa devolve o espaço, deixando a pegada vertical igual à de um py-6.
    <div
      className="relative -my-8 w-[clamp(300px,84vw,504px)] overflow-x-clip overflow-y-visible py-14"
      style={{
        maskImage: "linear-gradient(90deg, transparent, #000 7%, #000 93%, transparent)",
        WebkitMaskImage: "linear-gradient(90deg, transparent, #000 7%, #000 93%, transparent)",
      }}
    >
      <div className="reel-track motion-always">
        {[0, 1].map((copia) => (
          <ul
            key={copia}
            className="flex shrink-0 items-center"
            /* A cópia repetida some do leitor de tela e do tab: sem isso o mesmo
               editor apareceria duas vezes na navegação por teclado. */
            inert={copia > 0}
          >
            {trio.map((editor, i) => (
              <li
                key={editor.id}
                className="reel-frame pr-3 sm:pr-4"
                style={{ animationDelay: `calc(var(--reel-cycle) * ${FOCUS_DELAY[i]})` }}
              >
                <Frame editor={editor} tilt={TILT[i % TILT.length]} />
              </li>
            ))}
          </ul>
        ))}
      </div>
    </div>
  );
}

/** Um quadro 9:16 com capa, status e preço — o card do hero. */
function Frame({ editor, tilt }: { editor: EditorSummary; tilt: string }) {
  const thumb = coverThumbnail(editor.coverVideoUrl);
  const price = formatPrice(editor.priceMinCents);

  return (
    <Link
      href={`/${editor.username}`}
      style={{ "--tilt": tilt } as React.CSSProperties}
      /* `rotate`, `scale` e `translate` são propriedades separadas em CSS moderno,
         então compõem sem uma anular a outra — e a escala daqui multiplica a do
         reel-focus, que mora no <li>. */
      className="group relative block aspect-[9/16] w-[clamp(88px,26vw,152px)] rotate-[var(--tilt)] overflow-hidden rounded-[18px] border border-line transition duration-300 ease-out hover:-translate-y-2 hover:rotate-0 hover:scale-[1.12] hover:border-accent/60 hover:shadow-[var(--shadow)]"
    >
      {thumb ? (
        <Image
          src={thumb}
          alt=""
          fill
          sizes="152px"
          className="object-cover transition duration-500 group-hover:scale-105"
        />
      ) : (
        <span
          className="absolute inset-0"
          style={{ background: gradientFor(editor.slug) }}
          aria-hidden="true"
        />
      )}

      <span
        className="absolute inset-x-0 bottom-0 h-3/5 bg-gradient-to-t from-black/85 via-black/35 to-transparent"
        aria-hidden="true"
      />

      {editor.available && (
        <span className="absolute left-2.5 top-2.5 flex items-center gap-1.5 rounded-full bg-black/55 px-2 py-1 font-mono text-[10px] font-semibold tracking-wide text-white backdrop-blur-sm">
          <span className="h-1.5 w-1.5 rounded-full bg-good" />
          livre
        </span>
      )}

      <span className="absolute inset-x-2.5 bottom-2.5 block text-white">
        <span className="flex items-center gap-1">
          {editor.niches.slice(0, 3).map((n) => (
            <NicheIcon key={n} niche={n} size={13} />
          ))}
        </span>
        <span className="mt-1 block truncate text-sm font-bold tracking-tight">
          {editor.displayName}
        </span>
        {price && <span className="block font-mono text-[11px] text-white/70">{price}</span>}
      </span>
    </Link>
  );
}
