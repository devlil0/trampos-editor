import type { Metadata } from "next";
import Link from "next/link";
import { NewJobRequestForm } from "@/components/job-forms";
import { requireCurrentUser } from "@/lib/session";

export const metadata: Metadata = {
  title: "Novo pedido — TramposEditor",
  robots: { index: false },
};

export default async function NovoPedidoPage() {
  await requireCurrentUser();

  return (
    <div className="mx-auto max-w-3xl px-6 py-12">
      <Link href="/painel/pedidos" className="mb-4 inline-flex items-center gap-1.5 text-sm text-muted hover:text-accent">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
          <path d="m15 18-6-6 6-6" />
        </svg>
        Meus pedidos
      </Link>

      <h1 className="text-3xl font-extrabold tracking-tight">Postar um pedido</h1>
      <p className="mt-1 text-muted">
        Descreva o que você precisa. Os editores respondem dizendo que topam — você escolhe com
        quem falar.
      </p>

      <section className="mt-8 rounded-[18px] border border-line bg-card p-6">
        <NewJobRequestForm />
      </section>
    </div>
  );
}
