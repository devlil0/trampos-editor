import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { CloseDealForm, ReviewForm } from "@/components/deal-forms";
import { fetchMyDeals } from "@/lib/api";
import { getCurrentUser, getSessionToken } from "@/lib/session";
import type { Deal } from "@/lib/types";

export const metadata: Metadata = {
  title: "Trampos fechados — TramposEditor",
  robots: { index: false },
};

/**
 * O histórico (ESBOCO §7 item 4) — a tela que fecha dois laços.
 *
 * Reputação: é daqui que sai a avaliação que devolve a nota pro perfil e destrava a
 * ordenação da vitrine (§2.1). Demanda: é daqui que sai o "chamar de novo", o gatilho
 * mais barato pra trazer um cliente de volta (§3.2).
 */
export default async function TramposFechadosPage() {
  if (!(await getCurrentUser())) redirect("/entrar");

  const token = await getSessionToken();
  if (!token) redirect("/entrar");

  const deals = await fetchMyDeals(token);
  const abertos = deals.filter((d) => d.status === "OPEN");
  const encerrados = deals.filter((d) => d.status !== "OPEN");

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <Link
        href="/painel/meu-perfil/configuracoes"
        className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          aria-hidden="true"
        >
          <path d="m15 18-6-6 6-6" />
        </svg>
        Voltar pras configurações
      </Link>

      <h1 className="text-3xl font-extrabold tracking-tight">Trampos</h1>
      <p className="mt-1 text-muted">
        O que você combinou, o que entregou e o que achou de cada um. É a avaliação daqui
        que mexe na ordenação da vitrine.
      </p>

      {deals.length === 0 ? (
        <div className="mt-8 rounded-[18px] border border-line bg-card p-6">
          <p className="text-muted">
            Nenhum trampo ainda. Eles nascem quando você aceita alguém num pedido — ou
            quando registra um combinado dentro de uma conversa.
          </p>
          <div className="mt-4 flex flex-wrap gap-2.5">
            <Link href="/editores?available=true" className="btn btn-primary btn-sm">
              Ver editores disponíveis
            </Link>
            <Link href="/painel/trampos" className="btn btn-ghost btn-sm">
              Ver trampos abertos
            </Link>
          </div>
        </div>
      ) : (
        <>
          {abertos.length > 0 && (
            <section className="mt-8">
              <h2 className="mb-3 text-xs font-bold uppercase tracking-[0.09em] text-muted">
                Em andamento
              </h2>
              <div className="flex flex-col gap-4">
                {abertos.map((deal) => (
                  <DealCard key={deal.id} deal={deal} />
                ))}
              </div>
            </section>
          )}

          {encerrados.length > 0 && (
            <section className="mt-8">
              <h2 className="mb-3 text-xs font-bold uppercase tracking-[0.09em] text-muted">
                Encerrados
              </h2>
              <div className="flex flex-col gap-4">
                {encerrados.map((deal) => (
                  <DealCard key={deal.id} deal={deal} />
                ))}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  );
}

function DealCard({ deal }: { deal: Deal }) {
  const encerrado = deal.status !== "OPEN";

  return (
    <article className="rounded-[18px] border border-line bg-card p-6">
      <div className="flex flex-wrap items-start justify-between gap-2.5">
        <div className="min-w-0">
          <h3 className="text-lg font-bold tracking-tight">{deal.title}</h3>
          <p className="mt-1 text-sm text-muted">
            {deal.iAmClient ? "Com" : "Para"}{" "}
            {deal.counterpartSlug ? (
              <Link href={`/editores/${deal.counterpartSlug}`} className="font-semibold text-accent hover:underline">
                {deal.counterpartName}
              </Link>
            ) : (
              <b className="text-fg">{deal.counterpartName}</b>
            )}
          </p>
        </div>
        <StatusTag status={deal.status} />
      </div>

      <div className="mt-3.5 flex flex-wrap gap-2.5 text-sm">
        <Link href="/painel/conversas" className="text-accent hover:underline">
          Abrir conversa
        </Link>
        {/* O "chamar de novo" do §3.2: repetição de fornecedor em um clique. */}
        {encerrado && deal.counterpartSlug && (
          <Link href={`/editores/${deal.counterpartSlug}`} className="text-accent hover:underline">
            Chamar de novo
          </Link>
        )}
      </div>

      {/* Encerrar é só de quem contratou — quem recebe o trabalho é quem diz que chegou. */}
      {!encerrado && deal.iAmClient && <CloseDealForm dealId={deal.id} />}
      {!encerrado && !deal.iAmClient && (
        <p className="mt-4 rounded-xl border border-line bg-panel px-3.5 py-2.5 text-sm text-muted">
          Quando {deal.counterpartName} marcar como concluído, vocês dois podem se avaliar.
        </p>
      )}

      {deal.status === "DONE" && !deal.reviewedByMe && (
        <ReviewForm dealId={deal.id} counterpartName={deal.counterpartName} />
      )}
      {deal.status === "DONE" && deal.reviewedByMe && (
        <p className="mt-4 text-sm text-muted">Você já avaliou este trampo.</p>
      )}

      {deal.reviewOfMe && (
        <div className="mt-4 rounded-xl border border-line bg-panel px-3.5 py-3">
          <p className="text-xs font-bold uppercase tracking-[0.09em] text-muted">
            O que disseram de você
          </p>
          <p className="mt-1.5 text-sm">
            <span className="text-amber-400">{"★".repeat(deal.reviewOfMe.rating)}</span>
            <span className="text-muted">{"★".repeat(5 - deal.reviewOfMe.rating)}</span>
          </p>
          {deal.reviewOfMe.comment && (
            <p className="mt-1.5 text-sm text-muted">“{deal.reviewOfMe.comment}”</p>
          )}
        </div>
      )}
    </article>
  );
}

function StatusTag({ status }: { status: Deal["status"] }) {
  const map = {
    OPEN: { label: "Em andamento", className: "border-accent/40 bg-accent-soft text-accent" },
    DONE: { label: "Concluído", className: "border-good/40 bg-good/10 text-good" },
    CANCELLED: { label: "Não vingou", className: "border-line bg-panel text-muted" },
  } as const;

  const { label, className } = map[status];
  return (
    <span className={`shrink-0 rounded-full border px-3 py-1 text-xs font-bold ${className}`}>
      {label}
    </span>
  );
}
