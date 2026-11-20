import { NICHE_OPTIONS, STYLE_OPTIONS, TOOL_OPTIONS } from "./format";

function integer(value: string | undefined, min: number, max = 2_147_483_647) {
  if (!value || !/^\d+$/.test(value)) return undefined;
  const number = Number(value);
  return number >= min && number <= max ? String(number) : undefined;
}

export function normalizeFeedQuery(query: { niche?: string; page?: string }) {
  return {
    niche: NICHE_OPTIONS.some(([value]) => value === query.niche) ? query.niche : undefined,
    // Vitrine e feed aceitam até 50 itens por página. O offset do JPA é um int.
    page: integer(query.page, 0, Math.floor(2_147_483_647 / 50)),
  };
}

export function normalizeEditorQuery(query: {
  niche?: string; tool?: string; style?: string; available?: string;
  maxPriceCents?: string; q?: string; page?: string; size?: string;
}) {
  return {
    ...normalizeFeedQuery(query),
    tool: TOOL_OPTIONS.some(([value]) => value === query.tool) ? query.tool : undefined,
    style: STYLE_OPTIONS.some(([value]) => value === query.style) ? query.style : undefined,
    available: query.available === "true" || query.available === "false" ? query.available : undefined,
    maxPriceCents: integer(query.maxPriceCents, 0),
    q: query.q?.trim().slice(0, 120) || undefined,
    size: integer(query.size, 1),
  };
}

export function hasAdjustedFilters(raw: Record<string, string | undefined>, clean: Record<string, string | undefined>) {
  return Object.entries(raw).some(([key, value]) => Boolean(value) && value !== clean[key]);
}
