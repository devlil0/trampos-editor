"use server";

import { fetchWithTimeout } from "@/lib/api-fetch";
import { revalidatePath } from "next/cache";
import { getSessionToken } from "@/lib/session";
import type { EditorNiche, EditorStyle, EditorTool } from "@/lib/types";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

export interface ProfileFormState {
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

/** "80" ou "80,50" -> centavos. Campo vazio vira null (o backend aceita ausente). */
function toCents(raw: FormDataEntryValue | null): number | null {
  const text = String(raw ?? "").trim().replace(",", ".");
  if (!text) return null;
  const value = Number(text);
  if (!Number.isFinite(value) || value < 0) return null;
  return Math.round(value * 100);
}

function toInt(raw: FormDataEntryValue | null): number | null {
  const text = String(raw ?? "").trim();
  if (!text) return null;
  const value = Number.parseInt(text, 10);
  return Number.isFinite(value) && value >= 0 ? value : null;
}

export async function saveProfileAction(
  _prev: ProfileFormState,
  formData: FormData,
): Promise<ProfileFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre de novo." };

  const niches = formData.getAll("niches").map(String) as EditorNiche[];
  const tools = formData.getAll("tools").map(String) as EditorTool[];
  const styles = formData.getAll("styles").map(String) as EditorStyle[];

  if (niches.length === 0) return { error: "Escolha pelo menos uma plataforma que você atende." };
  if (tools.length === 0) return { error: "Escolha pelo menos uma ferramenta que você usa." };

  const priceMinCents = toCents(formData.get("priceMin"));
  const priceMaxCents = toCents(formData.get("priceMax"));
  if (priceMinCents != null && priceMaxCents != null && priceMaxCents < priceMinCents) {
    return { error: "O preço máximo não pode ser menor que o mínimo." };
  }

  const payload = {
    headline: String(formData.get("headline") ?? "").trim() || null,
    bio: String(formData.get("bio") ?? "").trim() || null,
    niches,
    tools,
    styles,
    priceMinCents,
    priceMaxCents,
    turnaroundDays: toInt(formData.get("turnaroundDays")),
    available: formData.get("available") === "on",
  };

  try {
    const res = await fetchWithTimeout(`${API_URL}/v1/me/editor-profile`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify(payload),
      cache: "no-store",
    });
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra salvar o perfil agora.") };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  // A vitrine e o perfil público leem do servidor a cada request, mas o painel
  // precisa refletir na hora o que acabou de ser salvo.
  revalidatePath("/painel/meu-perfil");
  return { ok: true };
}

export async function addPortfolioAction(
  _prev: ProfileFormState,
  formData: FormData,
): Promise<ProfileFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre de novo." };

  const title = String(formData.get("title") ?? "").trim();
  const videoUrl = String(formData.get("videoUrl") ?? "").trim();
  if (!title || !videoUrl) return { error: "Título e link do vídeo são obrigatórios." };

  try {
    const res = await fetchWithTimeout(`${API_URL}/v1/me/editor-profile/portfolio`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ title, videoUrl }),
      cache: "no-store",
    });
    if (!res.ok) {
      return {
        error: await readError(res, "Link inválido. Aceitamos YouTube e Vimeo."),
      };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  revalidatePath("/painel/meu-perfil");
  return { ok: true };
}

export async function removePortfolioAction(
  _prev: ProfileFormState,
  formData: FormData,
): Promise<ProfileFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre novamente." };
  const itemId = String(formData.get("itemId") ?? "");
  if (!itemId) return { error: "Vídeo não encontrado. Atualize a página." };

  try {
    const res = await fetchWithTimeout(API_URL + "/v1/me/editor-profile/portfolio/" + encodeURIComponent(itemId), {
      method: "DELETE",
      headers: { Authorization: "Bearer " + token },
      cache: "no-store",
    });
    if (!res.ok) return { error: await readError(res, "Não foi possível remover o vídeo. Tente novamente.") };
  } catch {
    return { error: "Não foi possível confirmar a remoção. Atualize a página antes de tentar novamente." };
  }

  revalidatePath("/painel/meu-perfil", "layout");
  return { ok: true };
}
