"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { getSessionToken } from "@/lib/session";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

export interface ConversationFormState {
  error?: string;
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
 * Porta 1 — o "Falar com" do perfil na vitrine.
 *
 * Redireciona pra conversa aberta em vez de voltar pro perfil: quem acabou de
 * escrever quer ver a mensagem enviada, não a página de onde saiu.
 */
export async function startConversationAction(
  _prev: ConversationFormState,
  formData: FormData,
): Promise<ConversationFormState> {
  const token = await getSessionToken();
  const slug = String(formData.get("slug") ?? "");

  // Deslogado: manda pro login carregando o destino, pra voltar pro lugar certo.
  if (!token) redirect(`/entrar?next=${encodeURIComponent(`/editores/${slug}`)}`);

  const body = String(formData.get("body") ?? "").trim();
  if (!slug) return { error: "Editor não identificado." };
  if (!body) return { error: "Escreva uma mensagem antes de enviar." };

  let conversationId: string;
  try {
    const res = await fetch(`${API_URL}/v1/me/conversations/with-editor/${encodeURIComponent(slug)}`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ body }),
      cache: "no-store",
    });
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra abrir a conversa agora.") };
    }
    conversationId = (await res.json()).id;
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  revalidatePath("/painel/conversas");
  redirect(`/painel/conversas/${conversationId}`);
}

export async function sendMessageAction(
  _prev: ConversationFormState,
  formData: FormData,
): Promise<ConversationFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre de novo." };

  const conversationId = String(formData.get("conversationId") ?? "");
  const body = String(formData.get("body") ?? "").trim();
  if (!conversationId) return { error: "Conversa não identificada." };
  if (!body) return { error: "Escreva alguma coisa antes de enviar." };

  try {
    const res = await fetch(`${API_URL}/v1/me/conversations/${conversationId}/messages`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ body }),
      cache: "no-store",
    });
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra enviar a mensagem agora.") };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  // Sem WebSocket: a própria revalidação repinta a conversa com a mensagem nova.
  // É o mesmo padrão do "Aceitar" em /painel/pedidos e basta nesta escala.
  revalidatePath(`/painel/conversas/${conversationId}`);
  revalidatePath("/painel/conversas");
  return {};
}
