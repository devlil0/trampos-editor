/**
 * Sessão do usuário — só roda no servidor.
 *
 * O access token do backend fica num cookie **httpOnly**: o browser nunca vê o JWT,
 * o que casa com a arquitetura SSR (§11.6, `API_URL` server-only) e tira o token do
 * alcance de XSS. Quem fala com a API continua sendo o servidor do Next.
 *
 * Login social (Google) entra depois sem mexer aqui: basta a nova action trocar o
 * código do provedor por um AuthResponse do nosso backend e chamar createSession.
 */
import { cache } from "react";
import { redirect } from "next/navigation";
import { fetchWithTimeout } from "./api-fetch";
import { cookies } from "next/headers";
import type { AuthResponse, AuthUser } from "./types";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

export const SESSION_COOKIE = "te_session";

export async function createSession(auth: AuthResponse): Promise<void> {
  const jar = await cookies();
  jar.set(SESSION_COOKIE, auth.accessToken, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    // Mesmo tempo de vida do JWT (12h por padrão) — não há refresh token ainda (§11.4).
    maxAge: auth.expiresIn,
  });
}

export async function destroySession(): Promise<void> {
  const jar = await cookies();
  jar.delete(SESSION_COOKIE);
}

export async function getSessionToken(): Promise<string | null> {
  const jar = await cookies();
  return jar.get(SESSION_COOKIE)?.value ?? null;
}


type SessionState =
  | { status: "active"; user: AuthUser }
  | { status: "anonymous" | "expired" | "unavailable"; user: null };

export const getSessionState = cache(async (): Promise<SessionState> => {
  const token = await getSessionToken();
  if (!token) return { status: "anonymous", user: null };

  try {
    const res = await fetchWithTimeout(API_URL + "/v1/users/me", {
      headers: { Authorization: "Bearer " + token },
      cache: "no-store",
    });
    if (res.status === 401 || res.status === 403) return { status: "expired", user: null };
    if (res.ok) return { status: "active", user: await res.json() as AuthUser };
  } catch {
    return { status: "unavailable", user: null };
  }
  return { status: "unavailable", user: null };
});

export async function getCurrentUser(): Promise<AuthUser | null> {
  const session = await getSessionState();
  if (session.status === "unavailable") throw new Error("Falha ao consultar a sessão");
  return session.user;
}

export async function requireCurrentUser(): Promise<AuthUser> {
  const session = await getSessionState();
  if (session.status === "unavailable") throw new Error("Falha ao consultar a sessão");
  if (session.status === "expired") redirect("/entrar?motivo=sessao");
  if (!session.user) redirect("/entrar");
  return session.user;
}
