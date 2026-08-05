import { redirect } from "next/navigation";
import { getCurrentUser } from "@/lib/session";

/**
 * O guard do username escolhido.
 *
 * Conta criada pelo provedor social nasce com um nome derivado e `usernameChosen: false` —
 * não há formulário no callback do Google onde perguntar. Aqui é onde essa dívida é
 * cobrada: enquanto a pessoa não escolher, ela não navega no painel.
 *
 * Fica no layout, e não em cada página, porque um guard que depende de alguém lembrar de
 * copiá-lo pra próxima tela não é guard. E fica aqui, e não num `proxy.ts`, porque a
 * decisão precisa do usuário resolvido — o proxy só teria o cookie, e checar isto a cada
 * requisição de asset seria pagar uma ida ao backend por arquivo.
 *
 * Não trata sessão ausente: cada página já redireciona pro `/entrar` com o próprio
 * contexto, e duplicar aqui só mudaria de onde vem o redirecionamento.
 */
export default async function PainelLayout({ children }: { children: React.ReactNode }) {
  const user = await getCurrentUser();
  if (user && !user.usernameChosen) redirect("/escolher-username");

  return <>{children}</>;
}
