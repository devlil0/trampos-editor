package com.devlil0.tramposeditor.database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Confirmação de e-mail (ESBOCO §2, fase 1.5).
 * <p>
 * Gêmeo do {@link PasswordResetTokenEntity}, e de propósito: os dois provam a mesma coisa
 * — que quem tem o link tem a caixa de entrada. Guarda o <b>hash</b>, nunca o token, então
 * vazar a tabela não vaza conta.
 * <p>
 * O prazo é mais generoso que o da senha (dias, não uma hora): redefinir senha é urgente
 * por natureza, confirmar e-mail não. Link de confirmação que expira enquanto a pessoa
 * almoça só produz "reenviar" — e um reenvio a mais é um e-mail a mais na reputação do
 * domínio.
 */
@Entity
@Table(name = "email_verification_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EmailVerificationTokenEntity {

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
