"use server";

import { revalidatePath } from "next/cache";
import { getSessionToken } from "@/lib/session";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

export interface AvatarFormState {
  error?: string;
  ok?: boolean;
  message?: string;
}

async function avatarError(res: Response, fallback: string): Promise<string> {
  // T07 vai padronizar o JSON. Hoje BusinessException usa error e multipart pode vir sem JSON.
  if (res.status === 413) return "A imagem precisa ter no máximo 5 MB.";
  const body = await res.json().catch(() => null);
  return typeof body?.error === "string" ? body.error : fallback;
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
      return { error: await avatarError(res, "Não deu pra salvar a foto agora.") };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  // O layout inteiro mostra a foto (barra de baixo), então revalida da raiz.
  revalidatePath("/", "layout");
  return { ok: true, message: "Foto atualizada." };
}

export async function removeAvatarAction(): Promise<AvatarFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre de novo." };

  try {
    const res = await fetch(`${API_URL}/v1/me/avatar`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    });
    if (!res.ok) return { error: await avatarError(res, "Não deu pra remover a foto agora.") };
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  revalidatePath("/", "layout");
  return { ok: true, message: "Foto removida." };
}
