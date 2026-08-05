import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { BotaoConfiguracoes, EditorProfileView } from "@/components/editor-profile-view";
import { avatarUrl, fetchMyEditorProfile } from "@/lib/api";
import { getCurrentUser, getSessionToken } from "@/lib/session";

export const metadata: Metadata = {
  title: "Meu perfil — TramposEditor",
  robots: { index: false },
};

/**
 * Meu perfil — o editor vendo o próprio perfil **como o cliente vê**.
 *
 * Era um formulário (2026-08-04). O problema não era o formulário existir, era ele ser a
 * tela: o editor nunca via o que estava publicando, só os campos que o alimentam. Num
 * produto cuja tese é "o trabalho fala" (FUNDACAO §5.1), a pessoa precisa olhar a própria
 * vitrine com os olhos de quem contrata.
 *
 * O formulário virou `/painel/meu-perfil/configuracoes`, atrás da engrenagem — mesma
 * divisão que rede social faz entre perfil e ajustes.
 */
export default async function MeuPerfilPage() {
  // Sem trava de papel: publicar este perfil é o que torna a conta editora.
  const user = await getCurrentUser();
  if (!user) redirect("/entrar");

  const token = await getSessionToken();
  const profile = token ? await fetchMyEditorProfile(token) : null;

  return (
    <div className="mx-auto max-w-4xl px-6 pt-6 pb-16">
      <Link href="/editores" className="mb-8 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Voltar pro feed
      </Link>

      {profile ? (
        <>
          <EditorProfileView
            profile={profile}
            viewerLoggedIn
            isOwnProfile
            avatarUrl={avatarUrl(user.username, user.avatarVersion)}
            headerExtra={<BotaoConfiguracoes />}
          />

          <p className="mt-10 text-center text-sm text-muted">
            Seu endereço público é{" "}
            <Link href={`/editores/${profile.slug}`} className="font-semibold text-accent hover:underline">
              /editores/{profile.slug}
            </Link>
          </p>
        </>
      ) : (
        /* Sem perfil não há o que visualizar — aqui a tela é convite, não vitrine vazia. */
        <div className="rounded-[18px] border border-line bg-card p-8 text-center">
          <h1 className="text-2xl font-extrabold tracking-tight">Monte seu perfil</h1>
          <p className="mx-auto mt-2 max-w-[46ch] text-muted">
            Publicar este perfil é o que te registra como editor — sua conta continua a mesma. Leva
            dois minutos e dá pra editar depois.
          </p>
          <Link href="/painel/meu-perfil/configuracoes" className="btn btn-primary mt-6">
            Começar
          </Link>
        </div>
      )}
    </div>
  );
}
