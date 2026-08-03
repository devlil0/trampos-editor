import { fetchSocialProviders, PUBLIC_API_URL } from "@/lib/api";

/**
 * Botões de login por provedor.
 *
 * Não renderiza nada quando o ambiente não tem credencial configurada: botão de "Entrar
 * com Google" que leva a uma tela de erro é pior que botão nenhum. Quem decide é o
 * backend, em `/v1/auth/social/providers`.
 *
 * O `href` é uma âncora, não `<Link>`: o destino é o backend (`/oauth2/authorization/...`)
 * e a navegação precisa sair do Next inteira, não virar transição de rota do lado cliente.
 */
export async function SocialLogin() {
  const providers = await fetchSocialProviders();
  if (providers.length === 0) return null;

  const apiUrl = PUBLIC_API_URL;

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-3 text-xs uppercase tracking-[0.09em] text-muted">
        <span className="h-px flex-1 bg-line" />
        ou
        <span className="h-px flex-1 bg-line" />
      </div>

      <div className="flex flex-col gap-2.5">
        {providers.includes("google") && (
          <ProviderButton href={`${apiUrl}/oauth2/authorization/google`} label="Continuar com Google">
            <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
              <path fill="#4285F4" d="M23 12.3c0-.8-.1-1.6-.2-2.3H12v4.5h6.2a5.3 5.3 0 0 1-2.3 3.5v2.9h3.7c2.2-2 3.4-5 3.4-8.6z" />
              <path fill="#34A853" d="M12 23.5c3.1 0 5.7-1 7.6-2.8l-3.7-2.9c-1 .7-2.3 1.1-3.9 1.1-3 0-5.5-2-6.4-4.7H1.8v3a11.5 11.5 0 0 0 10.2 6.3z" />
              <path fill="#FBBC05" d="M5.6 14.2a6.9 6.9 0 0 1 0-4.4v-3H1.8a11.5 11.5 0 0 0 0 10.4l3.8-3z" />
              <path fill="#EA4335" d="M12 5.4c1.7 0 3.2.6 4.4 1.7l3.3-3.3A11.5 11.5 0 0 0 1.8 6.8l3.8 3c.9-2.7 3.4-4.4 6.4-4.4z" />
            </svg>
          </ProviderButton>
        )}

        {providers.includes("github") && (
          <ProviderButton href={`${apiUrl}/oauth2/authorization/github`} label="Continuar com GitHub">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path d="M12 .5A11.5 11.5 0 0 0 .5 12a11.5 11.5 0 0 0 7.9 10.9c.6.1.8-.2.8-.6v-2c-3.2.7-3.9-1.5-3.9-1.5-.5-1.4-1.3-1.7-1.3-1.7-1-.7.1-.7.1-.7 1.1.1 1.7 1.2 1.7 1.2 1 1.7 2.7 1.2 3.4.9.1-.7.4-1.2.7-1.5-2.6-.3-5.3-1.3-5.3-5.8 0-1.3.5-2.3 1.2-3.1-.1-.3-.5-1.5.1-3.1 0 0 1-.3 3.2 1.2a11 11 0 0 1 5.8 0c2.2-1.5 3.2-1.2 3.2-1.2.6 1.6.2 2.8.1 3.1.8.8 1.2 1.8 1.2 3.1 0 4.5-2.7 5.5-5.3 5.8.4.4.8 1.1.8 2.2v3.3c0 .4.2.7.8.6A11.5 11.5 0 0 0 23.5 12 11.5 11.5 0 0 0 12 .5z" />
            </svg>
          </ProviderButton>
        )}
      </div>
    </div>
  );
}

function ProviderButton({
  href,
  label,
  children,
}: {
  href: string;
  label: string;
  children: React.ReactNode;
}) {
  return (
    <a
      href={href}
      className="inline-flex w-full items-center justify-center gap-2.5 rounded-xl border border-line bg-card px-4 py-2.5 text-base font-semibold transition hover:border-accent"
    >
      {children}
      {label}
    </a>
  );
}
