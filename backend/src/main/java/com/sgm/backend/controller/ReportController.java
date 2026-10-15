package com.sgm.backend.controller;

import com.sgm.backend.service.ReportService;
import com.sgm.shared.dto.ApiResponse;
import com.sgm.shared.dto.ReportDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/daily/{date}")
    public ResponseEntity<ApiResponse> daily(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ReportDTO report = reportService.dailyReport(date);
        return ResponseEntity.ok(ApiResponse.ok(report, "Reporte diario"));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse> lowStock() {
        var alerta = reportService.stockAlert();
        return ResponseEntity.ok(ApiResponse.ok(alerta, alerta.size() + " art\u00edculo(s) con stock m\u00ednimo"));
    }
}