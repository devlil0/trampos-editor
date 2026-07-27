package com.devlil0.tramposeditor.database.model;

import com.devlil0.tramposeditor.enums.PortfolioProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "portfolio_items")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PortfolioItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "editor_profile_id", nullable = false)
    private EditorProfileEntity editorProfile;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PortfolioProvider provider;

    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    /** Ordem de exibição no perfil. */
    @Column(nullable = false)
    @Builder.Default
    private int position = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
