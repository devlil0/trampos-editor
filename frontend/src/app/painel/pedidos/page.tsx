import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { acceptInterestAction, closeJobRequestAction } from "@/app/actions/job-requests";
import { NicheTag } from "@/components/niche-tag";
import { fetchMyJobRequests } from "@/lib/api";
import { formatPrice, formatTurnaround } from "@/lib/format";
import { getCurrentUser, getSessionToken } from "@/lib/session";
import type { JobInterest, JobRequest } from "@/lib/types";

export const metadata: Metadata = {
  title: "Meus pedidos — TramposEditor",
  robots: { index: false },
};

export default async function MeusPedidosPage() {
  if (!(await getCurrentUser())) redirect("/entrar");

  const token = await getSessionToken();
  const pedidos = token ? await fetchMyJobRequests(token) : [];

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <Link href="/painel/meu-perfil/configuracoes" className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Voltar pras configurações
      </Link>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-3xl font-extrabold tracking-tight">Meus pedidos</h1>
        <Link
          href="/painel/pedidos/novo"
          className="btn btn-primary btn-sm"
        >
          Postar pedido
        </Link>
      </div>

      {pedidos.length === 0 ? (
        <p className="mt-8 rounded-[18px] border border-line bg-card p-6 text-muted">
          Você ainda não postou nenhum pedido. Descreva o que precisa e deixe os editores virem
          até você — ou{" "}
          <Link href="/editores" className="font-semibold text-accent hover:underline">
            procure na vitrine
          </Link>
          .
        </p>
      ) : (
        <div className="mt-8 flex flex-col gap-5">
          {pedidos.map((pedido) => (
            <PedidoCard key={pedido.id} pedido={pedido} />
          ))}
        </div>
      )}
    </div>
  );
}

function PedidoCard({ pedido }: { pedido: JobRequest }) {
  const aberto = pedido.status === "OPEN";
  const orcamento = formatPrice(pedido.budgetCents);
  const prazo = formatTurnaround(pedido.deadlineDays);

  return (
    <article className="rounded-[18px] border border-line bg-card p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold tracking-tight">{pedido.title}</h2>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <NicheTag niche={pedido.niche} />
            {orcamento && <Meta>{orcamento}</Meta>}
            {prazo && <Meta>em até {prazo}</Meta>}
          </div>
        </div>
        <span
          className={`rounded-full px-3 py-1 text-xs font-bold ${
            aberto ? "bg-good/15 text-good" : "bg-panel text-muted"
          }`}
        >
          {aberto ? "Aberto" : "Fechado"}
        </span>
      </div>

      <p className="mt-3.5 whitespace-pre-line text-sm text-muted">{pedido.description}</p>

      {pedido.referenceUrl && (
        <a
          href={pedido.referenceUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-2 inline-block text-sm font-semibold text-accent hover:underline"
        >
          Referência anexada
        </a>
      )}

      <h3 className="mt-6 mb-3 text-xs font-bold uppercase tracking-[0.09em] text-muted">
        {pedido.interests.length === 0
          ? "Ninguém se candidatou ainda"
          : `${pedido.interests.length} ${pedido.interests.length === 1 ? "editor interessado" : "editores interessados"}`}
      </h3>

      {pedido.interests.length > 0 && (
        <ul className="flex flex-col gap-3">
          {pedido.interests.map((interesse) => (
            <InteresseItem key={interesse.id} jobId={pedido.id} interesse={interesse} />
          ))}
        </ul>
      )}

      {aberto && (
        <form action={closeJobRequestAction} className="mt-5">
          <input type="hidden" name="jobId" value={pedido.id} />
          <button type="submit" className="text-xs font-semibold text-muted hover:text-busy">
            Fechar pedido
          </button>
        </form>
      )}
    </article>
  );
}

function InteresseItem({ jobId, interesse }: { jobId: string; interesse: JobInterest }) {
  const aceito = interesse.status === "ACCEPTED";

  return (
    <li className="rounded-xl border border-line bg-panel p-3.5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="font-semibold">
          {interesse.editorSlug ? (
            <Link href={`/editores/${interesse.editorSlug}`} className="hover:text-accent">
              {interesse.editorDisplayName}
            </Link>
          ) : (
            interesse.editorDisplayName
          )}
        </span>
        {aceito ? (
          <span className="text-xs font-bold text-good">Aceito</span>
        ) : (
          // Aceitar é o que libera o contato dos dois lados (não há chat ainda).
          <form action={acceptInterestAction}>
            <input type="hidden" name="jobId" value={jobId} />
            <input type="hidden" name="interestId" value={interesse.id} />
            <button
              type="submit"
              className="btn btn-ghost btn-sm text-xs"
            >
              Aceitar e ver contato
            </button>
          </form>
        )}
      </div>

      <p className="mt-1.5 text-sm text-muted">{interesse.message}</p>

      {aceito && (
        <p className="mt-2 text-sm">
          {/* A conversa vem primeiro: o aceite já abriu uma, e é lá que a relação
              deve acontecer. O e-mail fica como alternativa até ser aposentado. */}
          <Link href="/painel/conversas" className="font-semibold text-accent hover:underline">
            Abrir conversa
          </Link>
          {interesse.editorEmail && (
            <>
              {" · ou por e-mail: "}
              <a href={`mailto:${interesse.editorEmail}`} className="text-muted hover:text-accent">
                {interesse.editorEmail}
              </a>
            </>
          )}
        </p>
      )}
    </li>
  );
}

function Meta({ children }: { children: React.ReactNode }) {
  return (
    <span className="rounded-md bg-panel px-2 py-1 text-xs font-semibold text-muted">{children}</span>
  );
}
