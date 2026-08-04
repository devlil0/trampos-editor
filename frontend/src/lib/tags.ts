import type { EditorNiche, EditorTool } from "./types";

/**
 * Páginas de tag — o SEO long-tail do produto (ESBOCO §2, fase 2).
 *
 * Por que elas existem, já que a vitrine tem filtro: `/editores?niche=TIKTOK` é a mesma
 * página pro Google. Sem URL, título, H1 e texto próprios não há o que rankear. O filtro
 * serve quem já está dentro do site; a tag page serve quem ainda está no Google. Uma
 * retém, a outra capta.
 *
 * O texto de cada página é escrito à mão de propósito. Nove páginas com o mesmo parágrafo
 * trocando uma palavra é *doorway page*: o Google pune, e é o oposto do §6.1 do ESBOCO
 * ("se a escolha pareceria natural num SaaS qualquer, ela não é 1%").
 */

/** Abaixo disto a página existe mas sai com `noindex` — ver `deveIndexar`. */
export const MIN_EDITORES_INDEXAVEL = 3;

export interface Tag {
  /** Segmento da URL: `/editores/{slug}`. */
  slug: string;
  /** Qual filtro da vitrine esta tag aciona. */
  tipo: "niche" | "tool";
  /** Valor do enum no backend. */
  valor: EditorNiche | EditorTool;
  /** `<h1>` da página. */
  h1: string;
  /** `<title>`. Fica fora do h1 porque busca e página têm alvos diferentes. */
  titulo: string;
  /** `<meta name="description">`. */
  descricao: string;
  /**
   * Parágrafo de abertura. É o que separa a página de um filtro com outra roupa —
   * precisa dizer algo que só vale pra esta tag.
   */
  texto: string;
}

/**
 * RASCUNHO DE TEXTO: os campos `h1`, `titulo`, `descricao` e `texto` foram escritos como
 * ponto de partida e esperam revisão do fundador — voz é pilar dele (ESBOCO §9).
 */
