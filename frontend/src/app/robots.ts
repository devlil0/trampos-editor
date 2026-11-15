import type { MetadataRoute } from "next";

export const dynamic = "force-dynamic";

/**
 * O `/painel` já sai com `robots: { index: false }` em cada página, mas bloquear no
 * robots.txt evita o rastreamento antes dele acontecer — a camada logada devolve
 * redirecionamento pro login, e não há nada ali que valha crawl budget.
 *
 * `/entrar/callback` fica de fora porque é Route Handler de sessão, não página.
 */
export default function robots(): MetadataRoute.Robots {
  const siteUrl = process.env.SITE_URL ?? "http://localhost:3000";

  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/painel/", "/entrar/", "/criar-conta", "/redefinir-senha", "/esqueci-senha"],
    },
    sitemap: `${siteUrl}/sitemap.xml`,
  };
}
