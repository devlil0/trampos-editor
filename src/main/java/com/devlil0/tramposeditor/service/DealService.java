package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.ConversationEntity;
import com.devlil0.tramposeditor.database.model.DealEntity;
import com.devlil0.tramposeditor.database.model.EditorProfileEntity;
import com.devlil0.tramposeditor.database.model.JobRequestEntity;
import com.devlil0.tramposeditor.database.model.ReviewEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IConversationRepository;
import com.devlil0.tramposeditor.database.repository.IDealRepository;
import com.devlil0.tramposeditor.database.repository.IEditorProfileRepository;
import com.devlil0.tramposeditor.database.repository.IReviewRepository;
import com.devlil0.tramposeditor.dto.DealCreateDTO;
import com.devlil0.tramposeditor.dto.DealDTO;
import com.devlil0.tramposeditor.dto.RatingAggregateDTO;
import com.devlil0.tramposeditor.dto.ReviewCreateDTO;
import com.devlil0.tramposeditor.dto.ReviewDTO;
import com.devlil0.tramposeditor.enums.DealStatus;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.ForbiddenException;
import com.devlil0.tramposeditor.exception.NotFoundException;
import com.devlil0.tramposeditor.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Bloco 6 — o marco de "acabou" e a avaliação (ESBOCO §7, item 4).
 * <p>
 * É o único item do plano que fecha <b>dois</b> laços:
 * <ul>
 *   <li><b>reputação</b> (§3.1): sem trampo fechado não há nota, e sem nota a ordenação
 *       da vitrine (§2.1) decide tudo por antiguidade;</li>
 *   <li><b>demanda</b> (§3.2): sem evento de fim, nada traz o cliente de volta.</li>
 * </ul>
 * Duas regras de quem pode o quê, ambas deliberadas:
 * <ol>
 *   <li><b>Quem fecha é o cliente.</b> É quem recebe o trabalho, então é quem diz que
 *       chegou. Deixar o editor marcar entregue convidaria a farmar avaliação.</li>
 *   <li><b>Quem avalia são os dois.</b> O ethos do §6 vale para os dois lados; nota de
 *       mão única viraria hierarquia em vez de padrão de conduta.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class DealService {

    private final IDealRepository dealRepository;
    private final IReviewRepository reviewRepository;
    private final IConversationRepository conversationRepository;
    private final IEditorProfileRepository editorProfileRepository;
    private final NotificationService notificationService;

    // ---------- Abrir ----------

    /**
     * Porta 1 — o aceite de um interesse. Chamado pelo {@code JobRequestService}: aceitar
     * já é o gesto de "fechamos", então não faz sentido pedir de novo numa tela.
     */
    @Transactional
    public DealEntity openForJob(ConversationEntity conversation, JobRequestEntity job, UserEntity editor){
        return dealRepository.findFirstByJobRequestIdOrderByCreatedAtAsc(job.getId())
                .orElseGet(() -> dealRepository.save(DealEntity.builder()
                        .conversation(conversation)
                        .jobRequest(job)
                        .client(job.getClient())
                        .editor(editor)
                        .title(job.getTitle())
                        .build()));
    }

    /**
     * Porta 2 — a conversa que veio da vitrine, onde não há pedido nenhum. Quem abre é o
     * cliente, e é o que dá fim ao caminho que antes só tinha começo.
     */
    @Transactional
    public DealDTO openFromConversation(UUID clientUserId, UUID conversationId, DealCreateDTO dto){
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversa não encontrada: " + conversationId));

        if (!conversation.isParticipant(clientUserId)) {
            throw new ForbiddenException("Essa conversa não é sua");
        }
        if (!conversation.isClient(clientUserId)) {
            throw new ForbiddenException("Quem combina o trampo é quem está contratando");
        }
        dealRepository.findByConversationIdAndStatus(conversationId, DealStatus.OPEN).ifPresent(d -> {
            throw new BusinessException("Já existe um trampo em aberto nesta conversa");
        });

        DealEntity deal = dealRepository.save(DealEntity.builder()
                .conversation(conversation)
                .client(conversation.getClient())
                .editor(conversation.getEditor())
                .title(dto.title().trim())
                .build());

        return toDTO(deal, clientUserId);
    }

    // ---------- Fechar ----------

    /**
     * O marco. Fechar dispara o e-mail que pede a nota pros dois lados — sem ele a
     * avaliação dependeria de alguém lembrar sozinho, que é o mesmo que não existir.
     */
    @Transactional
    public DealDTO close(UUID userId, UUID dealId, DealStatus outcome){
        DealEntity deal = loadParticipating(userId, dealId);

        if (!deal.isOpen()) {
            throw new BusinessException("Esse trampo já foi encerrado");
        }
        if (!deal.getClient().getId().equals(userId)) {
            throw new ForbiddenException("Quem marca o trampo como concluído é quem contratou");
        }
        if (outcome == DealStatus.OPEN) {
            throw new BusinessException("Encerrar exige um desfecho: concluído ou cancelado");
        }

        deal.close(outcome, LocalDateTime.now());
        dealRepository.save(deal);

        // Cancelado não pede avaliação: não houve trabalho pra avaliar, e cobrar nota de
        // algo que não vingou só produziria ruído na reputação dos dois.
        if (outcome == DealStatus.DONE) {
            notificationService.dealClosed(deal.getEditor(), deal.getClient(), deal.getTitle());
            notificationService.dealClosed(deal.getClient(), deal.getEditor(), deal.getTitle());
        }

        return toDTO(deal, userId);
    }

    // ---------- Avaliar ----------

    @Transactional
    public DealDTO review(UUID userId, UUID dealId, ReviewCreateDTO dto){
        DealEntity deal = loadParticipating(userId, dealId);

        if (deal.getStatus() != DealStatus.DONE) {
            throw new BusinessException("Só dá pra avaliar um trampo concluído");
        }
        if (deal.reviewedBy(userId)) {
            throw new BusinessException("Você já avaliou este trampo");
        }

        UserEntity author = deal.getClient().getId().equals(userId) ? deal.getClient() : deal.getEditor();
        UserEntity target = deal.counterpartOf(userId);

        deal.addReview(ReviewEntity.builder()
                .author(author)
                .target(target)
                .rating(dto.rating())
                .comment(dto.comment() == null || dto.comment().isBlank() ? null : dto.comment().trim())
                .build());

        dealRepository.save(deal);
        refreshRating(target);

        return toDTO(deal, userId);
    }

    /**
     * Devolve a nota pro perfil — é este passo que liga a avaliação de volta na ordenação
     * da vitrine (§2.1) e faz o laço de reputação realmente dar a volta.
     * <p>
     * Só editor tem perfil público: avaliar um cliente registra a conduta dele no trampo,
     * mas não alimenta vitrine nenhuma, porque ele não aparece em nenhuma.
     */
    private void refreshRating(UserEntity target){
        editorProfileRepository.findByUserId(target.getId()).ifPresent(profile -> {
            RatingAggregateDTO agg = reviewRepository.aggregateFor(target.getId());
            profile.setRatingAvg(BigDecimal.valueOf(agg.average()).setScale(1, RoundingMode.HALF_UP));
            profile.setRatingCount(agg.count().intValue());
            editorProfileRepository.save(profile);
        });
    }

    // ---------- Leitura ----------

    @Transactional(readOnly = true)
    public List<DealDTO> listMine(UUID userId){
        return dealRepository.findAllForUser(userId).stream()
                .map(d -> toDTO(d, userId))
                .toList();
    }

    // ---------- Helpers ----------

    private DealEntity loadParticipating(UUID userId, UUID dealId){
        DealEntity deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new NotFoundException("Trampo não encontrado: " + dealId));

        if (!deal.isParticipant(userId)) {
            throw new ForbiddenException("Esse trampo não é seu");
        }
        return deal;
    }

    private DealDTO toDTO(DealEntity deal, UUID userId){
        UserEntity counterpart = deal.counterpartOf(userId);
        boolean iAmClient = deal.getClient().getId().equals(userId);

        ReviewDTO reviewOfMe = deal.getReviews().stream()
                .filter(r -> r.getTarget().getId().equals(userId))
                .findFirst()
                .map(this::toReviewDTO)
                .orElse(null);

        return new DealDTO(
                deal.getId(),
                deal.getConversation().getId(),
                deal.getTitle(),
                deal.getStatus(),
                counterpart.getDisplayName(),
                // O slug só existe quando o outro lado é editor — é o que permite
                // "chamar de novo" direto do histórico.
                iAmClient ? slugOf(counterpart.getId()) : null,
                iAmClient,
                deal.reviewedBy(userId),
                reviewOfMe,
                deal.getCreatedAt(),
                deal.getClosedAt()
        );
    }

    private ReviewDTO toReviewDTO(ReviewEntity r){
        return new ReviewDTO(
                r.getId(),
                r.getAuthor().getDisplayName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }

    private String slugOf(UUID userId){
        return editorProfileRepository.findByUserId(userId)
                .map(EditorProfileEntity::getSlug)
                .orElse(null);
    }
}
