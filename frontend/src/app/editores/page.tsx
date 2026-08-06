import type { Metadata } from "next";
import { Suspense } from "react";
import { fetchEditors, type EditorQuery } from "@/lib/api";
import { EditorCard } from "@/components/editor-card";
import { FeedSwitch } from "@/components/feed-switch";
import { ToolStrip, VitrineSidebar } from "@/components/vitrine-controls";

export const metadata: Metadata = {
  title: "Editores de vídeo para Reels, TikTok e Shorts | TramposEditor",
  description:
    "Navegue por editores de vídeo short-form. Filtre por plataforma, ferramenta e preço, veja o portfólio de cada um e fale direto — sem lance e sem taxa pra conversar.",
  alternates: { canonical: "/editores" },
};

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>;

function pick(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

/**
 * Vitrine — o ativo de SEO do produto (§11.2.6). Mora em `/editores` desde que a raiz
 * virou landing; a URL casa com a do perfil (`/editores/{slug}`).
 *
 * Desde 2026-08-06 esta é também a classe `editores` do feed unificado (`FeedSwitch`), e
 * ela continua sendo **uma tela só**: a chave leva pra cá em vez de renderizar uma segunda
 * lista de editores dentro do painel. Foi essa duplicata que o ESBOCO §1 deletou, e o
 * único jeito de unificar sem recriá-la é a classe pública morar onde já morava.
 */
export default async function VitrinePage({ searchParams }: { searchParams: SearchParams }) {
  const sp = await searchParams;
  const query: EditorQuery = {
    niche: pick(sp.niche),
    tool: pick(sp.tool),
    style: pick(sp.style),
    // "Quem está livre agora" era o feed da oferta (`/painel/editores`); virou recorte
    // desta lista quando aquela tela saiu, em 2026-08-04.
    available: pick(sp.available),
    q: pick(sp.q),
    maxPriceCents: pick(sp.maxPriceCents),
    page: pick(sp.page),
  };

  const result = await fetchEditors(query);

  return (
    /* Largura de marketplace e respiro curto nas bordas: é o que empurra a sidebar pro
       canto esquerdo, como nas referências. Acompanha a largura da nav. */
    <div className="mx-auto max-w-[1440px] px-4 pt-7 sm:px-6">
      {/* Sidebar à esquerda e resultado à direita: o filtro fica visível junto com o que
          ele filtra. Vira uma coluna só no mobile, onde os filtros vêm antes da lista. */}
      <div className="grid grid-cols-1 items-start gap-x-8 gap-y-6 pb-16 lg:grid-cols-[210px_1fr]">
        <Suspense fallback={null}>
          <VitrineSidebar />
        </Suspense>

        <section>
          <Suspense fallback={null}>
            <ToolStrip />
          </Suspense>

          {/* O hero de marketing saiu daqui, mas o h1 não podia sair junto: esta é a
              página de aquisição do produto (§11.2.6) e página sem h1 rankeia pior. Ele
              ficou no formato das referências — título curto com a contagem embaixo. */}
          <div className="mt-6 flex flex-wrap items-end justify-between gap-x-6 gap-y-3 border-t border-line pt-4">
            <div>
              <h1 className="text-2xl font-extrabold tracking-tight">Editores de vídeo</h1>
              <p className="mt-0.5 text-sm text-muted">
                <b className="text-fg tabular-nums">{result.totalElements}</b>{" "}
                {result.totalElements === 1 ? "editor encontrado" : "editores encontrados"}
              </p>
            </div>

            {/* A chave fica ao lado do h1, não acima: o título é o que o Google lê primeiro
                nesta página, e empurrá-lo pra baixo por causa de um controle de navegação
                trocaria o ativo de aquisição por conveniência de quem já está dentro. */}
            <FeedSwitch active="editores" />
          </div>

          {result.content.length === 0 ? (
            <div className="my-16 text-center">
              <p className="text-lg font-semibold">Nenhum editor por aqui ainda.</p>
              <p className="mt-1 text-muted">
                Tente limpar os filtros ou volte em breve — estamos trazendo editores todo dia.
              </p>
            </div>
          ) : (
            // Lista vertical: um editor embaixo do outro, capa à esquerda.
            <div className="flex flex-col gap-3.5 pt-4">
              {result.content.map((editor) => (
                <EditorCard key={editor.id} editor={editor} />
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
