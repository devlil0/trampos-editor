import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { AuthShell } from "@/components/auth-shell";
import { UsernameForm } from "@/components/username-form";
import { getCurrentUser } from "@/lib/session";

export const metadata: Metadata = {
  title: "Escolha seu nome de usuário — TramposEditor",
  robots: { index: false },
};

/**
 * A escolha do username de quem entrou pelo provedor social.
 *
 * Existe porque o login social não tem formulário: a conta nasce no callback do Google ou
 * do GitHub, e lá não há onde perguntar. Até 2026-08-05 essa conta simplesmente recebia um
 * nome derivado e seguia a vida — o endereço público da pessoa era decidido por um
 * `slugify`. Esta tela é onde a dívida é paga.
 *
 * É obrigatória: o guard mora em `/painel/layout.tsx` e devolve pra cá quem ainda não
 * escolheu. Não tem "pular" de propósito — um "depois" que ninguém cumpre reproduz
 * exatamente o problema que esta tela conserta.
 *
 * O que ela NÃO faz: pedir de novo a quem já escolheu — chegar aqui com a escolha feita
 * cai no feed, como qualquer outra entrada.
 *
 * O destino mudou em 2026-08-06 junto com o de `/painel`: era as configurações. Esta é a
 * última tela do cadastro social, então ela é o "primeiro segundo" de quem entrou pelo
 * Google — e esse segundo pertence ao feed, não a um formulário de conta. Trocar só o
 * `/painel` deixaria quem entra por senha no feed e quem entra pelo Google nas
 * configurações, que é a incoerência que o §4.1 chama de produto montado por comitê.
 */
export default async function EscolherUsernamePage() {
  const user = await getCurrentUser();
  if (!user) redirect("/entrar");
  if (user.usernameChosen) redirect("/painel/feed");

  return (
    <AuthShell
      title="Escolha seu nome de usuário"
      subtitle="É o seu endereço aqui dentro, e o que o cliente vê antes de qualquer outra coisa."
    >
      <div className="flex flex-col gap-5">
        {/* O nome gerado aparece no campo como ponto de partida, não como fato consumado:
            a pessoa pode ficar com ele, mas tem que passar por esta decisão. */}
        <UsernameForm
          username={user.username}
          redirectTo="/painel/feed"
          ctaLabel="É esse mesmo"
        />

        <p className="text-xs text-muted">
          Sugerimos <span className="font-mono text-fg">/{user.username}</span> a partir do
          seu nome. Fique com ele ou troque agora — depois disso vale um limite de uma troca
          a cada 14 dias.
        </p>
      </div>
    </AuthShell>
  );
}
