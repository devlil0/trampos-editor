import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Captura de onde a pessoa veio, na primeira visita (ESBOCO §3.1, migration V16).
 *
 * É o único dado da instrumentação que não é derivável do banco depois: referrer e `utm_*`
 * existem por um instante e somem se ninguém gravar. Método de cadastro, cliente que
 * voltou e efeito da nota na ordenação todos saem de consulta — este não sai de lugar
 * nenhum.
 *
 * Roda aqui, no proxy, porque é o único lugar do Next que vê a requisição **antes** de
 * qualquer página renderizar e ainda pode gravar cookie. Um Server Component não escreve
 * cookie, e um componente de cliente só rodaria depois de o HTML já ter ido.
 *
 * Grava uma vez e nunca sobrescreve: a origem que interessa é a que **captou** a pessoa,
 * não a última que ela usou. Quem chega por uma página de tag, some por uma semana e volta
 * digitando o endereço continua tendo sido captado pela tag.
 *
 * Não é rastreamento de comportamento: um cookie, três campos, escrito no primeiro acesso e
 * lido uma vez só — no cadastro. Não segue navegação, não identifica ninguém e morre em 30
 * dias. Quem nunca cria conta nunca tem esse cookie lido por nada.
 */

export const ORIGIN_COOKIE = "te_origem";

/** Trinta dias: quem visita e volta pra criar conta faz isso em dias, não em meses. */
const MAX_AGE = 60 * 60 * 24 * 30;

export function proxy(request: NextRequest) {
  const response = NextResponse.next();

  if (request.cookies.has(ORIGIN_COOKIE)) return response;

  const referrer = request.headers.get("referer") ?? "";
  const utmSource = request.nextUrl.searchParams.get("utm_source") ?? "";

  const origem = {
    // Navegação interna não é origem: quem veio da nossa própria página já estava aqui.
    // Sem esta guarda, o primeiro acesso registrado seria sempre o do próprio site.
    referrer: referrer && !mesmoSite(referrer, request) ? referrer : "",
    utmSource,
    landing: request.nextUrl.pathname,
  };

  response.cookies.set(ORIGIN_COOKIE, JSON.stringify(origem), {
    // Legível pelo servidor do Next, que é quem monta o cadastro. Não precisa ser httpOnly
    // — não há segredo aqui —, mas fica assim mesmo: não existe motivo pro browser ler.
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: MAX_AGE,
  });

  return response;
}

function mesmoSite(referrer: string, request: NextRequest): boolean {
  try {
    return new URL(referrer).host === request.nextUrl.host;
  } catch {
    // Referer malformado é o mesmo que não ter.
    return true;
  }
}

export const config = {
  // Sem matcher isto rodaria em cada CSS, imagem e chunk — e o primeiro acesso registrado
  // seria o de um arquivo estático, não o da página que a pessoa viu.
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico|icon.png|apple-icon.png|robots.txt|sitemap.xml|.*\\.(?:png|jpg|jpeg|gif|svg|webp|ico|css|js|woff|woff2)$).*)",
  ],
};
