package com.devlil0.tramposeditor.notification;

/**
 * A versão HTML do e-mail — a estética do produto fora do site (ESBOCO §4, ponto 7).
 * <p>
 * O javadoc do {@link SmtpEmailSender} dizia que aqui era texto puro pra não cair na
 * "estética de newsletter" do §6.1. A recusa continua certa; a conclusão não era. O que o
 * §6.1 recusa é o SaaS pastel e a ilustração genérica — não é a marca aparecer. Um e-mail
 * que chega sem cara nenhuma não é sóbrio, é mudo, e o §4 lista essa voz como 🔴.
 *
 * <h2>Por que isto é HTML de 2005</h2>
 * Tabela aninhada, estilo inline, largura fixa. Não é desleixo: cliente de e-mail não é
 * browser. Gmail remove {@code <style>} de dentro do corpo em boa parte dos casos, Outlook
 * renderiza com o motor do Word (sem flexbox, sem grid, sem {@code border-radius} em
 * alguns), e nenhum deles carrega CSS externo. Cada regra aqui está inline porque é a
 * única que sobrevive aos três.
 *
 * <h2>O logo</h2>
 * Vai como anexo embutido ({@code cid:}), não como URL. Duas razões: o Gmail bloqueia
 * imagem remota por padrão até a pessoa autorizar, e — mais decisivo — <b>ainda não existe
 * servidor público</b> de onde servir o arquivo. A claquete viaja junto.
 * <p>
 * E vem acompanhada do nome escrito. Se a imagem não carregar (cliente sem imagens,
 * modo texto, cota de dados), o cabeçalho continua dizendo de quem é o e-mail.
 */
final class EmailHtml {

    /** O mesmo {@code Content-ID} que o {@link SmtpEmailSender} registra no anexo. */
    static final String LOGO_CID = "logo-tramposeditor";

    // A paleta do produto, igual ao :root do globals.css. Dark é a origem (FUNDACAO §11.6),
    // então o e-mail é dark também — claro aqui seria a marca de outra pessoa.
    private static final String BG = "#0e0d14";
    private static final String CARD = "#17151f";
    private static final String LINE = "#2b2838";
    private static final String FG = "#eceaf2";
    private static final String MUTED = "#9a96a8";
    private static final String ACCENT = "#8b5cf6";
    private static final String ACCENT_CONTRAST = "#14121c";

    /**
     * Pilha de fontes do sistema. Nada de webfont: {@code @font-face} não carrega na maior
     * parte dos clientes e o fallback silencioso sai pior que escolher o fallback.
     */
    private static final String SANS =
            "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";
    private static final String MONO =
            "ui-monospace,SFMono-Regular,Menlo,Consolas,'Liberation Mono',monospace";

    private EmailHtml(){
    }

    static String render(EmailContent content){
        StringBuilder corpo = new StringBuilder();
        for (String paragrafo : content.paragraphs()) {
            corpo.append("""
                    <p style="margin:0 0 16px;font-family:%s;font-size:15px;line-height:24px;color:%s;">%s</p>
                    """.formatted(SANS, FG, escape(paragrafo)));
        }

        String rodapeDoBotao = content.footnote() == null || content.footnote().isBlank()
                ? ""
                : """
                  <p style="margin:16px 0 0;font-family:%s;font-size:13px;line-height:20px;color:%s;">%s</p>
                  """.formatted(SANS, MUTED, escape(content.footnote()));

        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:%s;">
                <!-- Pré-cabeçalho: o trecho que a caixa de entrada mostra ao lado do assunto.
                     Escondido no corpo pra não repetir o assunto duas vezes na tela. -->
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;">%s</div>

                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:%s;">
                  <tr>
                    <td align="center" style="padding:32px 16px;">

                      <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%%;max-width:600px;">

                        <!-- Cabeçalho: a claquete e o nome. -->
                        <tr>
                          <td align="left" style="padding:0 0 24px;">
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                              <tr>
                                <td style="padding-right:12px;" valign="middle">
                                  <img src="cid:%s" width="36" height="36" alt=""
                                       style="display:block;border:0;border-radius:9px;">
                                </td>
                                <td valign="middle">
                                  <span style="font-family:%s;font-size:17px;font-weight:800;letter-spacing:-0.4px;color:%s;">Trampos<span style="color:%s;">Editor</span></span>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <!-- O cartão. -->
                        <tr>
                          <td style="background:%s;border:1px solid %s;border-radius:18px;padding:32px;">
                            <p style="margin:0 0 20px;font-family:%s;font-size:20px;font-weight:800;letter-spacing:-0.4px;line-height:28px;color:%s;">%s,</p>
                            %s
                            <!-- Botão em tabela, não <a> estilizado: no Outlook o padding de
                                 um link não vira área clicável. -->
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:8px 0 0;">
                              <tr>
                                <td align="center" bgcolor="%s" style="border-radius:12px;">
                                  <a href="%s"
                                     style="display:inline-block;padding:12px 24px;font-family:%s;font-size:14px;font-weight:600;line-height:20px;color:%s;text-decoration:none;border-radius:12px;">%s</a>
                                </td>
                              </tr>
                            </table>
                            %s
                          </td>
                        </tr>

                        <!-- Rodapé. O link cru existe porque botão que não abre precisa de
                             um plano B que a pessoa possa copiar. -->
                        <tr>
                          <td style="padding:24px 4px 0;">
                            <p style="margin:0 0 10px;font-family:%s;font-size:12px;line-height:18px;color:%s;">
                              Se o botão não abrir, cole no navegador:<br>
                              <span style="font-family:%s;color:%s;word-break:break-all;">%s</span>
                            </p>
                            <p style="margin:0;font-family:%s;font-size:12px;line-height:18px;color:%s;">
                              Você recebeu isto porque tem conta no TramposEditor.
                            </p>
                          </td>
                        </tr>

                      </table>

                    </td>
                  </tr>
                </table>
                </body>
                </html>
                """.formatted(
                escape(content.subject()), BG,
                escape(primeiroParagrafo(content)),
                BG,
                LOGO_CID,
                SANS, FG, ACCENT,
                CARD, LINE,
                SANS, FG, escape(content.greeting()),
                corpo,
                ACCENT,
                escape(content.ctaUrl()), SANS, ACCENT_CONTRAST, escape(content.ctaLabel()),
                rodapeDoBotao,
                SANS, MUTED,
                MONO, MUTED, escape(content.ctaUrl()),
                SANS, MUTED);
    }

    private static String primeiroParagrafo(EmailContent content){
        return content.paragraphs().isEmpty() ? "" : content.paragraphs().getFirst();
    }

    /**
     * Escapa o que veio de usuário — nome de quem mandou a mensagem, título do trampo.
     * <p>
     * Sem isto, alguém chamado {@code <script>} ou com aspas no título do trampo quebra o
     * markup do e-mail de <b>outra</b> pessoa. O destino é uma caixa de entrada, não um
     * browser, mas cliente de e-mail renderiza HTML — e o estrago aqui é o mesmo.
     */
    private static String escape(String valor){
        if (valor == null) return "";

        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
