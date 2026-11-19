package com.devlil0.tramposeditor.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;
import org.hibernate.validator.constraints.Length;

import java.nio.charset.StandardCharsets;

public record PasswordChangeDTO(
        @NotBlank @Length(max = 72) String currentPassword,
        @NotBlank @CodePointLength(min = 8, max = 72) String newPassword
) {
    @AssertTrue(message = "A nova senha deve ter no máximo 72 bytes em UTF-8.")
    @JsonIgnore
    public boolean isNewPasswordWithinBcryptLimit() {
        return newPassword == null || newPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override
    public String toString() {
        return "PasswordChangeDTO";
    }
}
