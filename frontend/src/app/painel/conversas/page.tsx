import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { fetchMyConversations } from "@/lib/api";
import { getCurrentUser, getSessionToken } from "@/lib/session";
import type { ConversationSummary } from "@/lib/types";

export const metadata: Metadata = {
  title: "Conversas — TramposEditor",
  robots: { index: false },
};

/**
 * A caixa de entrada única (ESBOCO §7). Não separa por papel de propósito: a conta é
 * uma só, e obrigar a pessoa a lembrar se naquela conversa ela contratava ou editava
 * seria trabalho dela pra resolver um problema do sistema.
 */
export default async function ConversasPage() {
  if (!(await getCurrentUser())) redirect("/entrar");

  const token = await getSessionToken();
  const conversas = token ? await fetchMyConversations(token) : [];
  const naoLidas = conversas.filter((c) => c.unread).length;

  return (
    <div className="mx-auto max-w-4xl px-6 py-12">
      <Link href="/editores" className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Voltar pro feed
      </Link>

      <h1 className="text-3xl font-extrabold tracking-tight">Conversas</h1>
      <p className="mt-1 text-muted">
        {naoLidas > 0
          ? `${naoLidas} ${naoLidas === 1 ? "conversa com mensagem nova" : "conversas com mensagem nova"}.`
          : "Tudo lido por aqui."}
      </p>

      {conversas.length === 0 ? (
        <div className="mt-8 rounded-[18px] border border-line bg-card p-6 text-muted">
          <p>Nenhuma conversa ainda. Elas começam de dois jeitos:</p>
          <ul className="mt-3 flex flex-col gap-1.5">
            <li>
              você chama alguém na{" "}
              <Link href="/editores" className="font-semibold text-accent hover:underline">
                vitrine
              </Link>
              ;
            </li>
            <li>
              ou um cliente aceita seu interesse num{" "}
              <Link href="/painel/feed" className="font-semibold text-accent hover:underline">
                trampo aberto
              </Link>
              .
            </li>
          </ul>
        </div>
      ) : (
        <ul className="mt-8 flex flex-col gap-3">
          {conversas.map((conversa) => (
            <li key={conversa.id}>
              <ConversaLinha conversa={conversa} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function ConversaLinha({ conversa }: { conversa: ConversationSummary }) {
  return (
    <Link
      href={`/painel/conversas/${conversa.id}`}
      className="flex items-start gap-3.5 rounded-[18px] border border-line bg-card p-5 hover:border-accent"
    >
      {/* O ponto é o único marcador de não lida: badge com número exigiria contar
          mensagem por mensagem, e não muda o que a pessoa faz a seguir. */}
      <span
        className={`mt-2 h-2 w-2 flex-none rounded-full ${conversa.unread ? "bg-accent" : "bg-transparent"}`}
        aria-hidden="true"
      />

      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
          <span className={`tracking-tight ${conversa.unread ? "font-extrabold" : "font-bold"}`}>
            {conversa.counterpartDisplayName}
          </span>
          {conversa.lastMessageAt && (
            <time
              dateTime={conversa.lastMessageAt}
              className="font-mono text-xs text-muted tabular-nums"
            >
              {formatarQuando(conversa.lastMessageAt)}
            </time>
          )}
        </div>

        {conversa.jobTitle && (
          <p className="mt-1 text-xs text-muted">
            sobre <span className="font-semibold text-fg">{conversa.jobTitle}</span>
          </p>
        )}

        <p className={`mt-1.5 truncate text-sm ${conversa.unread ? "text-fg" : "text-muted"}`}>
          {conversa.lastMessagePreview ?? "Conversa aberta — ninguém falou ainda."}
        </p>
      </div>

      {conversa.unread && (
        <span className="sr-only">Mensagem nova</span>
      )}
    </Link>
  );
}

/**
 * Hoje mostra a hora; antes disso, a data. É o que a pessoa precisa pra saber se
 * aquilo chegou agora — data completa em conversa de hoje é ruído.
 */
function formatarQuando(iso: string): string {
  const quando = new Date(iso);
  const agora = new Date();
  const mesmoDia =
    quando.getFullYear() === agora.getFullYear() &&
    quando.getMonth() === agora.getMonth() &&
    quando.getDate() === agora.getDate();

  return mesmoDia
    ? quando.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })
    : quando.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" });
}
