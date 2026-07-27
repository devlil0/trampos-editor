"use client";

import { useActionState } from "react";
import {
  createJobRequestAction,
  expressInterestAction,
  type JobFormState,
} from "@/app/actions/job-requests";
import { NICHE_OPTIONS } from "@/lib/format";
import { NicheIcon } from "./niche-tag";

const EMPTY: JobFormState = {};

const inputClass =
  "w-full rounded-xl border border-line bg-panel px-3.5 py-2.5 text-base outline-none placeholder:text-muted focus:border-accent";
const labelClass = "mb-1.5 block text-sm font-semibold";

function Aviso({ state, sucesso }: { state: JobFormState; sucesso: string }) {
  if (state.error) {
    return (
      <p role="alert" className="rounded-xl border border-busy/40 bg-busy/10 px-3.5 py-2.5 text-sm text-busy">
        {state.error}
      </p>
    );
  }
  if (state.ok) {
    return (
      <p role="status" className="rounded-xl border border-good/40 bg-good/10 px-3.5 py-2.5 text-sm text-good">
        {sucesso}
      </p>
    );
  }
  return null;
}

export function NewJobRequestForm() {
  const [state, formAction, pending] = useActionState(createJobRequestAction, EMPTY);

  return (
    <form action={formAction} className="flex flex-col gap-5">
      <Aviso state={state} sucesso="Pedido publicado." />

      <div>
        <label htmlFor="title" className={labelClass}>
          O que você precisa
        </label>
        <input
          id="title"
          name="title"
          type="text"
          required
          maxLength={120}
          placeholder="Ex.: 3 Reels por semana pro meu perfil de fitness"
          className={inputClass}
        />
      </div>

      <div>
        <label htmlFor="description" className={labelClass}>
          Detalhes
        </label>
        <textarea
          id="description"
          name="description"
          required
          rows={5}
          maxLength={2000}
          placeholder="Formato, duração, estilo de edição, se tem legenda, com que frequência…"
          className={`${inputClass} resize-y`}
        />
      </div>

      <fieldset>
        <legend className={labelClass}>Onde vai ser publicado</legend>
        <div className="flex flex-wrap gap-2.5">
          {NICHE_OPTIONS.map(([value, label], i) => (
            <label
              key={value}
              className="flex cursor-pointer items-center gap-2 rounded-full border border-line bg-panel px-3.5 py-2 text-sm font-medium hover:border-accent has-checked:border-accent has-checked:bg-accent-soft"
            >
              <input
                type="radio"
                name="niche"
                value={value}
                defaultChecked={i === 0}
                className="accent-[var(--accent)]"
              />
              <NicheIcon niche={value} size={16} />
              {label}
            </label>
          ))}
        </div>
      </fieldset>

      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor="budget" className={labelClass}>
            Orçamento (R$) — opcional
          </label>
          <input
            id="budget"
            name="budget"
            type="text"
            inputMode="decimal"
            placeholder="300"
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor="deadlineDays" className={labelClass}>
            Prazo (dias) — opcional
          </label>
          <input
            id="deadlineDays"
            name="deadlineDays"
            type="number"
            min={0}
            placeholder="3"
            className={inputClass}
          />
        </div>
      </div>

      <div>
        <label htmlFor="referenceUrl" className={labelClass}>
          Link de referência — opcional
        </label>
        <input
          id="referenceUrl"
          name="referenceUrl"
          type="url"
          placeholder="https://… um vídeo parecido com o que você quer"
          className={inputClass}
        />
      </div>

      <div>
        <button
          type="submit"
          disabled={pending}
          className="rounded-full bg-accent px-5 py-2.5 text-sm font-semibold text-accent-contrast hover:bg-accent-strong disabled:opacity-60"
        >
          {pending ? "Publicando…" : "Publicar pedido"}
        </button>
      </div>
      <p className="text-xs text-muted">
        Seu e-mail só é mostrado pra quem você aceitar. Sem lance, sem leilão.
      </p>
    </form>
  );
}

export function InterestForm({ jobId }: { jobId: string }) {
  const [state, formAction, pending] = useActionState(expressInterestAction, EMPTY);

  return (
    <form action={formAction} className="mt-4 flex flex-col gap-2.5">
      <Aviso state={state} sucesso="Interesse enviado. O cliente decide agora." />
      <input type="hidden" name="jobId" value={jobId} />
      <textarea
        name="message"
        required
        rows={2}
        maxLength={500}
        placeholder="Diga em uma linha por que você é boa escolha pra esse trabalho."
        className={`${inputClass} resize-y`}
      />
      <div>
        <button
          type="submit"
          disabled={pending}
          className="rounded-full bg-accent px-4 py-2 text-sm font-semibold text-accent-contrast hover:bg-accent-strong disabled:opacity-60"
        >
          {pending ? "Enviando…" : "Tenho interesse"}
        </button>
      </div>
    </form>
  );
}
