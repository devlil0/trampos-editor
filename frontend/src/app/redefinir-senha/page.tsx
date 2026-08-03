import type { Metadata } from "next";
import { ConfirmResetForm } from "@/components/password-forms";

export const metadata: Metadata = {
  title: "Redefinir senha — TramposEditor",
  robots: { index: false },
};

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>;

/** Destino do link do e-mail. O token chega na query string e nunca sai daqui. */
export default async function RedefinirSenhaPage({ searchParams }: { searchParams: SearchParams }) {
  const sp = await searchParams;
  const raw = sp.token;
  const token = Array.isArray(raw) ? raw[0] : (raw ?? "");

  return (
    <div className="mx-auto flex max-w-md flex-col gap-6 px-6 py-16">
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight">Nova senha</h1>
        <p className="mt-1.5 text-muted">Escolha uma senha e entre de novo.</p>
      </div>
      <ConfirmResetForm token={token} />
    </div>
  );
}
