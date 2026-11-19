package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.repository.IPasswordResetTokenRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.PasswordChangeDTO;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.ForbiddenException;
import com.devlil0.tramposeditor.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordChangeService {
    private static final String INVALID_SESSION = "Sessão inválida. Entre novamente.";

    private final IUserRepository users;
    private final IPasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthRateLimiter rateLimiter;

    @Transactional
    public void change(UUID userId, long sessionVersion, PasswordChangeDTO dto, String origin) {
        rateLimiter.checkPasswordChange(userId, origin);

        // A mesma ordem do reset: conta primeiro, tokens depois.
        var user = users.findLockedById(userId)
                .filter(account -> account.isActive() && account.getSessionVersion() == sessionVersion)
                .orElseThrow(() -> new UnauthorizedException(INVALID_SESSION));

        if (user.getPasswordHash() == null) {
            throw new BusinessException("Esta conta ainda não tem senha. Use a recuperação de senha para definir a primeira.");
        }
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPasswordHash())) {
            throw new ForbiddenException("Senha atual incorreta.");
        }

        String hash = passwordEncoder.encode(dto.newPassword());
        if (users.updatePasswordAndRevokeSessions(userId, hash, LocalDateTime.now()) == 0) {
            throw new UnauthorizedException(INVALID_SESSION);
        }
        resetTokens.deleteByUserId(userId);
    }
}
