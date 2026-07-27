// Espelha os DTOs do backend (com.devlil0.tramposeditor.dto).

export type Vertical = "VIDEO" | "AUDIO";
export type UserRole = "EDITOR" | "CLIENT";

export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
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
export type EditorNiche = "TIKTOK" | "REELS" | "YOUTUBE" | "PODCAST";
export type EditorTool =
  | "PREMIERE"
  | "AFTER_EFFECTS"
  | "DAVINCI_RESOLVE"
  | "FINAL_CUT_PRO"
  | "CAPCUT";
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
  slug: string;
  displayName: string;
  headline: string | null;
  niches: EditorNiche[];
  tools: EditorTool[];
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
  vertical: Vertical;
  headline: string | null;
  bio: string | null;
  niches: EditorNiche[];
  tools: EditorTool[];
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
