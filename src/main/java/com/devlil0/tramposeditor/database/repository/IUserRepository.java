package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    /** O caminho de toda página de perfil — `/{username}` resolve por aqui. */
    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
