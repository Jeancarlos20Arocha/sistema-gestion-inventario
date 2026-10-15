package com.sgm.shared.dto;

import com.sgm.shared.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

import static com.sgm.shared.constant.SecurityConstants.PASSWORD_PATTERN;

public record UserDTO(
        Long id,
        String username,
        String fullName,
        String email,
        Role role,
        boolean enabled,
        boolean accountLocked,
        int failedAttempts,
        LocalDateTime lockExpiresAt) {

    public record CreateUserRequest(
            @NotBlank(message = "El usuario es obligatorio")
            @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
            @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El usuario solo admite letras, n\u00fameros, punto, guion y guion bajo")
            String username,

            @NotBlank(message = "El nombre completo es obligatorio")
            @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
            String fullName,

            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "Correo electr\u00f3nico inv\u00e1lido")
            @Size(max = 120, message = "El correo no puede superar 120 caracteres")
            String email,

            @NotNull(message = "El rol es obligatorio")
            Role role,

            @NotBlank(message = "La contrase\u00f1a es obligatoria")
            @Size(min = 8, max = 100, message = "La contrase\u00f1a debe tener al menos 8 caracteres")
            @Pattern(regexp = PASSWORD_PATTERN, message = "La contrase\u00f1a requiere may\u00fasculas, min\u00fasculas, n\u00fameros y s\u00edmbolos")
            String password,

            @NotBlank(message = "Confirme la contrase\u00f1a")
            String confirmPassword,

            @Valid
            RegisterSecurityQuestionsRequest securityQuestions) {

        public boolean passwordsMatch() {
            return password != null && password.equals(confirmPassword);
        }
    }

    public record UnlockRequest(
            @NotNull(message = "El identificador del usuario es obligatorio")
            Long userId) {
    }

    public record ToggleEnabledRequest(
            @NotNull(message = "El estado es obligatorio")
            Boolean enabled) {
    }

    public record ApprovedQuestionDto(String question) {
    }

    public record SecurityQuestionsResponse(
            String username,
            java.util.List<ApprovedQuestionDto> questions,
            LocalDateTime requestedAt) {
    }
}