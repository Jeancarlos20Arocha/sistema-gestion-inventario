package com.sgm.shared.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VerifyAnswersRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 50, message = "El usuario no puede superar 50 caracteres")
        String username,

        @NotEmpty(message = "Debe responder las 3 preguntas de seguridad")
        @Size(min = 3, max = 3, message = "Deben ser exactamente 3 preguntas")
        List<@Valid SecurityAnswerRequest> answers) {
}