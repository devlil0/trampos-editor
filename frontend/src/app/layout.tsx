import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { Nav } from "@/components/nav";
import { getCurrentUser } from "@/lib/session";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

// Usado só no slogan "Simples" do hero (font-mono via token do tema).
const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-jetbrains",
  display: "swap",
});

const title = "TramposEditor — editores de vídeo short-form";
const description =
  "Encontre editores de vídeo profissionais pra Reels, TikTok e Shorts. Veja portfólio, prazo e preço, e fale direto — sem burocracia.";

// Domínio ainda não registrado: o site real entra por env no deploy.
const siteUrl = process.env.SITE_URL ?? "http://localhost:3000";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title,
  description,
  applicationName: "TramposEditor",
  // Ícones e preview de link vêm das convenções de arquivo em src/app/
  // (icon.png, apple-icon.png, favicon.ico, opengraph-image.png, twitter-image.png).
  openGraph: {
    type: "website",
    locale: "pt_BR",
    siteName: "TramposEditor",
    title,
    description,
    url: "/",
  },
  twitter: { card: "summary_large_image", title, description },
};

// Tema de origem = dark. Script inline evita flash aplicando a escolha salva antes da pintura.
const themeScript = `(function(){try{var t=localStorage.getItem("theme")||"dark";document.documentElement.setAttribute("data-theme",t);}catch(e){document.documentElement.setAttribute("data-theme","dark");}})();`;

export default async function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  // Ler a sessão aqui torna as rotas dinâmicas — aceitável porque a vitrine já era
  // `no-store` e continua renderizada no servidor (o HTML pro SEO não muda).
  const user = await getCurrentUser();

  return (
    <html lang="pt-BR" data-theme="dark" className={`${inter.variable} ${jetbrainsMono.variable} h-full`}>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
      <body className="min-h-full flex flex-col antialiased">
        <Nav user={user} />
        <main className="flex-1">{children}</main>
        <footer className="border-t border-line text-muted text-sm">
          <div className="mx-auto max-w-6xl px-6 py-7 flex flex-wrap justify-between gap-3">
            <span>© 2026 TramposEditor</span>
            <span>Reels, TikTok & Shorts</span>
          </div>
        </footer>
      </body>
    </html>
  );
}
