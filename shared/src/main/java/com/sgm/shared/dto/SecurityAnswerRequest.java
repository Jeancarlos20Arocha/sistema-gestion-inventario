package com.sgm.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Respuesta a una de las preguntas de seguridad estándar. La pregunta se
 * determina por la posición (índice) dentro de la lista, ya que son fijas.
 */
public record SecurityAnswerRequest(
        @NotBlank(message = "La respuesta es obligatoria")
        @Size(min = 2, max = 120, message = "La respuesta debe tener entre 2 y 120 caracteres")
        String answer) {
}
