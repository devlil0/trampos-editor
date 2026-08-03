package com.devlil0.tramposeditor.database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Pedido de redefinição de senha (ESBOCO §7 item 6).
 * <p>
 * Guarda o <b>hash</b> do token, não o token: o valor que vai no e-mail não existe em
 * lugar nenhum do banco, então vazar a tabela não vaza conta.
 */
@Entity
@Table(name = "password_reset_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PasswordResetTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Vale só uma vez e só dentro do prazo. */
    public boolean isUsable(LocalDateTime now){
        return usedAt == null && expiresAt.isAfter(now);
    }
}
