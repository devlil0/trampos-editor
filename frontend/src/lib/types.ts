// Espelha os DTOs do backend (com.devlil0.tramposeditor.dto).

export type Vertical = "VIDEO" | "AUDIO";
export type UserRole = "EDITOR" | "CLIENT";

export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
  /** Identidade no produto e endereço da pessoa em `/{username}`. */
  username: string;
  /**
   * False enquanto a pessoa não escolheu o próprio nome — só acontece em conta criada pelo
   * provedor social, onde não há formulário onde perguntar. É o que faz `/painel/layout`
   * interromper a navegação e mandar pra `/escolher-username`.
   */
  usernameChosen: boolean;
  /** Última troca; null se nunca trocou. Alimenta o cooldown mostrado nas configurações. */
  usernameChangedAt: string | null;
  /**
   * Se o dono já provou o endereço. False silencia todo aviso automático e impede publicar
   * perfil de editor — conta criada pelo Google/GitHub já nasce true.
   */
  emailVerified: boolean;
  /** Versão da foto de perfil; null quando não há. Entra na URL da imagem. */
  avatarVersion: string | null;
  /** Username do Instagram, sem "@". Null enquanto a pessoa não informar. */
  instagram: string | null;
  roles: UserRole[];
  active: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  /** Segundos até o token expirar (JwtService.ttl). */
  expiresIn: number;
  user: AuthUser;
}
export type EditorNiche = "TIKTOK" | "REELS" | "YOUTUBE" | "PODCAST" | "DARK";
export type EditorTool =
  | "PREMIERE"
  | "AFTER_EFFECTS"
  | "DAVINCI_RESOLVE"
  | "FINAL_CUT_PRO"
  | "CAPCUT";
/** Como o editor corta — ortogonal à plataforma. */
export type EditorStyle =
  | "DINAMICA"
  | "LEGENDADA"
  | "CLEAN"
  | "CINEMATOGRAFICA"
  | "MOTION_GRAPHICS"
  | "STORYTELLING"
  // DARK saiu daqui na V11: virou EditorNiche.
  | "MEME";
export type PortfolioProvider = "YOUTUBE" | "VIMEO";

export interface PortfolioItem {
  id: string;
  title: string;
  provider: PortfolioProvider;
  videoUrl: string;
  position: number;
}

export interface EditorSummary {
  id: string;
  /** Histórico: o endereço do perfil é o `username`, não isto. */
  slug: string;
  /** Onde o perfil mora hoje — `/{username}`. */
  username: string;
  /** Versão da foto; null quando não há. */
  avatarVersion: string | null;
  displayName: string;
  headline: string | null;
  niches: EditorNiche[];
  tools: EditorTool[];
  styles: EditorStyle[];
  priceMinCents: number | null;
  turnaroundDays: number | null;
  available: boolean;
  ratingAvg: number;
  ratingCount: number;
  coverVideoUrl: string | null;
}

export interface EditorProfile {
  id: string;
  userId: string;
  slug: string;
  displayName: string;
  /** Vem da conta do usuário, não do perfil de editor. */
  instagram: string | null;
  vertical: Vertical;
  headline: string | null;
  bio: string | null;
  niches: EditorNiche[];
  tools: EditorTool[];
  styles: EditorStyle[];
  priceMinCents: number | null;
  priceMaxCents: number | null;
  turnaroundDays: number | null;
  available: boolean;
  ratingAvg: number;
  ratingCount: number;
  portfolio: PortfolioItem[];
}

export type JobRequestStatus = "OPEN" | "CLOSED";
export type JobInterestStatus = "PENDING" | "ACCEPTED";

/** Candidatura de um editor, como o dono do pedido vê. E-mail só vem após o aceite. */
export interface JobInterest {
  id: string;
  editorDisplayName: string;
  editorSlug: string | null;
  message: string;
  status: JobInterestStatus;
  editorEmail: string | null;
  createdAt: string;
}

/** Pedido como o dono vê. */
export interface JobRequest {
  id: string;
  title: string;
  description: string;
  niche: EditorNiche;
  budgetCents: number | null;
  deadlineDays: number | null;
  referenceUrl: string | null;
  status: JobRequestStatus;
  interests: JobInterest[];
  createdAt: string;
}

/** Pedido como o editor vê. `clientEmail` só vem preenchido se a candidatura foi aceita. */
export interface JobRequestFeedItem {
  id: string;
  title: string;
  description: string;
  niche: EditorNiche;
  budgetCents: number | null;
  deadlineDays: number | null;
  referenceUrl: string | null;
  clientDisplayName: string;
  interestCount: number;
  myInterestStatus: JobInterestStatus | null;
  clientEmail: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

/**
 * Linha da caixa de entrada. O servidor já resolve o "outro lado" — a tela nunca
 * precisa saber se, naquela conversa, o usuário contrata ou edita.
 */
export interface ConversationSummary {
  id: string;
  counterpartDisplayName: string;
  /** Só existe quando o outro lado tem perfil público de editor. */
  counterpartSlug: string | null;
  /** Título do pedido de origem, ou null quando a conversa veio do perfil. */
  jobTitle: string | null;
  lastMessagePreview: string | null;
  unread: boolean;
  lastMessageAt: string | null;
  createdAt: string;
}

/** `mine` vem pronto do backend: o browser nunca vê o JWT pra deduzir isso. */
export interface Message {
  id: string;
  senderDisplayName: string;
  mine: boolean;
  body: string;
  createdAt: string;
}

export interface ConversationDetail {
  id: string;
  counterpartDisplayName: string;
  counterpartSlug: string | null;
  jobRequestId: string | null;
  jobTitle: string | null;
  /** Sou o lado que contrata — decide quem registra e encerra o trampo. */
  iAmClient: boolean;
  messages: Message[];
  createdAt: string;
}

export type DealStatus = "OPEN" | "DONE" | "CANCELLED";

export interface Review {
  id: string;
  authorName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

/**
 * Um trampo combinado. O `counterpartSlug` é o que faz o "chamar de novo" funcionar
 * em um clique — repetição de fornecedor é o gatilho do laço da demanda (ESBOCO §3.2).
 */
export interface Deal {
  id: string;
  conversationId: string;
  title: string;
  status: DealStatus;
  counterpartName: string;
  counterpartSlug: string | null;
  iAmClient: boolean;
  reviewedByMe: boolean;
  reviewOfMe: Review | null;
  createdAt: string;
  closedAt: string | null;
}

/**
 * A página de uma pessoa em `/{username}` — os dois papéis na mesma forma.
 *
 * `editor` vem nulo pra quem nunca publicou perfil: é o que distingue quem edita de quem
 * só contrata, sem precisar de um campo de tipo.
 */
export interface PublicProfile {
  username: string;
  displayName: string;
  /** Versão da foto; null quando não há. */
  avatarVersion: string | null;
  instagram: string | null;
  memberSince: string;
  editor: EditorProfile | null;
  dealsClosed: number;
  /** Avaliações que a pessoa escreveu — participação, não reputação. */
  reviewsWritten: number;
  ratingAvg: number;
  ratingCount: number;
}
