package com.sgm.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryDTO(
        Long id,
        String name,
        String description,
        boolean active) {

    public record CreateCategoryRequest(
            @NotBlank(message = "El nombre de la categor\u00eda es obligatorio")
            @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
            String name,

            @Size(max = 255, message = "La descripci\u00f3n no puede superar 255 caracteres")
            String description) {
    }

    public record UpdateCategoryRequest(
            @NotNull(message = "El identificador de la categor\u00eda es obligatorio")
            Long id,
            @NotBlank(message = "El nombre de la categor\u00eda es obligatorio")
            @Size(min = 2, max = 80, message = "El nombre debe tener entre 2 y 80 caracteres")
            String name,
            @Size(max = 255, message = "La descripci\u00f3n no puede superar 255 caracteres")
            String description,
            @NotNull(message = "El estado es obligatorio")
            Boolean active) {
    }
}