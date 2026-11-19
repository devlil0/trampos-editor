package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface IEmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationTokenEntity, UUID> {

    @Query("select t.user.id from EmailVerificationTokenEntity t where t.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(String tokenHash);

    Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);

    /** Reenviar invalida o anterior: um link vivo por conta. */
    void deleteByUserId(UUID userId);
}
