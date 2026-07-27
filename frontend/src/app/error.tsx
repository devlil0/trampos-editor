"use client";

export default function Error({ reset }: { error: Error; reset: () => void }) {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-4 px-6 py-24 text-center">
      <h1 className="text-xl font-bold">Não deu pra carregar a vitrine</h1>
      <p className="text-muted">
        Pode ser que o servidor esteja fora do ar por um instante. Tente de novo em alguns segundos.
      </p>
      <button
        type="button"
        onClick={reset}
        className="rounded-full bg-accent px-5 py-2.5 text-sm font-semibold text-accent-contrast hover:bg-accent-strong"
      >
        Tentar de novo
      </button>
    </div>
  );
}
