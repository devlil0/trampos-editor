"use server";

import { fetchWithTimeout } from "@/lib/api-fetch";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { getSessionToken } from "@/lib/session";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

export interface DealFormState {
  error?: string;
  ok?: boolean;
}

async function readError(res: Response, fallback: string): Promise<string> {
  if (res.status === 401) return "Sua sessão expirou. Entre novamente.";
  if (res.status === 403) return "Você não tem permissão para fazer isso.";
  if (res.status >= 500) return fallback;
  try {
    const body = await res.json();
    if (typeof body?.error === "string") return body.error;
    if (Array.isArray(body?.errors) && body.errors.length > 0) return body.errors.join(" · ");
  } catch {
    // sem corpo JSON
  }
  return fallback;
}

/** Toda ação daqui é um POST autenticado que revalida as mesmas telas. */
async function post(path: string, body?: unknown, extraPaths: string[] = []): Promise<DealFormState> {
  const token = await getSessionToken();
  if (!token) redirect("/entrar");

  try {
    const res = await fetchWithTimeout(`${API_URL}${path}`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        ...(body ? { "Content-Type": "application/json" } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
      cache: "no-store",
    });
    if (!res.ok) return { error: await readError(res, "Não deu pra concluir agora.") };
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  revalidatePath("/painel/trampos-fechados");
  revalidatePath("/painel/conversas");
  for (const extra of extraPaths) revalidatePath(extra);
  return { ok: true };
}

/**
 * Abre o trampo dentro de uma conversa que veio da vitrine — o caminho que não tinha
 * fecho nenhum (ESBOCO §3.2). Quem veio do feed já ganha o trampo no aceite.
 */
export async function openDealAction(
  _prev: DealFormState,
  formData: FormData,
): Promise<DealFormState> {
  const conversationId = String(formData.get("conversationId") ?? "");
  const title = String(formData.get("title") ?? "").trim();

  if (!conversationId) return { error: "Conversa não identificada." };
  if (!title) return { error: "Diga do que é o trampo." };

  return post(`/v1/me/deals/from-conversation/${conversationId}`, { title });
}

/** O marco de "acabou": é ele que dispara o e-mail pedindo a nota pros dois lados. */
export async function markDealDoneAction(
  _prev: DealFormState,
  formData: FormData,
): Promise<DealFormState> {
  const dealId = String(formData.get("dealId") ?? "");
  if (!dealId) return { error: "Trampo não identificado." };

  return post(`/v1/me/deals/${dealId}/done`);
}

export async function cancelDealAction(
  _prev: DealFormState,
  formData: FormData,
): Promise<DealFormState> {
  const dealId = String(formData.get("dealId") ?? "");
  if (!dealId) return { error: "Trampo não identificado." };

  return post(`/v1/me/deals/${dealId}/cancel`);
}

/** A nota volta pro perfil e mexe na ordenação da vitrine (ESBOCO §2.1). */
export async function reviewDealAction(
  _prev: DealFormState,
  formData: FormData,
): Promise<DealFormState> {
  const dealId = String(formData.get("dealId") ?? "");
  const rating = Number(formData.get("rating") ?? 0);
  const comment = String(formData.get("comment") ?? "").trim();

  if (!dealId) return { error: "Trampo não identificado." };
  if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
    return { error: "Escolha uma nota de 1 a 5." };
  }

  return post(
    `/v1/me/deals/${dealId}/review`,
    {
      rating,
      // Comentário é opcional de propósito: exigir texto faria a avaliação parar de
      // acontecer, e a nota é o que alimenta a ordenação.
      comment: comment || null,
    },
    // A vitrine entra na revalidação porque a nota nova muda a ordem da página pública.
    ["/editores"],
  );
}
