package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.UserIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IUserIdentityRepository extends JpaRepository<UserIdentityEntity, UUID> {

    Optional<UserIdentityEntity> findByProviderAndSubject(String provider, String subject);

    boolean existsByUserIdAndProvider(UUID userId, String provider);
}
