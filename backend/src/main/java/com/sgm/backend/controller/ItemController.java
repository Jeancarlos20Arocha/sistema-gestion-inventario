package com.sgm.backend.controller;

import com.sgm.backend.service.InventoryService;
import com.sgm.shared.dto.ApiResponse;
import com.sgm.shared.dto.ItemDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/items")
public class ItemController {

    private final InventoryService inventoryService;

    public ItemController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list(@RequestParam(defaultValue = "false") boolean soloActivos) {
        List<ItemDTO> items = inventoryService.listItems(soloActivos);
        return ResponseEntity.ok(ApiResponse.ok(items, items.size() + " art\u00edculo(s)"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody ItemDTO.CreateItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(inventoryService.createItem(request), "Art\u00edculo creado"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse> update(@Valid @RequestBody ItemDTO.UpdateItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.updateItem(request), "Art\u00edculo actualizado"));
    }
}