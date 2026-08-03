package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.ConversationEntity;
import com.devlil0.tramposeditor.database.model.JobRequestEntity;
import com.devlil0.tramposeditor.database.model.MessageEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IConversationRepository;
import com.devlil0.tramposeditor.database.repository.IEditorProfileRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.ConversationDetailDTO;
import com.devlil0.tramposeditor.dto.ConversationSummaryDTO;
import com.devlil0.tramposeditor.dto.MessageCreateDTO;
import com.devlil0.tramposeditor.dto.MessageDTO;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.ForbiddenException;
import com.devlil0.tramposeditor.exception.NotFoundException;
import com.devlil0.tramposeditor.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A caixa de entrada única (ESBOCO §7, item 1).
 * <p>
 * Duas portas levam à mesma conversa:
 * <ol>
 *   <li>{@link #startWithEditor} — o "Falar com" do perfil na vitrine;</li>
 *   <li>{@link #openForJob} — chamado pelo aceite de um interesse num pedido.</li>
 * </ol>
 * Ambas são idempotentes: chamar de novo devolve a conversa que já existe em vez de
 * criar outra, senão o mesmo par acabaria com uma pilha de conversas vazias.
 * <p>
 * <b>Dívida conhecida:</b> {@link #listMine} lê a última mensagem de cada conversa por
 * lazy-load, o que é N+1 na listagem — mesmo custo já aceito na vitrine (FUNDACAO
 * §11.5). Vale trocar por projeção quando a caixa de entrada passar de algumas dezenas.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int PREVIEW_LENGTH = 120;

    private final IConversationRepository conversationRepository;
    private final IEditorProfileRepository editorProfileRepository;
    private final IUserRepository userRepository;
    private final NotificationService notificationService;

    // ---------- Porta 1: vitrine ----------

    /**
     * Abre (ou reaproveita) a conversa direta entre quem contrata e um editor da vitrine.
     * É o destino do botão que antes era um link morto no perfil.
     */
    @Transactional
    public ConversationDetailDTO startWithEditor(UUID clientUserId, String editorSlug, MessageCreateDTO dto){
        var profile = editorProfileRepository.findBySlug(editorSlug)
                .orElseThrow(() -> new NotFoundException("Editor não encontrado: " + editorSlug));

        UUID editorUserId = profile.getUser().getId();
        if (editorUserId.equals(clientUserId)) {
            throw new BusinessException("Você não pode iniciar uma conversa com você mesmo");
        }

        ConversationEntity conversation = conversationRepository
                .findByClientIdAndEditorIdAndJobRequestIsNull(clientUserId, editorUserId)
                .orElseGet(() -> conversationRepository.save(ConversationEntity.builder()
                        .client(loadUser(clientUserId))
                        .editor(profile.getUser())
                        .build()));

        appendMessage(conversation, clientUserId, dto.body());
        return toDetail(conversation, clientUserId);
    }

    // ---------- Porta 2: aceite de interesse ----------

    /**
     * Conversa do pedido, criada quando o cliente aceita um interesse. Não manda
     * mensagem: quem fala primeiro são as pessoas, não o sistema.
     */
    @Transactional
    public ConversationEntity openForJob(JobRequestEntity job, UserEntity editor){
        UUID clientId = job.getClient().getId();
        return conversationRepository
                .findByClientIdAndEditorIdAndJobRequestId(clientId, editor.getId(), job.getId())
                .orElseGet(() -> conversationRepository.save(ConversationEntity.builder()
                        .client(job.getClient())
                        .editor(editor)
                        .jobRequest(job)
                        .build()));
    }

    // ---------- Leitura ----------

    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> listMine(UUID userId){
        return conversationRepository.findAllForUser(userId).stream()
                .map(c -> toSummary(c, userId))
                .toList();
    }

    /** Abrir a conversa é o que marca como lida — não existe botão pra isso. */
    @Transactional
    public ConversationDetailDTO open(UUID userId, UUID conversationId){
        ConversationEntity conversation = loadParticipating(userId, conversationId);
        conversation.markReadBy(userId, LocalDateTime.now());
        return toDetail(conversation, userId);
    }

    // ---------- Escrita ----------

    @Transactional
    public MessageDTO send(UUID userId, UUID conversationId, MessageCreateDTO dto){
        ConversationEntity conversation = loadParticipating(userId, conversationId);
        MessageEntity message = appendMessage(conversation, userId, dto.body());
        return toMessageDTO(message, userId);
    }

    // ---------- Helpers ----------

    private MessageEntity appendMessage(ConversationEntity conversation, UUID senderId, String body){
        UserEntity sender = loadUser(senderId);
        UserEntity recipient = conversation.counterpartOf(senderId);

        // Lido ANTES de anexar: depois de `addMessage` o `lastMessageAt` já mudou e todo
        // destinatário pareceria ter pendência, o que mataria a regra de um aviso por vez.
        boolean jaAvisado = conversation.hasUnreadFor(recipient.getId());

        MessageEntity message = MessageEntity.builder()
                .sender(sender)
                .body(body.trim())
                .createdAt(LocalDateTime.now())
                .build();

        conversation.addMessage(message);
        // Quem escreve está lendo a própria conversa: sem isto a mensagem recém-enviada
        // apareceria como não lida pro próprio autor.
        conversation.markReadBy(senderId, message.getCreatedAt());
        conversationRepository.save(conversation);

        notificationService.newMessage(recipient, sender, jaAvisado);
        return message;
    }

    private ConversationEntity loadParticipating(UUID userId, UUID conversationId){
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversa não encontrada: " + conversationId));

        if (!conversation.isParticipant(userId)) {
            // Mesma mensagem de "não é sua" usada no resto do domínio: não revela se a
            // conversa existe pra quem não faz parte dela.
            throw new ForbiddenException("Essa conversa não é sua");
        }
        return conversation;
    }

    private UserEntity loadUser(UUID id){
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException(id));
    }

    /** Slug do perfil público do outro lado, quando ele tem um. */
    private String slugOf(UUID userId){
        return editorProfileRepository.findByUserId(userId)
                .map(p -> p.getSlug())
                .orElse(null);
    }

    private ConversationSummaryDTO toSummary(ConversationEntity c, UUID userId){
        UserEntity other = c.counterpartOf(userId);
        List<MessageEntity> messages = c.getMessages();
        String preview = messages.isEmpty() ? null : truncate(messages.get(messages.size() - 1).getBody());

        return new ConversationSummaryDTO(
                c.getId(),
                other.getDisplayName(),
                slugOf(other.getId()),
                c.getJobRequest() != null ? c.getJobRequest().getTitle() : null,
                preview,
                c.hasUnreadFor(userId),
                c.getLastMessageAt(),
                c.getCreatedAt()
        );
    }

    private ConversationDetailDTO toDetail(ConversationEntity c, UUID userId){
        UserEntity other = c.counterpartOf(userId);
        JobRequestEntity job = c.getJobRequest();

        return new ConversationDetailDTO(
                c.getId(),
                other.getDisplayName(),
                slugOf(other.getId()),
                job != null ? job.getId() : null,
                job != null ? job.getTitle() : null,
                c.isClient(userId),
                c.getMessages().stream().map(m -> toMessageDTO(m, userId)).toList(),
                c.getCreatedAt()
        );
    }

    private MessageDTO toMessageDTO(MessageEntity m, UUID userId){
        return new MessageDTO(
                m.getId(),
                m.getSender().getDisplayName(),
                m.getSender().getId().equals(userId),
                m.getBody(),
                m.getCreatedAt()
        );
    }

    private String truncate(String body){
        String clean = body.replaceAll("\\s+", " ").trim();
        return clean.length() <= PREVIEW_LENGTH ? clean : clean.substring(0, PREVIEW_LENGTH - 1) + "…";
    }
}
