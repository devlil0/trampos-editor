import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ClientProfileView } from "@/components/client-profile-view";
import { EditorProfileView } from "@/components/editor-profile-view";
import { avatarUrl, fetchMyEditorProfile, fetchPublicProfile } from "@/lib/api";
import { getCurrentUser, getSessionToken } from "@/lib/session";

type Params = Promise<{ username: string }>;

export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const { username } = await params;
  const perfil = await fetchPublicProfile(username);
  if (!perfil) return { title: "Perfil não encontrado — TramposEditor" };

  const editor = perfil.editor;
  return {
    title: editor
      ? `${perfil.displayName} — editor de vídeo | TramposEditor`
      : `${perfil.displayName} (@${perfil.username}) | TramposEditor`,
    description:
      editor?.headline ??
      editor?.bio ??
      `Perfil de ${perfil.displayName} no TramposEditor.`,
    alternates: { canonical: `/${perfil.username}` },
    // Só editor entra no índice. Página de cliente é magra por natureza — indexá-la
    // encheria o site de páginas sem conteúdo, o mesmo erro que o limiar das tags evita.
    robots: editor ? undefined : { index: false, follow: true },
  };
}

/**
 * A página de uma pessoa: `/{username}`.
 *
 * A raiz virou namespace de usuário (2026-08-04), como em rede social. Isso tem um preço
 * assumido: toda rota do produto passa a ser nome reservado, pra sempre — a lista mora em
 * `UsernameService.RESERVED`, no backend, e é o que impede alguém chamado "entrar" de
 * tornar a tela de login inalcançável.
 *
 * Editor e cliente compartilham a rota e divergem na view: quem publicou perfil de editor
 * tem obra pra mostrar, quem só contrata tem histórico. `perfil.editor` nulo é o que
 * separa os dois — não há campo de tipo.
 */
export default async function PerfilPage({ params }: { params: Params }) {
  const { username } = await params;

  const [perfil, viewer, token] = await Promise.all([
    fetchPublicProfile(username),
    getCurrentUser(),
    getSessionToken(),
  ]);

  if (!perfil) notFound();

  const meuPerfil = token ? await fetchMyEditorProfile(token) : null;
  // Compara por username, que é único na conta — nome de exibição se repete, e o slug do
  // editor pode ter divergido do username depois de uma troca.
  const souEu = viewer?.username === perfil.username || meuPerfil?.slug === perfil.editor?.slug;

  return (
    <div className="mx-auto max-w-4xl px-6 pt-6 pb-16">
      <Link href="/editores" className="mb-8 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Ver todos os editores
      </Link>

      {perfil.editor ? (
        <EditorProfileView
          profile={perfil.editor}
          viewerLoggedIn={viewer != null}
          isOwnProfile={souEu}
          avatarUrl={avatarUrl(perfil.username, perfil.avatarVersion)}
        />
      ) : (
        <ClientProfileView
          perfil={perfil}
          isOwnProfile={souEu}
          avatarUrl={avatarUrl(perfil.username, perfil.avatarVersion)}
        />
      )}
    </div>
  );
}
