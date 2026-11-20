"use server";

import { fetchWithTimeout } from "@/lib/api-fetch";
import { redirect } from "next/navigation";
import { createSession, destroySession } from "@/lib/session";
import { readSignupOrigin } from "@/lib/signup-origin";
import type { AuthResponse, UserRole } from "@/lib/types";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

/** Estado devolvido pros formulários (useActionState). Sucesso redireciona, não retorna. */
export interface AuthFormState {
  error?: string;
  /** Ecoa o que o usuário digitou pra não perder o preenchimento no erro. */
  values?: { email?: string; displayName?: string; username?: string };
  /**
   * O cadastro passou do passo 1 e o código saiu (V18).
   * <p>
   * O sucesso do cadastro deixou de ser um redirecionamento: agora ele é um estado, porque
   * a pessoa continua na mesma tela pra digitar o código. É este campo que faz a tela
   * trocar de passo.
   */
  codeSent?: string;
  notice?: string;
  /** Prazo calculado a partir do Retry-After em segundos da T05. */
  retryAt?: number;
}

/**
 * Traduz a resposta de erro do backend numa frase só.
 * O handler global devolve {"error": "..."} ou {"errors": ["campo: msg", ...]}.
 */
async function readError(res: Response, fallback: string): Promise<Pick<AuthFormState, "error" | "retryAt">> {
  let error = fallback;
  if (res.status === 503) error = "Não foi possível enviar o e-mail agora. Tente novamente mais tarde.";
  if (res.status === 429) error = "Muitas tentativas. Aguarde e tente novamente.";
  try {
    const body = await res.json();
    if (typeof body?.error === "string") error = body.error;
    else if (Array.isArray(body?.errors) && body.errors.length > 0) {
      error = body.errors.filter((message: unknown) => typeof message === "string").join(" · ") || error;
    }
  } catch {
    // resposta sem corpo JSON
  }
  const rawDelay = res.headers.get("Retry-After");
  const seconds = rawDelay && /^\d+$/.test(rawDelay) ? Number(rawDelay) : 0;
  const retryAt = Date.now() + seconds * 1000;
  return {
    error,
    retryAt: res.status === 429 && seconds > 0 && Number.isSafeInteger(retryAt) ? retryAt : undefined,
  };
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
    const res = await fetchWithTimeout(`${API_URL}/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
      cache: "no-store",
    });

    if (res.status === 401) {
      return { error: "E-mail ou senha incorretos.", values };
    }
    if (!res.ok) {
      return { ...await readError(res, "Não deu pra entrar agora. Tente de novo."), values };
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
  const username = String(formData.get("username") ?? "").trim().toLowerCase();
  const password = String(formData.get("password") ?? "");
  const passwordConfirm = String(formData.get("passwordConfirm") ?? "");
  const values = { email, displayName, username };

  // A conta é uma só. Todo mundo entra como CLIENT; virar editor é uma condição que a
  // pessoa assume depois, publicando o perfil de editor (o backend concede o papel lá).
  const roles: UserRole[] = ["CLIENT"];

  if (!email || !displayName || !username || !password) {
    return { error: "Preencha todos os campos.", values };
  }
  if (password.length < 8) {
    return { error: "A senha precisa de pelo menos 8 caracteres.", values };
  }
  // Conferida aqui e não só no browser: o campo do HTML é conveniência, a Server Action é a
  // borda. Não vai pro backend de propósito — repetir a senha protege contra erro de
  // digitação de quem está cadastrando, e isso se resolve antes da rede.
  if (password !== passwordConfirm) {
    return { error: "As senhas não são iguais.", values };
  }
  // O formato do username não é checado aqui: quem tem a regra é o UsernameService do
  // backend, e é ele que responde a checagem que rodou enquanto a pessoa digitava. Uma
  // segunda cópia aqui poderia recusar um nome que o campo tinha acabado de aprovar.

  try {
    const res = await fetchWithTimeout(`${API_URL}/v1/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      // A origem da primeira visita (§3.1). Vai como campo opcional e nunca pode fazer o
      // cadastro falhar — o backend trunca o que for grande e ignora o que for nulo.
      body: JSON.stringify({
        email,
        password,
        displayName,
        username,
        roles,
        origin: await readSignupOrigin(),
      }),
      cache: "no-store",
    });

    // O 409 deixou de ter uma causa só quando o username entrou no formulário: pode ser
    // e-mail repetido ou nome já tomado. A frase do backend distingue os dois, então
    // repassar é mais útil que a mensagem fixa que estava aqui.
    if (res.status === 409) {
      return { ...await readError(res, "Já existe uma conta com esse e-mail."), values };
    }
    if (!res.ok) {
      return { ...await readError(res, "Não deu pra criar a conta agora."), values };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos.", values };
  }

  // Nem sessão nem redirecionamento: a conta ainda não existe. O que existe é um cadastro
  // pendente esperando o código — e é a tela que muda de passo.
  return { codeSent: email, values };
}

/**
 * Passo 2 do cadastro (V18): devolve o código e ganha a conta.
 *
 * É aqui que a sessão finalmente nasce, e por isso é aqui que o redirecionamento voltou.
 */
export async function confirmSignupAction(
  _prev: AuthFormState,
  formData: FormData,
): Promise<AuthFormState> {
  const email = String(formData.get("email") ?? "").trim();
  const code = String(formData.get("code") ?? "").replace(/\D/g, "");

  // Mantém o passo do código na tela em qualquer erro: perder o `codeSent` jogaria a pessoa
  // de volta no formulário vazio depois de um dígito errado.
  const fail = (error: string): AuthFormState => ({ error, codeSent: email });

  if (code.length !== 6) {
    return fail("O código tem seis dígitos.");
  }

  let auth: AuthResponse;
  try {
    const res = await fetchWithTimeout(`${API_URL}/v1/auth/register/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, code }),
      cache: "no-store",
    });

    if (!res.ok) {
      return {
        ...await readError(res, "Código inválido ou expirado. Use o código mais recente ou volte ao cadastro."),
        codeSent: email,
      };
    }
    auth = await res.json();
  } catch {
    return fail("Servidor fora do ar. Tente de novo em alguns segundos.");
  }

  await createSession(auth);
  redirect("/painel");
}

/**
 * Outro código pro mesmo cadastro.
 *
 * O 204 é genérico: também ocorre sem pendência vigente.
 */
export async function resendSignupCodeAction(email: string): Promise<AuthFormState> {
  try {
    const res = await fetchWithTimeout(`${API_URL}/v1/auth/register/resend`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email }),
      cache: "no-store",
    });
    if (!res.ok) {
      return { ...await readError(res, "Não deu pra pedir outro código agora."), codeSent: email };
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos.", codeSent: email };
  }
  return {
    codeSent: email,
    notice: "Se houver um cadastro pendente válido, um novo código será enviado. Se não chegar, volte ao cadastro.",
  };
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
  retryAt?: number;
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
    const res = await fetchWithTimeout(`${API_URL}/v1/auth/password-reset/request`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email }),
      cache: "no-store",
    });
    if (!res.ok) {
      return { ...await readError(res, "Não deu pra pedir o link agora."), values };
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
    const res = await fetchWithTimeout(`${API_URL}/v1/auth/password-reset/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token, password }),
      cache: "no-store",
    });
    if (!res.ok) {
      return await readError(res, "Esse link expirou ou já foi usado. Peça outro.");
    }
  } catch {
    return { error: "Servidor fora do ar. Tente de novo em alguns segundos." };
  }

  return { done: true };
}
