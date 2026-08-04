import type { EditorNiche, EditorStyle, EditorTool } from "./types";

const NICHE_LABELS: Record<EditorNiche, string> = {
  TIKTOK: "TikTok",
  REELS: "Reels",
  YOUTUBE: "YouTube",
  PODCAST: "Podcast (cortes)",
  // "dark" aqui é canal dark (sem rosto), não tema escuro — o rótulo tem que deixar claro.
  DARK: "Canal dark",
};

/**
 * Ícone da plataforma, gerado por scripts/gen-brand-assets.mjs a partir de media/enterprises.
 * Podcast não é plataforma, então cai no ícone de microfone desenhado no componente.
 */
const NICHE_ICONS: Record<EditorNiche, string | null> = {
  TIKTOK: "/plataformas/tiktok.png",
  REELS: "/plataformas/reels.png",
  YOUTUBE: "/plataformas/youtube.png",
  PODCAST: null,
  // Canal dark não é plataforma e a arte é nossa (media/dark-square-icon.png), mas entra
  // na mesma caixa: aqui marca é a regra, e o nicho sem ela é que é a exceção.
  DARK: "/plataformas/dark.png",
};

const TOOL_LABELS: Record<EditorTool, string> = {
  PREMIERE: "Premiere",
  AFTER_EFFECTS: "After Effects",
  DAVINCI_RESOLVE: "DaVinci",
  FINAL_CUT_PRO: "Final Cut",
  CAPCUT: "CapCut",
};

/** Gerados de media/enterprises por scripts/gen-brand-assets.mjs. */
const TOOL_ICONS: Record<EditorTool, string> = {
  CAPCUT: "/ferramentas/capcut.png",
  PREMIERE: "/ferramentas/premiere.png",
  AFTER_EFFECTS: "/ferramentas/after_effects.png",
  DAVINCI_RESOLVE: "/ferramentas/davinci_resolve.png",
  FINAL_CUT_PRO: "/ferramentas/final_cut_pro.png",
};

const STYLE_LABELS: Record<EditorStyle, string> = {
  DINAMICA: "Dinâmica",
  LEGENDADA: "Legendada",
  CLEAN: "Clean",
  CINEMATOGRAFICA: "Cinematográfica",
  MOTION_GRAPHICS: "Motion graphics",
  STORYTELLING: "Storytelling",
  MEME: "Meme",
  // DARK saiu daqui na V11: virou nicho, e o rótulo dele mora em NICHE_LABELS.
};

// Filtros da vitrine expõem os nichos como chips.
export const NICHE_OPTIONS = Object.entries(NICHE_LABELS) as [EditorNiche, string][];

// Mesma lista, usada nos checkboxes de "monte seu perfil".
export const TOOL_OPTIONS = Object.entries(TOOL_LABELS) as [EditorTool, string][];

export function nicheLabel(n: EditorNiche): string {
  return NICHE_LABELS[n] ?? n;
}

export function nicheIcon(n: EditorNiche): string | null {
  return NICHE_ICONS[n] ?? null;
}

export function toolLabel(t: EditorTool): string {
  return TOOL_LABELS[t] ?? t;
}

export function toolIcon(t: EditorTool): string | null {
  return TOOL_ICONS[t] ?? null;
}

export function styleLabel(s: EditorStyle): string {
  return STYLE_LABELS[s] ?? s;
}

// Checkboxes do "monte seu perfil" e chips de filtro.
export const STYLE_OPTIONS = Object.entries(STYLE_LABELS) as [EditorStyle, string][];

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
