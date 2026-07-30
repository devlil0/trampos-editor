package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.EditorStyle;
import com.devlil0.tramposeditor.enums.EditorTool;
import com.devlil0.tramposeditor.enums.Vertical;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Perfil completo — payload da página pública do editor (/editores/{slug}).
 */
public record EditorProfileDTO(
        UUID id,
        UUID userId,
        String slug,
        String displayName,
        /** Vem da conta (users.instagram), não do perfil — ver UserEntity. */
        String instagram,
        Vertical vertical,
        String headline,
        String bio,
        Set<EditorNiche> niches,
        Set<EditorTool> tools,
        Set<EditorStyle> styles,
        Integer priceMinCents,
        Integer priceMaxCents,
        Integer turnaroundDays,
        boolean available,
        BigDecimal ratingAvg,
        int ratingCount,
        List<PortfolioItemDTO> portfolio
) {
}
