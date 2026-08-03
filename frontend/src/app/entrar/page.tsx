import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { LoginForm } from "@/components/auth-forms";
import { AuthShell } from "@/components/auth-shell";
import { SocialLogin } from "@/components/social-login";
import { getCurrentUser } from "@/lib/session";

export const metadata: Metadata = {
  title: "Entrar — TramposEditor",
  description: "Acesse sua conta no TramposEditor.",
  // Tela de sessão não tem por que ser indexada.
  robots: { index: false },
};

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>;

export default async function EntrarPage({ searchParams }: { searchParams: SearchParams }) {
  if (await getCurrentUser()) redirect("/painel");

  // O login social que falha volta pra cá com o motivo — quem redireciona é o backend,
  // que não tem tela pra mostrar erro.
  const sp = await searchParams;
  const raw = sp.erro;
  const erro = Array.isArray(raw) ? raw[0] : raw;

  return (
    <AuthShell title="Bem-vindo de volta" subtitle="Entre pra continuar de onde parou.">
      <div className="flex flex-col gap-5">
        {erro && (
          <p role="alert" className="rounded-xl border border-busy/40 bg-busy/10 px-3.5 py-2.5 text-sm text-busy">
            {erro}
          </p>
        )}
        <LoginForm />
        <SocialLogin />
      </div>
    </AuthShell>
  );
}
