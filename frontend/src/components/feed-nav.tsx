import Link from "next/link";
import { NICHE_OPTIONS } from "@/lib/format";
import { NicheIcon } from "./niche-tag";
import type { EditorNiche, Page } from "@/lib/types";

/**
 * Controles dos dois feeds da área logada — trampos e editores.
 *
 * Diferente da `VitrineControls`, aqui não há `"use client"`: são links, não estado.
 * O feed troca de categoria e de página navegando, então funciona sem JS e o servidor
 * já devolve a lista certa na primeira resposta. A vitrine precisa de cliente porque
 * tem campo de busca com digitação; o feed não tem.
 */
export function CategoryChips({
  basePath,
  active,
}: {
  basePath: string;
  active?: string;
}) {
  return (
    <div className="flex flex-wrap gap-2" role="group" aria-label="Filtrar por categoria">
      {/* Sem `page` no href: trocar de categoria volta pra primeira página. */}
      <Chip href={basePath} label="Todas" active={!active} />
      {NICHE_OPTIONS.map(([value, label]) => (
        <Chip
          key={value}
          href={`${basePath}?niche=${value}`}
          label={label}
          niche={value}
          active={active === value}
        />
      ))}
    </div>
  );
}

function Chip({
  href,
  label,
  niche,
  active,
}: {
  href: string;
  label: string;
  niche?: EditorNiche;
  active: boolean;
}) {
  return (
    <Link
      href={href}
      aria-current={active ? "true" : undefined}
      className={
        active
          ? "inline-flex items-center gap-1.5 rounded-full border border-accent bg-accent px-4 py-[7px] text-sm font-medium text-accent-contrast"
          : "inline-flex items-center gap-1.5 rounded-full border border-line bg-card px-4 py-[7px] text-sm font-medium text-muted hover:border-accent hover:text-fg"
      }
    >
      {niche && <NicheIcon niche={niche} size={15} />}
      {label}
    </Link>
  );
}

/**
 * Anterior/próxima em vez de "carregar mais": mantém a página no URL, então voltar do
 * perfil de um editor não joga a pessoa de volta pro topo do feed.
 */
export function FeedPager({
  basePath,
  page,
  niche,
}: {
  basePath: string;
  page: Page<unknown>;
  niche?: string;
}) {
  if (page.totalPages <= 1) return null;

  const href = (n: number) => {
    const params = new URLSearchParams();
    if (niche) params.set("niche", niche);
    if (n > 0) params.set("page", String(n));
    const qs = params.toString();
    return qs ? `${basePath}?${qs}` : basePath;
  };

  return (
    <nav className="mt-8 flex items-center justify-between gap-4" aria-label="Paginação">
      {page.page > 0 ? (
        <Link href={href(page.page - 1)} className="btn btn-ghost btn-sm">
          ← Anterior
        </Link>
      ) : (
        <span />
      )}

      <span className="text-sm text-muted tabular-nums">
        Página <b className="text-fg">{page.page + 1}</b> de {page.totalPages}
      </span>

      {page.last ? (
        <span />
      ) : (
        <Link href={href(page.page + 1)} className="btn btn-ghost btn-sm">
          Próxima →
        </Link>
      )}
    </nav>
  );
}
