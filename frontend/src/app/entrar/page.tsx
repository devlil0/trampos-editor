import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { LoginForm } from "@/components/auth-forms";
import { AuthShell } from "@/components/auth-shell";
import { getCurrentUser } from "@/lib/session";

export const metadata: Metadata = {
  title: "Entrar — TramposEditor",
  description: "Acesse sua conta no TramposEditor.",
  // Tela de sessão não tem por que ser indexada.
  robots: { index: false },
};

export default async function EntrarPage() {
  if (await getCurrentUser()) redirect("/painel");

  return (
    <AuthShell title="Bem-vindo de volta" subtitle="Entre pra continuar de onde parou.">
      <LoginForm />
    </AuthShell>
  );
}