const TAGS: Tag[] = [
  {
    slug: "tiktok",
    tipo: "niche",
    valor: "TIKTOK",
    h1: "Editores de vídeo para TikTok",
    titulo: "Editor de vídeo para TikTok | TramposEditor",
    descricao:
      "Editores que cortam pra TikTok: retenção nos primeiros três segundos, legenda que segura e ritmo de feed. Veja portfólio, prazo e preço, e fale direto.",
    texto:
      "No TikTok o vídeo vive ou morre nos três primeiros segundos. Os editores desta lista trabalham com corte de retenção, legenda que acompanha a fala e ritmo pensado pro feed — não é vídeo de YouTube cortado em pé.",
  },
  {
    slug: "reels",
    tipo: "niche",
    valor: "REELS",
    h1: "Editores de vídeo para Reels",
    titulo: "Editor de vídeo para Reels do Instagram | TramposEditor",
    descricao:
      "Editores especializados em Reels: corte vertical, legenda, capa e ritmo que segura até o fim. Portfólio, prazo e preço à mostra, sem lance e sem taxa pra conversar.",
    texto:
      "Reels premia quem assiste até o fim e volta a assistir. Os editores aqui cortam pensando em looping, capa que para o dedo e legenda legível no mudo — que é como a maior parte do Instagram é consumida.",
  },
  {
    slug: "youtube",
    tipo: "niche",
    valor: "YOUTUBE",
    h1: "Editores de vídeo para YouTube",
    titulo: "Editor de vídeo para YouTube | TramposEditor",
    descricao:
      "Editores para YouTube: vídeo longo, Shorts, corte de podcast e edição que sustenta a retenção. Veja o trabalho de cada um, o prazo e o preço antes de falar.",
    texto:
      "YouTube é o formato em que a edição aparece menos e pesa mais: o que segura é ritmo, corte de respiro e apoio visual na hora certa. Os editores desta lista trabalham tanto o vídeo longo quanto o corte vertical que sai dele.",
  },
  {
    slug: "podcast",
    tipo: "niche",
    valor: "PODCAST",
    h1: "Editores de cortes de podcast",
    titulo: "Editor de cortes de podcast | TramposEditor",
    descricao:
      "Editores que transformam episódio de podcast em corte vertical: escolha do trecho, legenda e ritmo pra TikTok, Reels e Shorts.",
    texto:
      "Corte de podcast é meio trabalho de edição e meio trabalho de curadoria: alguém precisa achar, dentro de duas horas de conversa, os trinta segundos que sustentam sozinhos. Os editores aqui fazem as duas coisas.",
  },
  {
    slug: "canal-dark",
    tipo: "niche",
    valor: "DARK",
    h1: "Editores para canal dark",
    titulo: "Editor de vídeo para canal dark | TramposEditor",
    descricao:
      "Editores que trabalham canal sem rosto: narração sobre imagem de apoio, ritmo de retenção e roteiro que segura sem ninguém na tela.",
    texto:
      "Canal dark tira da edição o que outros formatos resolvem com carisma: não há rosto pra segurar ninguém, então quem segura é o ritmo, a imagem de apoio na hora certa e o corte que não deixa a narração respirar demais.",
  },
  {
    slug: "premiere",
    tipo: "tool",
    valor: "PREMIERE",
    h1: "Editores que trabalham no Premiere Pro",
    titulo: "Editor de vídeo Premiere Pro | TramposEditor",
    descricao:
      "Editores de vídeo que dominam o Adobe Premiere Pro, com portfólio, prazo e preço à mostra. Fale direto com quem edita, sem intermediário.",
    texto:
      "Premiere é o padrão de mercado, e importa quando o projeto precisa passar de mão ou conversar com After Effects. Se você já tem uma esteira montada nele, contratar quem trabalha no mesmo software poupa retrabalho de arquivo.",
  },
  {
    slug: "capcut",
    tipo: "tool",
    valor: "CAPCUT",
    h1: "Editores que trabalham no CapCut",
    titulo: "Editor de vídeo CapCut | TramposEditor",
    descricao:
      "Editores que usam CapCut para conteúdo vertical: entrega rápida, legenda automática revisada e formato nativo de TikTok e Reels.",
    texto:
      "CapCut virou padrão do vertical por um motivo prático: entrega rápido e nasceu dentro do formato. Vale pra quem publica em volume e precisa de giro curto, não pra quem quer composição pesada.",
  },
  {
    slug: "after-effects",
    tipo: "tool",
    valor: "AFTER_EFFECTS",
    h1: "Editores que trabalham no After Effects",
    titulo: "Editor de vídeo After Effects | TramposEditor",
    descricao:
      "Editores de After Effects para motion graphics, animação de texto e composição. Veja o portfólio de cada um antes de falar.",
    texto:
      "After Effects é o software de quando a edição precisa de coisa que não existe no material bruto: texto animado, elemento gráfico, composição. É trabalho de outra natureza que cortar — e costuma ter outro prazo.",
  },
  {
    slug: "davinci-resolve",
    tipo: "tool",
    valor: "DAVINCI_RESOLVE",
    h1: "Editores que trabalham no DaVinci Resolve",
    titulo: "Editor de vídeo DaVinci Resolve | TramposEditor",
    descricao:
      "Editores de DaVinci Resolve, com foco em correção de cor e acabamento. Portfólio, prazo e preço à mostra.",
    texto:
      "DaVinci é a referência em cor, e é por aí que ele entra na maioria dos projetos: quando o vídeo precisa ter cara de coisa tratada, não só de coisa cortada.",
  },
  {
    slug: "final-cut-pro",
    tipo: "tool",
    valor: "FINAL_CUT_PRO",
    h1: "Editores que trabalham no Final Cut Pro",
    titulo: "Editor de vídeo Final Cut Pro | TramposEditor",
    descricao:
      "Editores que usam Final Cut Pro, com portfólio, prazo e preço à mostra. Fale direto com quem vai editar.",
    texto:
      "Final Cut é rápido em máquina Apple e tem uma linha de montagem própria que quem vem dele defende. Se o seu fluxo já é Mac, contratar quem trabalha nele evita conversão de projeto no meio do caminho.",
  },
];

const POR_SLUG = new Map(TAGS.map((tag) => [tag.slug, tag]));

export function acharTag(slug: string): Tag | null {
  return POR_SLUG.get(slug) ?? null;
}

/**
 * Slugs que nenhum editor pode tomar. O perfil mora em `/editores/{slug}`, a mesma forma
 * da tag: sem reserva, alguém chamado "CapCut" geraria `capcut` e sequestraria a página.
 * O backend também recusa (ver `SlugService`) — aqui é só a lista compartilhada.
 */
export const SLUGS_RESERVADOS = TAGS.map((tag) => tag.slug);

/**
 * Página de tag com estoque magro não rankeia e ainda parece produto morto pra quem chega.
 * Pior: uma vez indexada, ela fica. Então a página existe e é navegável desde o primeiro
 * dia, mas só pede indexação quando tem gente pra mostrar.
 */
export function deveIndexar(totalEditores: number): boolean {
  return totalEditores >= MIN_EDITORES_INDEXAVEL;
}

export { TAGS };
