"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { NICHE_OPTIONS, STYLE_OPTIONS, TOOL_OPTIONS } from "@/lib/format";
import { NicheIcon, ToolIcon } from "./niche-tag";
import type { EditorNiche } from "@/lib/types";

/**
 * Controles da vitrine, em duas peças que moram em lugares diferentes do layout:
 * `ToolStrip` no topo dos resultados e `VitrineSidebar` na coluna da esquerda. A busca
 * saiu daqui e virou `NavSearch`, na barra de navegação — busca é entrada, e agora
 * funciona de qualquer página.
 *
 * A divisão segue a forma de marketplace grande (Mercado Livre, Shopee), e não é só
 * estética: filtro em coluna fixa fica **visível junto com o resultado**, então dá pra
 * refinar vendo o que muda. Empilhado acima da lista, cada ajuste empurra o resultado pra
 * fora da tela — que era o problema da versão anterior.
 *
 * A vitrine é a única lista de editores do produto desde que o feed da oferta saiu
 * (2026-08-04), então todo filtro que a API aceita precisa ter controle visível aqui.
 */

/** Faixas prontas em vez de campo livre: preço é filtro de triagem, não de precisão. */
const FAIXAS_DE_PRECO: [string, string][] = [
  ["10000", "até R$ 100"],
  ["25000", "até R$ 250"],
  ["50000", "até R$ 500"],
  ["100000", "até R$ 1.000"],
];

/** Navegação compartilhada pelas três peças — todas escrevem na mesma query string. */
function useFiltros() {
  const router = useRouter();
  const searchParams = useSearchParams();

  function pushWith(mutate: (params: URLSearchParams) => void) {
    const params = new URLSearchParams(searchParams.toString());
    mutate(params);
    params.delete("page"); // qualquer mudança de filtro volta pra primeira página
    const qs = params.toString();
    router.push(qs ? `/editores?${qs}` : "/editores");
  }

  /** Clicar no filtro ativo desliga — sem isso não há como voltar pra "todos". */
  function toggle(key: string, value: string) {
    pushWith((p) => {
      if (value && p.get(key) !== value) p.set(key, value);
      else p.delete(key);
    });
  }

  return { router, searchParams, pushWith, toggle };
}

/**
 * Faixa horizontal de ferramentas, no topo dos resultados — é onde o Mercado Livre põe as
 * marcas. Fica fora da sidebar de propósito: marca se reconhece pelo logo, e logo em lista
 * de texto perde justamente o que o torna rápido de achar.
 */
export function ToolStrip() {
  const { searchParams, toggle } = useFiltros();
  const activeTool = searchParams.get("tool") ?? "";

  return (
    <div
      className="flex gap-x-6 gap-y-4 overflow-x-auto pb-1"
      role="group"
      aria-label="Filtrar por ferramenta"
    >
      {TOOL_OPTIONS.map(([value, label]) => {
        const active = activeTool === value;
        return (
          <button
            key={value}
            type="button"
            onClick={() => toggle("tool", value)}
            aria-pressed={active}
            className="group flex w-[68px] flex-none flex-col items-center gap-1.5"
          >
            <span
              className={`grid h-15 w-15 place-items-center rounded-full border bg-card transition ${
                active ? "border-accent ring-2 ring-accent/40" : "border-line group-hover:border-accent"
              }`}
            >
              <ToolIcon tool={value} size={30} />
            </span>
            <span className={`text-center text-[11px] font-semibold leading-tight ${active ? "text-fg" : "text-muted"}`}>
              {label}
            </span>
          </button>
        );
      })}
    </div>
  );
}

