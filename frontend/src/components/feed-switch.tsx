import Link from "next/link";

export type FeedClass = "trampos" | "editores";

/**
 * A chave do feed — alterna entre os dois lados do mercado (2026-08-06).
 *
 * O feed é **um só**, com duas classes: quem precisa de edição (`trampos`) e quem edita
 * (`editores`). Antes eram duas telas sem relação declarada, e quem chegava numa não
 * descobria a outra.
 *
 * As duas classes moram em rotas diferentes, e isso é regra, não acaso:
 *
 * - `editores` vive em `/editores`, **público**. É o ativo de SEO do produto (ESBOCO
 *   §11.2.6) e a única lista de editores que existe — o §1 já derrubou uma duplicata
 *   dessa lista e não vale a pena criar outra atrás de um parâmetro.
 * - `trampos` vive em `/painel/feed`, **logado**. Pedido aberto não é conteúdo público:
 *   o `JobRequestController` recusa sem token justamente pra não virar lista de briefing
 *   pra raspagem, e a chave não é motivo pra afrouxar isso.
 *
 * Consequência pro deslogado: clicar em "Ver trampos" leva ao `/entrar`, porque a página
 * de destino exige sessão. É o mesmo comportamento que o link do rodapé já tinha — a
 * chave só o tornou visível no lugar onde a pessoa está olhando.
 *
 * Trocar de classe **zera filtro e página** (decisão do fundador, 2026-08-06): as duas
 * dimensões quase não se sobrepõem — trampo se filtra por nicho e orçamento, editor por
 * ferramenta, estilo, preço e disponibilidade — e carregar recorte de um lado pro outro
 * entregaria lista vazia sem a pessoa entender por quê.
 */
export function FeedSwitch({ active }: { active: FeedClass }) {
  return (
    <div
      role="group"
      aria-label="Alternar o que o feed mostra"
      className="inline-flex rounded-full border border-line bg-panel p-1"
    >
      <Opcao href="/painel/feed" label="Ver trampos" active={active === "trampos"} />
      <Opcao href="/editores" label="Ver editores" active={active === "editores"} />
    </div>
  );
}

function Opcao({ href, label, active }: { href: string; label: string; active: boolean }) {
  return (
    <Link
      href={href}
      aria-current={active ? "true" : undefined}
      className={
        active
          ? "rounded-full bg-accent px-4 py-1.5 text-sm font-bold text-accent-contrast"
          : "rounded-full px-4 py-1.5 text-sm font-semibold text-muted transition hover:text-fg"
      }
    >
      {label}
    </Link>
  );
}
