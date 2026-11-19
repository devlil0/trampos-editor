import { NextResponse } from "next/server";

export const OAUTH_ATTEMPT_MAX_AGE = 300;
export const OAUTH_PROOF_FORMAT = /^[0-9a-f]{64}$/;

export const oauthProofCookieOptions = {
  httpOnly: true,
  sameSite: "lax",
  secure: process.env.NODE_ENV === "production",
  path: "/entrar/callback",
} as const;

export function oauthProofCookie(attempt: string) {
  return "te_oauth_" + attempt;
}

const messages: Readonly<Record<string, string>> = {
  cancelled: "Login cancelado. Você pode tentar de novo.",
  expired: "Essa tentativa de login expirou ou é inválida. Comece de novo.",
  email_unverified: "Confirme seu e-mail no provedor ou entre com senha.",
  link_not_allowed: "Entre com seu método habitual ou tente recuperar a senha.",
  account_inactive: "Conta desativada.",
  unavailable: "Serviço de login indisponível. Tente de novo mais tarde.",
  failed: "Não foi possível concluir o login. Tente de novo.",
};

export function oauthErrorCode(value: string) {
  return Object.hasOwn(messages, value) ? value : "failed";
}

export function oauthErrorMessage(value: string) {
  return messages[oauthErrorCode(value)];
}

export function oauthRedirect(location: string) {
  return new NextResponse(null, {
    status: 303,
    headers: {
      Location: location,
      "Cache-Control": "no-store",
      "Referrer-Policy": "no-referrer",
    },
  });
}

export function oauthErrorRedirect(value: string) {
  return oauthRedirect("/entrar?oauth_error=" + oauthErrorCode(value));
}
