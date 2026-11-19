package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.AuthExchangeCodeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface IAuthExchangeCodeRepository extends JpaRepository<AuthExchangeCodeEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthExchangeCodeEntity> findLockedByCodeHash(String codeHash);
}
