import Link from "next/link";
import type { EditorSummary } from "@/lib/types";
import { formatPrice, formatTurnaround, nicheLabel, toolLabel } from "@/lib/format";
import { VideoThumb } from "./video-thumb";

export function EditorCard({ editor }: { editor: EditorSummary }) {
  const price = formatPrice(editor.priceMinCents);
  const turnaround = formatTurnaround(editor.turnaroundDays);

  return (
    <Link
      href={`/editores/${editor.slug}`}
      className="group flex flex-col overflow-hidden rounded-[18px] border border-line bg-card transition duration-200 hover:-translate-y-0.5 hover:border-accent/45 hover:shadow-[var(--shadow)]"
    >
      <VideoThumb url={editor.coverVideoUrl} alt={`Portfólio de ${editor.displayName}`} seed={editor.slug}>
        <span className="absolute left-3 top-3 inline-flex items-center gap-1.5 rounded-full bg-black/60 px-2.5 py-1 text-xs font-semibold text-white backdrop-blur-sm">
          <span
            className={`h-1.5 w-1.5 rounded-full ${
              editor.available ? "bg-good ring-3 ring-good/30" : "bg-busy"
            }`}
          />
          {editor.available ? "Disponível" : "Ocupado"}
        </span>
        {price && (
          <span className="absolute bottom-3 right-3 rounded-full bg-black/60 px-2.5 py-1 text-sm font-bold tabular-nums text-white backdrop-blur-sm">
            {price}
          </span>
        )}
      </VideoThumb>

      <div className="flex flex-1 flex-col gap-2.5 p-4">
        <div className="flex items-baseline justify-between gap-2.5">
          <span className="font-bold tracking-tight">{editor.displayName}</span>
          <span className="whitespace-nowrap text-sm text-muted tabular-nums">
            <span className="text-amber-400">★</span>{" "}
            <b className="text-fg">{editor.ratingAvg.toFixed(1)}</b> ({editor.ratingCount})
          </span>
        </div>

        {editor.headline && <p className="text-sm text-muted">{editor.headline}</p>}

        <div className="mt-auto flex flex-wrap gap-1.5 pt-1">
          {editor.niches.map((n) => (
            <span
              key={n}
              className="rounded-md bg-accent-soft px-2 py-1 text-xs font-semibold text-accent-strong"
            >
              {nicheLabel(n)}
            </span>
          ))}
          {editor.tools.map((t) => (
            <span key={t} className="rounded-md bg-panel px-2 py-1 text-xs font-semibold text-muted">
              {toolLabel(t)}
            </span>
          ))}
        </div>

        {turnaround && (
          <div className="flex items-center gap-1.5 border-t border-line pt-2.5 text-xs text-muted">
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
