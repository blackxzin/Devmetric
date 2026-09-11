package com.devmetrics.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = ".*[A-Za-z].*", message = "a senha precisa de ao menos uma letra")
        @Pattern(regexp = ".*\\d.*", message = "a senha precisa de ao menos um numero")
        String password,
        @NotBlank @Size(min = 2, max = 80) String displayName,
        @Size(max = 64) String timezone
) {
}
