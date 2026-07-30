"use client";

import { useEffect, useState } from "react";

const TYPE_MS = 85;
const ERASE_MS = 45;
const HOLD_MS = 1600;

/**
 * Palavra que se digita sozinha no fim do título, com cursor piscando.
 * <p>
 * O estado inicial é sempre a primeira palavra — a mesma coisa que o servidor
 * renderiza, então não há divergência de hidratação. Quem pediu menos movimento
 * (prefers-reduced-motion) vê a primeira palavra parada, sem cursor.
 */
export function TypedWord({ words }: { words: string[] }) {
  const [text, setText] = useState(words[0]);

  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    let indice = 0;
    let letras = words[0].length;
    let apagando = true;
    let timer: ReturnType<typeof setTimeout>;

    function passo() {
      const alvo = words[indice];

      if (apagando) {
        letras -= 1;
        if (letras <= 0) {
          apagando = false;
          indice = (indice + 1) % words.length;
        }
      } else {
        letras += 1;
        if (letras >= alvo.length) {
          apagando = true;
          setText(words[indice].slice(0, letras));
          timer = setTimeout(passo, HOLD_MS);
          return;
        }
      }

      setText(words[indice].slice(0, Math.max(0, letras)));
      timer = setTimeout(passo, apagando ? ERASE_MS : TYPE_MS);
    }

    timer = setTimeout(passo, HOLD_MS);
    return () => clearTimeout(timer);
  }, [words]);

  return (
    <span className="text-accent">
      {text}
      {/* O cursor é sempre renderizado (inclusive no HTML do servidor, o que evita
          salto ao hidratar) e some pra quem pediu menos movimento. */}
      <span
        aria-hidden="true"
        className="ml-0.5 inline-block w-[0.06em] bg-accent align-baseline [animation:cursor-blink_1.1s_steps(1)_infinite] motion-reduce:hidden"
        style={{ height: "0.78em" }}
      />
    </span>
  );
}
