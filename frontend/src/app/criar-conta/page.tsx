import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { RegisterForm } from "@/components/auth-forms";
import { AuthShell } from "@/components/auth-shell";
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
      <RegisterForm />
    </AuthShell>
  );
}
