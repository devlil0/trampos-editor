package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.PasswordResetTokenEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IPasswordResetTokenRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.PasswordResetConfirmDTO;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Esqueci a senha (ESBOCO §7, item 6).
 * <p>
 * Três decisões de segurança, todas do mesmo princípio — o e-mail é a única prova de
 * posse que temos, então ele não pode virar fonte de informação pra quem não é dono:
 * <ol>
 *   <li><b>Pedir nunca revela se a conta existe.</b> {@link #request} não devolve erro
 *       pra e-mail desconhecido: enumerar contas cadastradas seria um efeito colateral
 *       gratuito. É a mesma regra do {@code INVALID_CREDENTIALS} do login.</li>
 *   <li><b>O banco guarda o hash do token</b>, nunca o token. O valor que vai no e-mail
 *       não existe em lugar nenhum do servidor depois que ele sai.</li>
 *   <li><b>Uso único e com prazo.</b> Uma hora, e o primeiro uso queima o link.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration VALIDADE = Duration.ofHours(1);
    private static final String INVALID_LINK = "Esse link expirou ou já foi usado. Peça outro.";

    private final IPasswordResetTokenRepository tokenRepository;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    /**
     * Silencioso de propósito: quem pediu recebe o e-mail, quem não tem conta recebe a
     * mesma resposta vazia. A tela diz "se existir conta, o link chegou".
     */
    @Transactional
    public void request(String email){
        userRepository.findLockedByEmail(email.trim().toLowerCase(Locale.ROOT)).ifPresent(user -> {
            if (!user.isActive()) return;

            // Um link vivo por conta: pedir de novo invalida o anterior.
            tokenRepository.deleteByUserId(user.getId());

            String token = TokenSecrets.generate();
            tokenRepository.save(PasswordResetTokenEntity.builder()
                    .user(user)
                    .tokenHash(TokenSecrets.hash(token))
                    .expiresAt(LocalDateTime.now().plus(VALIDADE))
                    .build());

            notificationService.passwordReset(user, token);
        });
    }

    @Transactional
    public void confirm(PasswordResetConfirmDTO dto){
        String hash = TokenSecrets.hash(dto.token());
        // Emissão e consumo bloqueiam a mesma conta antes de tocar o token.
        UserEntity user = tokenRepository.findUserIdByTokenHash(hash)
                .flatMap(userRepository::findLockedById)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new BusinessException(INVALID_LINK));
        LocalDateTime now = LocalDateTime.now();
        PasswordResetTokenEntity token = tokenRepository.findByTokenHash(hash)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new BusinessException(INVALID_LINK));

        if (userRepository.updatePasswordAndRevokeSessions(
                user.getId(), passwordEncoder.encode(dto.password()), now) == 0) {
            throw new BusinessException(INVALID_LINK);
        }

        token.setUsedAt(now);
        tokenRepository.save(token);
    }

}
