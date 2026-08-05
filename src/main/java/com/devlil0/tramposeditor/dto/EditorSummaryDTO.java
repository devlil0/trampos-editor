package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.EditorStyle;
import com.devlil0.tramposeditor.enums.EditorTool;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Card da vitrine — versão enxuta do perfil pra listagem. `coverVideoUrl` é o
 * primeiro item do portfólio, pra o card ter uma prévia de vídeo.
 */
public record EditorSummaryDTO(
        UUID id,
        /** Histórico: o endereço do perfil é o {@code username}, não isto. */
        String slug,
        /** Onde o perfil mora hoje — {@code /{username}}. */
        String username,
        /** Versão da foto; nulo quando não há. Entra na URL da imagem. */
        UUID avatarVersion,
        String displayName,
        String headline,
        Set<EditorNiche> niches,
        Set<EditorTool> tools,
        Set<EditorStyle> styles,
        Integer priceMinCents,
        Integer turnaroundDays,
        boolean available,
        BigDecimal ratingAvg,
        int ratingCount,
        String coverVideoUrl
) {
}
