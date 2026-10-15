package com.sgm.shared.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record SaleRequest(
        @NotEmpty(message = "La venta debe contener al menos un art\u00edculo")
        List<SaleItem> items,

        @NotNull(message = "El efectivo recibido es obligatorio")
        @PositiveOrZero(message = "El efectivo recibido no puede ser negativo")
        BigDecimal cashReceived,

        @NotBlank(message = "El identificador de m\u00e1quina es obligatorio")
        @Size(max = 64, message = "machine_id no puede superar 64 caracteres")
        String machineId) {

    public record SaleItem(
            @NotNull(message = "El art\u00edculo es obligatorio")
            Long itemId,

            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
            BigDecimal quantity) {
    }
}