/** Coluna da esquerda: as dimensões que se lêem como lista, não como marca. */
export function VitrineSidebar() {
  const { router, searchParams, toggle } = useFiltros();
  const activeNiche = searchParams.get("niche") ?? "";
  const activeStyle = searchParams.get("style") ?? "";
  const activePrice = searchParams.get("maxPriceCents") ?? "";
  const activeTool = searchParams.get("tool") ?? "";
  const onlyAvailable = searchParams.get("available") === "true";
  const busca = searchParams.get("q") ?? "";

  const temFiltro = Boolean(
    activeNiche || activeTool || activeStyle || activePrice || onlyAvailable || busca,
  );

  return (
    <aside className="lg:sticky lg:top-[84px] lg:self-start">
      <div className="flex items-center justify-between gap-2 border-b border-line pb-3">
        <h2 className="flex items-center gap-2 font-mono text-[11px] font-bold tracking-[0.14em] text-fg">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <path d="M3 5h18l-7 8v6l-4 2v-8z" />
          </svg>
          FILTROS
        </h2>
        {temFiltro && (
          <button
            type="button"
            onClick={() => router.push("/editores")}
            className="text-xs text-muted underline-offset-4 hover:text-accent hover:underline"
          >
            Limpar
          </button>
        )}
      </div>

      {/* Disponibilidade vem primeiro: é o recorte que substituiu o feed da oferta, e o
          mais provável de quem chega querendo alguém pra hoje. */}
      <Grupo titulo="Disponibilidade">
        <button
          type="button"
          role="switch"
          aria-checked={onlyAvailable}
          onClick={() => toggle("available", "true")}
          className="inline-flex items-center gap-2.5 text-sm font-semibold"
        >
          <span
            className={`relative h-5 w-9 flex-none rounded-full transition ${
              onlyAvailable ? "bg-good" : "border border-line bg-panel"
            }`}
          >
            <span
              className={`absolute top-1/2 h-3.5 w-3.5 -translate-y-1/2 rounded-full bg-white transition-all ${
                onlyAvailable ? "left-[18px]" : "left-[3px]"
              }`}
            />
          </span>
          Livres agora
        </button>
      </Grupo>

      <Grupo titulo="Categorias">
        <Opcao label="Todas" active={activeNiche === ""} onClick={() => toggle("niche", "")} />
        {NICHE_OPTIONS.map(([value, label]) => (
          <Opcao
            key={value}
            label={label}
            niche={value}
            active={activeNiche === value}
            onClick={() => toggle("niche", value)}
          />
        ))}
      </Grupo>

      <Grupo titulo="Estilo">
        {STYLE_OPTIONS.map(([value, label]) => (
          <Opcao
            key={value}
            label={label}
            active={activeStyle === value}
            onClick={() => toggle("style", value)}
          />
        ))}
      </Grupo>

      <Grupo titulo="Preço por vídeo">
        {FAIXAS_DE_PRECO.map(([value, label]) => (
          <Opcao
            key={value}
            label={label}
            active={activePrice === value}
            onClick={() => toggle("maxPriceCents", value)}
          />
        ))}
      </Grupo>
    </aside>
  );
}

function Grupo({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <section className="border-b border-line py-4 last:border-b-0">
      <h3 className="mb-2.5 text-xs font-bold uppercase tracking-[0.08em] text-muted">{titulo}</h3>
      <div className="flex flex-col items-start gap-1.5">{children}</div>
    </section>
  );
}

/**
 * Uma linha da sidebar. É botão, e não checkbox de verdade, porque a seleção é única por
 * dimensão e vive na URL — o quadradinho é só a leitura visual de "marcado", como nas
 * referências.
 */
function Opcao({
  label,
  niche,
  active,
  onClick,
}: {
  label: string;
  niche?: EditorNiche;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`flex w-full items-center gap-2 rounded-md px-1 py-1 text-left text-sm transition ${
        active ? "font-semibold text-accent-strong" : "text-muted hover:text-fg"
      }`}
    >
      <span
        className={`grid h-4 w-4 flex-none place-items-center rounded border transition ${
          active ? "border-accent bg-accent" : "border-line bg-card"
        }`}
      >
        {active && (
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" className="text-accent-contrast" aria-hidden="true">
            <path d="M20 6 9 17l-5-5" />
          </svg>
        )}
      </span>
      {niche && <NicheIcon niche={niche} size={14} />}
      <span className="flex-1">{label}</span>
    </button>
  );
}
