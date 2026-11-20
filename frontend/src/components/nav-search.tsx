"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

/**
 * Busca na barra de navegação, no lugar em que marketplace grande a coloca: ao lado da
 * marca, ocupando o vão até as ações.
 *
 * Sai da vitrine e vem pra cá porque busca é entrada, não configuração — de qualquer
 * página do produto a pessoa pode querer procurar um editor, e antes ela precisava
 * primeiro chegar em `/editores`. Sempre navega pra vitrine, que é a única lista de
 * editores que existe.
 *
 * Preserva os filtros ativos quando já se está na vitrine: quem filtrou por CapCut e
 * depois busca "motion" espera refinar, não recomeçar.
 */
export function NavSearch({ mobile = false }: { mobile?: boolean }) {
  const params = useSearchParams();
  return <SearchForm key={params.get("q") ?? ""} mobile={mobile} />;
}

function SearchForm({ mobile }: { mobile: boolean }) {
  const pathname = usePathname();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [query, setQuery] = useState(searchParams.get("q") ?? "");

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const params = new URLSearchParams(pathname === "/editores" ? searchParams.toString() : "");
    // `page` some junto: resultado novo começa da primeira página.
    params.delete("page");
    if (query.trim()) params.set("q", query.trim());
    else params.delete("q");
    const qs = params.toString();
    router.push(qs ? `/editores?${qs}` : "/editores");
  }

  return (
    <form
      onSubmit={onSubmit}
      role="search"
      className={`${mobile ? "mb-4 flex md:hidden" : "hidden flex-1 md:flex"} min-w-0 items-center gap-1 rounded-xl border border-line bg-card p-1 pl-3.5 focus-within:border-accent`}
    >
      <input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Busque por nome ou chamada…"
        aria-label="Buscar editores por nome ou chamada"
        maxLength={120}
        className="min-w-0 flex-1 bg-transparent py-1.5 text-sm outline-none placeholder:text-muted"
      />
      <button
        type="submit"
        aria-label="Buscar"
        className="grid h-8 w-11 flex-none place-items-center rounded-lg bg-accent text-accent-contrast transition hover:bg-accent-strong"
      >
        <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
          <circle cx="11" cy="11" r="7" />
          <path d="m21 21-4.3-4.3" />
        </svg>
      </button>
    </form>
  );
}
