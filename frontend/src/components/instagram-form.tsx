"use client";

import { useActionState } from "react";
import { saveInstagramAction, type AccountFormState } from "@/app/actions/account";
import { InstagramGlyph, InstagramLink } from "./instagram-link";

const EMPTY: AccountFormState = {};

/**
 * Campo de Instagram da conta — o mesmo componente serve o cliente (painel) e o editor
 * (meu perfil), porque o campo é um só.
 *
 * Fica fora do formulário grande de perfil de propósito: o Instagram é da conta, não do
 * perfil de editor, e quem só contrata precisa dele sem nunca abrir aquele formulário.
 */
export function InstagramForm({ instagram }: { instagram: string | null }) {
  const [state, formAction, pending] = useActionState(saveInstagramAction, EMPTY);

  // Depois de salvar, o valor salvo vem do servidor via revalidatePath.
  return (
    <form action={formAction} className="flex flex-col gap-2.5">
      <label htmlFor="instagram" className="flex items-center gap-2 text-sm font-semibold">
        <InstagramGlyph size={17} />
        Instagram
      </label>

      <div className="flex flex-wrap items-center gap-2.5">
        {/* O "@" fica desenhado na caixa: assim ninguém digita o dele achando que falta,
            e o backend aceita das duas formas de qualquer jeito. */}
        <div className="flex min-w-0 flex-1 items-center rounded-xl border border-line bg-panel pl-3.5 focus-within:border-accent">
          <span aria-hidden="true" className="text-muted">
            @
          </span>
          <input
            id="instagram"
            name="instagram"
            type="text"
            maxLength={60}
            defaultValue={instagram ?? ""}
            placeholder="seuperfil"
            autoComplete="off"
            autoCapitalize="none"
            spellCheck={false}
            className="w-full bg-transparent px-1.5 py-2.5 text-base outline-none placeholder:text-muted"
          />
        </div>
        <button
          type="submit"
          disabled={pending}
          className="btn btn-ghost"
        >
          {pending ? "Salvando…" : "Salvar"}
        </button>
      </div>

      {state.error ? (
        <p role="alert" className="text-sm text-busy">
          {state.error}
        </p>
      ) : state.ok ? (
        <p role="status" className="text-sm text-good">
          Instagram salvo.
        </p>
      ) : instagram ? (
        <p className="text-xs text-muted">
          Aparece no seu perfil como <InstagramLink username={instagram} size={13} />
        </p>
      ) : (
        <p className="text-xs text-muted">
          Opcional. Pode colar o link do seu perfil — a gente extrai o usuário. Deixe vazio
          pra remover.
        </p>
      )}
    </form>
  );
}
