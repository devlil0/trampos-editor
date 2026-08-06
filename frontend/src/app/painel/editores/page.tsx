import { permanentRedirect } from "next/navigation";

/**
 * O feed da oferta deixou de existir como superfície (2026-08-04).
 *
 * Ele mostrava a mesma lista da vitrine com um filtro a menos, e a própria página apontava
 * pra `/editores` duas vezes dizendo que lá havia mais filtro — tela que recomenda outra
 * como versão melhor de si mesma não é tela distinta. A única diferença real era exibir só
 * quem está disponível, e isso é valor de filtro, não superfície: `/v1/editors` sempre
 * aceitou `available`.
 *
 * Isto derruba a "simetria dos dois feeds" do ESBOCO §1, e vale dizer por quê: ela era
 * falsa. `/painel/trampos` não tem equivalente público — não existe lista pública de
 * trampos —, então aquele feed continua sendo superfície legítima. Só este duplicava.
 *
 * A rota fica como redirecionamento permanente por causa de link salvo e de e-mail já
 * enviado. `permanentRedirect` devolve 308, que os buscadores tratam como mudança
 * definitiva de endereço.
 *
 * Atualizado em 2026-08-06: o destino perdeu o `?available=true`. Com a chave do feed
 * (`FeedSwitch`), "ver editores" virou um gesto de navegação em vez de um atalho pra um
 * recorte — e chegar já filtrado por disponibilidade escondia parte da vitrine de quem
 * clicou só pra ver quem existe. O toggle "Livres agora" continua na sidebar, a um clique.
 */
export default function FeedEditoresPage() {
  permanentRedirect("/editores");
}
