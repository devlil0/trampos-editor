/**
 * Fundo vivo do hero: manchas roxas desfocadas que vagam devagar, como luz de estúdio
 * atrás do conteúdo. Some sob o texto por opacidade baixa e blur alto — se der pra
 * apontar onde uma mancha começa, está forte demais.
 *
 * CSS puro (ver @keyframes aurora-a/aurora-b no globals.css). Sem JS, sem canvas: é
 * decoração, não pode custar frame nem bloquear o hero. `pointer-events-none` porque
 * a camada cobre os botões do hero, e `aria-hidden` porque não há o que narrar.
 */

/** Cada mancha: posição, tamanho, cor e o ciclo que ela percorre. */
const MANCHAS = [
  {
    className: "-top-40 -left-32 h-[520px] w-[520px] bg-accent/25",
    animation: "aurora-a 26s ease-in-out infinite alternate",
  },
  {
    className: "-top-24 left-1/3 h-[420px] w-[420px] bg-accent-strong/18",
    // Ritmo diferente do vizinho de propósito: durações próximas fariam as duas
    // manchas baterem juntas e o fundo pulsaria em vez de vagar.
    animation: "aurora-b 34s ease-in-out infinite alternate",
  },
  {
    className: "-bottom-48 right-[-10%] h-[460px] w-[460px] bg-accent/18",
    animation: "aurora-b 29s ease-in-out infinite alternate -8s",
  },
];

export function Aurora() {
  return (
    <div
      aria-hidden="true"
      className="motion-always pointer-events-none absolute inset-0 overflow-hidden"
    >
      {MANCHAS.map((mancha) => (
        <span
          key={mancha.className}
          className={`absolute rounded-full blur-[120px] ${mancha.className}`}
          style={{ animation: mancha.animation, willChange: "transform" }}
        />
      ))}
    </div>
  );
}
