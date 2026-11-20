"use client";

export default function Error() {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-4 px-6 py-24 text-center">
      <h1 className="text-xl font-bold">Não foi possível carregar esta página</h1>
      <p role="alert" className="text-muted">
        Pode ser que o servidor esteja fora do ar por um instante. Tente de novo em alguns segundos.
      </p>
      <button
        type="button"
        onClick={() => window.location.reload()}
        className="btn btn-primary"
      >
        Tentar de novo
      </button>
    </div>
  );
}
