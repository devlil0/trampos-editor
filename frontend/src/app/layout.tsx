import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { Nav } from "@/components/nav";

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

export const metadata: Metadata = {
  title: "TramposEditor — editores de vídeo short-form",
  description:
    "Encontre editores de vídeo profissionais pra Reels, TikTok e Shorts. Veja portfólio, prazo e preço, e fale direto — sem burocracia.",
};

// Tema de origem = dark. Script inline evita flash aplicando a escolha salva antes da pintura.
const themeScript = `(function(){try{var t=localStorage.getItem("theme")||"dark";document.documentElement.setAttribute("data-theme",t);}catch(e){document.documentElement.setAttribute("data-theme","dark");}})();`;

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-BR" data-theme="dark" className={`${inter.variable} ${jetbrainsMono.variable} h-full`}>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
      <body className="min-h-full flex flex-col antialiased">
        <Nav />
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
