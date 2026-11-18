import { createHash, randomBytes } from "node:crypto";
import type { NextRequest } from "next/server";
import { PUBLIC_API_URL } from "@/lib/api";
import {
  OAUTH_ATTEMPT_MAX_AGE,
  oauthErrorRedirect,
  oauthProofCookie,
  oauthProofCookieOptions,
  oauthRedirect,
} from "@/lib/social-login";

export function GET(request: NextRequest) {
  const provider = request.nextUrl.searchParams.get("provider");
  if (provider !== "google" && provider !== "github") return oauthErrorRedirect("failed");

  const proof = randomBytes(32).toString("hex");
  const challenge = createHash("sha256").update(proof).digest("hex");
  const url = new URL(PUBLIC_API_URL.replace(/\/$/, "") + "/oauth2/authorization/" + provider);
  url.searchParams.set("browser_challenge", challenge);

  const response = oauthRedirect(url.toString());
  response.cookies.set(oauthProofCookie(challenge), proof, {
    ...oauthProofCookieOptions,
    maxAge: OAUTH_ATTEMPT_MAX_AGE,
  });
  return response;
}
