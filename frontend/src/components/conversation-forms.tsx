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
      <Erro state={state} />
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
