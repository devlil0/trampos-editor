import Image from "next/image";
import type { EditorNiche, EditorStyle, EditorTool } from "@/lib/types";
import { nicheIcon, nicheLabel, styleLabel, toolIcon, toolLabel } from "@/lib/format";

/**
 * Ícone da plataforma do nicho. Sem hooks de propósito: é usado tanto em Server
 * Component (card, perfil) quanto em Client Component (filtros da vitrine).
 */
export function NicheIcon({ niche, size = 16 }: { niche: EditorNiche; size?: number }) {
  const icon = nicheIcon(niche);

  if (!icon) {
    // Podcast não tem marca — microfone desenhado, herda a cor do texto.
    return (
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        aria-hidden="true"
      >
        <rect x="9" y="3" width="6" height="11" rx="3" />
        <path d="M5 11a7 7 0 0 0 14 0M12 18v3" />
      </svg>
    );
  }

  return (
    <Image
      src={icon}
      alt=""
      width={size}
      height={size}
      // O ícone do YouTube é largo e os outros são quadrados: `contain` mantém
      // todos dentro da mesma caixa sem distorcer nenhum.
      className="object-contain"
      style={{ width: size, height: size }}
    />
  );
}

/**
 * Tag de plataforma. Fundo neutro de propósito — quem carrega a cor é a marca,
 * então o chip violeta sairia brigando com o vermelho do YouTube e o gradiente do Reels.
 */
export function NicheTag({ niche }: { niche: EditorNiche }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-md border border-line bg-panel px-2 py-1 text-xs font-semibold text-fg">
      <NicheIcon niche={niche} />
      {nicheLabel(niche)}
    </span>
  );
}

/** Ícone da ferramenta, quando existe arte pra ela. */
export function ToolIcon({ tool, size = 15 }: { tool: EditorTool; size?: number }) {
  const icon = toolIcon(tool);
  if (!icon) return null;

  return (
    <Image
      src={icon}
      alt=""
      width={size}
      height={size}
      className="object-contain"
      style={{ width: size, height: size }}
    />
  );
}

export function ToolTag({ tool }: { tool: EditorTool }) {
  return (
    <span className="inline-flex items-center gap-1.5 rounded-md bg-panel px-2 py-1 text-xs font-semibold text-muted">
      <ToolIcon tool={tool} />
      {toolLabel(tool)}
    </span>
  );
}

/**
 * Estilo de edição. Contorno tracejado em vez de fundo cheio: é a terceira dimensão
 * da tag e precisa se distinguir de plataforma (fundo) e ferramenta (chapado).
 */
export function StyleTag({ style }: { style: EditorStyle }) {
  return (
    <span className="inline-flex items-center rounded-md border border-dashed border-accent/45 px-2 py-1 text-xs font-semibold text-accent-strong">
      {styleLabel(style)}
    </span>
  );
}
