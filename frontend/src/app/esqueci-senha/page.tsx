import type { Metadata } from "next";
import { RequestResetForm } from "@/components/password-forms";

export const metadata: Metadata = {
  title: "Esqueci a senha — TramposEditor",
  robots: { index: false },
};

export default function EsqueciSenhaPage() {
  return (
    <div className="mx-auto flex max-w-md flex-col gap-6 px-6 py-16">
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight">Esqueci a senha</h1>
        <p className="mt-1.5 text-muted">
          Diga o e-mail da conta e mandamos um link pra você criar outra.
        </p>
      </div>
      <RequestResetForm />
    </div>
  );
}
