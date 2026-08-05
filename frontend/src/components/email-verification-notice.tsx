"use client";

import { useActionState } from "react";
import { resendEmailVerificationAction, type AccountFormState } from "@/app/actions/account";

const EMPTY: AccountFormState = {};

/**
 * O aviso de e-mail não confirmado (ESBOCO §2, fase 1.5).
 *
 * Diz o que está travado e por quê, em vez de só pedir. "Confirme seu e-mail" sozinho lê
 * como burocracia; dizer que sem isso ninguém consegue te avisar que te procurou explica
 * o que a pessoa perde — que é a única razão honesta de estar pedindo.
 *
 * Fica no topo das configurações e some sozinho quando o e-mail é confirmado. Não é modal
 * nem bloqueia a tela: o §11.9 é porta aberta, e o que a falta de confirmação impede é
 * pouco e específico.
 */
export function EmailVerificationNotice({ email }: { email: string }) {
  const [state, formAction, pending] = useActionState(resendEmailVerificationAction, EMPTY);

  return (
    <section className="mt-6 rounded-[18px] border border-busy/40 bg-busy/10 p-6">
      <h2 className="text-lg font-bold tracking-tight text-busy">Confirme seu e-mail</h2>
      <p className="mt-1.5 text-sm">
        Mandamos um link pra <b>{email}</b>. Enquanto ele não for aberto, os avisos de
        mensagem e de trampo não chegam em você — e seu perfil de editor não pode ir pra
        vitrine.
      </p>

      <form action={formAction} className="mt-4 flex flex-wrap items-center gap-3">
        <button type="submit" disabled={pending || state.ok} className="btn btn-ghost btn-sm">
          {pending ? "Mandando…" : state.ok ? "Link reenviado" : "Reenviar link"}
        </button>
        <span className="text-xs text-muted">
          {state.ok
            ? "Confira a caixa de entrada e o spam."
            : "Não achou? A gente manda de novo."}
        </span>
      </form>

      {state.error && (
        <p role="alert" className="mt-2 text-sm text-busy">
          {state.error}
        </p>
      )}
    </section>
  );
}
