package com.sgm.shared.dto;

import com.sgm.shared.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockMovementDTO(
        Long id,
        Long itemId,
        String itemName,
        String itemBarcode,
        Long batchId,
        String machineId,
        String username,
        MovementType movementType,
        BigDecimal quantity,
        String note,
        LocalDateTime createdAt) {
}