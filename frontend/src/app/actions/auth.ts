"use server";

import { redirect } from "next/navigation";
import { createSession, destroySession } from "@/lib/session";
import type { AuthResponse, UserRole } from "@/lib/types";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

/** Estado devolvido pros formulários (useActionState). Sucesso redireciona, não retorna. */
export interface AuthFormState {
  error?: string;
  /** Ecoa o que o usuário digitou pra não perder o preenchimento no erro. */
  values?: { email?: string; displayName?: string };
}

/**
 * Traduz a resposta de erro do backend numa frase só.
 * O handler global devolve {"error": "..."} ou {"errors": ["campo: msg", ...]}.
 */
async function readError(res: Response, fallback: string): Promise<string> {
  try {
    const body = await res.json();
    if (typeof body?.error === "string") return body.error;
    if (Array.isArray(body?.errors) && body.errors.length > 0) return body.errors.join(" · ");
  } catch {
    // resposta sem corpo JSON
  }
  return fallback;
}

export async function loginAction(
  _prev: AuthFormState,
  formData: FormData,
): Promise<AuthFormState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  const values = { email };

  if (!email || !password) {
    return { error: "Preencha e-mail e senha.", values };
  }

  let auth: AuthResponse;
  try {
    const res = await fetch(`${API_URL}/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
      cache: "no-store",
    });

    if (res.status === 401) {
      return { error: "E-mail ou senha incorretos.", values };
    }
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra entrar agora. Tente de novo."), values };
    }
    auth = await res.json();
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos.", values };
  }

  await createSession(auth);
  // redirect lança por dentro (NEXT_REDIRECT): tem que ficar fora do try/catch.
  redirect("/painel");
}

export async function registerAction(
  _prev: AuthFormState,
  formData: FormData,
): Promise<AuthFormState> {
  const email = String(formData.get("email") ?? "").trim();
  const displayName = String(formData.get("displayName") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  const values = { email, displayName };

  // A conta é uma só. Todo mundo entra como CLIENT; virar editor é uma condição que a
  // pessoa assume depois, publicando o perfil de editor (o backend concede o papel lá).
  const roles: UserRole[] = ["CLIENT"];

  if (!email || !displayName || !password) {
    return { error: "Preencha todos os campos.", values };
  }
  if (password.length < 8) {
    return { error: "A senha precisa de pelo menos 8 caracteres.", values };
  }

  let auth: AuthResponse;
  try {
    const res = await fetch(`${API_URL}/v1/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password, displayName, roles }),
      cache: "no-store",
    });

    if (res.status === 409) {
      return { error: "Já existe uma conta com esse e-mail.", values };
    }
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra criar a conta agora."), values };
    }
    auth = await res.json();
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos.", values };
  }

  await createSession(auth);
  redirect("/painel");
}

export async function logoutAction(): Promise<void> {
  await destroySession();
  redirect("/");
}

/** Estado das telas de senha — `sent`/`done` porque aqui o sucesso não redireciona. */
export interface PasswordResetState {
  error?: string;
  sent?: boolean;
  done?: boolean;
  values?: { email?: string };
}

/**
 * Pede o link. A resposta é a mesma exista a conta ou não, e a tela diz isso na cara —
 * é o backend não entregando quais e-mails estão cadastrados (ESBOCO §7 item 6).
 */
export async function requestPasswordResetAction(
  _prev: PasswordResetState,
  formData: FormData,
): Promise<PasswordResetState> {
  const email = String(formData.get("email") ?? "").trim();
  const values = { email };

  if (!email) return { error: "Diga o e-mail da conta.", values };

  try {
    const res = await fetch(`${API_URL}/v1/auth/password-reset/request`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email }),
      cache: "no-store",
    });
    if (!res.ok) {
      return { error: await readError(res, "Não deu pra enviar o link agora."), values };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos.", values };
  }

  return { sent: true };
}

export async function confirmPasswordResetAction(
  _prev: PasswordResetState,
  formData: FormData,
): Promise<PasswordResetState> {
  const token = String(formData.get("token") ?? "");
  const password = String(formData.get("password") ?? "");
  const confirm = String(formData.get("confirm") ?? "");

  if (!token) return { error: "Link inválido. Peça outro." };
  if (password.length < 8) return { error: "A senha precisa de pelo menos 8 caracteres." };
  if (password !== confirm) return { error: "As duas senhas não batem." };

  try {
    const res = await fetch(`${API_URL}/v1/auth/password-reset/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token, password }),
      cache: "no-store",
    });
    if (!res.ok) {
      return { error: await readError(res, "Esse link expirou ou já foi usado. Peça outro.") };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  return { done: true };
}
