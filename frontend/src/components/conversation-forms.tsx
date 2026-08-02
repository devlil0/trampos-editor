"use client";

import { useActionState, useEffect, useRef } from "react";
import {
  sendMessageAction,
  startConversationAction,
  type ConversationFormState,
} from "@/app/actions/conversations";

const EMPTY: ConversationFormState = {};

const inputClass =
  "w-full rounded-xl border border-line bg-panel px-3.5 py-2.5 text-base outline-none placeholder:text-muted focus:border-accent";

function Erro({ state }: { state: ConversationFormState }) {
  if (!state.error) return null;
  return (
    <p role="alert" className="rounded-xl border border-busy/40 bg-busy/10 px-3.5 py-2.5 text-sm text-busy">
      {state.error}
    </p>
  );
}

/**
 * Porta 1 — o formulário que substitui o antigo `href="#"` do perfil.
 *
 * Fica no próprio perfil, e não atrás de um clique que leva pra outra tela: pedir
 * pra pessoa navegar antes de escrever é a fricção que o produto inteiro evita.
 */
export function StartConversationForm({
  slug,
  firstName,
  loggedIn,
}: {
  slug: string;
  firstName: string;
  loggedIn: boolean;
}) {
  const [state, formAction, pending] = useActionState(startConversationAction, EMPTY);

  return (
    <form action={formAction} className="flex flex-col gap-2.5">
      <Erro state={state} />
      <input type="hidden" name="slug" value={slug} />
      <textarea
        name="body"
        required
        rows={3}
        maxLength={2000}
        placeholder={`Diga o que você precisa. Ex.: "Tenho 4 Reels por semana, topa?"`}
        className={`${inputClass} resize-y`}
      />
      <button type="submit" disabled={pending} className="btn btn-primary btn-block btn-lg disabled:opacity-60">
        {pending ? "Enviando…" : `Falar com ${firstName}`}
      </button>
      {!loggedIn && (
        // Não escondemos o formulário de quem está deslogado: ver o campo é o que
        // mostra que dá pra falar direto. O login vira um passo no caminho, não um portão.
        <p className="text-center text-xs text-muted">
          Você entra na conta ao enviar — leva alguns segundos.
        </p>
      )}
    </form>
  );
}

/** Campo de responder dentro da conversa aberta. */
export function SendMessageForm({ conversationId }: { conversationId: string }) {
  const [state, formAction, pending] = useActionState(sendMessageAction, EMPTY);
  const formRef = useRef<HTMLFormElement>(null);

  // Depois do envio bem-sucedido o campo tem que voltar vazio: sem isso o texto
  // fica lá e a pessoa reenvia a mesma mensagem sem perceber.
  useEffect(() => {
    if (!pending && !state.error) formRef.current?.reset();
  }, [pending, state]);

  return (
    <form ref={formRef} action={formAction} className="flex flex-col gap-2.5">
      <Erro state={state} />
      <input type="hidden" name="conversationId" value={conversationId} />
      <textarea
        name="body"
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
