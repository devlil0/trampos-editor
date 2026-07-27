package com.devlil0.tramposeditor.database.model;

import com.devlil0.tramposeditor.enums.JobInterestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um editor dizendo "eu topo esse trabalho". Sem preço e sem lance — só a mensagem.
 * <p>
 * Quando o cliente aceita, as duas pontas passam a ver o e-mail uma da outra: é o
 * substituto honesto do chat até o Bloco 5 existir.
 */
@Entity
@Table(
        name = "job_interests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_job_interests_editor",
                columnNames = {"job_request_id", "editor_user_id"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class JobInterestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_request_id", nullable = false)
    private JobRequestEntity jobRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "editor_user_id", nullable = false)
    private UserEntity editor;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobInterestStatus status = JobInterestStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
