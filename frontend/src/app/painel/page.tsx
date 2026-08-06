import { redirect } from "next/navigation";

/**
 * O hub `/painel` foi dissolvido em 2026-08-04.
 *
 * Ele não fazia nada além de listar links pras telas da conta — e listar links é
 * exatamente o que a tela de configurações faz. Manter os dois era um menu em dois
 * lugares, com o de cima aparecendo na barra e o de baixo na engrenagem.
 *
 * As telas continuam existindo (`/painel/pedidos`, `/painel/feed`,
 * `/painel/trampos-fechados`, `/painel/meu-perfil`) e agora são alcançadas de dentro das
 * configurações. Conversas ficou de fora dessa lista de propósito: mora na barra de baixo,
 * porque é a tela de maior frequência do produto.
 *
 * É `redirect` e não `permanentRedirect`: `/painel` nunca foi público nem indexado, então
 * não há autoridade de busca pra transferir — e um 307 deixa a porta aberta caso um hub
 * volte a fazer sentido depois.
 *
 * **O destino mudou em 2026-08-06: era `/painel/meu-perfil/configuracoes`, virou o feed.**
 * Esta rota é onde todo login desemboca — `loginAction`, `signupAction` e o callback social
 * mandam pra cá —, então ela decide o que a pessoa vê no primeiro segundo depois de entrar.
 * Configurações é tela de manutenção: quem acabou de logar não veio mexer na conta, veio
 * ver o que tem hoje. Abrir no feed é o §3 do ESBOCO cumprido na porta de entrada — feed é
 * a tela que oferece o próximo passo, e era ela que o laço de retorno precisava encontrar.
 */
export default function PainelPage() {
  redirect("/painel/feed");
}
