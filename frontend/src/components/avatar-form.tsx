"use client";

import { useActionState, useRef, useState } from "react";
import {
  removeAvatarAction,
  uploadAvatarAction,
  type AvatarFormState,
} from "@/app/actions/avatar";

const EMPTY: AvatarFormState = {};

/**
 * Troca da foto de perfil.
 *
 * Envia ao escolher o arquivo, sem botão de "salvar": trocar foto é uma decisão só, e um
 * segundo clique pra confirmar o que já foi escolhido no seletor do sistema é passo morto.
 *
 * A prévia é local (`URL.createObjectURL`) pra a mudança aparecer na hora, antes da volta
 * do servidor — o recorte quadrado que o backend aplica é central, então o que se vê aqui
 * é fiel ao que vai ficar.
 */
export function AvatarForm({
  displayName,
  avatarUrl,
}: {
  displayName: string;
  /** URL pronta, montada no servidor: `PUBLIC_API_URL` não existe no browser. */
  avatarUrl: string | null;
}) {
  const [state, formAction, pending] = useActionState(uploadAvatarAction, EMPTY);
  const formRef = useRef<HTMLFormElement>(null);
  const [previa, setPrevia] = useState<string | null>(null);

  const src = previa ?? avatarUrl;

  return (
    <div className="flex flex-wrap items-center gap-5">
      <span className="relative">
        {src ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={src}
            alt=""
            width={88}
            height={88}
            className="h-22 w-22 rounded-full object-cover"
          />
        ) : (
          <span className="grid h-22 w-22 place-items-center rounded-full bg-gradient-to-br from-accent to-accent-strong text-3xl font-extrabold text-white">
            {displayName.charAt(0).toUpperCase()}
          </span>
        )}
        {pending && (
          <span className="absolute inset-0 grid place-items-center rounded-full bg-black/50 text-xs font-semibold text-white">
            enviando…
          </span>
        )}
      </span>

      <div className="min-w-0 flex-1">
        <h2 className="text-lg font-bold tracking-tight">Foto de perfil</h2>
        <p className="mt-1 text-sm text-muted">
          JPG, PNG ou WebP, até 5 MB. Recortamos o centro em quadrado e reduzimos pra 256px.
        </p>

        <form ref={formRef} action={formAction} className="mt-3 flex flex-wrap items-center gap-2.5">
          <label className="btn btn-ghost btn-sm cursor-pointer">
            {avatarUrl ? "Trocar foto" : "Escolher foto"}
            <input
              type="file"
              name="file"
              accept="image/jpeg,image/png,image/webp"
              className="sr-only"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                setPrevia(URL.createObjectURL(file));
                formRef.current?.requestSubmit();
              }}
            />
          </label>

          {avatarUrl && (
            <button
              type="button"
              onClick={() => {
                setPrevia(null);
                void removeAvatarAction();
              }}
              className="text-sm font-semibold text-muted hover:text-busy"
            >
              Remover
            </button>
          )}
        </form>

        {state.error && (
          <p role="alert" className="mt-2.5 rounded-xl border border-busy/40 bg-busy/10 px-3.5 py-2 text-sm text-busy">
            {state.error}
          </p>
        )}
      </div>
    </div>
  );
}
