package com.devlil0.tramposeditor.notification;

import com.devlil0.tramposeditor.database.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * A voz do produto fora do site (ESBOCO §4, ponto 7) — e o <b>canal</b> do §7 item 3.
 * <p>
 * Sem isto o que foi construído gira só enquanto a pessoa está com a aba aberta: os dois
 * lados agem em tempos diferentes e ninguém descobre que foi procurado.
 * <p>
 * Duas regras que valem pro arquivo inteiro:
 * <ol>
 *   <li><b>Nunca derruba a ação.</b> Todo envio passa por {@link #quietly}: perder um
 *       aviso é ruim, perder a mensagem que o usuário acabou de escrever é pior.</li>
 *   <li><b>Um aviso por vez, não um por evento.</b> Ver {@link #newMessage} — dez
 *       mensagens seguidas mandam <i>um</i> e-mail, não dez. Marketplace que enche a
 *       caixa de entrada é exatamente o que os 99% fazem (§6).</li>
 * </ol>
 * O texto segue o léxico do §4.1: trampo, vitrine, conversa. Nada de "usuário",
 * "plataforma" ou "não responda a este e-mail".
 * <p>
 * Cada e-mail é declarado como {@link EmailContent} — partes, não texto pronto. Quem
 * transforma isso em texto puro ou em HTML com a cara do produto é o adaptador de saída,
 * e é por isso que o texto de cada aviso mora aqui uma vez só, e não duas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailSender emailSender;
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

        quietly(() -> emailSender.send(recipient.getEmail(), new EmailContent(
                sender.getDisplayName() + " te mandou uma mensagem",
                firstName(recipient),
                List.of(sender.getDisplayName() + " falou com você no TramposEditor."),
                "Abrir a conversa",
                properties.link("/painel/conversas"),
                null)));
    }

    /** O cliente aceitou o interesse do editor. Abre conversa sem mensagem, então este é o aviso. */
    public void interestAccepted(UserEntity editor, UserEntity client, String jobTitle){
        quietly(() -> emailSender.send(editor.getEmail(), new EmailContent(
                "Você foi escolhido para \"" + jobTitle + "\"",
                firstName(editor),
                List.of("%s aceitou seu interesse no trampo \"%s\". A conversa já está aberta — quem fala primeiro são vocês."
                        .formatted(client.getDisplayName(), jobTitle)),
                "Abrir a conversa",
                properties.link("/painel/conversas"),
                null)));
    }

    /** Chegou candidato num pedido aberto. Vai pro cliente, que é quem decide. */
    public void newInterest(UserEntity client, UserEntity editor, String jobTitle){
        quietly(() -> emailSender.send(client.getEmail(), new EmailContent(
                editor.getDisplayName() + " topou seu trampo",
                firstName(client),
                List.of("%s demonstrou interesse em \"%s\". Veja o perfil e o portfólio antes de escolher — o corte fala mais que a mensagem."
                        .formatted(editor.getDisplayName(), jobTitle)),
                "Ver quem topou",
                properties.link("/painel/pedidos"),
                null)));
    }

    /**
     * O trampo foi marcado como concluído. Este é o e-mail que faz o laço de reputação
     * girar (§3.1) — sem ele a avaliação depende de a pessoa lembrar sozinha.
     */
    public void dealClosed(UserEntity recipient, UserEntity counterpart, String subjectLine){
        quietly(() -> emailSender.send(recipient.getEmail(), new EmailContent(
                "Como foi trabalhar com " + counterpart.getDisplayName() + "?",
                firstName(recipient),
                List.of("O trampo \"%s\" foi marcado como concluído. Deixe sua avaliação — é ela que mostra quem está fazendo o que os outros não fazem."
                        .formatted(subjectLine)),
                "Avaliar",
                properties.link("/painel/trampos-fechados"),
                null)));
    }

    /** Link de redefinição de senha. É o único e-mail que a pessoa pediu explicitamente. */
    public void passwordReset(UserEntity user, String token){
        quietly(() -> emailSender.send(user.getEmail(), new EmailContent(
                "Redefinir sua senha",
                firstName(user),
                List.of("Alguém pediu pra redefinir a senha desta conta. Se não foi você, ignore este e-mail — nada muda."),
                "Redefinir senha",
                properties.link("/redefinir-senha?token=" + token),
                "O link vale por 1 hora.")));
    }

    /**
     * Roda o envio sem deixar exceção escapar. O {@code EmailSender} já engole falha de
     * SMTP; isto cobre o resto (destinatário inválido, template quebrado, o que vier).
     */
    private void quietly(Runnable send){
        try {
            send.run();
        } catch (RuntimeException e) {
            log.error("Falha ao montar ou enviar notificação", e);
        }
    }

    private String firstName(UserEntity user){
        String name = user.getDisplayName();
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }
}
