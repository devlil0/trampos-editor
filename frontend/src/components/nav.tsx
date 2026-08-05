"use client";

import Image from "next/image";
import Link from "next/link";
import { Suspense } from "react";
import { NavSearch } from "./nav-search";
import type { AuthUser } from "@/lib/types";

/**
 * O alternador de tema saiu daqui em 2026-08-04 e foi pras configurações do perfil
 * (`ThemeToggle`): tema é preferência de conta, ajustada uma vez, e o espaço na barra
 * serve melhor à busca.
 */
export function Nav({ user }: { user: AuthUser | null }) {
  return (
    <nav className="sticky top-0 z-20 border-b border-line bg-bg/80 backdrop-blur-md backdrop-saturate-150">
      {/* Largura de marketplace, não de site institucional: a barra acompanha a vitrine,
          que é a tela mais larga do produto. */}
      <div className="mx-auto flex h-16 max-w-[1440px] items-center gap-5 px-4 sm:px-6">
        <Link href="/" className="flex flex-none items-center gap-2.5 font-bold tracking-tight">
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

        {/* Suspense porque a busca lê a query string (useSearchParams). */}
        <Suspense fallback={<div className="hidden flex-1 md:block" />}>
          <NavSearch />
        </Suspense>

        {/* Duas portas simétricas em vez de um "Como funciona" genérico: a primeira
            decisão de quem chega num marketplace de dois lados é "qual desses eu sou?".
            Só pra quem está deslogado — quem entrou navega pela barra de baixo, e link
            repetido nos dois lugares é ruído. */}
        {!user && (
          <div className="hidden flex-none gap-5 text-sm text-muted lg:flex">
            <Link href="/para-clientes" className="hover:text-fg">
              Para clientes
            </Link>
            <Link href="/para-editores" className="hover:text-fg">
              Para editores
            </Link>
            {/* "Vitrine" e não "Editores" — é o nome que o resto do produto já usa. */}
            <Link href="/editores" className="hover:text-fg">
              Vitrine
            </Link>
          </div>
        )}

        <div className="ml-auto flex flex-none items-center gap-2.5">
          {user ? (
            /* Barra enxuta pra quem entrou: navegação é a barra de baixo, e aqui ficam só
               os dois atalhos que não cabem nela. Nome, "Painel", "Vitrine" e "Sair"
               saíram em 2026-08-04 — o logout foi pras configurações, onde rede social
               também o coloca. */
            <>
              <IconeSino />
              <Link
                href="/painel/meu-perfil/configuracoes"
                aria-label="Configurações"
                title="Configurações"
                className="grid h-9 w-9 place-items-center rounded-[10px] border border-line bg-card text-muted transition hover:border-accent hover:text-fg"
              >
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <circle cx="12" cy="12" r="3" />
                  <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
                </svg>
              </Link>
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

/**
 * Sino de notificações.
 *
 * Está aqui como pedido, mas **não leva a lugar nenhum ainda**: a notificação do produto
 * hoje é por e-mail (`NotificationService`), e não existe central in-app — nem tabela, nem
 * marcação de lida, nem tela. Fica desabilitado e dizendo isso em vez de fingir um destino:
 * botão que não responde ao clique confunde menos que botão que abre uma tela vazia.
 */
function IconeSino() {
  return (
    <button
      type="button"
      disabled
      aria-label="Notificações — em breve"
      title="Notificações chegam por e-mail por enquanto"
      className="grid h-9 w-9 cursor-not-allowed place-items-center rounded-[10px] border border-line bg-card text-muted opacity-50"
    >
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0" />
      </svg>
    </button>
  );
}
