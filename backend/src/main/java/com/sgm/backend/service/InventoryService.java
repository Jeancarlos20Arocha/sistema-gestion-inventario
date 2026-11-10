package com.sgm.backend.service;

import com.sgm.backend.entity.Category;
import com.sgm.backend.entity.Item;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.CategoryRepository;
import com.sgm.backend.repository.ItemRepository;
import com.sgm.shared.dto.CategoryDTO;
import com.sgm.shared.dto.ItemDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class InventoryService {

    private static final String CATEGORIA_NO_EXISTE = "Categor\u00eda no encontrada";
    private static final String ARTICULO_NO_EXISTE = "Art\u00edculo no encontrado";

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

    public InventoryService(CategoryRepository categoryRepository,
                            ItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public CategoryDTO createCategory(CategoryDTO.CreateCategoryRequest request) {
        if (categoryRepository.findByNameIgnoreCase(request.name().trim()).isPresent()) {
            throw ApiException.conflict("Ya existe una categor\u00eda con ese nombre");
        }
        Category category = categoryRepository.save(
                new Category(request.name().trim(), request.description()));
        return toDto(category);
    }

    @Transactional
    public CategoryDTO updateCategory(CategoryDTO.UpdateCategoryRequest request) {
        Long categoriaId = Objects.requireNonNull(request.id(), "El identificador de la categoría es obligatorio");
        Category category = categoryRepository.findById(categoriaId)
                .orElseThrow(() -> ApiException.notFound(CATEGORIA_NO_EXISTE));

        categoryRepository.findByNameIgnoreCase(request.name().trim()).ifPresent(existente -> {
            if (!existente.getId().equals(categoriaId)) {
                throw ApiException.conflict("Ya existe otra categor\u00eda con ese nombre");
            }
        });

        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setActive(Boolean.TRUE.equals(request.active()));
        return toDto(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> listCategories(boolean soloActivas) {
        List<Category> categorias = soloActivas
                ? categoryRepository.findAllByOrderByNameAsc().stream()
                        .filter(Category::isActive).toList()
                : categoryRepository.findAllByOrderByNameAsc();
        return categorias.stream().map(InventoryService::toDto).toList();
    }

    @Transactional
    public ItemDTO createItem(ItemDTO.CreateItemRequest request) {
        if (itemRepository.existsByBarcode(request.barcode().trim())) {
            throw ApiException.conflict("El c\u00f3digo de barras ya est\u00e1 registrado");
        }
        Category category = categoryRepository.findById(
                        Objects.requireNonNull(request.categoryId(), "La categoría del artículo es obligatoria"))
                .orElseThrow(() -> ApiException.notFound(CATEGORIA_NO_EXISTE));
        if (!category.isActive()) {
            throw ApiException.conflict("La categor\u00eda est\u00e1 deshabilitada");
        }

        Item item = new Item();
        item.setBarcode(request.barcode().trim());
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setCategory(category);
        item.setUnit(request.unit().trim());
        item.setCost(request.cost());
        item.setPrice(request.price());
        item.setMinStock(request.minStock());
        return toDto(itemRepository.save(item));
    }

    @Transactional
    public ItemDTO updateItem(ItemDTO.UpdateItemRequest request) {
        Item item = itemRepository.findById(
                        Objects.requireNonNull(request.id(), "El identificador del artículo es obligatorio"))
                .orElseThrow(() -> ApiException.notFound(ARTICULO_NO_EXISTE));

        if (itemRepository.existsByBarcodeAndIdNot(request.barcode().trim(), item.getId())) {
            throw ApiException.conflict("El c\u00f3digo de barras ya est\u00e1 registrado en otro art\u00edculo");
        }

        Category category = categoryRepository.findById(
                        Objects.requireNonNull(request.categoryId(), "La categoría del artículo es obligatoria"))
                .orElseThrow(() -> ApiException.notFound(CATEGORIA_NO_EXISTE));

        item.setBarcode(request.barcode().trim());
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setCategory(category);
        item.setUnit(request.unit().trim());
        item.setCost(request.cost());
        item.setPrice(request.price());
        item.setMinStock(request.minStock());
        item.setActive(Boolean.TRUE.equals(request.active()));
        return toDto(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<ItemDTO> listItems(boolean soloActivos) {
        List<Item> items = soloActivos
                ? itemRepository.findAllByActiveTrueOrderByNameAsc()
                : itemRepository.findAllByOrderByNameAsc();
        return items.stream().map(InventoryService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ItemDTO findByBarcode(String barcode) {
        Item item = itemRepository.findByBarcode(barcode.trim())
                .orElseThrow(() -> ApiException.notFound("Art\u00edculo no encontrado con ese c\u00f3digo de barras"));
        if (!item.isActive()) {
            throw ApiException.notFound("El art\u00edculo est\u00e1 deshabilitado");
        }
        return toDto(item);
    }

    @Transactional(readOnly = true)
    public List<ItemDTO> lowStock() {
        return itemRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(item -> item.getStock().compareTo(item.getMinStock()) <= 0)
                .map(InventoryService::toDto)
                .toList();
    }

    static CategoryDTO toDto(Category category) {
        return new CategoryDTO(category.getId(), category.getName(),
                category.getDescription(), category.isActive());
    }

    static ItemDTO toDto(Item item) {
        return new ItemDTO(
                item.getId(),
                item.getBarcode(),
                item.getName(),
                item.getDescription(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getUnit(),
                item.getCost(),
                item.getPrice(),
                item.getMinStock(),
                item.getStock(),
                item.isActive());
    }
}