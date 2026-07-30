import Link from "next/link";

export interface MarqueeTag {
  label: string;
  href: string;
  /** Ícone opcional — plataforma e ferramenta têm, estilo não. */
  icon?: React.ReactNode;
}

/**
 * Esteira de tags rolando pra esquerda.
 * <p>
 * CSS puro, sem JS: a faixa repete a lista quatro vezes e a animação a desloca em 25%,
 * o que faz a cópia seguinte cair exatamente onde a anterior começou (ver .marquee-track
 * no globals.css). As cópias repetidas somem pra leitores de tela e saem do tab.
 * `motion-always` mantém a faixa andando mesmo sob prefers-reduced-motion — decisão de
 * produto, ela é o movimento que dá vida à landing.
 */
export function TagMarquee({ tags }: { tags: MarqueeTag[] }) {
  if (tags.length === 0) return null;

  return (
    <div
      className="marquee-viewport relative overflow-hidden py-1"
      style={{
        // Some nas duas pontas em vez de cortar seco.
        maskImage:
          "linear-gradient(90deg, transparent, #000 5%, #000 95%, transparent)",
        WebkitMaskImage:
          "linear-gradient(90deg, transparent, #000 5%, #000 95%, transparent)",
      }}
    >
      <div className="marquee-track motion-always">
        {[0, 1, 2, 3].map((copia) => (
          <ul
            key={copia}
            className="flex shrink-0 gap-2.5 pr-2.5"
            aria-hidden={copia > 0 ? true : undefined}
          >
            {tags.map((tag) => (
              <li key={tag.label}>
                <Link
                  href={tag.href}
                  tabIndex={copia > 0 ? -1 : undefined}
                  className="inline-flex items-center gap-2 whitespace-nowrap rounded-full border border-line bg-card px-3.5 py-2 text-sm font-semibold transition hover:border-accent hover:text-accent"
                >
                  {tag.icon}
                  {tag.label}
                </Link>
              </li>
            ))}
          </ul>
        ))}
      </div>
    </div>
  );
}
