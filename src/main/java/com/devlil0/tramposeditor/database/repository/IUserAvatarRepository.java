package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.UserAvatarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface IUserAvatarRepository extends JpaRepository<UserAvatarEntity, UUID> {

    /**
     * Os bytes da foto de quem tem este username — o caminho que serve a imagem.
     * <p>
     * Uma consulta só, por username, em vez de resolver o usuário antes: servir avatar é
     * a requisição mais repetida de uma tela de listagem, e não vale duas idas ao banco.
     */
    @Query("""
            select a from UserAvatarEntity a
            where a.user.username = :username and a.user.active = true
            """)
    Optional<UserAvatarEntity> findByUsername(String username);
}
