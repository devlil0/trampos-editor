import { redirect } from "next/navigation";

/**
 * O hub `/painel` foi dissolvido em 2026-08-04.
 *
 * Ele não fazia nada além de listar links pras telas da conta — e listar links é
 * exatamente o que a tela de configurações faz. Manter os dois era um menu em dois
 * lugares, com o de cima aparecendo na barra e o de baixo na engrenagem.
 *
 * As telas continuam existindo (`/painel/pedidos`, `/painel/trampos`,
 * `/painel/trampos-fechados`, `/painel/meu-perfil`) e agora são alcançadas de dentro das
 * configurações. Conversas ficou de fora dessa lista de propósito: mora na barra de baixo,
 * porque é a tela de maior frequência do produto.
 *
 * É `redirect` e não `permanentRedirect`: `/painel` nunca foi público nem indexado, então
 * não há autoridade de busca pra transferir — e um 307 deixa a porta aberta caso um hub
 * volte a fazer sentido depois.
 */
export default function PainelPage() {
  redirect("/painel/meu-perfil/configuracoes");
}
