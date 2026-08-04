import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { removePortfolioAction } from "@/app/actions/editor-profile";
import { AddPortfolioForm, EditorProfileForm } from "@/components/editor-profile-form";
import { InstagramForm } from "@/components/instagram-form";
import { ThemeToggle } from "@/components/theme-toggle";
import { VideoThumb } from "@/components/video-thumb";
import { fetchMyEditorProfile } from "@/lib/api";
import { getCurrentUser, getSessionToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "Configurações do perfil — TramposEditor",
  robots: { index: false },
};

/**
 * Configurações do perfil — os campos que alimentam o que `/painel/meu-perfil` mostra.
 *
 * Era a própria tela de "meu perfil" até 2026-08-04, quando o perfil passou a ser exibido
 * como o cliente o vê. A separação é a mesma que rede social faz: o perfil é o resultado,
 * as configurações são os controles. Chega pela engrenagem, não pelo menu.
 */
export default async function ConfiguracoesDoPerfilPage() {
  const user = await getCurrentUser();
  if (!user) redirect("/entrar");

  const token = await getSessionToken();
  const profile = token ? await fetchMyEditorProfile(token) : null;

  return (
    <div className="mx-auto max-w-3xl px-6 py-12">
      <Link
        href={profile ? "/painel/meu-perfil" : "/painel"}
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        {profile ? "Voltar pro meu perfil" : "Voltar pro painel"}
      </Link>

      <h1 className="text-3xl font-extrabold tracking-tight">
        {profile ? "Configurações do perfil" : "Monte seu perfil"}
      </h1>
      <p className="mt-1 text-muted">
        {profile ? (
          <>
            Isto alimenta o que aparece em{" "}
            <Link href={`/editores/${profile.slug}`} className="font-semibold text-accent hover:underline">
              /editores/{profile.slug}
            </Link>
            .
          </>
        ) : (
          "Publicar este perfil é o que te registra como editor — sua conta continua a mesma. Leva dois minutos e dá pra editar depois."
        )}
      </p>

      {/* Aparência primeiro e fora do bloco de editor: vale pra qualquer conta, inclusive
          quem só contrata e nunca vai preencher o perfil de editor abaixo. */}
      <section className="mt-8 rounded-[18px] border border-line bg-card p-6">
        <ThemeToggle />
      </section>

      <section className="mt-6 rounded-[18px] border border-line bg-card p-6">
        <EditorProfileForm profile={profile} />
      </section>

      {/* Formulário próprio, e não mais um campo lá em cima: o Instagram é da conta e
          salva por outro endpoint. Junto no mesmo submit, um erro de username faria o
          perfil inteiro parecer que não salvou. */}
      <section className="mt-6 rounded-[18px] border border-line bg-card p-6">
        <InstagramForm instagram={user.instagram} />
        <p className="mt-2.5 text-xs text-muted">
          Fica visível no seu perfil público — é onde o cliente vê seu trabalho do dia a dia.
        </p>
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
