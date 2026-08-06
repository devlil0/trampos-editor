"use client";

import Link from "next/link";
import { useActionState, useState, useTransition } from "react";
import {
  confirmSignupAction,
  loginAction,
  registerAction,
  resendSignupCodeAction,
  type AuthFormState,
} from "@/app/actions/auth";
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

/**
 * O cadastro em dois passos (V18).
 *
 * O primeiro passo não cria conta — ele guarda o que foi preenchido e manda um código de
 * seis dígitos pro e-mail. É esse código que separa uma pessoa de um script: mandar o POST
 * ficou de graça, ler a caixa de entrada não.
 *
 * Os dois passos moram no mesmo componente porque são a mesma decisão da pessoa ("criar
 * minha conta"), interrompida no meio. Mandar pra outra rota criaria um endereço que só faz
 * sentido com estado — e que quebra se alguém recarregar.
 */
export function RegisterForm() {
  const [state, formAction, pending] = useActionState(registerAction, EMPTY);

  if (state.codeSent) {
    return <ConfirmSignupForm email={state.codeSent} />;
  }

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

      {/* Confirmar a senha entrou em 2026-08-06. Não é segurança — é o único jeito de
          pegar erro de digitação numa caixa que esconde o que foi digitado. Sem isto, quem
          errasse uma tecla só descobria no login seguinte, sem saber que tinha errado, e o
          caminho de volta seria "esqueci a senha" pra uma senha que ela acha que sabe. */}
      <div>
        <label htmlFor="passwordConfirm" className={labelClass}>
          Confirme a senha
        </label>
        <input
          id="passwordConfirm"
          name="passwordConfirm"
          type="password"
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={72}
          placeholder="A mesma senha de novo"
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

/**
 * Passo 2: o código de seis dígitos.
 *
 * Enquanto esta tela estiver aberta a conta <b>não existe</b> — o que existe é um cadastro
 * pendente com dez minutos de validade. Sair daqui sem confirmar não deixa lixo: o pendente
 * expira sozinho e nunca chega a ocupar o username.
 */
function ConfirmSignupForm({ email }: { email: string }) {
  const [state, formAction, pending] = useActionState(confirmSignupAction, EMPTY);
  const [reenviado, setReenviado] = useState(false);
  const [reenviando, startReenvio] = useTransition();

  return (
    <form action={formAction} className="flex flex-col gap-4">
      <ErrorBanner message={state.error} />

      {/* O e-mail viaja no formulário porque não há sessão nesta altura: a conta ainda não
          existe, então não há de onde o servidor inferir de quem é o código. */}
      <input type="hidden" name="email" value={email} />

      <div>
        <h2 className="text-lg font-bold tracking-tight">Confirme seu e-mail</h2>
        <p className="mt-1 text-sm text-muted">
          Mandamos um código de seis dígitos pra{" "}
          <b className="text-fg">{email}</b>. Ele vale por 10 minutos.
        </p>
      </div>

      <div>
        <label htmlFor="code" className={labelClass}>
          Código
        </label>
        <input
          id="code"
          name="code"
          // `text` com `inputMode numeric`, e não `type="number"`: o campo numérico traz
          // setinhas de incremento e come zero à esquerda — e o código pode começar com zero.
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          pattern="[0-9]{6}"
          maxLength={6}
          required
          autoFocus
          placeholder="000000"
          className={`${inputClass} text-center font-mono text-2xl tracking-[0.4em]`}
        />
      </div>

      <SubmitButton pending={pending}>Confirmar e entrar</SubmitButton>

      <p className="text-center text-sm text-muted">
        {reenviado ? (
          "Mandamos outro código."
        ) : (
          <>
            Não chegou?{" "}
            <button
              type="button"
              disabled={reenviando}
              onClick={() =>
                startReenvio(async () => {
                  await resendSignupCodeAction(email);
                  setReenviado(true);
                })
              }
              className="font-semibold text-accent hover:underline disabled:opacity-60"
            >
              Enviar de novo
            </button>
          </>
        )}
      </p>
    </form>
  );
}
