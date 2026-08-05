import type { Metadata } from "next";
import { notFound, permanentRedirect } from "next/navigation";
import { fetchUsernameBySlug } from "@/lib/api";
import { TagPage, contarEditoresDaTag, deveIndexar } from "@/components/tag-page";
import { acharTag } from "@/lib/tags";

type Params = Promise<{ slug: string }>;

export async function generateMetadata({ params }: { params: Params }): Promise<Metadata> {
  const { slug } = await params;

  const tag = acharTag(slug);
  if (tag) {
    const total = await contarEditoresDaTag(tag);
    return {
      title: tag.titulo,
      description: tag.descricao,
      alternates: { canonical: `/editores/${tag.slug}` },
      // Tag sem estoque continua navegável, mas não entra no índice: página magra
      // indexada não sai mais de lá. Ver `deveIndexar` em lib/tags.ts.
      robots: deveIndexar(total) ? undefined : { index: false, follow: true },
    };
  }

  // Perfil de editor não mora mais aqui — a rota só redireciona, e página que redireciona
  // não precisa de metadata.
  return { title: "TramposEditor" };
}

/**
 * Duas coisas moram nesta rota: a página de tag (`/editores/tiktok`) e o redirecionamento
 * das URLs antigas de perfil.
 *
 * O perfil do editor mudou de endereço em 2026-08-04: saiu de `/editores/{slug}` e foi
 * pra `/{username}`, junto com o de quem só contrata — o produto passou a ter uma
 * identidade por conta, em vez de uma página só pra quem edita.
 *
 * O 308 é obrigatório e não cosmético: essas URLs são o ativo de SEO do produto e já
 * circulam em link e índice. Sumir com elas jogaria fora a autoridade acumulada; o
 * redirecionamento permanente a transfere pro endereço novo.
 *
 * A tag é checada antes, e o slug dela continua reservado no cadastro do perfil.
 */
export default async function EditorSlugPage({ params }: { params: Params }) {
  const { slug } = await params;

  const tag = acharTag(slug);
  if (tag) return <TagPage tag={tag} />;

  const username = await fetchUsernameBySlug(slug);
  // Slug que nunca existiu continua 404: link morto tem que morrer como link morto, não
  // virar redirecionamento pra lugar nenhum.
  if (!username) notFound();

  permanentRedirect(`/${username}`);
}
