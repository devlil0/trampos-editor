package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    /** Pedir de novo invalida o anterior: um link vivo por conta. */
    void deleteByUserId(UUID userId);
}
