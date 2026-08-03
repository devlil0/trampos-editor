import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { RegisterForm } from "@/components/auth-forms";
import { AuthShell } from "@/components/auth-shell";
import { SocialLogin } from "@/components/social-login";
import { getCurrentUser } from "@/lib/session";

export const metadata: Metadata = {
  title: "Criar conta — TramposEditor",
  description: "Crie sua conta no TramposEditor pra contratar ou editar vídeos.",
  robots: { index: false },
};

export default async function CriarContaPage() {
  if (await getCurrentUser()) redirect("/painel");

  return (
    <AuthShell
      title="Criar conta"
      subtitle="É de graça, leva um minuto e não pede cartão."
    >
      <div className="flex flex-col gap-5">
        <RegisterForm />
        {/* Mesmo componente do login: entrar pelo provedor num e-mail que já tem conta
            vincula em vez de duplicar, então as duas telas podem oferecer o mesmo botão. */}
        <SocialLogin />
      </div>
    </AuthShell>
  );
}
