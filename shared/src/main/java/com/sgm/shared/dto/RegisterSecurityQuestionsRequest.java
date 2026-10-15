package com.sgm.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición para registrar las respuestas de las 3 preguntas de seguridad
 * estándar del sistema. Las preguntas son fijas y obligatorias, por lo que
 * el cliente solo envía las respuestas.
 */
public record RegisterSecurityQuestionsRequest(
        @NotBlank(message = "La respuesta 1 es obligatoria")
        @Size(min = 2, max = 120, message = "La respuesta 1 debe tener entre 2 y 120 caracteres")
        String answer1,

        @NotBlank(message = "La respuesta 2 es obligatoria")
        @Size(min = 2, max = 120, message = "La respuesta 2 debe tener entre 2 y 120 caracteres")
        String answer2,

        @NotBlank(message = "La respuesta 3 es obligatoria")
        @Size(min = 2, max = 120, message = "La respuesta 3 debe tener entre 2 y 120 caracteres")
        String answer3) {

    public String answer(int index) {
        return switch (index) {
            case 0 -> answer1;
            case 1 -> answer2;
            case 2 -> answer3;
            default -> throw new IndexOutOfBoundsException("Solo se soportan 3 respuestas");
        };
    }

    public boolean allAnswered() {
        return answer1 != null && !answer1.isBlank()
                && answer2 != null && !answer2.isBlank()
                && answer3 != null && !answer3.isBlank();
    }
}
