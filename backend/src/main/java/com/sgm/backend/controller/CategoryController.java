package com.sgm.backend.controller;

import com.sgm.backend.service.InventoryService;
import com.sgm.shared.dto.ApiResponse;
import com.sgm.shared.dto.CategoryDTO;
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
@RequestMapping("/api/admin/categories")
public class CategoryController {

    private final InventoryService inventoryService;

    public CategoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list(@RequestParam(defaultValue = "false") boolean soloActivas) {
        List<CategoryDTO> categories = inventoryService.listCategories(soloActivas);
        return ResponseEntity.ok(ApiResponse.ok(categories, categories.size() + " categor\u00eda(s)"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody CategoryDTO.CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(inventoryService.createCategory(request), "Categor\u00eda creada"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse> update(@Valid @RequestBody CategoryDTO.UpdateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.updateCategory(request), "Categor\u00eda actualizada"));
    }
}