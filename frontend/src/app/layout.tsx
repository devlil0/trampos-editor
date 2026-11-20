import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { Inter, JetBrains_Mono } from "next/font/google";
import Script from "next/script";
import "./globals.css";
import { BottomDock } from "@/components/bottom-dock";
import { Nav } from "@/components/nav";
import { avatarUrl } from "@/lib/api";
import { getSessionState } from "@/lib/session";

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
//
// Vai por `next/script` com `beforeInteractive`, e não como <script> cru no <head>: uma tag
// <script> dentro de componente React não executa quando o layout renderiza no cliente — o
// React avisa isso no console, e acontece de verdade aqui (o retorno do callback OAuth
// re-renderiza o layout). `beforeInteractive` injeta no HTML inicial do servidor e roda
// antes de qualquer módulo do Next, que é justamente o que o anti-flash precisa.
const themeScript = `(function(){try{var t=localStorage.getItem("theme")||"dark";document.documentElement.setAttribute("data-theme",t);}catch(e){document.documentElement.setAttribute("data-theme","dark");}})();`;

export default async function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  // Ler a sessão aqui torna as rotas dinâmicas — aceitável porque a vitrine já era
  // `no-store` e continua renderizada no servidor (o HTML pro SEO não muda).
  const { user, status } = await getSessionState();

  return (
    <html lang="pt-BR" data-theme="dark" className={`${inter.variable} ${jetbrainsMono.variable} h-full`}>
      <body className="min-h-full flex flex-col antialiased">
        {/* `id` é obrigatório em script inline — é como o Next rastreia e não duplica. */}
        <Script id="theme-init" strategy="beforeInteractive" dangerouslySetInnerHTML={{ __html: themeScript }} />
        <Nav user={user} sessionUnavailable={status === "unavailable"} />
        {/* pb quando logado: a barra flutuante cobre a base, e sem isso ela tapa o fim de
            toda página rolada até o fim. */}
        <main className={`flex-1 ${user ? "pb-24" : ""}`}>{children}</main>
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
                  <FooterLink href="/painel/feed">Trampos abertos</FooterLink>
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
                  {/* "Painel" saiu daqui em 2026-08-04 junto com o hub. Nada tomou o
                      lugar: quem está logado navega pela barra de baixo, e "Vitrine"
                      aqui seria a mesma coisa que o "Ver editores" da coluna ao lado. */}
                </ul>
              </nav>
            </div>

            <p className="mt-9 border-t border-line pt-6">© 2026 TramposEditor</p>
          </div>
        </footer>

        {/* Navegação de app, só pra quem entrou: as páginas públicas são funil de
            aquisição e seguem só com a barra do topo. */}
        {user && (
          <BottomDock
            username={user.username}
            displayName={user.displayName}
            avatarUrl={avatarUrl(user.username, user.avatarVersion)}
          />
        )}
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
