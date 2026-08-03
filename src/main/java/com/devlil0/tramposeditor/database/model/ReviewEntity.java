package com.devlil0.tramposeditor.database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A avaliação de um trampo fechado — o combustível do laço de reputação (ESBOCO §3.1).
 * <p>
 * Os dois lados avaliam. Não é simetria decorativa: o §6 diz que o ethos vale para
 * editor e cliente, e avaliação de mão única transformaria o padrão de conduta numa
 * hierarquia onde só um lado presta contas.
 */
@Entity
@Table(name = "reviews")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deal_id", nullable = false)
    private DealEntity deal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private UserEntity author;

    /** Quem está sendo avaliado — o outro lado do trampo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false)
    private UserEntity target;

    /** De 1 a 5, com o intervalo garantido também por CHECK no banco. */
    @Column(nullable = false)
    private short rating;

    @Column(length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
