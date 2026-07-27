"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

export function Nav() {
  const [theme, setTheme] = useState<"dark" | "light">("dark");

  useEffect(() => {
    const current = document.documentElement.getAttribute("data-theme");
    if (current === "light" || current === "dark") setTheme(current);
  }, []);

  function toggle() {
    const next = theme === "dark" ? "light" : "dark";
    setTheme(next);
    document.documentElement.setAttribute("data-theme", next);
    try {
      localStorage.setItem("theme", next);
    } catch {
      // localStorage indisponível (modo privado) — tema não persiste, sem problema.
    }
  }

  return (
    <nav className="sticky top-0 z-20 border-b border-line bg-bg/80 backdrop-blur-md backdrop-saturate-150">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-6">
        <Link href="/" className="flex items-center gap-2.5 font-bold tracking-tight">
          <span className="grid h-8 w-8 place-items-center rounded-[9px] bg-gradient-to-br from-accent to-accent-strong text-white">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M8 5v14l11-7z" />
            </svg>
          </span>
          <span>
            Trampos<span className="text-accent">Editor</span>
          </span>
        </Link>

        <div className="ml-2 hidden gap-5 text-sm text-muted sm:flex">
          <Link href="/" className="hover:text-fg">
            Editores
          </Link>
          <a href="#" className="hover:text-fg">
            Como funciona
          </a>
          <a href="#" className="hover:text-fg">
            Para editores
          </a>
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
          <a
            href="#"
            className="hidden rounded-full border border-line px-4 py-2 text-sm font-semibold hover:border-accent hover:text-accent sm:inline-block"
          >
            Entrar
          </a>
          <a
            href="#"
            className="rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-contrast hover:bg-accent-strong"
          >
            Criar conta
          </a>
        </div>
      </div>
    </nav>
  );
}
