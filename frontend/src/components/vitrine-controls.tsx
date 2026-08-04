"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { NICHE_OPTIONS, STYLE_OPTIONS, TOOL_OPTIONS } from "@/lib/format";
import { NicheIcon, ToolIcon } from "./niche-tag";
import type { EditorNiche, EditorTool } from "@/lib/types";

/**
 * Controles da vitrine.
 *
 * A vitrine é a ÚNICA lista de editores do produto desde que o feed da oferta saiu
 * (2026-08-04) — "quem está livre agora" virou o toggle daqui, não outra tela. Por isso
 * todo filtro que a API aceita precisa ter controle visível: antes, `tool`, `style`,
 * `available` e `maxPriceCents` existiam em `/v1/editors` e não tinham como ser acionados
 * pela interface, o que fazia a vitrine parecer mais pobre do que era.
 *
 * Ferramenta ganha círculo com marca, e não chip de texto como nicho: são duas dimensões
 * diferentes e a pessoa precisa distinguir "onde publico" de "com o que é feito" sem ler.
 */

/** Faixas prontas em vez de campo livre: preço é filtro de triagem, não de precisão. */
const FAIXAS_DE_PRECO: [string, string][] = [
  ["10000", "até R$ 100"],
  ["25000", "até R$ 250"],
  ["50000", "até R$ 500"],
  ["100000", "até R$ 1.000"],
];

export function VitrineControls() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const activeNiche = searchParams.get("niche") ?? "";
  const activeTool = searchParams.get("tool") ?? "";
  const activeStyle = searchParams.get("style") ?? "";
  const activePrice = searchParams.get("maxPriceCents") ?? "";
  const onlyAvailable = searchParams.get("available") === "true";
  const [query, setQuery] = useState(searchParams.get("q") ?? "");

  function pushWith(mutate: (params: URLSearchParams) => void) {
    const params = new URLSearchParams(searchParams.toString());
    mutate(params);
    params.delete("page"); // qualquer mudança de filtro volta pra primeira página
    const qs = params.toString();
    router.push(qs ? `/editores?${qs}` : "/editores");
  }

  function onSearch(e: React.FormEvent) {
    e.preventDefault();
    pushWith((p) => {
      if (query.trim()) p.set("q", query.trim());
      else p.delete("q");
    });
  }

  /** Clicar no filtro ativo desliga — sem isso não há como voltar pra "todos". */
  function toggle(key: string, value: string) {
    pushWith((p) => {
      if (value && p.get(key) !== value) p.set(key, value);
      else p.delete(key);
    });
  }

  const temFiltro = Boolean(activeNiche || activeTool || activeStyle || activePrice || onlyAvailable);

  return (
    <>
      <form onSubmit={onSearch} className="flex max-w-xl items-center gap-2 rounded-2xl border border-line bg-card p-[7px] pl-4 shadow-[var(--shadow)]">
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" className="shrink-0 text-muted" aria-hidden="true">
          <circle cx="11" cy="11" r="7" />
          <path d="m21 21-4.3-4.3" />
        </svg>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Busque por nome, estilo ou nicho…"
          aria-label="Buscar editores"
          className="flex-1 bg-transparent py-2 text-base outline-none placeholder:text-muted"
        />
        <button type="submit" className="btn btn-primary btn-sm">
          Buscar
        </button>
      </form>

      <div className="mt-6 flex flex-wrap gap-2" role="group" aria-label="Filtrar por nicho">
        <Chip label="Todos" active={activeNiche === ""} onClick={() => toggle("niche", "")} />
        {NICHE_OPTIONS.map(([value, label]) => (
          <Chip
            key={value}
            label={label}
            niche={value}
            active={activeNiche === value}
            onClick={() => toggle("niche", value)}
          />
        ))}
      </div>

      {/* Ferramentas como círculos de marca: a leitura é pelo logo, o rótulo é apoio. */}
      <div className="mt-7">
        <h2 className="mb-3 font-mono text-[11px] font-semibold tracking-[0.14em] text-muted">
          FERRAMENTA
        </h2>
        <div className="flex flex-wrap gap-x-5 gap-y-4" role="group" aria-label="Filtrar por ferramenta">
          {TOOL_OPTIONS.map(([value, label]) => (
            <ToolCircle
              key={value}
              tool={value}
              label={label}
              active={activeTool === value}
              onClick={() => toggle("tool", value)}
            />
          ))}
        </div>
      </div>

      <div className="mt-7 flex flex-col gap-5 sm:flex-row sm:gap-9">
        <div>
          <h2 className="mb-2.5 font-mono text-[11px] font-semibold tracking-[0.14em] text-muted">
            ESTILO
          </h2>
          <div className="flex flex-wrap gap-1.5" role="group" aria-label="Filtrar por estilo">
            {STYLE_OPTIONS.map(([value, label]) => (
              <SmallChip
                key={value}
                label={label}
                active={activeStyle === value}
                onClick={() => toggle("style", value)}
              />
            ))}
          </div>
        </div>

        <div>
          <h2 className="mb-2.5 font-mono text-[11px] font-semibold tracking-[0.14em] text-muted">
            PREÇO
          </h2>
          <div className="flex flex-wrap gap-1.5" role="group" aria-label="Filtrar por preço">
            {FAIXAS_DE_PRECO.map(([value, label]) => (
              <SmallChip
                key={value}
                label={label}
                active={activePrice === value}
                onClick={() => toggle("maxPriceCents", value)}
              />
            ))}
          </div>
        </div>
      </div>

      <div className="mt-7 flex flex-wrap items-center gap-4">
        {/* Era o feed inteiro; virou um toggle. */}
        <button
          type="button"
          role="switch"
          aria-checked={onlyAvailable}
          onClick={() => toggle("available", "true")}
          className="inline-flex items-center gap-2.5 text-sm font-semibold"
        >
          <span
            className={`relative h-5 w-9 rounded-full transition ${
              onlyAvailable ? "bg-good" : "bg-panel border border-line"
            }`}
          >
            <span
              className={`absolute top-1/2 h-3.5 w-3.5 -translate-y-1/2 rounded-full bg-white transition-all ${
                onlyAvailable ? "left-[18px]" : "left-[3px]"
              }`}
            />
          </span>
          Só quem está livre agora
        </button>

        {temFiltro && (
          <button
            type="button"
            onClick={() => router.push("/editores")}
            className="text-sm text-muted underline-offset-4 hover:text-accent hover:underline"
          >
            Limpar filtros
          </button>
        )}
      </div>
    </>
  );
}

