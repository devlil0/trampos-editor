"use client";

import { useActionState, useState, useTransition } from "react";
import {
  sendMessageAction,
  startConversationAction,
  type ConversationFormState,
} from "@/app/actions/conversations";

import { useRouter } from "next/navigation";
import { AuthFeedback } from "./auth-feedback";

const EMPTY: ConversationFormState = {};

const inputClass =
  "w-full rounded-xl border border-line bg-panel px-3.5 py-2.5 text-base outline-none placeholder:text-muted focus:border-accent";


/**
 * Abertura da conversa a partir do perfil. Um botão só.
 *
 * Tinha um campo de texto antes do botão (até 2026-08-04), e ele cobrava a redação da
 * primeira mensagem no meio da leitura do perfil — quem está decidindo se gosta do
 * trabalho ainda não sabe o que pedir. Agora o clique abre a conversa com uma linha de
 * apresentação e leva a pessoa pra tela de conversa, que é onde há espaço pra detalhar.
 *
 * O `body` continua obrigatório no backend, então ele vai daqui pronto — a conversa não
 * pode nascer vazia, senão o editor recebe notificação de nada.
 */
const PRIMEIRA_MENSAGEM = "Olá! Vi seu perfil e quero solicitar um trampo.";

export function StartConversationForm({ slug, loggedIn }: { slug: string; loggedIn: boolean }) {
  const [state, formAction, pending] = useActionState(startConversationAction, EMPTY);

  return (
    <form action={formAction} className="flex flex-col gap-2.5">
      <AuthFeedback state={state} />
      <input type="hidden" name="slug" value={slug} />
      <input type="hidden" name="body" value={PRIMEIRA_MENSAGEM} />
      <button type="submit" disabled={pending} className="btn btn-primary btn-block btn-lg disabled:opacity-60">
        {pending ? "Abrindo…" : "Solicitar um Trampo"}
      </button>
      {!loggedIn && (
        <p className="text-center text-xs text-muted">
          Você entra na conta ao solicitar — leva alguns segundos.
        </p>
      )}
    </form>
  );
}

/** Campo de responder dentro da conversa aberta. */
export function SendMessageForm({ conversationId }: { conversationId: string }) {
  const [body, setBody] = useState("");
  const [state, formAction, pending] = useActionState(async (previous: ConversationFormState, data: FormData) => {
    const result = await sendMessageAction(previous, data);
    if (result.ok) setBody("");
    return result;
  }, EMPTY);

  return (
    <form action={formAction} className="flex flex-col gap-2.5">
      <AuthFeedback state={state} />
      <input type="hidden" name="conversationId" value={conversationId} />
      <textarea
        name="body"
        aria-label="Mensagem"
        value={body}
        onChange={(event) => setBody(event.target.value)}
        readOnly={pending}
        required
        rows={3}
        maxLength={2000}
        placeholder="Escreva sua mensagem…"
        className={`${inputClass} resize-y`}
      />
      <div className="flex justify-end">
        <button type="submit" disabled={pending} className="btn btn-primary disabled:opacity-60">
          {pending ? "Enviando…" : "Enviar"}
        </button>
      </div>
    </form>
  );
}

export function RefreshConversationButton() {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  return (
    <button type="button" disabled={pending} onClick={() => startTransition(() => router.refresh())}
      className="mt-3 rounded px-2 py-1 text-sm font-semibold text-accent hover:underline">
      {pending ? "Atualizando mensagens…" : "Atualizar mensagens"}
    </button>
  );
}
