import type { EditorProfile, EditorSummary, Page } from "./types";

// Lido só no servidor (SSR). Nunca exposto ao browser.
const API_URL = process.env.API_URL ?? "http://localhost:8080";

export interface EditorQuery {
  niche?: string;
  tool?: string;
  available?: string;
  maxPriceCents?: string;
  q?: string;
  page?: string;
}

export function buildEditorParams(query: EditorQuery): URLSearchParams {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value != null && value !== "") params.set(key, value);
  }
  return params;
}

export async function fetchEditors(query: EditorQuery): Promise<Page<EditorSummary>> {
  const params = buildEditorParams(query);
  const res = await fetch(`${API_URL}/v1/editors?${params.toString()}`, {
    // Vitrine muda conforme editores entram/saem — mantém fresco no MVP.
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error(`Falha ao carregar a vitrine (HTTP ${res.status})`);
  }
  return res.json();
}

export async function fetchEditorBySlug(slug: string): Promise<EditorProfile | null> {
  const res = await fetch(`${API_URL}/v1/editors/${encodeURIComponent(slug)}`, {
    // Sempre fresco no MVP: reflete edições de perfil na hora e evita servir
    // taxonomia/dados defasados. Reintroduzir revalidação (com invalidação no
    // update do perfil) quando otimizarmos SEO/carga.
    cache: "no-store",
  });
  if (res.status === 404) return null;
  if (!res.ok) {
    throw new Error(`Falha ao carregar o editor (HTTP ${res.status})`);
  }
  return res.json();
}
