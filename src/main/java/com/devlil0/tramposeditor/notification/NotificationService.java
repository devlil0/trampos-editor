package com.devlil0.tramposeditor.notification;

import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.notification.NotificationDelivery.Kind;
import com.devlil0.tramposeditor.notification.NotificationDelivery.PreparedEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

/**
 * A voz do produto fora do site (ESBOCO §4, ponto 7) — e o <b>canal</b> do §7 item 3.
 * <p>
 * Sem isto o que foi construído gira só enquanto a pessoa está com a aba aberta: os dois
 * lados agem em tempos diferentes e ninguém descobre que foi procurado.
 * <p>
 * Três regras que valem pro arquivo inteiro:
 * <ol>
 *   <li><b>Avisos preservam a ação.</b> Envios obrigatórios de identidade propagam falha; perder um aviso é ruim,
 *       perder a mensagem que o usuário acabou de escrever é pior.</li>
 *   <li><b>Um aviso por vez, não um por evento.</b> Ver {@link #newMessage} — dez
 *       mensagens seguidas mandam <i>um</i> e-mail, não dez. Marketplace que enche a
 *       caixa de entrada é exatamente o que os 99% fazem (§6).</li>
 *   <li><b>Aviso automático só vai pra endereço provado.</b> {@link #quietly} recusa quem
 *       não confirmou; o envio obrigatório é a exceção, e vale para os e-mails de identidade que a
 *       própria pessoa pediu.</li>
 * </ol>
 * O texto segue o léxico do §4.1: trampo, vitrine, conversa. Nada de "usuário",
 * "plataforma" ou "não responda a este e-mail".
 * <p>
 * Cada e-mail é declarado como {@link EmailContent} — partes, não texto pronto. Quem
 * transforma isso em texto puro ou em HTML com a cara do produto é o adaptador de saída,
 * e é por isso que o texto de cada aviso mora aqui uma vez só, e não duas.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationDelivery delivery;
    private final NotificationProperties properties;

    /**
     * Alguém escreveu numa conversa.
     *
     * @param jaAvisado o destinatário já tinha mensagem não lida <b>antes</b> desta. Se
     *                  tinha, não manda: ele já foi avisado e ainda não voltou, e insistir
     *                  não acrescenta informação nenhuma.
     */
    public void newMessage(UserEntity recipient, UserEntity sender, boolean jaAvisado){
        if (jaAvisado) return;

        quietly(Kind.MESSAGE, recipient, () -> new EmailContent(
                sender.getDisplayName() + " te mandou uma mensagem",
                firstName(recipient),
                List.of(sender.getDisplayName() + " falou com você no TramposEditor."),
                "Abrir a conversa",
                properties.link("/painel/conversas"),
                null));
    }

    /** O cliente aceitou o interesse do editor. Abre conversa sem mensagem, então este é o aviso. */
    public void interestAccepted(UserEntity editor, UserEntity client, String jobTitle){
        quietly(Kind.INTEREST_ACCEPTED, editor, () -> new EmailContent(
                "Você foi escolhido para \"" + jobTitle + "\"",
                firstName(editor),
                List.of("%s aceitou seu interesse no trampo \"%s\". A conversa já está aberta — quem fala primeiro são vocês."
                        .formatted(client.getDisplayName(), jobTitle)),
                "Abrir a conversa",
                properties.link("/painel/conversas"),
                null));
    }

    /** Chegou candidato num pedido aberto. Vai pro cliente, que é quem decide. */
    public void newInterest(UserEntity client, UserEntity editor, String jobTitle){
        quietly(Kind.INTEREST, client, () -> new EmailContent(
                editor.getDisplayName() + " topou seu trampo",
                firstName(client),
                List.of("%s demonstrou interesse em \"%s\". Veja o perfil e o portfólio antes de escolher — o corte fala mais que a mensagem."
                        .formatted(editor.getDisplayName(), jobTitle)),
                "Ver quem topou",
                properties.link("/painel/pedidos"),
                null));
    }

    /**
     * O trampo foi marcado como concluído. Este é o e-mail que faz o laço de reputação
     * girar (§3.1) — sem ele a avaliação depende de a pessoa lembrar sozinha.
     */
    public void dealClosed(UserEntity recipient, UserEntity counterpart, String subjectLine){
        quietly(Kind.DEAL_CLOSED, recipient, () -> new EmailContent(
                "Como foi trabalhar com " + counterpart.getDisplayName() + "?",
                firstName(recipient),
                List.of("O trampo \"%s\" foi marcado como concluído. Deixe sua avaliação — é ela que mostra quem está fazendo o que os outros não fazem."
                        .formatted(subjectLine)),
                "Avaliar",
                properties.link("/painel/trampos-fechados"),
                null));
    }

    /**
     * O código de seis dígitos que confirma a criação da conta (V18).
     * <p>
     * Não recebe {@link UserEntity} como os outros: nesta altura <b>a conta não existe</b>.
     * É a assinatura do método que carrega o ponto da feature — se houvesse um usuário pra
     * passar aqui, o bot já teria conseguido o que queria.
     * <p>
     * Vai por envio obrigatório pelo motivo mais forte de todos: é literalmente o e-mail que
     * prova o endereço. Recusá-lo por falta de verificação seria exigir a verificação pra
     * poder verificar.
     */
    public void signupCode(String email, String displayName, String code){
        delivery.required(Kind.SIGNUP, () -> new PreparedEmail(email, new EmailContent(
                code + " é seu código de confirmação",
                firstNameOf(displayName),
                List.of("Use o código abaixo pra terminar de criar sua conta no TramposEditor.",
                        code),
                null,
                null,
                "O código vale por 10 minutos. Se não foi você quem pediu, ignore este e-mail — sem o código, nenhuma conta é criada.")));
    }

    /**
     * Confirmação do endereço (§2, fase 1.5).
     * <p>
     * Vai por envio obrigatório e não por {@link #quietly}: é o único aviso que <b>tem</b> que
     * chegar num endereço ainda não provado — recusá-lo por não estar verificado seria
     * exigir a verificação pra poder verificar.
     */
    public void verifyEmail(UserEntity user, String token){
        delivery.required(Kind.EMAIL_VERIFICATION, () -> new PreparedEmail(user.getEmail(), new EmailContent(
                "Confirme seu e-mail",
                firstName(user),
                List.of("Falta um clique pra sua conta ficar completa. É por este endereço que você fica sabendo quando alguém te procura."),
                "Confirmar e-mail",
                properties.link("/confirmar-email?token=" + token),
                "O link vale por 3 dias. Se não foi você quem criou a conta, ignore este e-mail.")));
    }

    /**
     * Link de redefinição de senha. Vai por envio obrigatório pelo mesmo motivo do
     * {@link #verifyEmail}: quem não conseguiu confirmar o e-mail e esqueceu a senha
     * ficaria trancado do lado de fora pra sempre.
     */
    public void passwordReset(UserEntity user, String token){
        delivery.required(Kind.PASSWORD_RESET, () -> new PreparedEmail(user.getEmail(), new EmailContent(
                "Redefinir sua senha",
                firstName(user),
                List.of("Alguém pediu pra redefinir a senha desta conta. Se não foi você, ignore este e-mail — nada muda."),
                "Redefinir senha",
                properties.link("/redefinir-senha?token=" + token),
                "O link vale por 1 hora.")));
    }

    /**
     * Aviso automático: só sai pra endereço já provado.
     * <p>
     * A regra veio com a verificação de e-mail (§2, fase 1.5) e não é burocracia — é
     * proteção de reputação. Endereço não confirmado pode ser erro de digitação ou o e-mail
     * de um terceiro, e insistir com aviso que ninguém pediu é o caminho mais curto pra
     * reclamação de spam. Uma reclamação custa a entregabilidade de <b>todo</b> o domínio,
     * inclusive dos e-mails que importam.
     * <p>
     * Não perde nada de verdade: a pessoa continua vendo tudo dentro do site, e volta a
     * receber assim que confirmar.
     */
    private void quietly(Kind kind, UserEntity recipient, Supplier<EmailContent> content) {
        delivery.afterCommit(kind, () -> recipient.isEmailVerified()
                ? new PreparedEmail(recipient.getEmail(), content.get())
                : null);
    }

    private String firstName(UserEntity user){
        return firstNameOf(user.getDisplayName());
    }

    /** A partir do nome cru — o cadastro pendente (V18) ainda não tem {@link UserEntity}. */
    private String firstNameOf(String name){
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }
}
