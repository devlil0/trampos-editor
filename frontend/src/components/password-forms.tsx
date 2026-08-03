"use client";

import Link from "next/link";
import { useActionState } from "react";
import {
  confirmPasswordResetAction,
  requestPasswordResetAction,
  type PasswordResetState,
} from "@/app/actions/auth";

const EMPTY: PasswordResetState = {};

const inputClass =
  "w-full rounded-xl border border-line bg-panel px-3.5 py-2.5 text-base outline-none placeholder:text-muted focus:border-accent";

function Erro({ state }: { state: PasswordResetState }) {
  if (!state.error) return null;
  return (
    <p role="alert" className="rounded-xl border border-busy/40 bg-busy/10 px-3.5 py-2.5 text-sm text-busy">
      {state.error}
    </p>
  );
}

export function RequestResetForm() {
  const [state, formAction, pending] = useActionState(requestPasswordResetAction, EMPTY);

  if (state.sent) {
    return (
      <div className="rounded-[18px] border border-good/40 bg-good/10 p-6">
        <p className="font-semibold">Se existir uma conta com esse e-mail, o link já saiu.</p>
        {/* A frase é condicional de propósito: confirmar que a conta existe entregaria
            quem está cadastrado pra qualquer um que digitasse um e-mail. */}
        <p className="mt-1.5 text-sm text-muted">
          Ele vale por 1 hora. Não chegou? Confira o spam antes de pedir outro.
        </p>
        <Link href="/entrar" className="mt-5 btn btn-ghost btn-sm">
          Voltar pro login
        </Link>
      </div>
    );
  }

  return (
    <form action={formAction} className="flex flex-col gap-3">
      <Erro state={state} />
      <div className="flex flex-col gap-1.5">
        <label htmlFor="email" className="text-sm font-semibold">
          E-mail da conta
        </label>
        <input
          id="email"
          name="email"
          type="email"
          required
          autoComplete="email"
          defaultValue={state.values?.email ?? ""}
          className={inputClass}
        />
      </div>
      <button type="submit" disabled={pending} className="btn btn-primary btn-block btn-lg disabled:opacity-60">
        {pending ? "Enviando…" : "Mandar o link"}
      </button>
      <p className="text-center text-sm text-muted">
        Lembrou?{" "}
        <Link href="/entrar" className="font-semibold text-accent hover:underline">
          Entrar
        </Link>
      </p>
    </form>
  );
}

export function ConfirmResetForm({ token }: { token: string }) {
  const [state, formAction, pending] = useActionState(confirmPasswordResetAction, EMPTY);

  if (state.done) {
    return (
      <div className="rounded-[18px] border border-good/40 bg-good/10 p-6">
        <p className="font-semibold">Senha trocada.</p>
        <p className="mt-1.5 text-sm text-muted">Agora é só entrar com ela.</p>
        <Link href="/entrar" className="mt-5 btn btn-primary btn-sm">
          Entrar
        </Link>
      </div>
    );
  }

  if (!token) {
    return (
      <div className="rounded-[18px] border border-busy/40 bg-busy/10 p-6">
        <p className="font-semibold">Link incompleto.</p>
        <p className="mt-1.5 text-sm text-muted">
          Abra o link direto do e-mail, ou peça outro.
        </p>
        <Link href="/esqueci-senha" className="mt-5 btn btn-ghost btn-sm">
          Pedir outro link
        </Link>
      </div>
    );
  }

  return (
    <form action={formAction} className="flex flex-col gap-3">
      <Erro state={state} />
      <input type="hidden" name="token" value={token} />
      <div className="flex flex-col gap-1.5">
        <label htmlFor="password" className="text-sm font-semibold">
          Senha nova
        </label>
        <input
          id="password"
          name="password"
          type="password"
          required
          minLength={8}
          autoComplete="new-password"
          className={inputClass}
        />
        <p className="text-xs text-muted">Pelo menos 8 caracteres.</p>
      </div>
      <div className="flex flex-col gap-1.5">
        <label htmlFor="confirm" className="text-sm font-semibold">
          Repita a senha
        </label>
        <input
          id="confirm"
          name="confirm"
          type="password"
          required
          minLength={8}
          autoComplete="new-password"
          className={inputClass}
        />
      </div>
      <button type="submit" disabled={pending} className="btn btn-primary btn-block btn-lg disabled:opacity-60">
        {pending ? "Trocando…" : "Trocar a senha"}
      </button>
    </form>
  );
}
