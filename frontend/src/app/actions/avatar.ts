"use server";

import { revalidatePath } from "next/cache";
import { getSessionToken } from "@/lib/session";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

export interface AvatarFormState {
  error?: string;
  ok?: boolean;
}

/**
 * Sobe a foto de perfil.
 *
 * O arquivo vai do browser pro servidor do Next e de lá pro backend — nunca direto: o JWT
 * mora num cookie httpOnly e o browser não o vê, então quem tem como autenticar a chamada
 * é o servidor. É o mesmo caminho de todo o resto do produto.
 *
 * A validação de verdade (é imagem? cabe no limite?) é do backend, que reencoda a imagem.
 * O que se checa aqui é só o que evita uma ida inútil ao servidor.
 */
export async function uploadAvatarAction(
  _prev: AvatarFormState,
  formData: FormData,
): Promise<AvatarFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre de novo." };

  const file = formData.get("file");
  if (!(file instanceof File) || file.size === 0) {
    return { error: "Escolha uma imagem." };
  }
  if (file.size > 5 * 1024 * 1024) {
    return { error: "A imagem precisa ter no máximo 5 MB." };
  }

  const upstream = new FormData();
  upstream.append("file", file);

  try {
    const res = await fetch(`${API_URL}/v1/me/avatar`, {
      method: "PUT",
      headers: { Authorization: `Bearer ${token}` },
      body: upstream,
      cache: "no-store",
    });
    if (!res.ok) {
      const corpo = await res.json().catch(() => null);
      return { error: corpo?.message ?? "Não deu pra salvar a foto agora." };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  // O layout inteiro mostra a foto (barra de baixo), então revalida da raiz.
  revalidatePath("/", "layout");
  return { ok: true };
}

export async function removeAvatarAction(): Promise<void> {
  const token = await getSessionToken();
  if (!token) return;

  await fetch(`${API_URL}/v1/me/avatar`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  }).catch(() => null);

  revalidatePath("/", "layout");
}
