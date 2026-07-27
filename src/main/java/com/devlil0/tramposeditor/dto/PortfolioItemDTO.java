package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.PortfolioProvider;

import java.util.UUID;

public record PortfolioItemDTO(
        UUID id,
        String title,
        PortfolioProvider provider,
        String videoUrl,
        int position
) {
}
