import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { EditorCard } from "@/components/editor-card";
import { CategoryChips, FeedPager } from "@/components/feed-nav";
import { fetchEditorFeed } from "@/lib/api";
import { getCurrentUser, getSessionToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "Editores disponíveis — TramposEditor",
  robots: { index: false },
};

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>;

function pick(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

/**
 * Feed da oferta — quem está livre pra pegar trampo agora. Par de `/painel/trampos`.
 *
 * Não substitui a vitrine (`/editores`): aquela é pública, indexável e mostra todo mundo,
 * porque é o funil de aquisição. Esta é pra quem já tem conta e quer saber o que tem hoje.
 */
export default async function FeedEditoresPage({ searchParams }: { searchParams: SearchParams }) {
  if (!(await getCurrentUser())) redirect("/entrar");

  const token = await getSessionToken();
  if (!token) redirect("/entrar");

  const sp = await searchParams;
  const niche = pick(sp.niche);
  const page = pick(sp.page);

  const feed = await fetchEditorFeed(token, { niche, page });

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <Link
        href="/painel"
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          aria-hidden="true"
        >
          <path d="m15 18-6-6 6-6" />
        </svg>
        Voltar pro painel
      </Link>

      <h1 className="text-3xl font-extrabold tracking-tight">Editores disponíveis</h1>
      <p className="mt-1 text-muted">
        Quem está livre pra pegar trampo agora. Abra o perfil, veja o corte e fale direto — sem
        taxa pra conversar.
      </p>

      <section className="mt-8">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-xs font-bold uppercase tracking-[0.09em] text-muted">
            Livres agora
          </h2>
          <span className="text-sm text-muted tabular-nums">
            <b className="text-fg">{feed.totalElements}</b>{" "}
            {feed.totalElements === 1 ? "editor" : "editores"}
          </span>
        </div>

        <div className="mb-6">
          <CategoryChips basePath="/painel/editores" active={niche} />
        </div>

        {feed.content.length === 0 ? (
          <p className="rounded-[18px] border border-line bg-card p-6 text-muted">
            {niche
              ? "Ninguém livre nessa categoria agora. Tente outra — ou "
              : "Ninguém marcado como disponível agora. Isso muda o dia inteiro; enquanto isso, "}
            <Link href="/editores" className="font-semibold text-accent hover:underline">
              veja a vitrine inteira
            </Link>
            , que mostra também quem está ocupado.
          </p>
        ) : (
          <div className="flex flex-col gap-5">
            {feed.content.map((editor) => (
              <EditorCard key={editor.id} editor={editor} />
            ))}
          </div>
        )}

        <FeedPager basePath="/painel/editores" page={feed} niche={niche} />
      </section>

      <p className="mt-10 text-center text-sm text-muted">
        Precisa de mais gente pra escolher?{" "}
        <Link href="/editores" className="font-semibold text-accent hover:underline">
          A vitrine
        </Link>{" "}
        tem filtro por ferramenta, estilo e preço.
      </p>
    </div>
  );
}
