"use client";

import Image from "next/image";
import Link from "next/link";
import { useCallback, useSyncExternalStore } from "react";
import { logoutAction } from "@/app/actions/auth";
import type { AuthUser } from "@/lib/types";

/**
 * A fonte da verdade do tema é o atributo `data-theme` no <html>, escrito pelo script
 * inline do layout antes da primeira pintura. O botão lê esse DOM externo em vez de
 * duplicá-lo em estado — por isso useSyncExternalStore e não useState+useEffect.
 */
function subscribeToTheme(onChange: () => void) {
  const observer = new MutationObserver(onChange);
  observer.observe(document.documentElement, { attributeFilter: ["data-theme"] });
  return () => observer.disconnect();
}

function readTheme(): "dark" | "light" {
  return document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark";
}

export function Nav({ user }: { user: AuthUser | null }) {
  // No servidor o tema é sempre o de origem (dark) — igual ao que o HTML entrega.
  const theme = useSyncExternalStore(subscribeToTheme, readTheme, () => "dark" as const);

  const toggle = useCallback(() => {
    const next = document.documentElement.getAttribute("data-theme") === "light" ? "dark" : "light";
    document.documentElement.setAttribute("data-theme", next);
    try {
      localStorage.setItem("theme", next);
    } catch {
      // localStorage indisponível (modo privado) — tema não persiste, sem problema.
    }
  }, []);

  return (
    <nav className="sticky top-0 z-20 border-b border-line bg-bg/80 backdrop-blur-md backdrop-saturate-150">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-6">
        <Link href="/" className="flex items-center gap-2.5 font-bold tracking-tight">
          {/* alt vazio: o wordmark ao lado já nomeia o link, evita leitura duplicada */}
          <Image
            src="/logo-mark.png"
            alt=""
            width={36}
            height={36}
            priority
            className="rounded-full"
          />
          <span>
            Trampos<span className="text-accent">Editor</span>
          </span>
        </Link>

        <div className="ml-2 hidden gap-5 text-sm text-muted sm:flex">
          <Link href="/editores" className="hover:text-fg">
            Editores
          </Link>
          <Link href="/como-funciona" className="hover:text-fg">
            Como funciona
          </Link>
          {user ? (
            <Link href="/painel" className="hover:text-fg">
              Painel
            </Link>
          ) : (
            <Link href="/como-funciona#para-editores" className="hover:text-fg">
              Para editores
            </Link>
          )}
        </div>

        <div className="ml-auto flex items-center gap-2.5">
          <button
            type="button"
            onClick={toggle}
            aria-label="Alternar tema claro e escuro"
            className="grid h-9 w-9 place-items-center rounded-[10px] border border-line bg-card text-muted hover:border-accent hover:text-fg"
          >
            {theme === "dark" ? (
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                <circle cx="12" cy="12" r="4" />
                <path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M18.4 5.6 17 7M7 17l-1.4 1.4" />
              </svg>
            ) : (
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
              </svg>
            )}
          </button>
          {user ? (
            <>
              <Link
                href="/painel"
                className="hidden items-center gap-2 rounded-xl border border-line py-1 pr-3.5 pl-1 text-sm font-semibold hover:border-accent sm:inline-flex"
              >
                <span className="grid h-7 w-7 place-items-center rounded-full bg-accent text-xs font-bold text-accent-contrast">
                  {user.displayName.charAt(0).toUpperCase()}
                </span>
                {user.displayName.split(" ")[0]}
              </Link>
              <form action={logoutAction}>
                <button
                  type="submit"
                  className="btn btn-quiet btn-sm"
                >
                  Sair
                </button>
              </form>
            </>
          ) : (
            <>
              <Link
                href="/entrar"
                className="hidden btn btn-ghost btn-sm sm:inline-block"
              >
                Entrar
              </Link>
              <Link
                href="/criar-conta"
                className="btn btn-primary btn-sm"
              >
                Criar conta
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
