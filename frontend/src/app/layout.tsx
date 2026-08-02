import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
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
        <footer className="border-t border-line text-sm text-muted">
          <div className="mx-auto max-w-6xl px-6 py-10">
            <div className="flex flex-wrap gap-x-10 gap-y-7">
              <div className="min-w-[200px] flex-1">
                <Link href="/" className="flex items-center gap-2.5 font-bold tracking-tight text-fg">
                  <Image src="/logo-mark.png" alt="" width={28} height={28} className="rounded-full" />
                  Trampos<span className="-ml-2.5 text-accent">Editor</span>
                </Link>
                <p className="mt-3 max-w-[34ch]">
                  Editores de vídeo para Reels, TikTok e Shorts. Sem lance, sem assinatura.
                </p>
              </div>

              <nav aria-label="Para quem contrata">
                <h2 className="mb-2.5 font-mono text-[11px] font-semibold tracking-[0.14em] text-fg">
                  CONTRATAR
                </h2>
                <ul className="flex flex-col gap-2">
                  <FooterLink href="/editores">Ver editores</FooterLink>
                  <FooterLink href="/painel/pedidos/novo">Solicitar serviço</FooterLink>
                  <FooterLink href="/para-clientes">Como funciona</FooterLink>
                </ul>
              </nav>

              <nav aria-label="Para quem edita">
                <h2 className="mb-2.5 font-mono text-[11px] font-semibold tracking-[0.14em] text-fg">
                  EDITAR
                </h2>
                <ul className="flex flex-col gap-2">
                  <FooterLink href="/criar-conta">Publicar meu perfil</FooterLink>
                  <FooterLink href="/painel/trampos">Trampos abertos</FooterLink>
                  <FooterLink href="/para-editores">Como funciona</FooterLink>
                </ul>
              </nav>

              <nav aria-label="Conta">
                <h2 className="mb-2.5 font-mono text-[11px] font-semibold tracking-[0.14em] text-fg">
                  CONTA
                </h2>
                <ul className="flex flex-col gap-2">
                  <FooterLink href="/entrar">Entrar</FooterLink>
                  <FooterLink href="/criar-conta">Criar conta</FooterLink>
                  <FooterLink href="/painel">Painel</FooterLink>
                </ul>
              </nav>
            </div>

            <p className="mt-9 border-t border-line pt-6">© 2026 TramposEditor</p>
          </div>
        </footer>
      </body>
    </html>
  );
}

function FooterLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <li>
      <Link href={href} className="hover:text-accent">
        {children}
      </Link>
    </li>
  );
}
