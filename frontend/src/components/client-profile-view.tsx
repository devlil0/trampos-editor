import Link from "next/link";
import { InstagramLink } from "@/components/instagram-link";
import type { PublicProfile } from "@/lib/types";

/**
 * A página de quem só contrata.
 *
 * Enxuta de propósito, e por dois motivos. O primeiro é que não há o que inflar: cliente
 * não tem obra, tem histórico. O segundo é que uma página magra que finge ser cheia é
 * pior que uma curta e honesta — a mesma razão pela qual o card do editor esconde a nota
 * enquanto ninguém avaliou (ESBOCO §7 item 5).
 *
 * O que ela mostra é o que dá lastro pro editor decidir se responde: quantos trampos essa
 * pessoa levou até o fim e quantas avaliações ela deixou. Cliente que fecha e avalia é
 * cliente que alimenta o laço de reputação (§3.1) — e isso merece ser visível.
 */
export function ClientProfileView({
  perfil,
  isOwnProfile,
  avatarUrl,
}: {
  perfil: PublicProfile;
  isOwnProfile: boolean;
  /** URL pronta da foto; null quando não há. Montada no servidor. */
  avatarUrl?: string | null;
}) {
  const desde = new Date(perfil.memberSince).toLocaleDateString("pt-BR", {
    month: "long",
    year: "numeric",
  });

  return (
    <>
      <header className="flex flex-col items-center gap-7 sm:flex-row sm:items-start sm:gap-12">
        {avatarUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={avatarUrl}
            alt={`Foto de ${perfil.displayName}`}
            width={152}
            height={152}
            className="h-32 w-32 flex-none rounded-full object-cover sm:h-38 sm:w-38"
          />
        ) : (
          <span className="grid h-32 w-32 flex-none place-items-center rounded-full bg-gradient-to-br from-accent to-accent-strong text-5xl font-extrabold tracking-tight text-white sm:h-38 sm:w-38">
            {perfil.displayName.charAt(0).toUpperCase()}
          </span>
        )}

        <div className="min-w-0 flex-1 text-center sm:text-left">
          <h1 className="text-2xl font-extrabold tracking-tight">{perfil.displayName}</h1>
          <p className="mt-0.5 font-mono text-sm text-muted">@{perfil.username}</p>

          <dl className="mt-4 flex flex-wrap justify-center gap-x-7 gap-y-2 text-sm sm:justify-start">
            <div className="flex items-baseline gap-1.5">
              <dd className="font-bold tabular-nums">{perfil.dealsClosed}</dd>
              <dt className="text-muted">
                {perfil.dealsClosed === 1 ? "trampo fechado" : "trampos fechados"}
              </dt>
            </div>
            <div className="flex items-baseline gap-1.5">
              <dd className="font-bold tabular-nums">{perfil.reviewsWritten}</dd>
              <dt className="text-muted">
                {perfil.reviewsWritten === 1 ? "avaliação deixada" : "avaliações deixadas"}
              </dt>
            </div>
            {/* Cliente também é avaliado pelo editor — quando existe nota, ela vale tanto
                quanto a do outro lado. */}
            {perfil.ratingCount > 0 && (
              <div className="flex items-baseline gap-1.5">
                <dd className="font-bold tabular-nums">
                  <span className="text-amber-400">★</span> {perfil.ratingAvg.toFixed(1)}
                </dd>
                <dt className="text-muted">
                  {perfil.ratingCount === 1 ? "avaliação" : "avaliações"}
                </dt>
              </div>
            )}
          </dl>

          <p className="mt-4 text-sm text-muted">Na TramposEditor desde {desde}.</p>

          {perfil.instagram && (
            <div className="mt-3 flex justify-center sm:justify-start">
              <InstagramLink username={perfil.instagram} />
            </div>
          )}

          {isOwnProfile && (
            <div className="mt-6 max-w-md rounded-[18px] border border-dashed border-line p-5 text-center sm:text-left">
              <p className="font-semibold">Você edita vídeo também?</p>
              <p className="mt-1 text-sm text-muted">
                Publicar um perfil de editor não cria outra conta — é esta mesma, com uma
                vitrine. Aí esta página passa a mostrar seu trabalho.
              </p>
              <Link href="/painel/meu-perfil/configuracoes" className="btn btn-primary btn-sm mt-4">
                Publicar meu perfil
              </Link>
            </div>
          )}
        </div>
      </header>

      {perfil.dealsClosed === 0 && !isOwnProfile && (
        <p className="mt-12 border-t border-line pt-8 text-center text-muted">
          Esta pessoa ainda não fechou nenhum trampo por aqui.
        </p>
      )}
    </>
  );
}
