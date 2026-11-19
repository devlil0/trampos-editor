package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface IPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    @Query("select t.user.id from PasswordResetTokenEntity t where t.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(String tokenHash);

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    /** Pedir de novo invalida o anterior: um link vivo por conta. */
    void deleteByUserId(UUID userId);
}
