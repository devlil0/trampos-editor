"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

/**
 * Navegação principal de quem está logado — barra flutuante no centro inferior.
 *
 * Fica embaixo, e não numa coluna à esquerda, por decisão do fundador (2026-08-04): o
 * polegar alcança a base da tela, e no desktop o rodapé é a borda que menos disputa
 * espaço com o conteúdo. É a forma que app de celular consagrou, trazida pro desktop.
 *
 * Três destinos, e só. Notificação ficou de fora de propósito — não existe central in-app,
 * só e-mail, e ícone que não leva a lugar nenhum é pior que ausência.
 *
 * Aparece apenas na área logada: as páginas públicas são o funil de aquisição e seguem com
 * a barra do topo, que é linguagem de site, não de app.
 */
const ITENS = [
  {
    href: "/editores",
    label: "Feed",
    // Casa com as duas classes do feed (2026-08-06): `editores` mora em /editores, junto
    // das páginas de tag, e `trampos` em /painel/feed. São uma tela só pra quem navega,
    // então o item precisa acender nas duas — senão a chave parece sair do feed.
    match: (p: string) =>
      p === "/editores" || p.startsWith("/editores/") || p.startsWith("/painel/feed"),
    icon: (
      <path d="M3 10.5 12 3l9 7.5M5.5 9.5V20h13V9.5" />
    ),
  },
  {
    href: "/painel/conversas",
    label: "Mensagens",
    match: (p: string) => p.startsWith("/painel/conversas"),
    icon: <path d="M21 11.5a8 8 0 0 1-8.5 8 9 9 0 0 1-3.9-.8L3 20l1.4-4.2A8 8 0 0 1 12 3.5a8 8 0 0 1 9 8z" />,
  },
] as const;

export function BottomDock({
  username,
  displayName,
  avatarUrl,
}: {
  username: string;
  displayName: string;
  /** URL pronta: montada no servidor, onde `PUBLIC_API_URL` existe. */
  avatarUrl: string | null;
}) {
  const pathname = usePathname();
  const perfilAtivo = pathname === `/${username}` || pathname.startsWith("/painel/meu-perfil");

  return (
    // pb com safe-area: no iPhone a barra de gestos come a base da tela.
    <nav
      aria-label="Navegação principal"
      className="fixed inset-x-0 bottom-0 z-30 flex justify-center px-4 pb-[max(1rem,env(safe-area-inset-bottom))]"
    >
      <div className="pointer-events-auto flex items-center gap-1 rounded-2xl border border-line bg-card/90 p-1.5 shadow-[var(--shadow)] backdrop-blur-md">
        {ITENS.map((item) => (
          <DockLink
            key={item.href}
            href={item.href}
            label={item.label}
            active={item.match(pathname)}
          >
            <svg width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              {item.icon}
            </svg>
          </DockLink>
        ))}

        <DockLink href={`/${username}`} label="Perfil" active={perfilAtivo}>
          {/* A foto, como no menu do Instagram: é o item que precisa parecer "você", não
              mais um ícone de linha. Sem foto, a inicial no círculo. */}
          {avatarUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={avatarUrl} alt="" width={21} height={21} className="h-[21px] w-[21px] rounded-full object-cover" />
          ) : (
            <span
              className={`grid h-[21px] w-[21px] place-items-center rounded-full text-[10px] font-extrabold ${
                perfilAtivo ? "bg-accent-contrast text-accent" : "bg-accent text-accent-contrast"
              }`}
            >
              {displayName.charAt(0).toUpperCase()}
            </span>
          )}
        </DockLink>
      </div>
    </nav>
  );
}

function DockLink({
  href,
  label,
  active,
  children,
}: {
  href: string;
  label: string;
  active: boolean;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      aria-current={active ? "page" : undefined}
      className={`flex items-center gap-2 rounded-xl px-3.5 py-2 text-sm font-semibold transition ${
        active ? "bg-accent text-accent-contrast" : "text-muted hover:bg-panel hover:text-fg"
      }`}
    >
      {children}
      {/* O rótulo some em tela estreita: três ícones cabem, três ícones com texto não. */}
      <span className="hidden sm:inline">{label}</span>
    </Link>
  );
}
