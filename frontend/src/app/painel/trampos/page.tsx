import { permanentRedirect } from "next/navigation";

/**
 * O feed da demanda mudou de endereço em 2026-08-06.
 *
 * Não foi renomeação cosmética: os dois feeds viraram **um** feed com uma chave de
 * seleção (`FeedSwitch`), e `/painel/trampos` era um nome que só descrevia metade do que
 * a tela passou a ser. A rota é `/painel/feed`, e a classe é o que muda.
 *
 * Fica como 308 por causa de link salvo, de e-mail já enviado e do rodapé público, que
 * apontava pra cá desde o começo.
 */
export default function TramposPage() {
  permanentRedirect("/painel/feed");
}
