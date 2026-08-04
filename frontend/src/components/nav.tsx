"use client";

import Image from "next/image";
import Link from "next/link";
import { Suspense } from "react";
import { logoutAction } from "@/app/actions/auth";
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

        <div className="hidden flex-none gap-5 text-sm text-muted lg:flex">
          {/* Duas portas simétricas em vez de um "Como funciona" genérico: a primeira
              decisão de quem chega num marketplace de dois lados é "qual desses eu
              sou?", e antes só o editor tinha porta. Somem pra quem está logado —
              a essa altura a pessoa já entrou, e o lugar dela é o painel. */}
          {user ? (
            <Link href="/painel" className="hover:text-fg">
              Painel
            </Link>
          ) : (
            <>
              <Link href="/para-clientes" className="hover:text-fg">
                Para clientes
              </Link>
              <Link href="/para-editores" className="hover:text-fg">
                Para editores
              </Link>
            </>
          )}
          {/* Por último: as portas de público vêm antes porque a pergunta "qual desses
              eu sou?" precede o "me mostra quem tem". "Vitrine" e não "Editores" — é
              o nome que o resto do produto já usa ("Voltar pra vitrine", no perfil). */}
          <Link href="/editores" className="hover:text-fg">
            Vitrine
          </Link>
        </div>

        <div className="ml-auto flex flex-none items-center gap-2.5">
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
