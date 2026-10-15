package com.sgm.backend.controller;

import com.sgm.backend.security.UserPrincipal;
import com.sgm.backend.service.ReportService;
import com.sgm.backend.service.StockService;
import com.sgm.shared.dto.ApiResponse;
import com.sgm.shared.dto.StockMovementDTO;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/stock")
public class StockController {

    private final StockService stockService;
    private final ReportService reportService;

    public StockController(StockService stockService, ReportService reportService) {
        this.stockService = stockService;
        this.reportService = reportService;
    }

    public record AdjustRequest(
            @NotNull(message = "El art\u00edculo es obligatorio") Long itemId,
            @DecimalMin(value = "-9999999999", message = "Ajuste inv\u00e1lido") BigDecimal delta,
            String note) {
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse> adjust(@RequestBody AdjustRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        StockMovementDTO movement = stockService.adjust(
                request.itemId(), request.delta(), request.note(),
                principal.machineId(), principal.userId());
        return ResponseEntity.ok(ApiResponse.ok(movement, "Ajuste de stock registrado"));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse> movements(@RequestParam(required = false) Long itemId,
                                                 @RequestParam(required = false) LocalDate desde,
                                                 @RequestParam(required = false) LocalDate hasta) {
        List<StockMovementDTO> movimientos = reportService.movements(itemId, desde, hasta);
        return ResponseEntity.ok(ApiResponse.ok(movimientos, movimientos.size() + " movimiento(s)"));
    }
}