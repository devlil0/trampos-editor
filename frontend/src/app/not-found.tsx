import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center gap-4 px-6 py-24 text-center">
      <h1 className="text-xl font-bold">Página não encontrada</h1>
      <p className="text-muted">Esse editor pode ter saído do ar ou o link está errado.</p>
      <Link
        href="/"
        className="rounded-full bg-accent px-5 py-2.5 text-sm font-semibold text-accent-contrast hover:bg-accent-strong"
      >
        Voltar pra vitrine
      </Link>
    </div>
  );
}
