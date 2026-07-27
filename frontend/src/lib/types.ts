// Espelha os DTOs do backend (com.devlil0.tramposeditor.dto).

export type Vertical = "VIDEO" | "AUDIO";
export type EditorNiche = "SHORTS" | "YOUTUBE" | "PODCAST";
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

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
