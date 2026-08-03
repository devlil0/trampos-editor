"use client";

import { useActionState, useState } from "react";
import {
  cancelDealAction,
  markDealDoneAction,
  openDealAction,
  reviewDealAction,
  type DealFormState,
} from "@/app/actions/deals";

const EMPTY: DealFormState = {};

const inputClass =
  "w-full rounded-xl border border-line bg-panel px-3.5 py-2.5 text-base outline-none placeholder:text-muted focus:border-accent";

function Erro({ state }: { state: DealFormState }) {
  if (!state.error) return null;
  return (
    <p role="alert" className="rounded-xl border border-busy/40 bg-busy/10 px-3.5 py-2.5 text-sm text-busy">
      {state.error}
    </p>
  );
}

/**
 * Abre o trampo dentro de uma conversa da vitrine. Só aparece pra quem contrata —
 * quem combina o trampo é quem está pagando.
 */
export function OpenDealForm({ conversationId }: { conversationId: string }) {
  const [state, formAction, pending] = useActionState(openDealAction, EMPTY);

  return (
    <form action={formAction} className="flex flex-col gap-2.5">
      <Erro state={state} />
      <input type="hidden" name="conversationId" value={conversationId} />
      <label htmlFor="deal-title" className="text-sm text-muted">
        Combinaram um trampo? Registre pra poder avaliar quando terminar.
      </label>
      <input
        id="deal-title"
        name="title"
        required
        maxLength={140}
        placeholder="Ex.: 4 Reels por semana"
        className={inputClass}
      />
      <button type="submit" disabled={pending} className="btn btn-ghost btn-sm disabled:opacity-60">
        {pending ? "Registrando…" : "Registrar trampo"}
      </button>
    </form>
  );
}

/**
 * O marco de "acabou". Fica só com o cliente: é quem recebe o trabalho, então é quem
 * diz que chegou — deixar o editor marcar entregue convidaria a farmar avaliação.
 */
export function CloseDealForm({ dealId }: { dealId: string }) {
  const [doneState, doneAction, donePending] = useActionState(markDealDoneAction, EMPTY);
  const [cancelState, cancelAction, cancelPending] = useActionState(cancelDealAction, EMPTY);

  return (
    <div className="mt-4 flex flex-col gap-2.5">
      <Erro state={doneState} />
      <Erro state={cancelState} />
      <div className="flex flex-wrap gap-2.5">
        <form action={doneAction}>
          <input type="hidden" name="dealId" value={dealId} />
          <button type="submit" disabled={donePending} className="btn btn-primary btn-sm disabled:opacity-60">
            {donePending ? "Encerrando…" : "Marcar como concluído"}
          </button>
        </form>
        <form action={cancelAction}>
          <input type="hidden" name="dealId" value={dealId} />
          <button type="submit" disabled={cancelPending} className="btn btn-ghost btn-sm disabled:opacity-60">
            {cancelPending ? "Cancelando…" : "Não vingou"}
          </button>
        </form>
      </div>
    </div>
  );
}

/**
 * A avaliação. Comentário é opcional de propósito: exigir texto faria a nota parar de
 * acontecer, e é a nota que alimenta a ordenação da vitrine.
 */
export function ReviewForm({ dealId, counterpartName }: { dealId: string; counterpartName: string }) {
  const [state, formAction, pending] = useActionState(reviewDealAction, EMPTY);
  const [rating, setRating] = useState(0);

  return (
    <form action={formAction} className="mt-4 flex flex-col gap-2.5">
      <Erro state={state} />
      <input type="hidden" name="dealId" value={dealId} />
      <input type="hidden" name="rating" value={rating} />

      <fieldset className="flex flex-col gap-2">
        <legend className="text-sm text-muted">Como foi trabalhar com {counterpartName}?</legend>
        <div className="flex gap-1.5" role="radiogroup" aria-label="Nota de 1 a 5">
          {[1, 2, 3, 4, 5].map((n) => (
            <button
              key={n}
              type="button"
              role="radio"
              aria-checked={rating === n}
              aria-label={`${n} de 5`}
              onClick={() => setRating(n)}
              className={
                n <= rating
                  ? "rounded-lg border border-amber-400/60 bg-amber-400/15 px-3 py-1.5 text-lg leading-none text-amber-400"
                  : "rounded-lg border border-line bg-panel px-3 py-1.5 text-lg leading-none text-muted hover:border-amber-400/50"
              }
            >
              ★
            </button>
          ))}
        </div>
      </fieldset>

      <textarea
        name="comment"
        rows={2}
        maxLength={1000}
        placeholder="Opcional: o que fez esse trampo ser o que foi."
        className={`${inputClass} resize-y`}
      />

      <button
        type="submit"
        disabled={pending || rating === 0}
        className="btn btn-primary btn-sm disabled:opacity-60"
      >
        {pending ? "Enviando…" : "Enviar avaliação"}
      </button>
    </form>
  );
}
