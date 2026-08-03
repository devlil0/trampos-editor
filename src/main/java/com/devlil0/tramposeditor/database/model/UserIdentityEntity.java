package com.devlil0.tramposeditor.database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vínculo entre uma conta e um provedor de login (Google, GitHub).
 * <p>
 * Guarda o {@code subject} do provedor, não o e-mail: e-mail a pessoa troca, o
 * identificador não. É ele que mantém o vínculo de pé quando alguém muda de endereço.
 */
@Entity
@Table(name = "user_identities")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserIdentityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /** O {@code registrationId} do Spring Security: {@code google} ou {@code github}. */
    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, length = 190)
    private String subject;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
