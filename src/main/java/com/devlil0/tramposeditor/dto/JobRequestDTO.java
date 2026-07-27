package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.JobRequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Pedido como o dono vê: com a lista de quem demonstrou interesse. */
public record JobRequestDTO(
        UUID id,
        String title,
        String description,
        EditorNiche niche,
        Integer budgetCents,
        Integer deadlineDays,
        String referenceUrl,
        JobRequestStatus status,
        List<JobInterestDTO> interests,
        LocalDateTime createdAt
) {
}
