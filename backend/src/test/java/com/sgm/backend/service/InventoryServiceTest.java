package com.sgm.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import com.sgm.backend.entity.Category;
import com.sgm.backend.entity.Item;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.CategoryRepository;
import com.sgm.backend.repository.ItemRepository;
import com.sgm.shared.dto.CategoryDTO;
import com.sgm.shared.dto.ItemDTO;

/**
 * Pruebas unitarias del inventario: categorías, artículos y alertas de stock.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @NonNull
    private static final Long ID_CATEGORIA = Objects.requireNonNull(Long.valueOf(3L));
    @NonNull
    private static final Long ID_ITEM = Objects.requireNonNull(Long.valueOf(9L));
    @NonNull
    private static final Long ID_OTRA_CATEGORIA = Objects.requireNonNull(Long.valueOf(4L));

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Category categoria;

    @BeforeEach
    void preparar() {
        categoria = new Category("L\u00e1cteos", "Productos l\u00e1cteos");
        categoria.setId(ID_CATEGORIA);
    }

    private Item articulo(Long id, String barcode, BigDecimal stock, BigDecimal minStock, boolean activo) {
        Item articulo = new Item();
        articulo.setId(id);
        articulo.setBarcode(barcode);
        articulo.setName("Art\u00edculo " + barcode);
        articulo.setCategory(categoria);
        articulo.setCost(new BigDecimal("1.00"));
        articulo.setPrice(new BigDecimal("2.50"));
        articulo.setMinStock(minStock);
        articulo.setStock(stock);
        articulo.setActive(activo);
        return articulo;
    }

    @Test
    @DisplayName("Crear una categoría con nombre duplicado devuelve 409")
    @SuppressWarnings("null")
    void crearCategoriaDuplicadaLanzaConflict() {
        when(categoryRepository.findByNameIgnoreCase("L\u00e1cteos")).thenReturn(Optional.of(categoria));

        ApiException ex = catchThrowableOfType(() -> inventoryService
                .createCategory(new CategoryDTO.CreateCategoryRequest("L\u00e1cteos", "Descripci\u00f3n")),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("Ya existe una categor\u00eda con ese nombre");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear una categoría exitosa recorta el nombre y devuelve el DTO")
    @SuppressWarnings("null")
    void crearCategoriaExitosaDevuelveDto() {
        when(categoryRepository.findByNameIgnoreCase("Bebidas")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocacion -> {
            Category guardada = invocacion.getArgument(0);
            guardada.setId(Long.valueOf(4L));
            return guardada;
        });

        CategoryDTO dto = inventoryService
                .createCategory(new CategoryDTO.CreateCategoryRequest(" Bebidas ", "Bebidas fr\u00edas"));

        ArgumentCaptor<Category> categoriaCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoriaCaptor.capture());
        assertThat(categoriaCaptor.getValue().getName()).isEqualTo("Bebidas");
        assertThat(dto.id()).isEqualTo(Long.valueOf(4L));
        assertThat(dto.name()).isEqualTo("Bebidas");
        assertThat(dto.active()).isTrue();
    }

    @Test
    @DisplayName("Actualizar una categoría al nombre de otra devuelve 409")
    @SuppressWarnings("null")
    void actualizarCategoriaConNombreDuplicadoLanzaConflict() {
        Category otra = new Category("Bebidas", "Otra");
        otra.setId(Long.valueOf(9L));
        when(categoryRepository.findById(ID_CATEGORIA)).thenReturn(Optional.of(categoria));
        when(categoryRepository.findByNameIgnoreCase("Bebidas")).thenReturn(Optional.of(otra));

        ApiException ex = catchThrowableOfType(() -> inventoryService.updateCategory(
                new CategoryDTO.UpdateCategoryRequest(ID_CATEGORIA, "Bebidas", "Cambio", Boolean.TRUE)),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("Ya existe otra categor\u00eda con ese nombre");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar una categoría exitosa cambia nombre y estado")
    @SuppressWarnings("null")
    void actualizarCategoriaExitosaCambiaEstado() {
        when(categoryRepository.findById(ID_CATEGORIA)).thenReturn(Optional.of(categoria));
        when(categoryRepository.findByNameIgnoreCase("Bebidas")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        CategoryDTO dto = inventoryService.updateCategory(
                new CategoryDTO.UpdateCategoryRequest(ID_CATEGORIA, "Bebidas", "Cambio", Boolean.FALSE));

        assertThat(dto.name()).isEqualTo("Bebidas");
        assertThat(dto.active()).isFalse();
        assertThat(categoria.isActive()).isFalse();
    }

    @Test
    @DisplayName("Crear un artículo con código de barras duplicado devuelve 409")
    @SuppressWarnings("null")
    void crearItemConBarcodeDuplicadoLanzaConflict() {
        doReturn(Boolean.TRUE).when(itemRepository).existsByBarcode("750100");

        ApiException ex = catchThrowableOfType(() -> inventoryService.createItem(new ItemDTO.CreateItemRequest(
                "750100", "Caf\u00e9", "Grano molido", ID_CATEGORIA, "UND",
                new BigDecimal("1.00"), new BigDecimal("2.50"), new BigDecimal("5"))), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("El c\u00f3digo de barras ya est\u00e1 registrado");
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear un artículo con categoría inexistente devuelve 404")
    void crearItemConCategoriaInexistenteLanzaNotFound() {
        doReturn(Boolean.FALSE).when(itemRepository).existsByBarcode("750100");
        when(categoryRepository.findById(ID_CATEGORIA)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(() -> inventoryService.createItem(new ItemDTO.CreateItemRequest(
                "750100", "Caf\u00e9", "Grano molido", ID_CATEGORIA, "UND",
                new BigDecimal("1.00"), new BigDecimal("2.50"), new BigDecimal("5"))), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Categor\u00eda no encontrada");
    }

    @Test
    @DisplayName("Crear un artículo sobre una categoría deshabilitada devuelve 409")
    @SuppressWarnings("null")
    void crearItemConCategoriaInactivaLanzaConflict() {
        categoria.setActive(false);
        doReturn(Boolean.FALSE).when(itemRepository).existsByBarcode("750100");
        when(categoryRepository.findById(ID_CATEGORIA)).thenReturn(Optional.of(categoria));

        ApiException ex = catchThrowableOfType(() -> inventoryService.createItem(new ItemDTO.CreateItemRequest(
                "750100", "Caf\u00e9", "Grano molido", ID_CATEGORIA, "UND",
                new BigDecimal("1.00"), new BigDecimal("2.50"), new BigDecimal("5"))), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("La categor\u00eda est\u00e1 deshabilitada");
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear un artículo exitoso devuelve el DTO con los datos normalizados")
    @SuppressWarnings("null")
    void crearItemExitosoDevuelveDto() {
        doReturn(Boolean.FALSE).when(itemRepository).existsByBarcode("750100");
        when(categoryRepository.findById(ID_CATEGORIA)).thenReturn(Optional.of(categoria));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocacion -> {
            Item guardado = invocacion.getArgument(0);
            guardado.setId(ID_ITEM);
            return guardado;
        });

        ItemDTO dto = inventoryService.createItem(new ItemDTO.CreateItemRequest("750100", "Caf\u00e9",
                "Grano molido", ID_CATEGORIA, "UND", new BigDecimal("1.00"), new BigDecimal("2.50"),
                new BigDecimal("5")));

        assertThat(dto.id()).isEqualTo(ID_ITEM);
        assertThat(dto.barcode()).isEqualTo("750100");
        assertThat(dto.categoryId()).isEqualTo(ID_CATEGORIA);
        assertThat(dto.categoryName()).isEqualTo("L\u00e1cteos");
        assertThat(dto.active()).isTrue();
        assertThat(dto.minStock()).isEqualByComparingTo("5");
        assertThat(dto.stock()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Actualizar un artículo al código de barras de otro devuelve 409")
    @SuppressWarnings("null")
    void actualizarItemConBarcodeDeOtroArticuloLanzaConflict() {
        Item existente = articulo(ID_ITEM, "750100", new BigDecimal("10"), new BigDecimal("5"), true);
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(existente));
        doReturn(Boolean.TRUE).when(itemRepository).existsByBarcodeAndIdNot("750200", ID_ITEM);

        ApiException ex = catchThrowableOfType(() -> inventoryService.updateItem(new ItemDTO.UpdateItemRequest(
                ID_ITEM, "750200", "Caf\u00e9 Premium", "Cambio", ID_CATEGORIA, "UND",
                new BigDecimal("1.00"), new BigDecimal("3.00"), new BigDecimal("5"), Boolean.TRUE)),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("El c\u00f3digo de barras ya est\u00e1 registrado en otro art\u00edculo");
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar un artículo exitosa cambia categoría y precio")
    @SuppressWarnings("null")
    void actualizarItemExitosoCambiaCategoriaYPrecio() {
        Item existente = articulo(ID_ITEM, "750100", new BigDecimal("10"), new BigDecimal("5"), true);
        Category otraCategoria = new Category("Bebidas", "Bebidas");
        otraCategoria.setId(Long.valueOf(4L));
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(existente));
        doReturn(Boolean.FALSE).when(itemRepository).existsByBarcodeAndIdNot("750100", ID_ITEM);
        when(categoryRepository.findById(ID_OTRA_CATEGORIA)).thenReturn(Optional.of(otraCategoria));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ItemDTO dto = inventoryService.updateItem(new ItemDTO.UpdateItemRequest(
                ID_ITEM, "750100", "Caf\u00e9 Premium", "Cambio", Long.valueOf(4L), "UND",
                new BigDecimal("1.00"), new BigDecimal("3.00"), new BigDecimal("5"), Boolean.FALSE));

        assertThat(dto.categoryId()).isEqualTo(Long.valueOf(4L));
        assertThat(dto.price()).isEqualByComparingTo("3.00");
        assertThat(dto.active()).isFalse();
    }

    @Test
    @DisplayName("Buscar por código de barras un artículo deshabilitado devuelve 404")
    void buscarPorBarcodeArticuloDeshabilitadoLanzaNotFound() {
        Item inactivo = articulo(ID_ITEM, "750100", new BigDecimal("10"), new BigDecimal("5"), false);
        when(itemRepository.findByBarcode("750100")).thenReturn(Optional.of(inactivo));

        ApiException ex = catchThrowableOfType(() -> inventoryService.findByBarcode("750100"), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("El art\u00edculo est\u00e1 deshabilitado");
    }

    @Test
    @DisplayName("Buscar por código de barras un artículo inexistente devuelve 404")
    void buscarPorBarcodeInexistenteLanzaNotFound() {
        when(itemRepository.findByBarcode("999999")).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(() -> inventoryService.findByBarcode("999999"), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Art\u00edculo no encontrado con ese c\u00f3digo de barras");
    }

    @Test
    @DisplayName("El reporte de stock bajo incluye los artículos en o bajo el mínimo")
    void stockBajoFiltraPorElMinimo() {
        Item bajo = articulo(Long.valueOf(11L), "750111", new BigDecimal("4"), new BigDecimal("5"), true);
        Item justo = articulo(Long.valueOf(12L), "750112", new BigDecimal("5"), new BigDecimal("5"), true);
        Item alto = articulo(Long.valueOf(13L), "750113", new BigDecimal("20"), new BigDecimal("5"), true);
        when(itemRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(bajo, justo, alto));

        List<ItemDTO> resultado = inventoryService.lowStock();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(ItemDTO::id)
                .containsExactlyInAnyOrder(Long.valueOf(11L), Long.valueOf(12L));
    }

    @Test
    @DisplayName("Listar categorías solo activas excluye las deshabilitadas")
    void listarSoloCategoriasActivas() {
        Category inactiva = new Category("Bebidas", "Bebidas");
        inactiva.setId(Long.valueOf(4L));
        inactiva.setActive(false);
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(categoria, inactiva));

        List<CategoryDTO> activas = inventoryService.listCategories(true);
        List<CategoryDTO> todas = inventoryService.listCategories(false);

        assertThat(activas).hasSize(1);
        assertThat(activas.get(0).name()).isEqualTo("L\u00e1cteos");
        assertThat(todas).hasSize(2);
    }
}
