import type { MetadataRoute } from "next";
import { fetchEditors } from "@/lib/api";
import { deveIndexar, TAGS } from "@/lib/tags";

const SITE_URL = process.env.SITE_URL ?? "http://localhost:3000";

/**
 * Sitemap da camada pública. A camada logada fica de fora por definição: ela não capta
 * ninguém e já sai com `robots: { index: false }` (ESBOCO §1).
 *
 * Tag com estoque magro não entra — o mesmo limiar do `noindex` na página. Pedir
 * indexação de uma página que manda `noindex` é contradição que gasta crawl budget e
 * ainda sinaliza descuido.
 */
export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const estaticas: MetadataRoute.Sitemap = [
    { url: SITE_URL, changeFrequency: "weekly", priority: 1 },
    { url: `${SITE_URL}/editores`, changeFrequency: "daily", priority: 0.9 },
    { url: `${SITE_URL}/para-clientes`, changeFrequency: "monthly", priority: 0.7 },
    { url: `${SITE_URL}/para-editores`, changeFrequency: "monthly", priority: 0.7 },
  ];

  // Uma consulta por tag, e são nove — barato, e o sitemap é lido por robô, não por
  // gente esperando a página abrir.
  const tags = await Promise.all(
    TAGS.map(async (tag) => {
      const { totalElements } = await fetchEditors({ [tag.tipo]: tag.valor, size: "1" });
      return { tag, totalElements };
    }),
  );

  const deTags: MetadataRoute.Sitemap = tags
    .filter(({ totalElements }) => deveIndexar(totalElements))
    .map(({ tag }) => ({
      url: `${SITE_URL}/editores/${tag.slug}`,
      changeFrequency: "weekly" as const,
      priority: 0.8,
    }));

  // Perfil de editor é o ativo de SEO principal (ESBOCO §2), então entra inteiro.
  //
  // Pelo `username`, e não pelo `slug`: o perfil mudou de endereço em 2026-08-04 e
  // `/editores/{slug}` só responde 308 agora. Sitemap que aponta pra URL que redireciona é
  // erro de verdade — gasta crawl budget e sinaliza descuido.
  const { content } = await fetchEditors({ size: "500" });
  const perfis: MetadataRoute.Sitemap = content.map((editor) => ({
    url: `${SITE_URL}/${editor.username}`,
    changeFrequency: "weekly" as const,
    priority: 0.8,
  }));

  return [...estaticas, ...deTags, ...perfis];
}
