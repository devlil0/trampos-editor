"use client";

import { useActionState } from "react";
import { saveUsernameAction, type AccountFormState } from "@/app/actions/account";
import { UsernameField } from "./username-field";

const EMPTY: AccountFormState = {};

/**
 * O formulário que grava o username. Serve os dois momentos em que ele é decidido:
 *
 * - `/escolher-username`, onde quem entrou pelo Google escolhe o dele pela primeira vez
 *   (com `redirectTo`, porque a pessoa está barrada e precisa ser solta);
 * - as configurações, onde ela troca.
 *
 * Formulário próprio, e não mais um campo no formulário grande de perfil, pela mesma razão
 * do Instagram: isto salva por outro endpoint, e um "esse nome já está em uso" no meio do
 * submit faria o perfil inteiro parecer que não salvou.
 */
export function UsernameForm({
  username,
  bloqueadoAte = 0,
  redirectTo,
  ctaLabel = "Salvar",
}: {
  /** O nome atual. Vazio quando ainda não há escolha de verdade — o gerado não conta. */
  username: string;
  /** Dias que faltam pro cooldown liberar; 0 quer dizer que pode trocar agora. */
  bloqueadoAte?: number;
  /** Pra onde ir depois de salvar. Só o onboarding usa. */
  redirectTo?: string;
  ctaLabel?: string;
}) {
  const [state, formAction, pending] = useActionState(saveUsernameAction, EMPTY);
  const bloqueado = bloqueadoAte > 0;

  return (
    <form action={formAction} className="flex flex-col gap-3.5">
      {redirectTo && <input type="hidden" name="redirectTo" value={redirectTo} />}

      <UsernameField
        defaultValue={username}
        disabled={bloqueado}
        autoFocus={Boolean(redirectTo)}
        label="Nome de usuário"
        hint={
          bloqueado
            ? `Você trocou faz pouco. Vai poder trocar de novo em ${bloqueadoAte} ${
                bloqueadoAte === 1 ? "dia" : "dias"
              }.`
            : "Letras, números e hífen. De 3 a 30 caracteres."
        }
      />

      {!bloqueado && (
        <div className="flex flex-wrap items-center gap-3">
          <button type="submit" disabled={pending} className="btn btn-primary">
            {pending ? "Salvando…" : ctaLabel}
          </button>
          {/* O aviso fica ao lado do botão, não escondido num tooltip: trocar o endereço
              quebra todo link que já circula, e a pessoa tem que saber disso ANTES de
              clicar — não depois de descobrir que o link que ela mandou pro cliente
              morreu. */}
          <p className="text-xs text-muted">
            O endereço antigo deixa de funcionar e fica livre pra outra pessoa.
          </p>
        </div>
      )}

      {state.error && (
        <p role="alert" className="text-sm text-busy">
          {state.error}
        </p>
      )}
      {state.ok && (
        <p role="status" className="text-sm text-good">
          Pronto. Seu perfil agora mora em /{username}.
        </p>
      )}
    </form>
  );
}
