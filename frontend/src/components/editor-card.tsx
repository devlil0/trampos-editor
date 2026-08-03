import Link from "next/link";
import type { EditorSummary } from "@/lib/types";
import { formatPrice, formatTurnaround } from "@/lib/format";
import { NicheTag, StyleTag, ToolTag } from "./niche-tag";
import { VideoThumb } from "./video-thumb";

/**
 * Linha da vitrine — capa à esquerda, informação à direita (formato de lista tipo
 * "próximos vídeos" do YouTube). Em telas estreitas empilha: capa em cima, texto embaixo.
 */
export function EditorCard({ editor }: { editor: EditorSummary }) {
  const price = formatPrice(editor.priceMinCents);
  const turnaround = formatTurnaround(editor.turnaroundDays);

  return (
    <Link
      href={`/editores/${editor.slug}`}
      className="group flex flex-col gap-4 rounded-[18px] border border-line bg-card p-3 transition duration-200 hover:border-accent/45 hover:shadow-[var(--shadow)] sm:flex-row"
    >
      <div className="relative w-full shrink-0 overflow-hidden rounded-[13px] sm:w-[292px]">
        <VideoThumb url={editor.coverVideoUrl} alt={`Portfólio de ${editor.displayName}`} seed={editor.slug}>
          <span className="absolute left-2.5 top-2.5 inline-flex items-center gap-1.5 rounded-full bg-black/60 px-2.5 py-1 text-xs font-semibold text-white backdrop-blur-sm">
            <span
              className={`h-1.5 w-1.5 rounded-full ${
                editor.available ? "bg-good ring-3 ring-good/30" : "bg-busy"
              }`}
            />
            {editor.available ? "Disponível" : "Ocupado"}
          </span>
          {price && (
            <span className="absolute bottom-2.5 right-2.5 rounded-full bg-black/60 px-2.5 py-1 text-sm font-bold tabular-nums text-white backdrop-blur-sm">
              {price}
            </span>
          )}
        </VideoThumb>
      </div>

      <div className="flex min-w-0 flex-1 flex-col gap-2 sm:py-1 sm:pr-1">
        <div className="flex items-baseline justify-between gap-2.5">
          <span className="truncate text-lg font-bold tracking-tight">{editor.displayName}</span>
          {/* Sem avaliação nenhuma, o card não diz nada sobre nota: "★ 0.0" lê como
              acusação, e a ausência é neutra (ESBOCO §7 item 5). */}
          {editor.ratingCount > 0 && (
            <span className="shrink-0 whitespace-nowrap text-sm text-muted tabular-nums">
              <span className="text-amber-400">★</span>{" "}
              <b className="text-fg">{editor.ratingAvg.toFixed(1)}</b> ({editor.ratingCount})
            </span>
          )}
        </div>

        {editor.headline && <p className="text-sm text-muted">{editor.headline}</p>}

        <div className="flex flex-wrap gap-1.5 pt-0.5">
          {editor.niches.map((n) => (
            <NicheTag key={n} niche={n} />
          ))}
          {editor.styles.map((s) => (
            <StyleTag key={s} style={s} />
          ))}
          {editor.tools.map((t) => (
            <ToolTag key={t} tool={t} />
          ))}
        </div>

        {turnaround && (
          <div className="mt-auto flex items-center gap-1.5 pt-1 text-xs text-muted">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 2" />
            </svg>
            Entrega em {turnaround}
          </div>
        )}
      </div>
    </Link>
  );
}
