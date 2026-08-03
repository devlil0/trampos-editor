import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { createSession } from "@/lib/session";
import type { AuthResponse } from "@/lib/types";

export const metadata: Metadata = {
  title: "Entrando… — TramposEditor",
  robots: { index: false },
};

const API_URL = process.env.API_URL ?? "http://localhost:8080";

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>;

function falhou(motivo: string): never {
  redirect("/entrar?erro=" + encodeURIComponent(motivo));
}

/**
 * Fim do login social. O backend redireciona pra cá com um código de uso único; aqui,
 * no servidor, ele vira sessão.
 *
 * O código existe pra que o JWT nunca apareça numa URL — URL vaza em log de servidor,
 * histórico e Referer. Esta troca é servidor a servidor e o browser continua só com o
 * cookie httpOnly, igual ao login por senha.
 */
export default async function CallbackPage({ searchParams }: { searchParams: SearchParams }) {
  const sp = await searchParams;
  const raw = sp.code;
  const code = Array.isArray(raw) ? raw[0] : raw;

  if (!code) falhou("Login não completou. Tente de novo.");

  // Nada de `redirect()` dentro do try: ele sinaliza por exceção, e o catch daqui
  // engoliria o redirecionamento e trocaria a mensagem por "servidor fora do ar".
  let auth: AuthResponse | null = null;
  let erro: string | null = null;

  try {
    const res = await fetch(`${API_URL}/v1/auth/social/exchange`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code }),
      cache: "no-store",
    });
    if (res.ok) {
      auth = await res.json();
    } else {
      erro = "Esse login expirou. Tente de novo.";
    }
  } catch {
    erro = "Servidor fora do ar. Tente de novo em alguns segundos.";
  }

  if (!auth) falhou(erro ?? "Login não completou. Tente de novo.");

  await createSession(auth);
  redirect("/painel");
}
