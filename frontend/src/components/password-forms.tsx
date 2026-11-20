"use client";

import Link from "next/link";
import { useActionState } from "react";
import {
  confirmPasswordResetAction,
  requestPasswordResetAction,
  type PasswordResetState,
} from "@/app/actions/auth";
import { AuthFeedback, AuthSubmitButton } from "./auth-feedback";

const EMPTY: PasswordResetState = {};

const inputClass =
  "w-full rounded-xl border border-line bg-panel px-3.5 py-2.5 text-base outline-none placeholder:text-muted focus:border-accent";

function Erro({ state }: { state: PasswordResetState }) {
  return <AuthFeedback state={state} />;
}

export function RequestResetForm() {
  const [state, formAction, pending] = useActionState(requestPasswordResetAction, EMPTY);

  if (state.sent) {
    return (
      <div className="rounded-[18px] border border-good/40 bg-good/10 p-6">
        <AuthFeedback state={{ notice: "Se houver uma conta elegível, tentaremos enviar as instruções. Se não receber, tente novamente mais tarde." }} />
        <p className="mt-1.5 text-sm text-muted">
          Confira também a pasta de spam.
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
      <AuthSubmitButton key={state.retryAt ?? 0} pending={pending} retryAt={state.retryAt}>
        Mandar o link
      </AuthSubmitButton>
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
