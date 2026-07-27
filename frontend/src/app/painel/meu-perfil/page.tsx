import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { removePortfolioAction } from "@/app/actions/editor-profile";
import { AddPortfolioForm, EditorProfileForm } from "@/components/editor-profile-form";
import { VideoThumb } from "@/components/video-thumb";
import { fetchMyEditorProfile } from "@/lib/api";
import { getCurrentUser, getSessionToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "Meu perfil — TramposEditor",
  robots: { index: false },
};

export default async function MeuPerfilPage() {
  // Sem trava de papel: publicar este perfil é o que torna a conta editora.
  const user = await getCurrentUser();
  if (!user) redirect("/entrar");

  const token = await getSessionToken();
  const profile = token ? await fetchMyEditorProfile(token) : null;

  return (
    <div className="mx-auto max-w-3xl px-6 py-12">
      <Link href="/painel" className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Voltar pro painel
      </Link>

      <h1 className="text-3xl font-extrabold tracking-tight">
        {profile ? "Meu perfil" : "Monte seu perfil"}
      </h1>
      <p className="mt-1 text-muted">
        {profile ? (
          <>
            É isso que o cliente vê em{" "}
            <Link href={`/editores/${profile.slug}`} className="font-semibold text-accent hover:underline">
              /editores/{profile.slug}
            </Link>
            .
          </>
        ) : (
          "Publicar este perfil é o que te registra como editor — sua conta continua a mesma. Leva dois minutos e dá pra editar depois."
        )}
      </p>

      <section className="mt-8 rounded-[18px] border border-line bg-card p-6">
        <EditorProfileForm profile={profile} />
      </section>

      {/* Portfólio só existe depois que o perfil existe — é ele que dá o id no backend. */}
      {profile && (
        <section className="mt-6 rounded-[18px] border border-line bg-card p-6">
          <h2 className="text-lg font-bold tracking-tight">Portfólio</h2>
          <p className="mt-1 mb-5 text-sm text-muted">
            O primeiro vídeo vira a capa do seu card na vitrine.
          </p>

          <AddPortfolioForm />

          {profile.portfolio.length > 0 && (
            <ul className="mt-6 grid gap-3.5 sm:grid-cols-2">
              {profile.portfolio.map((item) => (
                <li key={item.id} className="flex flex-col gap-2">
                  <div className="overflow-hidden rounded-2xl border border-line">
                    <VideoThumb url={item.videoUrl} alt={item.title} seed={item.id} />
                  </div>
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-sm font-semibold">{item.title}</span>
                    <form action={removePortfolioAction}>
                      <input type="hidden" name="itemId" value={item.id} />
                      <button
                        type="submit"
                        className="text-xs font-semibold text-muted hover:text-busy"
                      >
                        Remover
                      </button>
                    </form>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
}
