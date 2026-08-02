package com.devlil0.tramposeditor.database.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Conversa entre quem contrata e quem edita — a caixa de entrada única do produto
 * (ESBOCO §7). Nasce por uma de duas portas:
 * <ul>
 *   <li><b>Vitrine:</b> o cliente clica em "Falar com" no perfil ({@code jobRequest} nulo);</li>
 *   <li><b>Pedido:</b> o cliente aceita o interesse de um editor ({@code jobRequest} preenchido).</li>
 * </ul>
 * As duas terminam aqui de propósito: manter dois fluxos separados obrigaria o usuário
 * a procurar a mesma conversa em dois lugares.
 */
@Entity
@Table(name = "conversations")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Lado que contrata. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_user_id", nullable = false)
    private UserEntity client;

    /** Lado que edita. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "editor_user_id", nullable = false)
    private UserEntity editor;

    /** Pedido que originou a conversa, quando ela veio pela porta do feed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_request_id")
    private JobRequestEntity jobRequest;

    /**
     * Espelha o {@code createdAt} da última mensagem. Redundante de propósito: sem ele,
     * ordenar a caixa de entrada exigiria agregar `messages` a cada carregamento.
     */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "client_read_at")
    private LocalDateTime clientReadAt;

    @Column(name = "editor_read_at")
    private LocalDateTime editorReadAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<MessageEntity> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void addMessage(MessageEntity message){
        message.setConversation(this);
        messages.add(message);
        lastMessageAt = message.getCreatedAt() != null ? message.getCreatedAt() : LocalDateTime.now();
    }

    /** Participa quem é uma das duas pontas — ninguém mais lê nem escreve. */
    public boolean isParticipant(UUID userId){
        return client.getId().equals(userId) || editor.getId().equals(userId);
    }

    public boolean isClient(UUID userId){
        return client.getId().equals(userId);
    }

    /** O outro lado, do ponto de vista de quem está olhando. */
    public UserEntity counterpartOf(UUID userId){
        return isClient(userId) ? editor : client;
    }

    public LocalDateTime readAtFor(UUID userId){
        return isClient(userId) ? clientReadAt : editorReadAt;
    }

    public void markReadBy(UUID userId, LocalDateTime when){
        if (isClient(userId)) clientReadAt = when;
        else editorReadAt = when;
    }

    /**
     * Há mensagem que este usuário ainda não viu. Comparar dois instantes basta —
     * recibo por mensagem seria caro e não muda o que a interface mostra (um ponto).
     */
    public boolean hasUnreadFor(UUID userId){
        if (lastMessageAt == null) return false;
        LocalDateTime readAt = readAtFor(userId);
        return readAt == null || readAt.isBefore(lastMessageAt);
    }
}
