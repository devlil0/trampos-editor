import type { Metadata } from "next";
import { hasAdjustedFilters, normalizeFeedQuery } from "@/lib/query-filters";
import Link from "next/link";
import { redirect } from "next/navigation";
import { CategoryChips, FeedPager } from "@/components/feed-nav";
import { FeedSwitch } from "@/components/feed-switch";
import { InterestForm } from "@/components/job-forms";
import { NicheTag } from "@/components/niche-tag";
import { fetchJobFeed, fetchMyEditorProfile, fetchMyJobInterests } from "@/lib/api";
import { formatPrice, formatTurnaround } from "@/lib/format";
import { requireCurrentUser, getSessionToken } from "@/lib/session";
import type { JobRequestFeedItem } from "@/lib/types";

export const metadata: Metadata = {
  title: "Trampos abertos — TramposEditor",
  robots: { index: false },
};

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>;

function pick(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

/**
 * O feed, classe `trampos` — o que os clientes estão pedindo.
 *
 * Mora aqui desde 2026-08-06, quando os dois feeds viraram um com uma chave de seleção
 * (ver `FeedSwitch`). Era `/painel/trampos`, que hoje redireciona pra cá.
 *
 * A classe `editores` **não** é servida por esta rota: ela é a vitrine pública, e
 * duplicá-la atrás de um parâmetro recriaria exatamente a tela que o ESBOCO §1 deletou.
 * `?class=editores` sai daqui pra `/editores`.
 */
export default async function FeedPage({ searchParams }: { searchParams: SearchParams }) {
  const sp = await searchParams;

  // Antes do guard de sessão: a vitrine é pública, e mandar pro `/entrar` alguém que só
  // queria a lista de editores seria uma porta trancada sem motivo.
  if (pick(sp.class) === "editores") redirect("/editores");

  await requireCurrentUser();

  const token = await getSessionToken();
  if (!token) redirect("/entrar");

  const raw = { niche: pick(sp.niche), page: pick(sp.page) };
  const { niche, page } = normalizeFeedQuery(raw);

  const perfil = await fetchMyEditorProfile(token);
  const [feed, candidaturas] = await Promise.all([
    fetchJobFeed(token, { niche, page }),
    fetchMyJobInterests(token),
  ]);

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      {/* Sem link de "voltar": desde 2026-08-06 esta é a tela onde o login desemboca, então
          não existe um "de onde se veio". O que havia aqui apontava pras configurações e
          ficou invertido no instante em que o feed virou a porta de entrada. */}
      <h1 className="text-3xl font-extrabold tracking-tight">Trampos abertos</h1>
      <p className="mt-1 text-muted">
        Pedidos publicados por quem precisa de edição. Diga que topa — o cliente escolhe e libera o
        contato.
      </p>

      {/* A chave logo abaixo do título: é a primeira coisa depois de saber onde se está. */}
      <div className="mt-5">
        <FeedSwitch active="trampos" />
      </div>

      {hasAdjustedFilters(raw, { niche, page }) && (
        <p role="status" className="mt-4 text-sm text-muted">
          Alguns filtros foram ajustados. <Link href="/painel/feed" className="underline">Limpar filtros</Link>
        </p>
      )}

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
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-xs font-bold uppercase tracking-[0.09em] text-muted">
            Pedidos abertos
          </h2>
          <span className="text-sm text-muted tabular-nums">
            <b className="text-fg">{feed.totalElements}</b>{" "}
            {feed.totalElements === 1 ? "trampo" : "trampos"}
          </span>
        </div>

        <div className="mb-6">
          <CategoryChips basePath="/painel/feed" active={niche} />
        </div>

        {feed.content.length === 0 ? (
          <p className="rounded-[18px] border border-line bg-card p-6 text-muted">
            {niche
              ? "Nenhum trampo aberto nessa categoria agora. Tente outra — ou deixe em “Todas”."
              : "Nenhum trampo aberto agora. Enquanto isso, deixe seu perfil afiado — é por ele que a maioria dos clientes chega."}
          </p>
        ) : (
          <div className="flex flex-col gap-5">
            {feed.content.map((job) => (
              <FeedCard key={job.id} job={job} podeCandidatar={perfil != null} />
            ))}
          </div>
        )}

        <FeedPager basePath="/painel/feed" page={feed} niche={niche} />
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

      <p className="mt-3.5 whitespace-pre-line [overflow-wrap:anywhere] text-sm text-muted">{job.description}</p>

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
