package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.AuthExchangeCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IAuthExchangeCodeRepository extends JpaRepository<AuthExchangeCodeEntity, UUID> {

    Optional<AuthExchangeCodeEntity> findByCodeHash(String codeHash);
}