function Chip({
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
      className={
        active
          ? "inline-flex items-center gap-1.5 rounded-full border border-accent bg-accent px-4 py-[7px] text-sm font-medium text-accent-contrast"
          : "inline-flex items-center gap-1.5 rounded-full border border-line bg-card px-4 py-[7px] text-sm font-medium text-muted hover:border-accent hover:text-fg"
      }
    >
      {niche && <NicheIcon niche={niche} size={15} />}
      {label}
    </button>
  );
}

/** Chip miúdo pra estilo e preço — dimensões secundárias não podem competir com nicho. */
function SmallChip({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={
        active
          ? "rounded-md border border-accent bg-accent/15 px-2.5 py-1 text-xs font-semibold text-accent-strong"
          : "rounded-md border border-line bg-card px-2.5 py-1 text-xs font-semibold text-muted hover:border-accent hover:text-fg"
      }
    >
      {label}
    </button>
  );
}

function ToolCircle({
  tool,
  label,
  active,
  onClick,
}: {
  tool: EditorTool;
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button type="button" onClick={onClick} aria-pressed={active} className="group flex w-16 flex-col items-center gap-1.5">
      <span
        className={`grid h-14 w-14 place-items-center rounded-full border bg-card transition ${
          active ? "border-accent ring-2 ring-accent/40" : "border-line group-hover:border-accent"
        }`}
      >
        <ToolIcon tool={tool} size={28} />
      </span>
      <span className={`text-center text-[11px] font-semibold leading-tight ${active ? "text-fg" : "text-muted"}`}>
        {label}
      </span>
    </button>
  );
}
