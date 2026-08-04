import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { fetchMyConversations, fetchMyEditorProfile } from "@/lib/api";
import { getCurrentUser, getSessionToken } from "@/lib/session";
import { InstagramForm } from "@/components/instagram-form";
import { NicheTag } from "@/components/niche-tag";

export const metadata: Metadata = {
  title: "Painel — TramposEditor",
  robots: { index: false },
};

/**
 * Área logada — a landing (`/`) segue pública e indexável; aqui é o lugar do
 * usuário autenticado. Primeira versão: mostra o estado da conta e o próximo
 * passo de cada papel. Chat e pedidos entram nos Blocos 5–7.
 */
export default async function PainelPage() {
  const user = await getCurrentUser();
  if (!user) redirect("/entrar");

  const token = await getSessionToken();
  // A conta é uma só: todo mundo é cliente, e é ter perfil publicado que faz de
  // alguém editor. Por isso o perfil é buscado sempre, não só pra quem tem o papel.
  const [profile, conversas] = token
    ? await Promise.all([fetchMyEditorProfile(token), fetchMyConversations(token)])
    : [null, []];

  const naoLidas = conversas.filter((c) => c.unread).length;

  const firstName = user.displayName.split(" ")[0];

  return (
    <div className="mx-auto max-w-6xl px-6 py-12">
      <h1 className="text-3xl font-extrabold tracking-tight">Olá, {firstName}</h1>
      <p className="mt-1 text-muted">
        {profile
          ? "Você contrata e também pega trampo por aqui."
          : "Encontre um editor — ou publique seu perfil e comece a pegar trampo."}
      </p>

      <div className="mt-8 grid gap-5 md:grid-cols-2">
          <section className="rounded-[18px] border border-line bg-card p-6">
            <h2 className="text-lg font-bold tracking-tight">
              {profile ? "Seu perfil público" : "Você também edita?"}
            </h2>

            {profile ? (
              <>
                <p className="mt-1 text-sm text-muted">
                  {profile.available
                    ? "Você está aparecendo como disponível na vitrine."
                    : "Você está marcado como ocupado — não aparece no topo da vitrine."}
                </p>

                {profile.niches.length > 0 && (
                  <div className="mt-3.5 flex flex-wrap gap-1.5">
                    {profile.niches.map((n) => (
                      <NicheTag key={n} niche={n} />
                    ))}
                  </div>
                )}

                <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
                  <Stat label="Vídeos no portfólio" value={String(profile.portfolio.length)} />
                  <Stat
                    label="Avaliações"
                    value={profile.ratingCount > 0 ? profile.ratingCount.toString() : "—"}
                  />
                </dl>

                <div className="mt-5 flex flex-wrap gap-2.5">
                  <Link
                    href="/painel/meu-perfil"
                    className="btn btn-primary btn-sm"
                  >
                    Editar perfil
                  </Link>
                  <Link
                    href={`/editores/${profile.slug}`}
                    className="btn btn-ghost btn-sm"
                  >
                    Ver na vitrine
                  </Link>
                </div>
              </>
            ) : (
              <>
                <p className="mt-1 text-sm text-muted">
                  Publique seu perfil pra aparecer na vitrine e receber contato de quem procura
                  editor. Sua conta continua a mesma — não tem cadastro separado.
                </p>
                <Link
                  href="/painel/meu-perfil"
                  className="mt-5 btn btn-primary btn-sm"
                >
                  Montar meu perfil
                </Link>
              </>
            )}
          </section>

          <section className="rounded-[18px] border border-line bg-card p-6">
            <h2 className="text-lg font-bold tracking-tight">Preciso de um editor</h2>
            <p className="mt-1 text-sm text-muted">
              Poste o que você precisa e deixe os editores virem até você — ou veja quem está livre
              agora e chame direto.
            </p>
            <div className="mt-5 flex flex-wrap gap-2.5">
              <Link
                href="/painel/pedidos/novo"
                className="btn btn-primary btn-sm"
              >
                Postar pedido
              </Link>
              <Link
                href="/painel/pedidos"
                className="btn btn-ghost btn-sm"
              >
                Meus pedidos
              </Link>
              {/* Aponta direto pra vitrine filtrada: "disponíveis" é recorte dela, não
                  outra tela (o feed da oferta saiu em 2026-08-04). */}
              <Link
                href="/editores?available=true"
                className="btn btn-ghost btn-sm"
              >
                Editores disponíveis
              </Link>
            </div>
          </section>

          {profile && (
            <section className="rounded-[18px] border border-line bg-card p-6">
              <h2 className="text-lg font-bold tracking-tight">Trampos abertos</h2>
              <p className="mt-1 text-sm text-muted">
                Pedidos de quem está procurando editor agora. Diga que topa e o cliente libera o
                contato.
              </p>
              <Link
                href="/painel/trampos"
                className="mt-5 btn btn-ghost btn-sm"
              >
                Ver pedidos abertos
              </Link>
            </section>
          )}

        {/* Primeira resposta do painel pra "o que mudou desde que eu saí?". Sem isso
            a pessoa só descobre uma mensagem nova se lembrar de abrir a caixa. */}
        <section className="rounded-[18px] border border-line bg-card p-6">
          <div className="flex flex-wrap items-center gap-2.5">
            <h2 className="text-lg font-bold tracking-tight">Conversas</h2>
            {naoLidas > 0 && (
              <span className="rounded-full bg-accent px-2.5 py-0.5 text-xs font-bold text-accent-contrast tabular-nums">
                {naoLidas} nova{naoLidas > 1 ? "s" : ""}
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-muted">
            {conversas.length === 0
              ? "Nenhuma conversa ainda. Elas começam quando você chama alguém na vitrine ou aceita um interesse."
              : naoLidas > 0
                ? "Tem gente esperando sua resposta."
                : "Tudo lido por aqui."}
          </p>
          <Link href="/painel/conversas" className="mt-5 btn btn-ghost btn-sm">
            Abrir conversas
          </Link>
        </section>

        {/* O fim do ciclo (ESBOCO §3.2): é daqui que sai a avaliação e o "chamar de novo". */}
        <section className="rounded-[18px] border border-line bg-card p-6">
          <h2 className="text-lg font-bold tracking-tight">Trampos</h2>
          <p className="mt-1 text-sm text-muted">
            O que está em andamento e o que já foi entregue. É a avaliação daqui que mexe na
            ordenação da vitrine.
          </p>
          <Link href="/painel/trampos-fechados" className="mt-5 btn btn-ghost btn-sm">
            Ver meus trampos
          </Link>
        </section>

        <section className="rounded-[18px] border border-line bg-card p-6">
          <h2 className="text-lg font-bold tracking-tight">Sua conta</h2>
          <dl className="mt-3.5 flex flex-col gap-2.5 text-sm">
            <Row label="Nome">{user.displayName}</Row>
            <Row label="E-mail">{user.email}</Row>
            <Row label="Conta">{profile ? "Cliente e editor" : "Cliente"}</Row>
          </dl>

          <div className="mt-5 border-t border-line pt-5">
            <InstagramForm instagram={user.instagram} />
          </div>
        </section>
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-line bg-panel px-3.5 py-2.5">
      <dt className="text-xs text-muted">{label}</dt>
      <dd className="text-lg font-bold tabular-nums">{value}</dd>
    </div>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-3 border-t border-line pt-2.5">
      <dt className="text-muted">{label}</dt>
      <dd className="font-semibold">{children}</dd>
    </div>
  );
}
