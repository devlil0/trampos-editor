package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    /** O caminho de toda página de perfil — `/{username}` resolve por aqui. */
    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByIdAndActiveTrueAndSessionVersion(UUID id, long sessionVersion);

    @Modifying
    @Query("""
            update UserEntity u
            set u.passwordHash = :passwordHash,
                u.sessionVersion = u.sessionVersion + 1,
                u.updatedAt = :now
            where u.id = :id and u.active = true
            """)
    int updatePasswordAndRevokeSessions(@Param("id") UUID id,
                                        @Param("passwordHash") String passwordHash,
                                        @Param("now") LocalDateTime now);
}
