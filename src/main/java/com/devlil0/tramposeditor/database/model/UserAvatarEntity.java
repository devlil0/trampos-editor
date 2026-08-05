package com.devlil0.tramposeditor.database.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A foto de perfil, em tabela própria.
 * <p>
 * Separada de {@link UserEntity} de propósito: {@code bytea} grande na tabela principal
 * empurra a linha pro TOAST e encarece toda consulta que só quer nome e e-mail — que é
 * quase todas. Aqui os bytes só são lidos quando alguém pede a imagem.
 */
@Entity
@Table(name = "user_avatars")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UserAvatarEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    /**
     * Sem {@code @Lob} de propósito. No Postgres, {@code @Lob byte[]} não vira
     * {@code bytea}: o Hibernate mapeia pra {@code oid}, que é o Large Object — outra
     * mecânica, com tabela do sistema à parte e handle que precisa de transação aberta pra
     * ser lido. {@code byte[]} puro é {@code bytea}, que é o que a V14 cria e o que serve
     * pra um arquivo de ~20KB.
     */
    @Column(nullable = false)
    private byte[] bytes;

    /**
     * Muda a cada upload e entra na URL da imagem. É o que permite servir com
     * {@code Cache-Control} longo sem a foto velha grudar no browser depois de trocada.
     */
    @Column(nullable = false)
    private UUID version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
