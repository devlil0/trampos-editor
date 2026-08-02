import Link from "next/link";

/**
 * Peças compartilhadas pelas duas páginas de público (`/para-clientes` e
 * `/para-editores`). Elas nasceram como funções locais da antiga `/como-funciona`;
 * quando a página virou duas, viraram componentes pra que o layout não divirja entre
 * os dois lados — o que faria a marca parecer dois produtos diferentes.
 *
 * A antiga `Passos` recebia `id` e `scroll-mt` porque as duas seções moravam na mesma
 * página e a nav chegava por âncora. Com uma página por público isso deixou de existir.
 */

export type Passo = { titulo: string; texto: string };

/** Cabeçalho da página de público: rótulo miúdo, manchete e linha de apoio. */
export function GuiaHero({
  etiqueta,
  titulo,
  children,
}: {
  etiqueta: string;
  titulo: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="pt-14 pb-4">
      <p className="font-mono text-xs font-semibold tracking-[0.18em] text-accent">{etiqueta}</p>
      <h1 className="mt-4 max-w-[18ch] text-balance text-4xl font-extrabold leading-[1.04] tracking-tight sm:text-5xl">
        {titulo}
      </h1>
      <p className="mt-3.5 max-w-[56ch] text-lg text-muted">{children}</p>
    </section>
  );
}

/** Os três passos numerados, com o CTA do público logo abaixo. */
export function Passos({
  titulo,
  passos,
  cta,
}: {
  titulo: string;
  passos: Passo[];
  cta: { href: string; label: string };
}) {
  return (
    <section className="border-t border-line py-12">
      <h2 className="text-2xl font-extrabold tracking-tight">{titulo}</h2>

      <ol className="mt-6 grid gap-5 md:grid-cols-3">
        {passos.map((passo, i) => (
          <li key={passo.titulo} className="rounded-[18px] border border-line bg-card p-6">
            <span className="grid h-9 w-9 place-items-center rounded-full bg-accent-soft text-sm font-bold text-accent-strong">
              {i + 1}
            </span>
            <h3 className="mt-3.5 font-bold tracking-tight">{passo.titulo}</h3>
            <p className="mt-1.5 text-sm text-muted">{passo.texto}</p>
          </li>
        ))}
      </ol>

      <Link href={cta.href} className="mt-6 btn btn-ghost">
        {cta.label}
      </Link>
    </section>
  );
}

/** Bloco de perguntas. O texto aberto, sem acordeão: dobrado não é lido nem indexado. */
export function Perguntas({ children }: { children: React.ReactNode }) {
  return (
    <section className="border-t border-line py-12">
      <h2 className="text-2xl font-extrabold tracking-tight">Perguntas diretas</h2>
      <div className="mt-6 grid gap-5 md:grid-cols-2">{children}</div>
    </section>
  );
}

export function Faq({ pergunta, children }: { pergunta: string; children: React.ReactNode }) {
  return (
    <div className="rounded-[18px] border border-line bg-card p-5">
      <h3 className="font-bold tracking-tight">{pergunta}</h3>
      <p className="mt-1.5 text-sm text-muted">{children}</p>
    </div>
  );
}

/**
 * Fechamento da página. O segundo link leva pro outro público de propósito: quem chegou
 * na porta errada tem uma saída em vez de um beco — uma pessoa pode ser os dois
 * (FUNDACAO §5.2, conta única).
 */
export function CtaFinal({
  titulo,
  texto,
  principal,
  secundario,
}: {
  titulo: string;
  texto: string;
  principal: { href: string; label: string };
  secundario: { href: string; label: string };
}) {
  return (
    <section className="mb-16 rounded-[18px] border border-line bg-card p-8 text-center">
      <h2 className="text-2xl font-extrabold tracking-tight">{titulo}</h2>
      <p className="mx-auto mt-2 max-w-[46ch] text-muted">{texto}</p>
      <div className="mt-6 flex flex-wrap justify-center gap-3">
        <Link href={principal.href} className="btn btn-primary">
          {principal.label}
        </Link>
        <Link href={secundario.href} className="btn btn-ghost">
          {secundario.label}
        </Link>
      </div>
    </section>
  );
}
