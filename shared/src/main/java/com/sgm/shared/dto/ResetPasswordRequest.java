package com.sgm.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.sgm.shared.constant.SecurityConstants.PASSWORD_PATTERN;

public record ResetPasswordRequest(
        @NotBlank(message = "El token de restablecimiento es obligatorio")
        String resetToken,

        @NotBlank(message = "La nueva contrase\u00f1a es obligatoria")
        @Size(min = 8, max = 100, message = "La contrase\u00f1a debe tener al menos 8 caracteres")
        @Pattern(regexp = PASSWORD_PATTERN, message = "La contrase\u00f1a debe tener may\u00fasculas, min\u00fasculas, n\u00fameros y s\u00edmbolos")
        String newPassword,

        @NotBlank(message = "Confirme la nueva contrase\u00f1a")
        String confirmPassword) {

    public boolean passwordsMatch() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}