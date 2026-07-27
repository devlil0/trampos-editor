import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { fetchEditorBySlug } from "@/lib/api";
import { VideoThumb } from "@/components/video-thumb";
import {
  formatPrice,
  formatTurnaround,
  nicheLabel,
  toolLabel,
} from "@/lib/format";

type Params = Promise<{ slug: string }>;

export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const { slug } = await params;
  const editor = await fetchEditorBySlug(slug);
  if (!editor) return { title: "Editor não encontrado — TramposEditor" };
  return {
    title: `${editor.displayName} — editor de vídeo | TramposEditor`,
    description: editor.headline ?? editor.bio ?? `Perfil de ${editor.displayName} no TramposEditor.`,
  };
}

export default async function EditorProfilePage({ params }: { params: Params }) {
  const { slug } = await params;
  const editor = await fetchEditorBySlug(slug);

  if (!editor) notFound();

  const priceMin = formatPrice(editor.priceMinCents);
  const priceMax = formatPrice(editor.priceMaxCents);
  const priceRange =
    priceMin && priceMax && priceMin !== priceMax
      ? `${priceMin} – ${priceMax}`
      : (priceMin ?? priceMax);
  const turnaround = formatTurnaround(editor.turnaroundDays);
  const firstName = editor.displayName.split(" ")[0];

  return (
    <div className="mx-auto max-w-6xl px-6">
      <Link href="/" className="mt-6 mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Voltar pra vitrine
      </Link>

      <div className="grid grid-cols-1 items-start gap-7 pb-16 lg:grid-cols-[1fr_320px]">
        <div>
          <header className="mb-2 flex items-center gap-4.5">
            <span className="grid h-18 w-18 flex-none place-items-center rounded-[18px] bg-gradient-to-br from-accent to-accent-strong text-2xl font-extrabold tracking-tight text-white">
              {editor.displayName.charAt(0).toUpperCase()}
            </span>
            <div>
              <h1 className="text-2xl font-extrabold tracking-tight">{editor.displayName}</h1>
              {editor.headline && <p className="text-muted">{editor.headline}</p>}
              <span
                className={`mt-2 inline-flex items-center gap-1.5 text-sm font-semibold ${
                  editor.available ? "text-good" : "text-busy"
                }`}
              >
                <span className={`h-1.5 w-1.5 rounded-full ${editor.available ? "bg-good" : "bg-busy"}`} />
                {editor.available ? "Disponível pra novos projetos" : "Sem vaga no momento"}
              </span>
            </div>
          </header>

          {editor.bio && (
            <>
              <h2 className="mt-8 mb-3 text-xs font-bold uppercase tracking-[0.09em] text-muted">Sobre</h2>
              <p className="max-w-[62ch] whitespace-pre-line">{editor.bio}</p>
            </>
          )}

          <div className="mt-3.5 flex flex-wrap gap-1.5">
            {editor.niches.map((n) => (
              <span key={n} className="rounded-md bg-accent-soft px-2.5 py-1 text-xs font-semibold text-accent-strong">
                {nicheLabel(n)}
              </span>
            ))}
            {editor.tools.map((t) => (
              <span key={t} className="rounded-md bg-panel px-2.5 py-1 text-xs font-semibold text-muted">
                {toolLabel(t)}
              </span>
            ))}
          </div>

          <h2 className="mt-8 mb-3 text-xs font-bold uppercase tracking-[0.09em] text-muted">Portfólio</h2>
          {editor.portfolio.length === 0 ? (
            <p className="text-muted">Este editor ainda não adicionou vídeos ao portfólio.</p>
          ) : (
            <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2">
              {editor.portfolio.map((item) => (
                <a
                  key={item.id}
                  href={item.videoUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="group flex flex-col gap-2"
                >
                  <div className="overflow-hidden rounded-2xl border border-line">
                    <VideoThumb url={item.videoUrl} alt={item.title} seed={item.id} />
                  </div>
                  <span className="text-sm font-semibold">{item.title}</span>
                </a>
              ))}
            </div>
          )}
        </div>

        <aside className="sticky top-[84px] flex flex-col gap-3.5 rounded-[18px] border border-line bg-card p-5">
          {priceRange && (
            <div className="text-2xl font-extrabold tracking-tight tabular-nums">
              {priceMin}
              <span className="text-sm font-medium text-muted"> / vídeo · a partir de</span>
            </div>
          )}
          <a
            href="#"
            className="w-full rounded-full bg-accent px-4 py-3 text-center text-sm font-semibold text-accent-contrast hover:bg-accent-strong"
          >
            Falar com {firstName}
          </a>
          <Row label="Nota">
            <span className="text-amber-400">★</span> {editor.ratingAvg.toFixed(1)} · {editor.ratingCount}{" "}
            {editor.ratingCount === 1 ? "avaliação" : "avaliações"}
          </Row>
          {turnaround && <Row label="Prazo médio">{turnaround}</Row>}
          {priceRange && <Row label="Faixa de preço">{priceRange}</Row>}
          <Row label="Vertical">{editor.vertical === "VIDEO" ? "Vídeo" : "Áudio"}</Row>
          <p className="text-center text-xs text-muted">Sem taxa pra falar. Vocês combinam direto.</p>
        </aside>
      </div>
    </div>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex justify-between border-t border-line py-2.5 text-sm">
      <span className="text-muted">{label}</span>
      <b className="font-semibold tabular-nums">{children}</b>
    </div>
  );
}
