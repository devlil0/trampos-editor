"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { getSessionToken } from "@/lib/session";
import type { EditorNiche } from "@/lib/types";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

export interface JobFormState {
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

export async function createJobRequestAction(
  _prev: JobFormState,
  formData: FormData,
): Promise<JobFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre de novo." };

  const title = String(formData.get("title") ?? "").trim();
  const description = String(formData.get("description") ?? "").trim();
  const niche = String(formData.get("niche") ?? "") as EditorNiche;

  if (!title || !description) return { error: "Título e descrição são obrigatórios." };
  if (!niche) return { error: "Escolha onde o vídeo vai ser publicado." };

  try {
    const res = await fetch(`${API_URL}/v1/me/job-requests`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({
        title,
        description,
        niche,
        budgetCents: toCents(formData.get("budget")),
        deadlineDays: toInt(formData.get("deadlineDays")),
        referenceUrl: String(formData.get("referenceUrl") ?? "").trim() || null,
      }),
      cache: "no-store",
    });
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra publicar o pedido agora.") };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  revalidatePath("/painel/pedidos");
  redirect("/painel/pedidos");
}

export async function expressInterestAction(
  _prev: JobFormState,
  formData: FormData,
): Promise<JobFormState> {
  const token = await getSessionToken();
  if (!token) return { error: "Sua sessão expirou. Entre de novo." };

  const jobId = String(formData.get("jobId") ?? "");
  const message = String(formData.get("message") ?? "").trim();
  if (!jobId || !message) return { error: "Escreva uma mensagem pro cliente." };

  try {
    const res = await fetch(`${API_URL}/v1/job-requests/${jobId}/interests`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ message }),
      cache: "no-store",
    });
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra enviar seu interesse agora.") };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  revalidatePath("/painel/trabalhos");
  return { ok: true };
}

export async function acceptInterestAction(formData: FormData): Promise<void> {
  const token = await getSessionToken();
  const jobId = String(formData.get("jobId") ?? "");
  const interestId = String(formData.get("interestId") ?? "");
  if (!token || !jobId || !interestId) return;

  await fetch(`${API_URL}/v1/me/job-requests/${jobId}/interests/${interestId}/accept`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });

  revalidatePath("/painel/pedidos");
}

export async function closeJobRequestAction(formData: FormData): Promise<void> {
  const token = await getSessionToken();
  const jobId = String(formData.get("jobId") ?? "");
  if (!token || !jobId) return;

  await fetch(`${API_URL}/v1/me/job-requests/${jobId}/close`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });

  revalidatePath("/painel/pedidos");
}
