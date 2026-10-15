package com.sgm.shared.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemDTO(
        Long id,
        String barcode,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        String unit,
        BigDecimal cost,
        BigDecimal price,
        BigDecimal minStock,
        BigDecimal stock,
        boolean active) {

    public record CreateItemRequest(
            @NotBlank(message = "El c\u00f3digo de barras es obligatorio")
            @Size(max = 60, message = "El c\u00f3digo de barras no puede superar 60 caracteres")
            String barcode,

            @NotBlank(message = "El nombre del art\u00edculo es obligatorio")
            @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
            String name,

            @Size(max = 255, message = "La descripci\u00f3n no puede superar 255 caracteres")
            String description,

            @NotNull(message = "La categor\u00eda es obligatoria")
            Long categoryId,

            @NotBlank(message = "La unidad es obligatoria")
            @Size(max = 20, message = "La unidad no puede superar 20 caracteres")
            String unit,

            @NotNull(message = "El costo es obligatorio")
            @DecimalMin(value = "0.01", message = "El costo debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "El costo tiene formato inv\u00e1lido")
            BigDecimal cost,

            @NotNull(message = "El precio de venta es obligatorio")
            @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "El precio tiene formato inv\u00e1lido")
            BigDecimal price,

            @NotNull(message = "El stock m\u00ednimo es obligatorio")
            @PositiveOrZero(message = "El stock m\u00ednimo no puede ser negativo")
            BigDecimal minStock) {
    }

    public record UpdateItemRequest(
            @NotNull(message = "El identificador del art\u00edculo es obligatorio")
            Long id,

            @NotBlank(message = "El c\u00f3digo de barras es obligatorio")
            @Size(max = 60, message = "El c\u00f3digo de barras no puede superar 60 caracteres")
            String barcode,

            @NotBlank(message = "El nombre del art\u00edculo es obligatorio")
            @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
            String name,

            @Size(max = 255, message = "La descripci\u00f3n no puede superar 255 caracteres")
            String description,

            @NotNull(message = "La categor\u00eda es obligatoria")
            Long categoryId,

            @NotBlank(message = "La unidad es obligatoria")
            @Size(max = 20, message = "La unidad no puede superar 20 caracteres")
            String unit,

            @NotNull(message = "El costo es obligatorio")
            @DecimalMin(value = "0.01", message = "El costo debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "El costo tiene formato inv\u00e1lido")
            BigDecimal cost,

            @NotNull(message = "El precio de venta es obligatorio")
            @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "El precio tiene formato inv\u00e1lido")
            BigDecimal price,

            @NotNull(message = "El stock m\u00ednimo es obligatorio")
            @PositiveOrZero(message = "El stock m\u00ednimo no puede ser negativo")
            BigDecimal minStock,

            @NotNull(message = "El estado es obligatorio")
            Boolean active) {
    }
}