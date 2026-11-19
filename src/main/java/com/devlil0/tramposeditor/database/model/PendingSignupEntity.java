package com.devlil0.tramposeditor.database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um cadastro que ainda não é conta (V18).
 * <p>
 * Fica entre o formulário preenchido e a linha em {@code users}: guarda tudo que a pessoa
 * digitou e espera ela devolver o código de seis dígitos que foi pro e-mail. Só então a
 * conta nasce — e nasce com o endereço já provado.
 * <p>
 * <b>Não é um {@link UserEntity} incompleto.</b> A diferença é o ponto da feature: uma
 * conta inativa ainda ocupa username, aparece em consulta e conta como usuário; isto aqui
 * não existe pro resto do sistema. Bot que não lê e-mail para nesta tabela, e o que ele
 * deixa pra trás expira sozinho.
 * <p>
 * Some por três caminhos: confirmado (vira conta), substituído (pediu código de novo) ou
 * vencido. Nenhum deles precisa de intervenção.
 */
@Entity
@Table(name = "pending_signups")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PendingSignupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 180)
    private String email;

    /** Já cifrada. Ver o comentário da coluna na V18. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false, length = 60)
    private String username;

    /** Papéis pedidos, separados por vírgula. Quem traduz é o {@code SignupService}. */
    @Column(nullable = false, length = 120)
    private String roles;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    @Builder.Default
    private short attempts = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "signup_referrer", length = 255)
    private String signupReferrer;

    @Column(name = "signup_utm_source", length = 120)
    private String signupUtmSource;

    @Column(name = "signup_landing", length = 255)
    private String signupLanding;

    public boolean isExpired(LocalDateTime now){
        return !expiresAt.isAfter(now);
    }
}
