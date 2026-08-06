"use client";

import { startTransition, useActionState, useEffect, useRef, useState } from "react";
import {
  removeAvatarAction,
  uploadAvatarAction,
  type AvatarFormState,
} from "@/app/actions/avatar";
import { AvatarCropper } from "./avatar-cropper";

const EMPTY: AvatarFormState = {};

/**
 * Troca da foto de perfil.
 *
 * Até 2026-08-06 isto enviava no instante da escolha, sem botão de salvar, com o argumento
 * de que trocar foto é uma decisão só. O argumento caía num ponto: a decisão não era uma —
 * a pessoa escolhia o arquivo e o **enquadramento era do servidor**, que recortava o centro.
 * Quem não tinha o rosto no meio da foto não tinha recurso nenhum.
 *
 * Agora há um passo entre escolher e enviar: o {@link AvatarCropper}. O envio continua sem
 * botão de "salvar" separado — "Usar esta foto" é ao mesmo tempo a confirmação do
 * enquadramento e o envio, então não voltou o clique morto que a versão anterior evitava.
 *
 * A prévia local só troca depois que o recorte é confirmado, e é o recorte de verdade (o
 * mesmo blob que sobe), não mais uma aproximação do que o servidor faria.
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
  const [escolhido, setEscolhido] = useState<File | null>(null);
  const [previa, setPrevia] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // A prévia é um object URL: sem revogar, cada troca de foto na mesma visita vaza um blob.
  useEffect(() => {
    return () => {
      if (previa) URL.revokeObjectURL(previa);
    };
  }, [previa]);

  const src = previa ?? avatarUrl;

  function limparEscolha() {
    setEscolhido(null);
    // Zera o input: sem isto, escolher o mesmo arquivo de novo não dispara `change` e o
    // recorte não reabre — o caso exato de quem cancelou e se arrependeu.
    if (inputRef.current) inputRef.current.value = "";
  }

  return (
    <div>
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
            JPG, PNG ou WebP, até 5 MB. Você escolhe o enquadramento antes de enviar.
          </p>

          <div className="mt-3 flex flex-wrap items-center gap-2.5">
            <label className="btn btn-ghost btn-sm cursor-pointer">
              {avatarUrl ? "Trocar foto" : "Escolher foto"}
              <input
                ref={inputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="sr-only"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) setEscolhido(file);
                }}
              />
            </label>

            {avatarUrl && !escolhido && (
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
          </div>
        </div>
      </div>

      {escolhido && (
        <AvatarCropper
          // Remonta a cada arquivo em vez de zerar estado por dentro: trocar de foto com o
          // recorte aberto (o botão "Trocar foto" continua ali) precisa começar do zero —
          // zoom, posição e um eventual erro do arquivo anterior não valem pro novo.
          key={`${escolhido.name}:${escolhido.size}:${escolhido.lastModified}`}
          file={escolhido}
          pending={pending}
          onCancel={limparEscolha}
          onConfirm={(blob) => {
            setPrevia(URL.createObjectURL(blob));

            // A action é chamada direto com o FormData, sem `<form>`: o que sobe não é mais
            // o arquivo que o input tem, é o recorte que acabou de sair do canvas.
            //
            // O `startTransition` é obrigatório aqui, e não é formalidade: este callback vem
            // do `canvas.toBlob`, que é assíncrono, então já saiu do evento do clique. Uma
            // action de `useActionState` chamada fora de transition não alimenta o
            // `pending` — e é o `pending` que desenha o "enviando…" por cima da foto.
            const dados = new FormData();
            dados.append("file", new File([blob], "avatar.jpg", { type: "image/jpeg" }));
            startTransition(() => formAction(dados));

            limparEscolha();
          }}
        />
      )}

      {state.error && (
        <p role="alert" className="mt-2.5 rounded-xl border border-busy/40 bg-busy/10 px-3.5 py-2 text-sm text-busy">
          {state.error}
        </p>
      )}
    </div>
  );
}
