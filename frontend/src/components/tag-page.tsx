import Link from "next/link";
import { EditorCard } from "@/components/editor-card";
import { fetchEditors } from "@/lib/api";
import { deveIndexar, TAGS, type Tag } from "@/lib/tags";

/**
 * Uma página de tag: `/editores/tiktok`, `/editores/premiere`.
 *
 * Renderiza a mesma lista da vitrine, mas com endereço, título e texto próprios — que é o
 * que a torna indexável (ver `lib/tags.ts`). O link pra vitrine filtrada fica visível de
 * propósito: quem chegou pelo Google e quer refinar deve cair no lugar onde o filtro mora,
 * em vez de a tag page começar a crescer filtros e virar uma segunda vitrine.
 */
export async function TagPage({ tag }: { tag: Tag }) {
  const result = await fetchEditors({ [tag.tipo]: tag.valor });
  const vitrineHref = `/editores?${tag.tipo}=${tag.valor}`;

  return (
    <div className="mx-auto max-w-6xl px-6">
      <section className="pt-14 pb-6">
        <Link href="/editores" className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <path d="m15 18-6-6 6-6" />
          </svg>
          Ver todos os editores
        </Link>

        <h1 className="max-w-[20ch] text-balance text-4xl font-extrabold leading-[1.04] tracking-tight sm:text-5xl">
          {tag.h1}
        </h1>
        <p className="mt-3.5 max-w-[58ch] text-lg text-muted">{tag.texto}</p>
      </section>

      <section>
        <div className="flex flex-wrap items-center justify-between gap-3 py-2 text-sm text-muted">
          <span>
            <b className="text-fg tabular-nums">{result.totalElements}</b>{" "}
            {result.totalElements === 1 ? "editor encontrado" : "editores encontrados"}
          </span>
          {result.totalElements > 0 && (
            <Link href={vitrineHref} className="hover:text-accent">
              Filtrar por preço e estilo →
            </Link>
          )}
        </div>

        {result.content.length === 0 ? (
          // Estado vazio que oferece saída em vez de só constatar: quem chegou do Google
          // procurando isto não pode bater num beco.
          <div className="my-16 text-center">
            <p className="text-lg font-semibold">Ainda não há editor com essa tag.</p>
            <p className="mt-1 text-muted">
              Estamos trazendo editores todo dia. Enquanto isso, veja quem já está na vitrine.
            </p>
            <Link href="/editores" className="btn btn-primary mt-5">
              Ver todos os editores
            </Link>
          </div>
        ) : (
          <div className="flex flex-col gap-3.5 pt-3">
            {result.content.map((editor) => (
              <EditorCard key={editor.id} editor={editor} />
            ))}
          </div>
        )}

        {/* Links entre tags: dão ao Google como descobrir as outras páginas sem depender
            só do sitemap, e a quem chegou, um segundo caminho quando esta não serve. */}
        <nav aria-label="Outras especialidades" className="border-t border-line py-8 mt-10 mb-16">
          <h2 className="mb-3 font-mono text-[11px] font-semibold tracking-[0.14em] text-fg">
            OUTRAS ESPECIALIDADES
          </h2>
          <div className="flex flex-wrap gap-2">
            {TAGS.filter((outra) => outra.slug !== tag.slug).map((outra) => (
              <Link
                key={outra.slug}
                href={`/editores/${outra.slug}`}
                className="rounded-full border border-line px-3 py-1.5 text-sm hover:border-accent"
              >
                {outra.h1.replace(/^Editores (de vídeo )?(para |que trabalham no )?/, "")}
              </Link>
            ))}
          </div>
        </nav>
      </section>
    </div>
  );
}

/** Quantos editores a tag tem — usado pelo `generateMetadata` pra decidir o `noindex`. */
export async function contarEditoresDaTag(tag: Tag): Promise<number> {
  // `size=1` porque só o total interessa aqui; a lista vem na renderização da página.
  const result = await fetchEditors({ [tag.tipo]: tag.valor, size: "1" });
  return result.totalElements;
}

export { deveIndexar };
