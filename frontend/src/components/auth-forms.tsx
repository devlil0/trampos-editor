"use client";

import Link from "next/link";
import { useActionState } from "react";
import { loginAction, registerAction, type AuthFormState } from "@/app/actions/auth";
import { UsernameField } from "./username-field";

const EMPTY: AuthFormState = {};

const inputClass =
  "w-full rounded-xl border border-line bg-panel px-3.5 py-2.5 text-base outline-none placeholder:text-muted focus:border-accent";
const labelClass = "mb-1.5 block text-sm font-semibold";

function ErrorBanner({ message }: { message?: string }) {
  if (!message) return null;
  return (
    <p role="alert" className="rounded-xl border border-busy/40 bg-busy/10 px-3.5 py-2.5 text-sm text-busy">
      {message}
    </p>
  );
}

function SubmitButton({ pending, children }: { pending: boolean; children: React.ReactNode }) {
  return (
    <button
      type="submit"
      disabled={pending}
      className="btn btn-primary btn-block btn-lg"
    >
      {pending ? "Um instante…" : children}
    </button>
  );
}

export function LoginForm() {
  const [state, formAction, pending] = useActionState(loginAction, EMPTY);

  return (
    <form action={formAction} className="flex flex-col gap-4">
      <ErrorBanner message={state.error} />

      <div>
        <label htmlFor="email" className={labelClass}>
          E-mail
        </label>
        <input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          required
          defaultValue={state.values?.email}
          placeholder="voce@exemplo.com"
          className={inputClass}
        />
      </div>

      <div>
        <div className="flex items-baseline justify-between gap-2">
          <label htmlFor="password" className={labelClass}>
            Senha
          </label>
          {/* A saída precisa estar onde a pessoa trava, não numa página de ajuda. */}
          <Link href="/esqueci-senha" className="text-sm text-muted hover:text-accent">
            Esqueci
          </Link>
        </div>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          placeholder="••••••••"
          className={inputClass}
        />
      </div>

      <SubmitButton pending={pending}>Entrar</SubmitButton>

      <p className="text-center text-sm text-muted">
        Ainda não tem conta?{" "}
        <Link href="/criar-conta" className="font-semibold text-accent hover:underline">
          Criar conta
        </Link>
      </p>
    </form>
  );
}

export function RegisterForm() {
  const [state, formAction, pending] = useActionState(registerAction, EMPTY);

  return (
    <form action={formAction} className="flex flex-col gap-4">
      <ErrorBanner message={state.error} />

      <div>
        <label htmlFor="displayName" className={labelClass}>
          Como você quer aparecer
        </label>
        <input
          id="displayName"
          name="displayName"
          type="text"
          autoComplete="name"
          required
          maxLength={80}
          defaultValue={state.values?.displayName}
          placeholder="Seu nome ou nome do canal"
          className={inputClass}
        />
      </div>

      {/* Logo depois do nome de exibição, e antes do e-mail: é a segunda coisa que a
          pessoa decide sobre si, não um detalhe técnico do fim do formulário. Até 2026-08-05
          este campo não existia e o username era derivado do nome acima — ninguém escolhia
          o próprio endereço. */}
      <UsernameField
        defaultValue={state.values?.username}
        hint="É o seu endereço aqui: tramposeditor.com/seunome. Dá pra trocar depois."
      />

      <div>
        <label htmlFor="email" className={labelClass}>
          E-mail
        </label>
        <input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          required
          defaultValue={state.values?.email}
          placeholder="voce@exemplo.com"
          className={inputClass}
        />
      </div>

      <div>
        <label htmlFor="password" className={labelClass}>
          Senha
        </label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={72}
          placeholder="Pelo menos 8 caracteres"
          className={inputClass}
        />
      </div>

      {/* Sem escolher papel: a conta é uma só. Editar é uma condição que a pessoa
          assume depois, publicando o perfil de editor. */}
      <p className="rounded-xl border border-line bg-panel px-3.5 py-2.5 text-xs text-muted">
        Uma conta serve pros dois lados: você já sai contratando, e se quiser pegar trampo é
        só publicar seu perfil de editor depois — no painel.
      </p>

      <SubmitButton pending={pending}>Criar conta</SubmitButton>

      <p className="text-center text-sm text-muted">
        Já tem conta?{" "}
        <Link href="/entrar" className="font-semibold text-accent hover:underline">
          Entrar
        </Link>
      </p>
    </form>
  );
}

