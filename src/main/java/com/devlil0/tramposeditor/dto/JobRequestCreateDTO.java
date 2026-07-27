package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.EditorNiche;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

/** O que o cliente descreve ao postar um pedido. Sem lance: não há campo de proposta. */
public record JobRequestCreateDTO(

        @NotBlank
        @Length(max = 120)
        String title,

        @NotBlank
        @Length(max = 2000)
        String description,

        @NotNull
        EditorNiche niche,

        @PositiveOrZero
        Integer budgetCents,

        @PositiveOrZero
        Integer deadlineDays,

        @URL
        @Length(max = 500)
        String referenceUrl
) {
}
