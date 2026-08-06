import type {
  ConversationDetail,
  ConversationSummary,
  Deal,
  EditorProfile,
  EditorSummary,
  JobRequest,
  JobRequestFeedItem,
  Page,
  PublicProfile,
} from "./types";

// Lido só no servidor (SSR). Nunca exposto ao browser.
const API_URL = process.env.API_URL ?? "http://localhost:8080";

/**
 * Endereço do backend **como o browser o alcança**. Diferente de `API_URL`, que pode ser
 * um host interno: o login social manda a pessoa direto pro backend
 * (`/oauth2/authorization/google`), então esse endereço precisa existir do lado de fora.
 * Em dev os dois coincidem.
 */
export const PUBLIC_API_URL = process.env.PUBLIC_API_URL ?? API_URL;

/**
 * Quais provedores de login estão ligados. Lista vazia quando o ambiente não tem
 * credencial — aí as telas de login ficam só com e-mail e senha.
 */
export async function fetchSocialProviders(): Promise<string[]> {
  try {
    const res = await fetch(`${API_URL}/v1/auth/social/providers`, { cache: "no-store" });
    if (!res.ok) return [];
    return res.json();
  } catch {
    // Backend fora do ar não pode derrubar a tela de login: sem provedor, ela ainda
    // serve pra entrar com senha.
    return [];
  }
}

export interface EditorQuery {
  niche?: string;
  tool?: string;
  style?: string;
  available?: string;
  maxPriceCents?: string;
  q?: string;
  page?: string;
  /** Útil pra quem só quer o `totalElements` — `size=1` evita trazer a página toda. */
  size?: string;
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

async function authGet<T>(path: string, token: string, fallback: T): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  // Sessão inválida não derruba a página — o layout já trata como deslogado.
  if (res.status === 401 || res.status === 403) return fallback;
  if (!res.ok) throw new Error(`Falha ao carregar ${path} (HTTP ${res.status})`);
  return res.json();
}

/** Pedidos que eu publiquei, com as candidaturas recebidas. */
export function fetchMyJobRequests(token: string): Promise<JobRequest[]> {
  return authGet<JobRequest[]>("/v1/me/job-requests", token, []);
}

/**
 * A classe `trampos` do feed tem recorte curto de propósito: uma categoria e uma página.
 * Filtro fino é assunto da vitrine pública, que existe pra ser garimpada — e é pra lá que
 * a chave do feed manda quem quer garimpar (`FeedSwitch`).
 */
export interface FeedQuery {
  niche?: string;
  page?: string;
}

function feedParams(query: FeedQuery): string {
  const params = new URLSearchParams();
  if (query.niche) params.set("niche", query.niche);
  if (query.page && query.page !== "0") params.set("page", query.page);
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

/** Sessão caída não derruba a tela — devolve página vazia com a forma do envelope real. */
function emptyPage<T>(): Page<T> {
  return { content: [], page: 0, size: 0, totalElements: 0, totalPages: 0, last: true };
}

/** Feed da demanda: trampos abertos que outras pessoas publicaram. */
export function fetchJobFeed(
  token: string,
  query: FeedQuery = {},
): Promise<Page<JobRequestFeedItem>> {
  return authGet<Page<JobRequestFeedItem>>(
    `/v1/job-requests${feedParams(query)}`,
    token,
    emptyPage<JobRequestFeedItem>(),
  );
}

// `fetchEditorFeed` saiu em 2026-08-04 junto com o feed da oferta: a lista de editores
// disponíveis virou `fetchEditors({ available: "true" })`, um recorte da vitrine.

/** Pedidos em que eu me candidatei. */
export function fetchMyJobInterests(token: string): Promise<JobRequestFeedItem[]> {
  return authGet<JobRequestFeedItem[]>("/v1/me/job-interests", token, []);
}

/** Perfil de editor do usuário logado. 404 = ainda não criou o perfil. */
export async function fetchMyEditorProfile(token: string): Promise<EditorProfile | null> {
  const res = await fetch(`${API_URL}/v1/me/editor-profile`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  if (res.status === 404 || res.status === 401 || res.status === 403) return null;
  if (!res.ok) {
    throw new Error(`Falha ao carregar seu perfil (HTTP ${res.status})`);
  }
  return res.json();
}

/** O histórico: trampos combinados pelos dois lados, mais recente primeiro. */
export function fetchMyDeals(token: string): Promise<Deal[]> {
  return authGet<Deal[]>("/v1/me/deals", token, []);
}

/** A caixa de entrada: conversas das duas portas, mais recente primeiro. */
export function fetchMyConversations(token: string): Promise<ConversationSummary[]> {
  return authGet<ConversationSummary[]>("/v1/me/conversations", token, []);
}

/**
 * Abre uma conversa. Não é leitura pura: o backend marca como lida no mesmo passo,
 * porque abrir *é* ler — não existe botão de "marcar como lida".
 * Devolve null pra conversa inexistente ou de outra pessoa, e a página trata como 404.
 */
export async function fetchConversation(token: string, id: string): Promise<ConversationDetail | null> {
  const res = await fetch(`${API_URL}/v1/me/conversations/${encodeURIComponent(id)}`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  if (res.status === 404 || res.status === 403 || res.status === 401) return null;
  if (!res.ok) throw new Error(`Falha ao carregar a conversa (HTTP ${res.status})`);
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

/**
 * O perfil público de uma pessoa, por username — a página de `/{username}`.
 *
 * Devolve null em 404 pra que a rota chame `notFound()` em vez de estourar: a raiz virou
 * namespace de usuário, então qualquer endereço digitado errado cai aqui.
 */
export async function fetchPublicProfile(username: string): Promise<PublicProfile | null> {
  const res = await fetch(`${API_URL}/v1/profiles/${encodeURIComponent(username)}`, {
    cache: "no-store",
  });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Falha ao carregar o perfil (HTTP ${res.status})`);
  return res.json();
}

/**
 * Pra onde uma URL antiga de editor (`/editores/{slug}`) deve apontar agora.
 * Null quando o slug não existe — aí o link morto continua 404, em vez de virar
 * redirecionamento pra lugar nenhum.
 */
export async function fetchUsernameBySlug(slug: string): Promise<string | null> {
  const res = await fetch(`${API_URL}/v1/profiles/by-slug/${encodeURIComponent(slug)}`, {
    cache: "no-store",
  });
  if (!res.ok) return null;
  return (await res.json()).username ?? null;
}

/**
 * URL da foto de perfil, ou null quando não há.
 *
 * Montada aqui e passada pronta às telas: `PUBLIC_API_URL` é lido do ambiente do servidor
 * e não existe no browser. A versão vai na query porque o backend serve a imagem com
 * cache de um ano — é a troca dela que faz o browser buscar de novo.
 */
export function avatarUrl(username: string, version: string | null | undefined): string | null {
  if (!version) return null;
  return `${PUBLIC_API_URL}/v1/avatars/${encodeURIComponent(username)}?v=${version}`;
}
