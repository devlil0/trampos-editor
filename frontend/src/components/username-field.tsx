"use client";

import { useEffect, useId, useState } from "react";

/** Pausa na digitação antes de perguntar ao servidor. */
const DEBOUNCE_MS = 400;

/**
 * O que o servidor respondeu, e sobre qual texto.
 *
 * O `para` é o que dispensa controle de corrida: resposta de uma digitação antiga chega
 * carimbada com o texto antigo e simplesmente não casa com o atual. "neutro" é a falha de
 * rede — não dá pra dizer que está livre nem que está tomado.
 */
type Resultado = {
  para: string;
  kind: "livre" | "ocupado" | "neutro";
  message: string | null;
};

/**
 * O campo de username, com a checagem de disponibilidade enquanto se digita.
 *
 * É um componente só porque o campo aparece em três lugares — cadastro, escolha depois do
 * login social e configurações — e nos três a regra é a mesma. Ter três cópias seria ter
 * três momentos de divergir.
 *
 * O `/` desenhado na caixa não é enfeite: é o que diz que este nome vira um endereço, e é
 * o que separa esta pergunta de "escolha um apelido". Mesma escolha do `@` no campo de
 * Instagram.
 *
 * A checagem é informativa, nunca bloqueante: quem decide é o backend no submit. Se a
 * resposta se perder no caminho, o pior caso é a pessoa descobrir o problema um clique
 * depois — e não um campo travado dizendo que está tudo errado.
 */
export function UsernameField({
  defaultValue = "",
  disabled = false,
  label = "Seu nome de usuário",
  hint,
  autoFocus = false,
}: {
  defaultValue?: string;
  disabled?: boolean;
  label?: string;
  hint?: React.ReactNode;
  autoFocus?: boolean;
}) {
  const id = useId();
  const [valor, setValor] = useState(defaultValue);
  const [resultado, setResultado] = useState<Resultado | null>(null);

  const alvo = valor.trim();
  // Não perguntar sobre o nome que a pessoa já tem: ele "está em uso" — por ela.
  const inativo = !alvo || alvo === defaultValue;

  useEffect(() => {
    if (inativo) return;

    const controller = new AbortController();
    const timer = setTimeout(async () => {
      try {
        const res = await fetch(
          `/api/username-availability?username=${encodeURIComponent(alvo)}`,
          { signal: controller.signal },
        );
        const body = (await res.json()) as { available: boolean; message: string | null };

        setResultado({
          para: alvo,
          kind: body.available ? "livre" : "ocupado",
          message: body.message ?? "Esse nome não está disponível.",
        });
      } catch {
        // Inclui o abort de quem continuou digitando: aquele `alvo` deixou de ser o atual,
        // então este resultado não vai casar com nada e some sozinho.
        setResultado({ para: alvo, kind: "neutro", message: null });
      }
    }, DEBOUNCE_MS);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [alvo, inativo]);

  // O status é DERIVADO, não guardado: enquanto a resposta que chegou não for sobre o que
  // está escrito agora, o campo está checando. Assim não há um segundo estado pra manter em
  // dia com o primeiro — e nenhum setState precisa rodar no corpo do efeito.
  const kind = inativo ? "vazio" : resultado?.para === alvo ? resultado.kind : "checando";

  const borda =
    kind === "livre"
      ? "border-good"
      : kind === "ocupado"
        ? "border-busy"
        : "border-line focus-within:border-accent";

  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-semibold">
        {label}
      </label>

      <div
        className={`flex items-center rounded-xl border bg-panel pl-3.5 ${borda} ${
          disabled ? "opacity-60" : ""
        }`}
      >
        <span aria-hidden="true" className="font-mono text-muted">
          /
        </span>
        <input
          id={id}
          name="username"
          type="text"
          required
          maxLength={30}
          disabled={disabled}
          autoFocus={autoFocus}
          value={valor}
          // Minúscula na hora: é a forma canônica que vai pro banco, e deixar a pessoa
          // digitar "Fulano" pra receber "fulano" depois parece que o site corrigiu ela.
          onChange={(e) => setValor(e.target.value.toLowerCase())}
          placeholder="seunome"
          autoComplete="off"
          autoCapitalize="none"
          spellCheck={false}
          aria-describedby={`${id}-status`}
          className="w-full bg-transparent px-1 py-2.5 font-mono text-base outline-none placeholder:font-sans placeholder:text-muted disabled:cursor-not-allowed"
        />
      </div>

      <p id={`${id}-status`} aria-live="polite" className="mt-1.5 text-xs">
        {kind === "checando" ? (
          <span className="text-muted">Vendo se está livre…</span>
        ) : kind === "livre" ? (
          <span className="font-semibold text-good">/{alvo} está livre.</span>
        ) : kind === "ocupado" ? (
          <span className="text-busy">{resultado?.message}</span>
        ) : (
          // "neutro" (falha de rede) cai aqui junto com "vazio": melhor a dica de sempre
          // que uma acusação que o servidor não fez.
          <span className="text-muted">{hint}</span>
        )}
      </p>
    </div>
  );
}
