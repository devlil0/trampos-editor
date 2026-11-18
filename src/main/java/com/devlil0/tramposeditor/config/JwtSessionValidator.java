package com.devlil0.tramposeditor.config;

import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.service.JwtService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

final class JwtSessionValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2TokenValidatorResult INVALID_SESSION =
            OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid session", null));

    private final IUserRepository users;

    JwtSessionValidator(IUserRepository users) {
        this.users = users;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object version = jwt.getClaims().get(JwtService.SESSION_VERSION_CLAIM);
        if (!(version instanceof Integer || version instanceof Long) || jwt.getSubject() == null) {
            return INVALID_SESSION;
        }

        UUID id;
        try {
            id = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            return INVALID_SESSION;
        }
        return users.existsByIdAndActiveTrueAndSessionVersion(id, ((Number) version).longValue())
                ? OAuth2TokenValidatorResult.success()
                : INVALID_SESSION;
    }
}
