import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { fetchEditorBySlug, fetchMyEditorProfile } from "@/lib/api";
import { StartConversationForm } from "@/components/conversation-forms";
import { InstagramLink } from "@/components/instagram-link";
import { NicheTag, StyleTag, ToolTag } from "@/components/niche-tag";
import { VideoThumb } from "@/components/video-thumb";
import { TagPage, contarEditoresDaTag, deveIndexar } from "@/components/tag-page";
import { formatPrice, formatTurnaround } from "@/lib/format";
import { getCurrentUser, getSessionToken } from "@/lib/session";
import { acharTag } from "@/lib/tags";

type Params = Promise<{ slug: string }>;

export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const { slug } = await params;

  const tag = acharTag(slug);
  if (tag) {
    const total = await contarEditoresDaTag(tag);
    return {
      title: tag.titulo,
      description: tag.descricao,
      alternates: { canonical: `/editores/${tag.slug}` },
      // Tag sem estoque continua navegável, mas não entra no índice: página magra
      // indexada não sai mais de lá. Ver `deveIndexar` em lib/tags.ts.
      robots: deveIndexar(total) ? undefined : { index: false, follow: true },
    };
  }

  const editor = await fetchEditorBySlug(slug);
  if (!editor) return { title: "Editor não encontrado — TramposEditor" };
  return {
    title: `${editor.displayName} — editor de vídeo | TramposEditor`,
    description: editor.headline ?? editor.bio ?? `Perfil de ${editor.displayName} no TramposEditor.`,
    alternates: { canonical: `/editores/${editor.slug}` },
  };
}

/**
 * Duas páginas moram nesta rota: o perfil do editor e a página de tag
 * (`/editores/tiktok`). É um segmento dinâmico só porque o Next não permite dois no mesmo
 * nível — e a URL plana era o desenho pedido no ESBOCO §2.
 *
 * A tag vem primeiro na checagem, e o slug dela é reservado no cadastro do perfil, senão
 * um editor chamado "CapCut" tomaria a página de uma ferramenta.
 */
export default async function EditorProfilePage({ params }: { params: Params }) {
  const { slug } = await params;

  const tag = acharTag(slug);
  if (tag) return <TagPage tag={tag} />;

  const [editor, viewer, token] = await Promise.all([
    fetchEditorBySlug(slug),
    getCurrentUser(),
    getSessionToken(),
  ]);

  if (!editor) notFound();

  // Quem está vendo o próprio perfil não recebe o campo de contato: o backend recusa
  // conversa consigo mesmo, e mostrar o formulário só pra falhar seria pegadinha.
  // A comparação é por slug, que é único — nome de exibição se repete.
  const meuPerfil = token ? await fetchMyEditorProfile(token) : null;
  const isOwnProfile = meuPerfil?.slug === editor.slug;

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
      <Link href="/editores" className="mt-6 mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
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
              <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1.5 text-sm">
                <span
                  className={`inline-flex items-center gap-1.5 font-semibold ${
                    editor.available ? "text-good" : "text-busy"
                  }`}
                >
                  <span className={`h-1.5 w-1.5 rounded-full ${editor.available ? "bg-good" : "bg-busy"}`} />
                  {editor.available ? "Disponível pra novos projetos" : "Sem vaga no momento"}
                </span>
                {editor.instagram && <InstagramLink username={editor.instagram} />}
              </div>
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
              <NicheTag key={n} niche={n} />
            ))}
            {editor.styles.map((s) => (
              <StyleTag key={s} style={s} />
            ))}
            {editor.tools.map((t) => (
              <ToolTag key={t} tool={t} />
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
          {isOwnProfile ? (
            <Link href="/painel/meu-perfil" className="btn btn-ghost btn-block btn-lg">
              Este perfil é seu — editar
            </Link>
          ) : (
            <StartConversationForm slug={editor.slug} firstName={firstName} loggedIn={viewer != null} />
          )}
          {/* Perfil sem nota não mostra linha de nota — dizer "★ 0.0" seria pior que
              não dizer nada (ESBOCO §7 item 5). */}
          {editor.ratingCount > 0 && (
            <Row label="Nota">
              <span className="text-amber-400">★</span> {editor.ratingAvg.toFixed(1)} · {editor.ratingCount}{" "}
              {editor.ratingCount === 1 ? "avaliação" : "avaliações"}
            </Row>
          )}
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
