package com.sgm.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportDTO(
        LocalDate date,
        int totalSales,
        BigDecimal totalRevenue,
        BigDecimal totalItemsSold,
        List<SaleDTO> sales) {

    public static ReportDTO empty(LocalDate date) {
        return new ReportDTO(date, 0, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }
}