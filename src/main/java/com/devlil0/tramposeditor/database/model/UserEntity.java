package com.devlil0.tramposeditor.database.model;

import com.devlil0.tramposeditor.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    /**
     * Nulo em conta que só existe por provedor (Google/GitHub). Todo caminho de login
     * por senha precisa tratar isso como "esta conta não entra por senha".
     */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    /**
     * Identidade da pessoa no produto e endereço dela em {@code /{username}}.
     * <p>
     * Vive na conta, e não no perfil de editor, porque os dois papéis precisam existir
     * como página: quem edita e quem contrata. O {@code slug} do editor continua na tabela
     * dele, mas só como histórico — quem manda no endereço é este campo (V12).
     * <p>
     * Nomes que colidem com rota do produto são recusados no {@code UsernameService}:
     * a raiz virou namespace de usuário, então {@code /entrar} e {@code /editores} não
     * podem ser username de ninguém.
     */
    @Column(nullable = false, unique = true, length = 30)
    private String username;

    /**
     * Username do Instagram, sem "@" — normalizado no UserService. Vive na conta porque
     * serve aos dois papéis: é vitrine do editor e identidade de quem contrata.
     */
    @Column(length = 30)
    private String instagram;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean hasRole(UserRole role){
        return roles.contains(role);
    }
}
