package com.devlil0.tramposeditor.database.model;

import com.devlil0.tramposeditor.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@DynamicUpdate
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
     * Se a pessoa <b>escolheu</b> este username ou apenas herdou o gerado no cadastro.
     * <p>
     * Falso só existe num caso: conta nascida pelo provedor social, onde não há formulário
     * onde perguntar. Enquanto for falso a pessoa não navega no {@code /painel} — cai na
     * tela de escolha. Todo o resto entra com verdadeiro, inclusive as contas anteriores
     * à V15, que já publicaram o endereço que têm.
     */
    @Column(name = "username_chosen", nullable = false)
    @Builder.Default
    private boolean usernameChosen = true;

    /**
     * Última <b>troca</b> de username; nulo enquanto não houve nenhuma. É daqui que sai o
     * cooldown de {@code UsernameService.COOLDOWN_DIAS}.
     * <p>
     * A primeira escolha não conta como troca de propósito — ver a V15.
     */
    @Column(name = "username_changed_at")
    private LocalDateTime usernameChangedAt;

    /**
     * Username do Instagram, sem "@" — normalizado no UserService. Vive na conta porque
     * serve aos dois papéis: é vitrine do editor e identidade de quem contrata.
     */
    @Column(length = 30)
    private String instagram;

    /**
     * Ponteiro pra foto de perfil: nulo quando não há. Fica aqui, e não só em
     * {@code user_avatars}, pra que listar perfis saiba se existe foto sem tocar no
     * {@code bytea} — e pra que a URL da imagem carregue a versão, o que permite cache
     * longo no browser sem a foto velha grudar depois de trocada.
     */
    @Column(name = "avatar_version")
    private UUID avatarVersion;

    /**
     * Quando o dono provou que o endereço é dele; nulo enquanto não provou.
     * <p>
     * Conta criada por provedor social já nasce preenchida — o Google e o GitHub só
     * entregam e-mail confirmado, e uma prova melhor que a nossa não se refaz.
     * <p>
     * Data em vez de booleano de propósito: "quando" responde o que "se" não responde —
     * quanto tempo a pessoa levou, e quantas nunca voltaram.
     */
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    /**
     * De onde a pessoa veio quando chegou pela <b>primeira vez</b> — não quando se
     * cadastrou. É o único dado do §3.1 que não é derivável do resto do banco: existe por
     * um instante e some se ninguém gravar (V16).
     * <p>
     * Nulo é resposta legítima, e vai ser a maioria: acesso direto e app que suprime o
     * Referer entram assim.
     */
    @Column(name = "signup_referrer", length = 255)
    private String signupReferrer;

    /** {@code utm_source} da primeira visita. Ver V16 pra por que só ele. */
    @Column(name = "signup_utm_source", length = 120)
    private String signupUtmSource;

    /** A primeira página vista — é ela que responde "qual conteúdo capta?". */
    @Column(name = "signup_landing", length = 255)
    private String signupLanding;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "session_version", nullable = false, updatable = false)
    private long sessionVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean hasRole(UserRole role){
        return roles.contains(role);
    }

    public boolean isEmailVerified(){
        return emailVerifiedAt != null;
    }

    /**
     * Grava a origem da primeira visita, e só se ainda não houver nenhuma.
     * <p>
     * A guarda existe porque o login social chama isto na troca do código, e a mesma conta
     * pode passar por lá de novo — sobrescrever trocaria a origem de quem captou a pessoa
     * pela origem do último login dela.
     */
    public void recordSignupOrigin(String referrer, String utmSource, String landing){
        if (signupReferrer != null || signupUtmSource != null || signupLanding != null) {
            return;
        }

        this.signupReferrer = referrer;
        this.signupUtmSource = utmSource;
        this.signupLanding = landing;
    }
}
