package com.devlil0.tramposeditor.database.model;

import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.EditorTool;
import com.devlil0.tramposeditor.enums.Vertical;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "editor_profiles")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EditorProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Um perfil de editor pertence a exatamente um usuário (que tem o papel EDITOR). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    /** Chave humana da URL pública (/editores/{slug}) — estável pra não quebrar SEO. */
    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Vertical vertical = Vertical.VIDEO;

    @Column(length = 120)
    private String headline;

    @Column(length = 2000)
    private String bio;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "editor_profile_niches", joinColumns = @JoinColumn(name = "editor_profile_id"))
    @Column(name = "niche", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<EditorNiche> niches = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "editor_profile_tools", joinColumns = @JoinColumn(name = "editor_profile_id"))
    @Column(name = "tool", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<EditorTool> tools = new HashSet<>();

    /** Faixa de preço em centavos de BRL — inteiro evita imprecisão de ponto flutuante em dinheiro. */
    @Column(name = "price_min_cents")
    private Integer priceMinCents;

    @Column(name = "price_max_cents")
    private Integer priceMaxCents;

    /** Prazo médio de entrega em dias (turnaround). */
    @Column(name = "turnaround_days")
    private Integer turnaroundDays;

    @Column(nullable = false)
    @Builder.Default
    private boolean available = true;

    /** Rating agregado — populado no Bloco 6 (reviews). Fica em 0 até lá. */
    @Column(name = "rating_avg", nullable = false, precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private int ratingCount = 0;

    @OneToMany(mappedBy = "editorProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<PortfolioItemEntity> portfolio = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addPortfolioItem(PortfolioItemEntity item){
        item.setEditorProfile(this);
        portfolio.add(item);
    }
}
