import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { InterestForm } from "@/components/job-forms";
import { NicheTag } from "@/components/niche-tag";
import { fetchJobFeed, fetchMyEditorProfile, fetchMyJobInterests } from "@/lib/api";
import { formatPrice, formatTurnaround } from "@/lib/format";
import { getCurrentUser, getSessionToken } from "@/lib/session";
import type { JobRequestFeedItem } from "@/lib/types";

export const metadata: Metadata = {
  title: "Trampos abertos — TramposEditor",
  robots: { index: false },
};

export default async function TramposPage() {
  if (!(await getCurrentUser())) redirect("/entrar");

  const token = await getSessionToken();
  if (!token) redirect("/entrar");

  const perfil = await fetchMyEditorProfile(token);
  const [feed, candidaturas] = await Promise.all([
    fetchJobFeed(token),
    fetchMyJobInterests(token),
  ]);

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <Link href="/painel" className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Voltar pro painel
      </Link>

      <h1 className="text-3xl font-extrabold tracking-tight">Trampos abertos</h1>
      <p className="mt-1 text-muted">
        Pedidos publicados por quem precisa de edição. Diga que topa — o cliente escolhe e libera o
        contato.
      </p>

      {!perfil && (
        <p className="mt-6 rounded-xl border border-accent/40 bg-accent-soft px-4 py-3 text-sm">
          Pra se candidatar você precisa ter um perfil publicado.{" "}
          <Link href="/painel/meu-perfil" className="font-semibold text-accent hover:underline">
            Monte o seu
          </Link>{" "}
          — leva dois minutos.
        </p>
      )}

      {candidaturas.length > 0 && (
        <section className="mt-8">
          <h2 className="mb-3 text-xs font-bold uppercase tracking-[0.09em] text-muted">
            Minhas candidaturas
          </h2>
          <div className="flex flex-col gap-3.5">
            {candidaturas.map((job) => (
              <CandidaturaCard key={job.id} job={job} />
            ))}
          </div>
        </section>
      )}

      <section className="mt-8">
        <h2 className="mb-3 text-xs font-bold uppercase tracking-[0.09em] text-muted">
          Pedidos abertos
        </h2>

        {feed.length === 0 ? (
          <p className="rounded-[18px] border border-line bg-card p-6 text-muted">
            Nenhum trampo aberto agora. Enquanto isso, deixe seu perfil afiado — é por ele que a
            maioria dos clientes chega.
          </p>
        ) : (
          <div className="flex flex-col gap-5">
            {feed.map((job) => (
              <FeedCard key={job.id} job={job} podeCandidatar={perfil != null} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function FeedCard({ job, podeCandidatar }: { job: JobRequestFeedItem; podeCandidatar: boolean }) {
  const orcamento = formatPrice(job.budgetCents);
  const prazo = formatTurnaround(job.deadlineDays);
  const jaCandidatou = job.myInterestStatus != null;

  return (
    <article className="rounded-[18px] border border-line bg-card p-6">
      <h3 className="text-lg font-bold tracking-tight">{job.title}</h3>
      <div className="mt-2 flex flex-wrap items-center gap-2">
        <NicheTag niche={job.niche} />
        {orcamento && <Meta>{orcamento}</Meta>}
        {prazo && <Meta>em até {prazo}</Meta>}
        <Meta>
          {job.interestCount} {job.interestCount === 1 ? "interessado" : "interessados"}
        </Meta>
      </div>

      <p className="mt-3.5 whitespace-pre-line text-sm text-muted">{job.description}</p>

      {job.referenceUrl && (
        <a
          href={job.referenceUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-2 inline-block text-sm font-semibold text-accent hover:underline"
        >
          Ver referência
        </a>
      )}

      <p className="mt-3 text-xs text-muted">Pedido de {job.clientDisplayName}</p>

      {jaCandidatou ? (
        <StatusCandidatura job={job} />
      ) : podeCandidatar ? (
        <InterestForm jobId={job.id} />
      ) : null}
    </article>
  );
}

function CandidaturaCard({ job }: { job: JobRequestFeedItem }) {
  return (
    <article className="rounded-[18px] border border-line bg-card p-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-bold tracking-tight">{job.title}</h3>
        <NicheTag niche={job.niche} />
      </div>
      <StatusCandidatura job={job} />
    </article>
  );
}

function StatusCandidatura({ job }: { job: JobRequestFeedItem }) {
  if (job.myInterestStatus === "ACCEPTED") {
    return (
      <p className="mt-3 rounded-xl border border-good/40 bg-good/10 px-3.5 py-2.5 text-sm">
        <b className="text-good">Aceito.</b>{" "}
        <Link href="/painel/conversas" className="font-semibold text-accent hover:underline">
          Falar com {job.clientDisplayName}
        </Link>
        {job.clientEmail && (
          <>
            {" · ou por e-mail: "}
            <a href={`mailto:${job.clientEmail}`} className="text-muted hover:text-accent">
              {job.clientEmail}
            </a>
          </>
        )}
      </p>
    );
  }

  return (
    <p className="mt-3 rounded-xl border border-line bg-panel px-3.5 py-2.5 text-sm text-muted">
      Interesse enviado — esperando o cliente responder.
    </p>
  );
}

function Meta({ children }: { children: React.ReactNode }) {
  return (
    <span className="rounded-md bg-panel px-2 py-1 text-xs font-semibold text-muted">{children}</span>
  );
}
