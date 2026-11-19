package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.EmailVerificationTokenEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IEmailVerificationTokenRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.NotFoundException;
import com.devlil0.tramposeditor.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Verificação de e-mail (ESBOCO §2, fase 1.5).
 * <p>
 * O e-mail é a única prova de posse que o produto tem, e é por ele que os quatro laços do
 * §3 voltam. Endereço errado quebra o ciclo em silêncio: a pessoa nunca descobre que foi
 * procurada e conclui que o site é morto. Endereço de terceiro é pior — vira reclamação de
 * spam, que custa a reputação do domínio inteiro.
 * <p>
 * <b>O que a falta de verificação bloqueia é deliberadamente pouco</b>, e a régua é uma
 * só: só entra na lista o que <i>alcança outra pessoa</i>. Hoje isso é (1) receber
 * notificação, que o {@link NotificationService} recusa mandar pra endereço não provado, e
 * (2) publicar perfil de editor, que colocaria uma conta não confirmada na vitrine
 * indexada. Navegar, conversar e se candidatar continuam livres — a porta aberta do
 * {@code FUNDACAO} §11.9 não se fecha por burocracia.
 * <p>
 * Quem entra por Google ou GitHub nunca passa por aqui: chega verificado do provedor.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    /**
     * Prazo generoso de propósito. Redefinir senha é urgente e vale uma hora; confirmar
     * e-mail não é — e link que expira enquanto a pessoa almoça só gera reenvio, que é mais
     * um e-mail no contador de reputação do domínio.
     */
    private static final Duration VALIDADE = Duration.ofDays(3);
    private static final String INVALID_LINK = "Esse link expirou ou já foi usado. Peça outro.";

    private final IEmailVerificationTokenRepository tokenRepository;
    private final IUserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Manda (ou remanda) o link com a conta bloqueada.
     * <p>
     * Silencioso pra conta já verificada: reenviar não é erro que mereça tela vermelha, é
     * um clique a mais numa aba velha.
     */
    private void send(UserEntity user){
        if (!user.isActive() || user.isEmailVerified()) return;

        // Um link vivo por conta: pedir de novo invalida o anterior.
        tokenRepository.deleteByUserId(user.getId());

        String token = TokenSecrets.generate();
        tokenRepository.save(EmailVerificationTokenEntity.builder()
                .user(user)
                .tokenHash(TokenSecrets.hash(token))
                .expiresAt(LocalDateTime.now().plus(VALIDADE))
                .build());

        notificationService.verifyEmail(user, token);
    }

    /** Reenvio pedido por quem está logado. */
    @Transactional
    public void resend(UUID userId){
        send(userRepository.findLockedById(userId).orElseThrow(() -> new NotFoundException(userId)));
    }

    /**
     * Confirma o endereço. Token inexistente, expirado e já usado dão a mesma resposta:
     * distinguir contaria a quem tenta adivinhar que ele existiu um dia — mesma regra do
     * {@link PasswordResetService}.
     */
    @Transactional
    public void confirm(String token){
        String hash = TokenSecrets.hash(token);
        UserEntity user = tokenRepository.findUserIdByTokenHash(hash)
                .flatMap(userRepository::findLockedById)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new BusinessException(INVALID_LINK));
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationTokenEntity entity = tokenRepository.findByTokenHash(hash)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new BusinessException(INVALID_LINK));

        user.setEmailVerifiedAt(now);
        entity.setUsedAt(now);
        tokenRepository.save(entity);
    }
}
