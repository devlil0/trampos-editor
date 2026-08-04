import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { fetchEditorBySlug, fetchMyEditorProfile } from "@/lib/api";
import { EditorProfileView } from "@/components/editor-profile-view";
import { TagPage, contarEditoresDaTag, deveIndexar } from "@/components/tag-page";
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

  return (
    <div className="mx-auto max-w-4xl px-6 pt-6 pb-16">
      <Link href="/editores" className="mb-8 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Voltar pra vitrine
      </Link>

      <EditorProfileView profile={editor} viewerLoggedIn={viewer != null} isOwnProfile={isOwnProfile} />
    </div>
  );
}

