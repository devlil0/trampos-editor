package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String email,
        String displayName,
        /** Username do Instagram, sem "@". Null enquanto a pessoa não informar. */
        String instagram,
        Set<UserRole> roles,
        boolean active,
        LocalDateTime createdAt
) {
}
