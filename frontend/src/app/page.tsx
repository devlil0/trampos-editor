import { Suspense } from "react";
import { fetchEditors, type EditorQuery } from "@/lib/api";
import { EditorCard } from "@/components/editor-card";
import { VitrineControls } from "@/components/vitrine-controls";

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>;

function pick(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export default async function VitrinePage({ searchParams }: { searchParams: SearchParams }) {
  const sp = await searchParams;
  const query: EditorQuery = {
    niche: pick(sp.niche),
    tool: pick(sp.tool),
    q: pick(sp.q),
    maxPriceCents: pick(sp.maxPriceCents),
    page: pick(sp.page),
  };

  const result = await fetchEditors(query);

  return (
    <div className="mx-auto max-w-6xl px-6">
      <section className="pt-14 pb-6">
      <h1 className="max-w-[16ch] text-balance text-4xl font-extrabold leading-[1.04] tracking-tight sm:text-5xl">
                <span className="text-accent">Simples</span>
              </h1>
        <h1 className="max-w-[16ch] text-balance text-4xl font-extrabold leading-[1.04] tracking-tight sm:text-5xl">
          Encontre o editor certo pro seu <span className="font-mono text-accent">vídeo</span>.
        </h1>
        <p className="mt-3.5 mb-6 max-w-[52ch] text-lg text-muted">
          Encontre aqui os melhores editores para o seu vídeo. Veja o portfólio deles, prazo de entrega e o seu preço - fale direto, sem burocracia.
        </p>
        <Suspense fallback={null}>
          <VitrineControls />
        </Suspense>
      </section>

      <section>
        <div className="flex items-center py-2 text-sm text-muted">
          <span>
            <b className="text-fg tabular-nums">{result.totalElements}</b>{" "}
            {result.totalElements === 1 ? "editor encontrado" : "editores encontrados"}
          </span>
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
          <div className="flex flex-col gap-3.5 pb-16 pt-3">
            {result.content.map((editor) => (
              <EditorCard key={editor.id} editor={editor} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
