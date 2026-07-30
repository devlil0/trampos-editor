"use server";

import { revalidatePath } from "next/cache";
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
