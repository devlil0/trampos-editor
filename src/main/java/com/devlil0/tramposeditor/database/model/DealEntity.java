package com.devlil0.tramposeditor.database.model;

import com.devlil0.tramposeditor.enums.DealStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O trampo combinado — o marco de "acabou" que faltava (ESBOCO §3.2).
 * <p>
 * Nasce por duas portas, as mesmas da conversa: junto do aceite de um interesse
 * ({@code jobRequest} preenchido) ou aberto à mão dentro de uma conversa direta da
 * vitrine ({@code jobRequest} nulo). Sem ele o produto terminava no aceite e nada
 * registrava que o trabalho havia acabado.
 */
@Entity
@Table(name = "deals")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DealEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_request_id")
    private JobRequestEntity jobRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_user_id", nullable = false)
    private UserEntity client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "editor_user_id", nullable = false)
    private UserEntity editor;

    /** Do que se trata, pra que o histórico se leia sem abrir a conversa. */
    @Column(nullable = false, length = 140)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DealStatus status = DealStatus.OPEN;

    @OneToMany(mappedBy = "deal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewEntity> reviews = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public boolean isOpen(){
        return status == DealStatus.OPEN;
    }

    public boolean isParticipant(UUID userId){
        return client.getId().equals(userId) || editor.getId().equals(userId);
    }

    public UserEntity counterpartOf(UUID userId){
        return client.getId().equals(userId) ? editor : client;
    }

    /** Fechar é o que libera a avaliação — e o que dispara o e-mail que pede a nota. */
    public void close(DealStatus outcome, LocalDateTime when){
        this.status = outcome;
        this.closedAt = when;
    }

    /** Já avaliei este trampo? Uma nota por pessoa por trampo (constraint no banco). */
    public boolean reviewedBy(UUID userId){
        return reviews.stream().anyMatch(r -> r.getAuthor().getId().equals(userId));
    }

    public void addReview(ReviewEntity review){
        review.setDeal(this);
        reviews.add(review);
    }
}
