package com.devlil0.tramposeditor.database.model;

import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.JobRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pedido publicado por quem precisa de edição ("postar pedido rápido", FUNDACAO §10.2).
 * <p>
 * Deliberadamente <b>sem sistema de lances</b>: o editor demonstra interesse, não propõe
 * preço num leilão. É a versão leve do Modelo B que o §5.1 pede.
 */
@Entity
@Table(name = "job_requests")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class JobRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Quem publicou. Qualquer conta pode — todo usuário é cliente por padrão. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_user_id", nullable = false)
    private UserEntity client;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    /** Onde o vídeo vai ser publicado — mesma taxonomia da vitrine. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EditorNiche niche;

    /** Orçamento em centavos de BRL. Opcional: nem todo cliente sabe quanto custa. */
    @Column(name = "budget_cents")
    private Integer budgetCents;

    @Column(name = "deadline_days")
    private Integer deadlineDays;

    /** Link de referência (vídeo parecido, briefing, material bruto). */
    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JobRequestStatus status = JobRequestStatus.OPEN;

    @OneToMany(mappedBy = "jobRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<JobInterestEntity> interests = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isOpen(){
        return status == JobRequestStatus.OPEN;
    }

    public void addInterest(JobInterestEntity interest){
        interest.setJobRequest(this);
        interests.add(interest);
    }
}
