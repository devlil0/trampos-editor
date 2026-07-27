"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { NICHE_OPTIONS } from "@/lib/format";

export function VitrineControls() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const activeNiche = searchParams.get("niche") ?? "";
  const [query, setQuery] = useState(searchParams.get("q") ?? "");

  function pushWith(mutate: (params: URLSearchParams) => void) {
    const params = new URLSearchParams(searchParams.toString());
    mutate(params);
    params.delete("page"); // qualquer mudança de filtro volta pra primeira página
    const qs = params.toString();
    router.push(qs ? `/?${qs}` : "/");
  }

  function onSearch(e: React.FormEvent) {
    e.preventDefault();
    pushWith((p) => {
      if (query.trim()) p.set("q", query.trim());
      else p.delete("q");
    });
  }

  function selectNiche(niche: string) {
    pushWith((p) => {
      if (niche) p.set("niche", niche);
      else p.delete("niche");
    });
  }

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
        <button
          type="submit"
          className="rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-contrast hover:bg-accent-strong"
        >
          Buscar
        </button>
      </form>

      <div className="mt-6 flex flex-wrap gap-2" role="group" aria-label="Filtrar por nicho">
        <Chip label="Todos" active={activeNiche === ""} onClick={() => selectNiche("")} />
        {NICHE_OPTIONS.map(([value, label]) => (
          <Chip
            key={value}
            label={label}
            active={activeNiche === value}
            onClick={() => selectNiche(value)}
          />
        ))}
      </div>
    </>
  );
}

function Chip({
  label,
  active,
  onClick,
}: {
  label: string;
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
          ? "rounded-full border border-accent bg-accent px-4 py-[7px] text-sm font-medium text-accent-contrast"
          : "rounded-full border border-line bg-card px-4 py-[7px] text-sm font-medium text-muted hover:border-accent hover:text-fg"
      }
    >
      {label}
    </button>
  );
}
