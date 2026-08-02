package com.devlil0.tramposeditor.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma mensagem na tela da conversa. {@code mine} vem pronto do servidor porque o
 * front não deve precisar do id do usuário logado só pra saber de que lado alinhar
 * o balão — o token não é lido no cliente (FUNDACAO §11.8).
 */
public record MessageDTO(
        UUID id,
        String senderDisplayName,
        boolean mine,
        String body,
        LocalDateTime createdAt
) {}
