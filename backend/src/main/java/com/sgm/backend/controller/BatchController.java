package com.sgm.backend.controller;

import com.sgm.backend.security.UserPrincipal;
import com.sgm.backend.service.BatchService;
import com.sgm.shared.dto.ApiResponse;
import com.sgm.shared.dto.BatchDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list() {
        List<BatchDTO> batches = batchService.findAll();
        return ResponseEntity.ok(ApiResponse.ok(batches, batches.size() + " lote(s)"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> register(
            @Valid @RequestBody BatchDTO.CreateBatchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        batchService.register(request, principal.machineId(), principal.userId()),
                        "Lote registrado y stock actualizado"));
    }
}