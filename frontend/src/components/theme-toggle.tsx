"use client";

import { useCallback, useSyncExternalStore } from "react";

/**
 * Alternador de tema.
 *
 * A fonte da verdade é o atributo `data-theme` no <html>, escrito pelo script inline do
 * layout antes da primeira pintura. O botão lê esse DOM externo em vez de duplicá-lo em
 * estado — por isso useSyncExternalStore e não useState+useEffect.
 *
 * Mora nas configurações do perfil, e não na barra de navegação (mudou em 2026-08-04):
 * tema é preferência de conta, coisa que se ajusta uma vez, e ocupava na nav um espaço
 * que a busca usa melhor.
 */
function subscribeToTheme(onChange: () => void) {
  const observer = new MutationObserver(onChange);
  observer.observe(document.documentElement, { attributeFilter: ["data-theme"] });
  return () => observer.disconnect();
}

function readTheme(): "dark" | "light" {
  return document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark";
}

export function ThemeToggle() {
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
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 className="text-lg font-bold tracking-tight">Aparência</h2>
        <p className="mt-1 text-sm text-muted">
          O escuro é o tema de origem do produto. Fica salvo neste navegador.
        </p>
      </div>

      <button
        type="button"
        onClick={toggle}
        aria-label={theme === "dark" ? "Mudar para o tema claro" : "Mudar para o tema escuro"}
        className="inline-flex items-center gap-2 rounded-xl border border-line bg-panel px-3.5 py-2 text-sm font-semibold text-muted transition hover:border-accent hover:text-fg"
      >
        {theme === "dark" ? (
          <>
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <circle cx="12" cy="12" r="4" />
              <path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M18.4 5.6 17 7M7 17l-1.4 1.4" />
            </svg>
            Mudar para claro
          </>
        ) : (
          <>
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
            </svg>
            Mudar para escuro
          </>
        )}
      </button>
    </div>
  );
}
