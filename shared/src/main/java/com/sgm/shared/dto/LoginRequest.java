package com.sgm.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 50, message = "El usuario no puede superar 50 caracteres")
        String username,

        @NotBlank(message = "La contrase\u00f1a es obligatoria")
        @Size(min = 1, max = 100, message = "Contrase\u00f1a inv\u00e1lida")
        String password,

        @NotBlank(message = "El identificador de m\u00e1quina es obligatorio")
        @Size(max = 64, message = "machine_id no puede superar 64 caracteres")
        String machineId) {
}