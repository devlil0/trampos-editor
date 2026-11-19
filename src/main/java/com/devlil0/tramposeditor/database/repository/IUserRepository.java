package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserEntity> findLockedByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserEntity> findLockedById(UUID id);

    boolean existsByEmail(String email);

    /** O caminho de toda página de perfil — `/{username}` resolve por aqui. */
    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query(value = """
            select case
                when exists (
                    select 1 from users u
                    where u.username = :username and u.id is distinct from cast(:ownerId as uuid)
                ) then 'IN_USE'
                when exists (
                    select 1 from pending_signups p
                    where p.username = :username and p.expires_at > :now
                        and p.email is distinct from cast(:registrationEmail as text)
                ) then 'RESERVED'
                else 'AVAILABLE'
            end
            """, nativeQuery = true)
    String findUsernameAvailability(@Param("username") String username,
                                    @Param("ownerId") UUID ownerId,
                                    @Param("registrationEmail") String registrationEmail,
                                    @Param("now") LocalDateTime now);

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
