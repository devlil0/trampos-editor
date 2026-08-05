package com.devlil0.tramposeditor.database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Código de uso único que fecha o login social.
 * <p>
 * Existe porque o provedor devolve pro backend, mas quem guarda a sessão é o frontend,
 * num cookie httpOnly. Mandar o JWT na URL de redirecionamento vazaria em log, histórico
 * e Referer — então vai um código curto, trocado por JWT numa chamada servidor a
 * servidor. Mesmo padrão do authorization code do OAuth, pela mesma razão.
 * <p>
 * Guarda o hash, como {@link PasswordResetTokenEntity} — a diferença é o prazo, aqui
 * medido em segundos, porque a troca acontece no mesmo instante do redirecionamento.
 */
@Entity
@Table(name = "auth_exchange_codes")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AuthExchangeCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    /**
     * A conta nasceu junto com este código, ou já existia e a pessoa só entrou de novo.
     * <p>
     * Serve à atribuição de origem (V16): ela só pode valer pra cadastro novo. Vive aqui, e
     * não numa variável do serviço, porque quem cria a conta é o callback do provedor e
     * quem recebe a origem é a troca do código — dois requests diferentes.
     */
    @Column(name = "new_account", nullable = false)
    @Builder.Default
    private boolean newAccount = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isUsable(LocalDateTime now){
        return usedAt == null && expiresAt.isAfter(now);
    }
}
