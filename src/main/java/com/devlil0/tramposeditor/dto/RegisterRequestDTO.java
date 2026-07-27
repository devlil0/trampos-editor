package com.devlil0.tramposeditor.dto;

import com.devlil0.tramposeditor.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.Length;

import java.util.Set;

public record RegisterRequestDTO(

        @NotBlank
        @Email
        @Length(max = 180)
        String email,

        @NotBlank
        @Length(min = 8, max = 72)
        String password,

        @NotBlank
        @Length(max = 80)
        String displayName,

        @NotEmpty
        Set<UserRole> roles
) {
}
