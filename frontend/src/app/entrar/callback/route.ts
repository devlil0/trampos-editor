import { redirect } from "next/navigation";
import type { NextRequest } from "next/server";
import { createSession } from "@/lib/session";
import type { AuthResponse } from "@/lib/types";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

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
 *
 * É Route Handler, e não `page.tsx`, porque grava cookie: o Next só permite `.set` em
 * Server Function ou Route Handler. Numa página o `fetch` de troca até funcionava, mas
 * `createSession` estourava — e como o código é de uso único, a tentativa seguinte já
 * vinha com "esse login expirou", escondendo a causa real.
 *
 * Como esta rota nunca renderiza UI, também não precisa de `metadata` com `noindex`:
 * não há HTML pra indexar.
 */
export async function GET(request: NextRequest) {
  const code = request.nextUrl.searchParams.get("code");

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

  // Conta nova por provedor entra sem username escolhido — não houve onde perguntar. Manda
  // pra escolha direto, em vez de deixar o guard do `/painel` fazer o desvio: a pessoa
  // chega na tela certa de primeira, sem um pisca-pisca de redirecionamento.
  redirect(auth.user.usernameChosen ? "/painel" : "/escolher-username");
}
