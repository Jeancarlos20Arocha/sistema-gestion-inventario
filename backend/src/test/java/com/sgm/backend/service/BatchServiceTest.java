package com.sgm.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.sgm.backend.entity.Batch;
import com.sgm.backend.entity.Item;
import com.sgm.backend.entity.MachineTerminal;
import com.sgm.backend.entity.StockMovement;
import com.sgm.backend.entity.User;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.BatchRepository;
import com.sgm.backend.repository.ItemRepository;
import com.sgm.backend.repository.StockMovementRepository;
import com.sgm.backend.repository.UserRepository;
import com.sgm.shared.dto.BatchDTO;
import com.sgm.shared.enums.MovementType;

/**
 * Pruebas unitarias del registro de lotes y su impacto en el stock.
 */
@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @NonNull
    private static final Long ID_ITEM = Objects.requireNonNull(Long.valueOf(9L));
    @NonNull
    private static final Long ID_OPERADOR = Objects.requireNonNull(Long.valueOf(5L));

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MachineService machineService;

    @InjectMocks
    private BatchService batchService;

    private Item cafe;

    @BeforeEach
    void preparar() {
        cafe = new Item();
        cafe.setId(ID_ITEM);
        cafe.setBarcode("750100");
        cafe.setName("Caf\u00e9");
        cafe.setStock(new BigDecimal("10"));
        cafe.setActive(true);
    }

    private User operador() {
        User operador = new User();
        operador.setId(ID_OPERADOR);
        operador.setUsername("operador");
        operador.setPassword("hash");
        operador.setFullName("Operador Uno");
        operador.setEmail("operador@sgm.com");
        return operador;
    }

    private BatchDTO.CreateBatchRequest solicitud() {
        return new BatchDTO.CreateBatchRequest(ID_ITEM, "L-001 ", "Proveedor X",
                new BigDecimal("25"), new BigDecimal("0.80"), LocalDate.of(2027, 1, 15));
    }

    @Test
    @DisplayName("Registrar un lote de un artículo inexistente devuelve 404")
    void registrarLoteDeArticuloInexistenteLanzaNotFound() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> batchService.register(solicitud(), "POS-1", ID_OPERADOR), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Art\u00edculo no encontrado");
    }

    @Test
    @DisplayName("Registrar un lote de un artículo deshabilitado devuelve 409")
    @SuppressWarnings("null")
    void registrarLoteDeArticuloDeshabilitadoLanzaConflict() {
        cafe.setActive(false);
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(cafe));

        ApiException ex = catchThrowableOfType(
                () -> batchService.register(solicitud(), "POS-1", ID_OPERADOR), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("El art\u00edculo est\u00e1 deshabilitado");
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registrar un lote duplicado para el mismo artículo devuelve 409")
    @SuppressWarnings("null")
    void registrarLoteDuplicadoLanzaConflict() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(cafe));
        doReturn(Boolean.TRUE).when(batchRepository).existsByItemIdAndBatchCode(ID_ITEM, "L-001");

        ApiException ex = catchThrowableOfType(
                () -> batchService.register(solicitud(), "POS-1", ID_OPERADOR), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("El lote proporcionado ya existe para este art\u00edculo");
        verify(batchRepository, never()).save(any());
        assertThat(cafe.getStock()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Registrar un lote exitoso incrementa el stock y registra la entrada")
    @SuppressWarnings("null")
    void registrarLoteExitosoIncrementaStockYRegistraEntrada() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(cafe));
        doReturn(Boolean.FALSE).when(batchRepository).existsByItemIdAndBatchCode(ID_ITEM, "L-001");
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocacion -> {
            Batch lote = invocacion.getArgument(0);
            lote.setId(Long.valueOf(55L));
            return lote;
        });
        when(machineService.registerIfAbsent("POS-1"))
                .thenReturn(new MachineTerminal("POS-1", "Terminal POS-1"));
        when(userRepository.findById(ID_OPERADOR)).thenReturn(Optional.of(operador()));

        BatchDTO dto = batchService.register(solicitud(), "POS-1", ID_OPERADOR);

        assertThat(dto.id()).isEqualTo(Long.valueOf(55L));
        assertThat(dto.itemId()).isEqualTo(ID_ITEM);
        assertThat(dto.itemName()).isEqualTo("Caf\u00e9");
        assertThat(dto.batchCode()).isEqualTo("L-001");
        assertThat(dto.quantity()).isEqualByComparingTo("25");
        assertThat(cafe.getStock()).isEqualByComparingTo("35");
        verify(itemRepository).save(Objects.requireNonNull(cafe));

        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(movementCaptor.capture());
        StockMovement movement = movementCaptor.getValue();
        assertThat(movement.getMovementType()).isEqualTo(MovementType.ENTRADA);
        assertThat(movement.getQuantity()).isEqualByComparingTo("25");
        assertThat(movement.getNote()).isEqualTo("Ingreso de lote L-001");
        assertThat(movement.getUser().getUsername()).isEqualTo("operador");
    }
}
