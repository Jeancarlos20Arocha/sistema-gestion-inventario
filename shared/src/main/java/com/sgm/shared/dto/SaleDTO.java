package com.sgm.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleDTO(
        Long id,
        String username,
        String machineId,
        BigDecimal total,
        BigDecimal cashReceived,
        BigDecimal changeAmount,
        String status,
        LocalDateTime createdAt,
        List<SaleDetail> details) {

    public record SaleDetail(
            Long itemId,
            String itemName,
            String barcode,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal subtotal) {
    }
}