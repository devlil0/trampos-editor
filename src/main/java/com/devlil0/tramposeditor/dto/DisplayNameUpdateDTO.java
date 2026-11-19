package com.devlil0.tramposeditor.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

public record DisplayNameUpdateDTO(@NotBlank @CodePointLength(max = 80) String displayName) {
    public DisplayNameUpdateDTO {
        displayName = displayName == null ? null : displayName.strip();
    }
}
