import type { Metadata } from "next";
import Link from "next/link";
import { AuthShell } from "@/components/auth-shell";

export const metadata: Metadata = {
  title: "Confirmar e-mail — TramposEditor",
  robots: { index: false },
};

const API_URL = process.env.API_URL ?? "http://localhost:8080";

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>;

/**
 * Destino do link de confirmação (ESBOCO §2, fase 1.5).
 *
 * Confirma no próprio carregamento, sem botão: a pessoa já clicou uma vez, no e-mail — pedir
 * um segundo clique pra fazer o que o primeiro prometeu é atrito puro. É o oposto do
 * `/redefinir-senha`, que precisa de formulário porque falta a senha nova.
 *
 * Não exige sessão. O link costuma abrir no navegador do celular, dias depois, e transformar
 * a confirmação num "entre primeiro" perderia justamente quem o produto está tentando
 * confirmar.
 */
export default async function ConfirmarEmailPage({ searchParams }: { searchParams: SearchParams }) {
  const sp = await searchParams;
  const raw = sp.token;
  const token = Array.isArray(raw) ? raw[0] : (raw ?? "");

  const ok = token ? await confirmar(token) : false;

  return (
    <AuthShell
      title={ok ? "E-mail confirmado" : "Esse link não vale mais"}
      subtitle={
        ok
          ? "Pronto. Agora você fica sabendo quando alguém te procura."
          : "Links de confirmação valem por 3 dias e só funcionam uma vez."
      }
    >
      <div className="flex flex-col gap-4">
        {ok ? (
          <>
            <p className="text-sm text-muted">
              Seu perfil de editor já pode ser publicado, e os avisos de mensagem, interesse
              e trampo fechado passam a chegar nesta caixa.
            </p>
            <Link href="/painel/meu-perfil/configuracoes" className="btn btn-primary btn-block">
              Ir pro meu perfil
            </Link>
          </>
        ) : (
          <>
            <p className="text-sm text-muted">
              Entre na sua conta e peça um link novo nas configurações — leva um segundo.
            </p>
            <Link href="/entrar" className="btn btn-primary btn-block">
              Entrar
            </Link>
          </>
        )}
      </div>
    </AuthShell>
  );
}

/**
 * Uma tentativa, sem distinguir os motivos de falha.
 *
 * Token inexistente, expirado e já usado dão a mesma tela de propósito — é a mesma regra do
 * backend, e contar qual foi ajudaria só quem está adivinhando token.
 */
async function confirmar(token: string): Promise<boolean> {
  try {
    const res = await fetch(`${API_URL}/v1/auth/email-verification/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token }),
      cache: "no-store",
    });
    return res.ok;
  } catch {
    return false;
  }
}
