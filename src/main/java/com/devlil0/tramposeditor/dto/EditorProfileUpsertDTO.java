package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.EditorTool;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.Length;

import java.util.Set;

/**
 * Entrada do editor pra criar/atualizar o próprio perfil.
 * A vertical não vem daqui: no MVP é sempre VIDEO (definida no service).
 * O slug também não — é derivado do displayName e imutável depois de criado (SEO estável).
 */
public record EditorProfileUpsertDTO(

        @Length(max = 120)
        String headline,

        @Length(max = 2000)
        String bio,

        @NotEmpty
        Set<EditorNiche> niches,

        @NotEmpty
        Set<EditorTool> tools,

        @PositiveOrZero
        Integer priceMinCents,

        @PositiveOrZero
        Integer priceMaxCents,

        @PositiveOrZero
        Integer turnaroundDays,

        Boolean available
) {
}
