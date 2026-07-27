import type { EditorNiche, EditorTool } from "./types";

const NICHE_LABELS: Record<EditorNiche, string> = {
  SHORTS: "Shorts",
  YOUTUBE: "YouTube",
  PODCAST: "Podcast (cortes)",
};

const TOOL_LABELS: Record<EditorTool, string> = {
  PREMIERE: "Premiere",
  AFTER_EFFECTS: "After Effects",
  DAVINCI_RESOLVE: "DaVinci",
  FINAL_CUT_PRO: "Final Cut",
  CAPCUT: "CapCut",
};

// Filtros da vitrine expõem os nichos como chips.
export const NICHE_OPTIONS = Object.entries(NICHE_LABELS) as [EditorNiche, string][];

export function nicheLabel(n: EditorNiche): string {
  return NICHE_LABELS[n] ?? n;
}

export function toolLabel(t: EditorTool): string {
  return TOOL_LABELS[t] ?? t;
}

/** Centavos de BRL -> "R$ 120". Sem casas quando é valor redondo. */
export function formatPrice(cents: number | null): string | null {
  if (cents == null) return null;
  const reais = cents / 100;
  return reais.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: Number.isInteger(reais) ? 0 : 2,
    maximumFractionDigits: 2,
  });
}

export function formatTurnaround(days: number | null): string | null {
  if (days == null) return null;
  return `${days} ${days === 1 ? "dia" : "dias"}`;
}

/** Extrai o ID de um link do YouTube (watch, youtu.be, shorts, embed). */
export function youtubeId(url: string): string | null {
  const m = url.match(
    /(?:youtu\.be\/|youtube\.com\/(?:watch\?v=|embed\/|shorts\/|live\/))([\w-]{11})/,
  );
  return m ? m[1] : null;
}

/** Thumbnail de capa a partir da URL do vídeo. Null = usar gradiente de fallback. */
export function coverThumbnail(url: string | null | undefined): string | null {
  if (!url) return null;
  const id = youtubeId(url);
  return id ? `https://i.ytimg.com/vi/${id}/hqdefault.jpg` : null;
}
