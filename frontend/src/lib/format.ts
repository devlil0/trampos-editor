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

type PortfolioVideo = { url: string; youtubeId?: string };

/** Espelha a validação do backend e também protege a renderização de itens antigos. */
function parsePortfolioVideo(value: string): PortfolioVideo | null {
  const input = value.trim();
  const parts = /^(https?):\/\/([^/?#]+)([^?#]*)/i.exec(input);
  if (!parts || /[\s\\]|%(?![0-9a-f]{2})/i.test(input)) return null;

  let url: URL;
  try {
    url = new URL(input);
  } catch {
    return null;
  }

  const host = url.hostname;
  const authority = parts[2].toLowerCase();
  const defaultPort = url.protocol === "https:" ? "443" : "80";
  if (authority !== host && authority !== `${host}:${defaultPort}`) return null;
  const path = parts[3];

  let id: string | undefined;
  switch (host) {
    case "youtu.be":
      id = /^\/([\w-]{11})\/?$/.exec(path)?.[1];
      break;
    case "youtube.com":
    case "www.youtube.com":
    case "m.youtube.com":
      if (path === "/watch") {
        const ids = url.searchParams.getAll("v");
        if (ids.length === 1) id = ids[0];
      } else {
        id = /^\/(?:shorts|embed|live)\/([\w-]{11})\/?$/.exec(path)?.[1];
      }
      break;
    case "vimeo.com":
    case "www.vimeo.com": {
      const match = /^\/([0-9]+)(?:\/([a-z0-9]+))?\/?$/i.exec(path);
      return match ? { url: `https://vimeo.com/${match[1]}${match[2] ? `/${match[2]}` : ""}` } : null;
    }
    case "player.vimeo.com": {
      const match = /^\/video\/([0-9]+)\/?$/.exec(path);
      const hashes = url.searchParams.getAll("h");
      if (!match || hashes.length > 1 || (hashes.length === 1 && !/^[a-z0-9]+$/i.test(hashes[0]))) return null;
      return { url: `https://vimeo.com/${match[1]}${hashes.length ? `/${hashes[0]}` : ""}` };
    }
    default:
      return null;
  }
  return id && /^[\w-]{11}$/.test(id)
    ? { url: `https://www.youtube.com/watch?v=${id}`, youtubeId: id }
    : null;
}

export function portfolioVideoUrl(url: string): string | null {
  return parsePortfolioVideo(url)?.url ?? null;
}

/** Thumbnail de capa a partir da URL do vídeo. Null = usar gradiente de fallback. */
export function coverThumbnail(url: string | null | undefined): string | null {
  if (!url) return null;
  const id = parsePortfolioVideo(url)?.youtubeId;
  return id ? `https://i.ytimg.com/vi/${id}/hqdefault.jpg` : null;
}
