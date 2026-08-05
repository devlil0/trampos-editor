import Link from "next/link";
import { StartConversationForm } from "@/components/conversation-forms";
import { InstagramLink } from "@/components/instagram-link";
import { NicheTag, StyleTag, ToolTag } from "@/components/niche-tag";
import { VideoThumb } from "@/components/video-thumb";
import { formatPrice, formatTurnaround } from "@/lib/format";
import type { EditorProfile } from "@/lib/types";

/**
 * O perfil do editor, na forma de perfil de rede social.
 *
 * Uma view só, usada em dois lugares: a página pública (`/editores/{slug}`) e o "meu
 * perfil" do painel. É o que faz valer a regra de que o editor vê exatamente o que o
 * cliente vê — antes o painel mostrava um formulário, então o dono do perfil nunca via o
 * próprio perfil, só o formulário dele.
 *
 * O desenho segue perfil de Instagram porque a pergunta que a página responde é a mesma:
 * "quem é essa pessoa e o que ela faz?" — foto redonda grande, números em linha, texto
 * curto, ação primária logo abaixo e a obra numa grade. A diferença de um marketplace é
 * que preço e prazo entram na linha de números: aqui eles são parte de quem a pessoa é.
 */
export function EditorProfileView({
  profile,
  viewerLoggedIn,
  isOwnProfile,
  avatarUrl,
  headerExtra,
}: {
  profile: EditorProfile;
  viewerLoggedIn: boolean;
  isOwnProfile: boolean;
  /** URL pronta da foto; null quando não há. Montada no servidor. */
  avatarUrl?: string | null;
  /** Slot pro botão de configurações — só o painel usa. */
  headerExtra?: React.ReactNode;
}) {
  const priceMin = formatPrice(profile.priceMinCents);
  const priceMax = formatPrice(profile.priceMaxCents);
  const priceRange =
    priceMin && priceMax && priceMin !== priceMax ? `${priceMin} – ${priceMax}` : (priceMin ?? priceMax);
  const turnaround = formatTurnaround(profile.turnaroundDays);

  return (
    <>
      <header className="flex flex-col items-center gap-7 sm:flex-row sm:items-start sm:gap-12">
        {/* Foto redonda e grande, como em rede social. Sem foto: a inicial no gradiente
            da marca, que é o que o card da vitrine já usa. */}
        {avatarUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={avatarUrl}
            alt={`Foto de ${profile.displayName}`}
            width={152}
            height={152}
            className="h-32 w-32 flex-none rounded-full object-cover sm:h-38 sm:w-38"
          />
        ) : (
          <span className="grid h-32 w-32 flex-none place-items-center rounded-full bg-gradient-to-br from-accent to-accent-strong text-5xl font-extrabold tracking-tight text-white sm:h-38 sm:w-38">
            {profile.displayName.charAt(0).toUpperCase()}
          </span>
        )}

        <div className="min-w-0 flex-1 text-center sm:text-left">
          <div className="flex flex-wrap items-center justify-center gap-x-3 gap-y-2 sm:justify-start">
            <h1 className="text-2xl font-extrabold tracking-tight">{profile.displayName}</h1>
            <span
              className={`inline-flex items-center gap-1.5 text-sm font-semibold ${
                profile.available ? "text-good" : "text-busy"
              }`}
            >
              <span className={`h-1.5 w-1.5 rounded-full ${profile.available ? "bg-good" : "bg-busy"}`} />
              {profile.available ? "Disponível" : "Sem vaga"}
            </span>
            {headerExtra}
          </div>

          <p className="mt-0.5 font-mono text-sm text-muted">@{profile.slug}</p>

          {/* A linha de números do Instagram, traduzida pro que importa aqui. */}
          <dl className="mt-4 flex flex-wrap justify-center gap-x-7 gap-y-2 text-sm sm:justify-start">
            <Stat label={profile.portfolio.length === 1 ? "vídeo" : "vídeos"} value={profile.portfolio.length} />
            {/* Perfil sem nota não mostra nota — "★ 0.0" faz a vitrine parecer morta
                (ESBOCO §7 item 5). */}
            {profile.ratingCount > 0 && (
              <Stat
                label={profile.ratingCount === 1 ? "avaliação" : "avaliações"}
                value={
                  <>
                    <span className="text-amber-400">★</span> {profile.ratingAvg.toFixed(1)}
                    <span className="ml-1.5 font-normal text-muted">({profile.ratingCount})</span>
                  </>
                }
              />
            )}
            {priceRange && <Stat label="por vídeo" value={priceMin!} />}
            {turnaround && <Stat label="prazo médio" value={turnaround} />}
          </dl>

          {profile.headline && <p className="mt-4 font-semibold">{profile.headline}</p>}
          {profile.bio && (
            <p className="mt-1 max-w-[62ch] whitespace-pre-line text-[15px] leading-relaxed">
              {profile.bio}
            </p>
          )}

          <div className="mt-3.5 flex flex-wrap justify-center gap-1.5 sm:justify-start">
            {profile.niches.map((n) => (
              <NicheTag key={n} niche={n} />
            ))}
            {profile.styles.map((s) => (
              <StyleTag key={s} style={s} />
            ))}
            {profile.tools.map((t) => (
              <ToolTag key={t} tool={t} />
            ))}
          </div>

          {profile.instagram && (
            <div className="mt-3.5 flex justify-center sm:justify-start">
              <InstagramLink username={profile.instagram} />
            </div>
          )}

          {/* Ação primária logo abaixo do texto, como o "Enviar mensagem" do Instagram. */}
          <div className="mt-5 max-w-md">
            {isOwnProfile ? (
              <p className="rounded-xl border border-dashed border-line px-4 py-2.5 text-center text-sm text-muted">
                É assim que o cliente vê seu perfil.
              </p>
            ) : (
              <>
                <StartConversationForm slug={profile.slug} loggedIn={viewerLoggedIn} />
                <p className="mt-2 text-center text-xs text-muted sm:text-left">
                  Sem taxa pra falar. Vocês combinam direto.
                </p>
              </>
            )}
          </div>
        </div>
      </header>

      {/* Grade da obra, com a divisória e o rótulo que o Instagram usa como aba. */}
      <section className="mt-12 border-t border-line">
        <h2 className="-mt-px inline-flex items-center gap-2 border-t-2 border-fg py-3.5 font-mono text-[11px] font-bold tracking-[0.14em] text-fg">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <rect x="3" y="3" width="7" height="7" rx="1" />
            <rect x="14" y="3" width="7" height="7" rx="1" />
            <rect x="3" y="14" width="7" height="7" rx="1" />
            <rect x="14" y="14" width="7" height="7" rx="1" />
          </svg>
          PORTFÓLIO
        </h2>

        {profile.portfolio.length === 0 ? (
          <div className="py-16 text-center">
            <p className="text-lg font-semibold">
              {isOwnProfile ? "Seu portfólio está vazio." : "Este editor ainda não adicionou vídeos."}
            </p>
            {isOwnProfile && (
              <p className="mt-1 text-muted">
                É o que o cliente olha primeiro — o primeiro vídeo vira a capa do seu card na vitrine.
              </p>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-1 pt-1 sm:grid-cols-3 sm:gap-1.5">
            {profile.portfolio.map((item) => (
              <a
                key={item.id}
                href={item.videoUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="group relative overflow-hidden"
              >
                <VideoThumb url={item.videoUrl} alt={item.title} seed={item.id} />
                {/* Título só no hover, como a sobreposição de métricas do Instagram: a
                    grade tem que ler como obra, não como lista com legenda. */}
                <span className="absolute inset-0 flex items-end bg-gradient-to-t from-black/80 to-transparent p-2.5 text-xs font-semibold text-white opacity-0 transition group-hover:opacity-100">
                  {item.title}
                </span>
              </a>
            ))}
          </div>
        )}
      </section>
    </>
  );
}

function Stat({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-baseline gap-1.5">
      <dt className="order-2 text-muted">{label}</dt>
      <dd className="order-1 font-bold tabular-nums">{value}</dd>
    </div>
  );
}

/** Botão de engrenagem que leva às configurações — usado só pelo dono, no painel. */
export function BotaoConfiguracoes() {
  return (
    <Link
      href="/painel/meu-perfil/configuracoes"
      aria-label="Configurações do perfil"
      title="Configurações do perfil"
      className="grid h-9 w-9 place-items-center rounded-[10px] border border-line bg-card text-muted transition hover:border-accent hover:text-fg"
    >
      <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="3" />
        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
      </svg>
    </Link>
  );
}
