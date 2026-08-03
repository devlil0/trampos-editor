package com.devlil0.tramposeditor.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewDTO(
        UUID id,
        String authorName,
        short rating,
        String comment,
        LocalDateTime createdAt
) {
}
