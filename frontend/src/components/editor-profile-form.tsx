"use client";

import { useActionState } from "react";
import {
  addPortfolioAction,
  saveProfileAction,
  type ProfileFormState,
} from "@/app/actions/editor-profile";
import { NICHE_OPTIONS, STYLE_OPTIONS, TOOL_OPTIONS } from "@/lib/format";
import { NicheIcon, ToolIcon } from "./niche-tag";
import type { EditorProfile } from "@/lib/types";

const EMPTY: ProfileFormState = {};

const inputClass =
  "w-full rounded-xl border border-line bg-panel px-3.5 py-2.5 text-base outline-none placeholder:text-muted focus:border-accent";
const labelClass = "mb-1.5 block text-sm font-semibold";

function Feedback({ state, sucesso }: { state: ProfileFormState; sucesso: string }) {
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

/** Centavos -> "80" ou "80.50", pro valor inicial do input. */
function centsToInput(cents: number | null): string {
  if (cents == null) return "";
  return Number.isInteger(cents / 100) ? String(cents / 100) : (cents / 100).toFixed(2);
}

export function EditorProfileForm({ profile }: { profile: EditorProfile | null }) {
  const [state, formAction, pending] = useActionState(saveProfileAction, EMPTY);

  return (
    <form action={formAction} className="flex flex-col gap-5">
      <Feedback state={state} sucesso="Perfil salvo. Já está valendo na vitrine." />

      <div>
        <label htmlFor="headline" className={labelClass}>
          Chamada
        </label>
        <input
          id="headline"
          name="headline"
          type="text"
          maxLength={120}
          defaultValue={profile?.headline ?? ""}
          placeholder="Ex.: Cortes de Reels que seguram até o final"
          className={inputClass}
        />
        <p className="mt-1.5 text-xs text-muted">
          É a primeira linha que o cliente lê no seu card. Diga o que você faz de melhor.
        </p>
      </div>

      <div>
        <label htmlFor="bio" className={labelClass}>
          Sobre você
        </label>
        <textarea
          id="bio"
          name="bio"
          rows={5}
          maxLength={2000}
          defaultValue={profile?.bio ?? ""}
          placeholder="Conte sua experiência, seu estilo de edição e como você gosta de trabalhar."
          className={`${inputClass} resize-y`}
        />
      </div>

      <fieldset>
        <legend className={labelClass}>Plataformas que você atende</legend>
        <div className="grid gap-2.5 sm:grid-cols-2">
          {NICHE_OPTIONS.map(([value, label]) => (
            <label
              key={value}
              className="flex cursor-pointer items-center gap-2.5 rounded-xl border border-line bg-panel px-3.5 py-2.5 text-sm font-semibold hover:border-accent has-checked:border-accent has-checked:bg-accent-soft"
            >
              <input
                type="checkbox"
                name="niches"
                value={value}
                defaultChecked={profile?.niches.includes(value)}
                className="accent-[var(--accent)]"
              />
              <NicheIcon niche={value} size={17} />
              {label}
            </label>
          ))}
        </div>
      </fieldset>

      <fieldset>
        <legend className={labelClass}>Seu estilo de edição</legend>
        <p className="mb-2.5 text-xs text-muted">
          É por aqui que muita gente procura — &ldquo;quero legenda animada&rdquo;, &ldquo;quero
          algo clean&rdquo;. Marque quantos combinarem com você.
        </p>
        <div className="flex flex-wrap gap-2.5">
          {STYLE_OPTIONS.map(([value, label]) => (
            <label
              key={value}
              className="flex cursor-pointer items-center gap-2 rounded-full border border-line bg-panel px-3.5 py-2 text-sm font-medium hover:border-accent has-checked:border-accent has-checked:bg-accent-soft"
            >
              <input
                type="checkbox"
                name="styles"
                value={value}
                defaultChecked={profile?.styles.includes(value)}
                className="accent-[var(--accent)]"
              />
              {label}
            </label>
          ))}
        </div>
      </fieldset>

      <fieldset>
        <legend className={labelClass}>Ferramentas que você usa</legend>
        <div className="flex flex-wrap gap-2.5">
          {TOOL_OPTIONS.map(([value, label]) => (
            <label
              key={value}
              className="flex cursor-pointer items-center gap-2 rounded-full border border-line bg-panel px-3.5 py-2 text-sm font-medium hover:border-accent has-checked:border-accent has-checked:bg-accent-soft"
            >
              <input
                type="checkbox"
                name="tools"
                value={value}
                defaultChecked={profile?.tools.includes(value)}
                className="accent-[var(--accent)]"
              />
              <ToolIcon tool={value} size={16} />
              {label}
            </label>
          ))}
        </div>
      </fieldset>

      <div className="grid gap-4 sm:grid-cols-3">
        <div>
          <label htmlFor="priceMin" className={labelClass}>
            Preço mínimo (R$)
          </label>
          <input
            id="priceMin"
            name="priceMin"
            type="text"
            inputMode="decimal"
            defaultValue={centsToInput(profile?.priceMinCents ?? null)}
            placeholder="80"
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor="priceMax" className={labelClass}>
            Preço máximo (R$)
          </label>
          <input
            id="priceMax"
            name="priceMax"
            type="text"
            inputMode="decimal"
            defaultValue={centsToInput(profile?.priceMaxCents ?? null)}
            placeholder="400"
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor="turnaroundDays" className={labelClass}>
            Prazo médio (dias)
          </label>
          <input
            id="turnaroundDays"
            name="turnaroundDays"
            type="number"
            min={0}
            defaultValue={profile?.turnaroundDays ?? ""}
            placeholder="2"
            className={inputClass}
          />
        </div>
      </div>

      <label className="flex cursor-pointer items-start gap-2.5 rounded-xl border border-line bg-panel p-3.5 has-checked:border-accent">
        <input
          type="checkbox"
          name="available"
          defaultChecked={profile?.available ?? true}
          className="mt-0.5 accent-[var(--accent)]"
        />
        <span>
          <span className="block text-sm font-semibold">Estou disponível pra novos projetos</span>
          <span className="block text-xs text-muted">
            Quem está disponível aparece primeiro na vitrine. Desmarque quando encher a agenda.
          </span>
        </span>
      </label>

      <div>
        <button
          type="submit"
          disabled={pending}
          className="btn btn-primary disabled:opacity-60"
        >
          {pending ? "Salvando…" : profile ? "Salvar alterações" : "Publicar meu perfil"}
        </button>
      </div>
    </form>
  );
}

export function AddPortfolioForm() {
  const [state, formAction, pending] = useActionState(addPortfolioAction, EMPTY);

  return (
    <form action={formAction} className="flex flex-col gap-3.5">
      <Feedback state={state} sucesso="Vídeo adicionado ao portfólio." />

      <div className="grid gap-3.5 sm:grid-cols-[1fr_1.3fr_auto] sm:items-end">
        <div>
          <label htmlFor="title" className={labelClass}>
            Título
          </label>
          <input
            id="title"
            name="title"
            type="text"
            maxLength={120}
            required
            placeholder="Reel de lançamento"
            className={inputClass}
          />
        </div>
        <div>
          <label htmlFor="videoUrl" className={labelClass}>
            Link do vídeo
          </label>
          <input
            id="videoUrl"
            name="videoUrl"
            type="url"
            required
            placeholder="https://youtu.be/…"
            className={inputClass}
          />
        </div>
        <button
          type="submit"
          disabled={pending}
          className="btn btn-ghost disabled:opacity-60"
        >
          {pending ? "Adicionando…" : "Adicionar"}
        </button>
      </div>
      <p className="text-xs text-muted">Aceitamos YouTube e Vimeo — o embed vem do próprio link.</p>
    </form>
  );
}
