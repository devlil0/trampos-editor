package com.devlil0.tramposeditor.dto;

public record AuthResponseDTO(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponseDTO user
) {
}
