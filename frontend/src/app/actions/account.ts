"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { getSessionToken } from "@/lib/session";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

export interface AccountFormState {
  error?: string;
  ok?: boolean;
}

async function readError(res: Response, fallback: string): Promise<string> {
  try {
    const body = await res.json();
    if (typeof body?.error === "string") return body.error;
    if (Array.isArray(body?.errors) && body.errors.length > 0) return body.errors.join(" · ");
  } catch {
    // sem corpo JSON
  }
  return fallback;
}

/**
 * Grava o Instagram da conta. Vale pros dois papéis: cliente e editor usam este mesmo
 * campo, porque a conta é uma só.
 *
 * O campo vai cru pro backend de propósito — é lá que "@fulano" e a URL colada do
 * navegador viram username, e validar aqui também criaria duas regras pra manter.
 * Enviar vazio apaga o que estava salvo.
 */
export async function saveInstagramAction(
  _prev: AccountFormState,
  formData: FormData,
): Promise<AccountFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre de novo." };

  const instagram = String(formData.get("instagram") ?? "").trim();

  try {
    const res = await fetch(`${API_URL}/v1/users/me`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ instagram: instagram || null }),
      cache: "no-store",
    });
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra salvar seu Instagram agora.") };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  // O painel mostra o valor salvo e o perfil público do editor exibe o @ pro visitante.
  revalidatePath("/painel");
  revalidatePath("/painel/meu-perfil");
  return { ok: true };
}

/**
 * Reenvia o link de confirmação de e-mail.
 *
 * Sempre "deu certo" quando o backend responde, inclusive pra conta já verificada: a
 * pessoa clicou num botão que promete mandar um e-mail, e explicar que ela não precisava
 * seria informação que não muda nada pra ela.
 */
export async function resendEmailVerificationAction(
  _prev: AccountFormState,
): Promise<AccountFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre de novo." };

  try {
    const res = await fetch(`${API_URL}/v1/users/me/email-verification/resend`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra reenviar o link agora.") };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  return { ok: true };
}

/**
 * Escolhe ou troca o username — o endereço da pessoa em `/{username}`.
 *
 * Serve os dois momentos: a escolha obrigatória de quem entrou pelo Google (que ainda não
 * tem nome próprio) e a troca nas configurações. A diferença entre eles é do backend, não
 * daqui — inclusive o cooldown de 14 dias, que volta como 409 com a frase pronta.
 *
 * Sucesso redireciona quando `redirectTo` vem preenchido, que é o caso do onboarding: a
 * pessoa estava barrada e precisa ser solta. Nas configurações não redireciona — ela está
 * onde queria estar.
 */
export async function saveUsernameAction(
  _prev: AccountFormState,
  formData: FormData,
): Promise<AccountFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre de novo." };

  const username = String(formData.get("username") ?? "").trim().toLowerCase();
  const redirectTo = String(formData.get("redirectTo") ?? "");

  if (!username) return { error: "Escolha um nome de usuário." };

  try {
    const res = await fetch(`${API_URL}/v1/users/me/username`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ username }),
      cache: "no-store",
    });
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra salvar seu nome de usuário agora.") };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  // O endereço antigo deixou de existir: qualquer tela que mostre `/{username}` — o
  // próprio perfil, as configurações, a barra — está com o valor velho em cache.
  revalidatePath("/", "layout");

  // redirect lança por dentro (NEXT_REDIRECT): tem que ficar fora do try/catch.
  if (redirectTo) redirect(redirectTo);
  return { ok: true };
}
