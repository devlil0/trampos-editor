import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { SendMessageForm } from "@/components/conversation-forms";
import { OpenDealForm } from "@/components/deal-forms";
import { fetchConversation, fetchMyDeals } from "@/lib/api";
import { getCurrentUser, getSessionToken } from "@/lib/session";
import type { Message } from "@/lib/types";

export const metadata: Metadata = {
  title: "Conversa — TramposEditor",
  robots: { index: false },
};

type Params = Promise<{ id: string }>;

/**
 * Uma conversa aberta. Carregar a página é o que marca como lida — o backend faz
 * isso ao servir os dados, então não existe botão de "marcar como lida".
 */
export default async function ConversaPage({ params }: { params: Params }) {
  const { id } = await params;

  if (!(await getCurrentUser())) redirect("/entrar");
  const token = await getSessionToken();
  if (!token) redirect("/entrar");

  const conversa = await fetchConversation(token, id);
  // Conversa de outra pessoa devolve o mesmo 404 de conversa inexistente: quem não
  // participa não deve nem descobrir que ela existe.
  if (!conversa) notFound();

  // O trampo mais recente desta conversa, se houver. O histórico já vem ordenado do
  // servidor, então o primeiro encontrado é o que interessa.
  const trampo = (await fetchMyDeals(token)).find((d) => d.conversationId === conversa.id);

  return (
    <div className="mx-auto max-w-3xl px-6 py-12">
      <Link href="/painel/conversas" className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Todas as conversas
      </Link>

      <header className="border-b border-line pb-5">
        <h1 className="text-2xl font-extrabold tracking-tight">
          {conversa.counterpartSlug ? (
            <Link href={`/editores/${conversa.counterpartSlug}`} className="hover:text-accent">
              {conversa.counterpartDisplayName}
            </Link>
          ) : (
            conversa.counterpartDisplayName
          )}
        </h1>
        {conversa.jobTitle ? (
          <p className="mt-1 text-sm text-muted">
            Conversa aberta pelo trampo{" "}
            <span className="font-semibold text-fg">{conversa.jobTitle}</span>
          </p>
        ) : (
          <p className="mt-1 text-sm text-muted">Conversa direta, começada pela vitrine.</p>
        )}
      </header>

      {conversa.messages.length === 0 ? (
        <p className="mt-8 rounded-[18px] border border-line bg-card p-6 text-muted">
          Ninguém falou ainda. Puxe o assunto — quem responde primeiro sai na frente.
        </p>
      ) : (
        <ol className="mt-8 flex flex-col gap-4">
          {conversa.messages.map((mensagem) => (
            <li key={mensagem.id}>
              <Balao mensagem={mensagem} />
            </li>
          ))}
        </ol>
      )}

      <div className="mt-8 border-t border-line pt-6">
        <SendMessageForm conversationId={conversa.id} />
        <p className="mt-3 text-xs text-muted">
          Pra mandar material, cole o link — o site não recebe arquivo, do mesmo jeito que o
          portfólio funciona por link.
        </p>
      </div>

      {/* O trampo da conversa direta (ESBOCO §3.2): sem este registro, o caminho da
          vitrine tem começo e não tem fim, e nada nunca vira avaliação. */}
      <div className="mt-6 rounded-[18px] border border-line bg-card p-5">
        {trampo ? (
          <p className="text-sm text-muted">
            Trampo registrado: <b className="text-fg">{trampo.title}</b>{" "}
            <Link href="/painel/trampos-fechados" className="font-semibold text-accent hover:underline">
              {trampo.status === "OPEN" ? "encerrar e avaliar" : "ver no histórico"}
            </Link>
          </p>
        ) : conversa.iAmClient ? (
          <OpenDealForm conversationId={conversa.id} />
        ) : (
          <p className="text-sm text-muted">
            Quando {conversa.counterpartDisplayName} registrar o trampo, ele aparece aqui e
            vocês dois podem se avaliar no fim.
          </p>
        )}
      </div>
    </div>
  );
}

/**
 * O lado do balão vem do `mine` que o servidor calcula. O browser nunca vê o token
 * (sessão é cookie httpOnly), então ele não teria como deduzir isso sozinho.
 */
function Balao({ mensagem }: { mensagem: Message }) {
  const quando = new Date(mensagem.createdAt).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });

  return (
    <div className={`flex flex-col gap-1 ${mensagem.mine ? "items-end" : "items-start"}`}>
      <div
        className={`max-w-[85%] rounded-[18px] px-4 py-3 ${
          mensagem.mine
            ? "border border-accent/45 bg-accent-soft"
            : "border border-line bg-card"
        }`}
      >
        {!mensagem.mine && (
          <p className="mb-1 text-xs font-bold tracking-tight text-muted">
            {mensagem.senderDisplayName}
          </p>
        )}
        <p className="whitespace-pre-line text-sm">{mensagem.body}</p>
      </div>
      <time dateTime={mensagem.createdAt} className="font-mono text-[11px] text-muted tabular-nums">
        {quando}
      </time>
    </div>
  );
}
