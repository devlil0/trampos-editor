package com.devlil0.tramposeditor.notification;

import java.util.List;

/**
 * Um e-mail do produto, em partes — não em texto pronto.
 * <p>
 * Existe porque o mesmo e-mail sai em dois formatos: texto puro e HTML. Se cada
 * notificação escrevesse os dois, seriam dez templates pra manter em dia em vez de cinco,
 * e eles divergiriam na primeira correção de vírgula. Aqui a notificação declara <b>o que
 * diz</b>, e quem decide como isso vira parágrafo é o renderizador.
 * <p>
 * A forma é sempre a mesma porque os cinco e-mails do produto são a mesma coisa: quem é
 * você, o que aconteceu, o botão que resolve. E-mail que precisa de layout próprio é
 * e-mail que está querendo ser uma página.
 *
 * @param greeting   o primeiro nome, sem vírgula — quem põe a pontuação é o template
 * @param paragraphs corpo, um item por parágrafo
 * @param ctaLabel   texto do botão, no imperativo ("Abrir a conversa")
 * @param ctaUrl     link absoluto pro frontend
 * @param footnote   ressalva curta abaixo do botão; nulo quando não há
 */
public record EmailContent(
        String subject,
        String greeting,
        List<String> paragraphs,
        String ctaLabel,
        String ctaUrl,
        String footnote
) {

    public EmailContent {
        paragraphs = List.copyOf(paragraphs);
    }

    /**
     * A versão em texto puro.
     * <p>
     * Não é plano B: vai junto no mesmo envio, e é o que aparece em cliente que bloqueia
     * HTML, em leitor de tela e na pré-visualização da caixa de entrada. Também é o que o
     * {@link LogEmailSender} imprime no console — conferir texto e link em dev não deve
     * exigir ler markup.
     */
    public String toPlainText(){
        StringBuilder texto = new StringBuilder(greeting).append(",\n\n");

        for (String paragrafo : paragraphs) {
            texto.append(paragrafo).append("\n\n");
        }

        // Rótulo e URL na mesma linha: em texto puro o botão não existe, e "Abrir a
        // conversa: https://..." diz o que o clique faria.
        texto.append(ctaLabel).append(": ").append(ctaUrl).append("\n\n");

        if (footnote != null && !footnote.isBlank()) {
            texto.append(footnote).append("\n\n");
        }

        return texto.append("— TramposEditor").toString();
    }
}
