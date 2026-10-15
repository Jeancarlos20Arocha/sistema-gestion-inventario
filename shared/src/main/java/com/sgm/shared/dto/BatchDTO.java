package com.sgm.shared.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchDTO(
        Long id,
        Long itemId,
        String itemName,
        String itemBarcode,
        String batchCode,
        String provider,
        BigDecimal quantity,
        BigDecimal cost,
        LocalDate expiryDate,
        LocalDate receivedDate) {

    public record CreateBatchRequest(
            @NotNull(message = "El art\u00edculo es obligatorio")
            Long itemId,

            @NotBlank(message = "El c\u00f3digo de lote es obligatorio")
            @Size(max = 60, message = "El c\u00f3digo de lote no puede superar 60 caracteres")
            String batchCode,

            @Size(max = 120, message = "El proveedor no puede superar 120 caracteres")
            String provider,

            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "Cantidad inv\u00e1lida")
            BigDecimal quantity,

            @NotNull(message = "El costo es obligatorio")
            @DecimalMin(value = "0.01", message = "El costo debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "Costo inv\u00e1lido")
            BigDecimal cost,

            LocalDate expiryDate) {
    }
}