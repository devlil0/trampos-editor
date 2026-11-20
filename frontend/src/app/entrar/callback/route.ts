import { fetchWithTimeout } from "@/lib/api-fetch";
import type { NextRequest } from "next/server";
import { createSession } from "@/lib/session";
import { readSignupOrigin } from "@/lib/signup-origin";
import {
  OAUTH_PROOF_FORMAT,
  oauthErrorRedirect,
  oauthProofCookie,
  oauthProofCookieOptions,
  oauthRedirect,
} from "@/lib/social-login";
import type { AuthResponse } from "@/lib/types";

const API_URL = process.env.API_URL ?? "http://localhost:8080";

export async function GET(request: NextRequest) {
  const params = request.nextUrl.searchParams;
  const code = params.get("code");
  const attempt = params.get("attempt");
  const providerError = params.get("oauth_error");
  if (!attempt || !OAUTH_PROOF_FORMAT.test(attempt)) return oauthErrorRedirect("expired");

  const cookieName = oauthProofCookie(attempt);
  const finish = (response: ReturnType<typeof oauthRedirect>) => {
    response.cookies.set(cookieName, "", { ...oauthProofCookieOptions, expires: new Date(0) });
    return response;
  };

  if (providerError) return finish(oauthErrorRedirect(providerError));
  const browserProof = request.cookies.get(cookieName)?.value;
  if (!code || !browserProof || !OAUTH_PROOF_FORMAT.test(browserProof)) {
    return finish(oauthErrorRedirect("expired"));
  }

  try {
    const res = await fetchWithTimeout(API_URL + "/v1/auth/social/exchange", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code, browserProof, origin: await readSignupOrigin() }),
      cache: "no-store",
    });
    if (!res.ok) {
      return finish(oauthErrorRedirect(res.status === 400 || res.status === 401 ? "expired" : "unavailable"));
    }

    const auth: AuthResponse = await res.json();
    const destination = auth.user.usernameChosen ? "/painel" : "/escolher-username";
    await createSession(auth);
    return finish(oauthRedirect(destination));
  } catch {
    return finish(oauthErrorRedirect("unavailable"));
  }
}
