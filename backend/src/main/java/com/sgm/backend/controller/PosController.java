package com.sgm.backend.controller;

import com.sgm.backend.security.UserPrincipal;
import com.sgm.backend.service.InventoryService;
import com.sgm.backend.service.PosService;
import com.sgm.shared.dto.ApiResponse;
import com.sgm.shared.dto.SaleDTO;
import com.sgm.shared.dto.SaleRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pos")
public class PosController {

    private final PosService posService;
    private final InventoryService inventoryService;

    public PosController(PosService posService, InventoryService inventoryService) {
        this.posService = posService;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/items/{barcode}")
    public ResponseEntity<ApiResponse> consultStock(@PathVariable String barcode) {
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.findByBarcode(barcode), "Consulta de stock"));
    }

    @PostMapping("/sales")
    public ResponseEntity<ApiResponse> sell(@Valid @RequestBody SaleRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        SaleDTO sale = posService.process(request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(sale, "Venta registrada #" + sale.id()));
    }
}