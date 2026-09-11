package com.devmetrics.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = ".*[A-Za-z].*", message = "a senha precisa de ao menos uma letra")
        @Pattern(regexp = ".*\\d.*", message = "a senha precisa de ao menos um numero")
        String newPassword
) {
}
