package com.sgm.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import com.sgm.backend.entity.Item;
import com.sgm.backend.entity.MachineTerminal;
import com.sgm.backend.entity.StockMovement;
import com.sgm.backend.entity.User;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.ItemRepository;
import com.sgm.backend.repository.StockMovementRepository;
import com.sgm.backend.repository.UserRepository;
import com.sgm.shared.dto.StockMovementDTO;
import com.sgm.shared.enums.MovementType;

/**
 * Pruebas unitarias de los ajustes manuales de stock.
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @NonNull
    private static final Long ID_ITEM = Objects.requireNonNull(Long.valueOf(9L));
    @NonNull
    private static final Long ID_OPERADOR = Objects.requireNonNull(Long.valueOf(5L));

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MachineService machineService;

    @InjectMocks
    private StockService stockService;

    private Item cafe;

    @BeforeEach
    void preparar() {
        cafe = new Item();
        cafe.setId(ID_ITEM);
        cafe.setBarcode("750100");
        cafe.setName("Caf\u00e9");
        cafe.setStock(new BigDecimal("5"));
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

    @Test
    @DisplayName("Un ajuste de monto cero devuelve 400")
    void ajusteConMontoCeroLanzaBadRequest() {
        ApiException ex = catchThrowableOfType(() -> stockService.adjust(ID_ITEM, BigDecimal.ZERO,
                "Reconteo", "POS-1", ID_OPERADOR), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("El ajuste debe ser distinto de cero");
        verifyNoInteractions(itemRepository);
    }

    @Test
    @DisplayName("Un ajuste nulo devuelve 400")
    void ajusteConMontoNuloLanzaBadRequest() {
        ApiException ex = catchThrowableOfType(() -> stockService.adjust(ID_ITEM, null,
                "Reconteo", "POS-1", ID_OPERADOR), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("El ajuste debe ser distinto de cero");
        verifyNoInteractions(itemRepository);
    }

    @Test
    @DisplayName("Un ajuste de un artículo inexistente devuelve 404")
    void ajusteDeArticuloInexistenteLanzaNotFound() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(() -> stockService.adjust(ID_ITEM, new BigDecimal("3"),
                "Reconteo", "POS-1", ID_OPERADOR), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Art\u00edculo no encontrado");
    }

    @Test
    @DisplayName("Un ajuste que deja el stock negativo devuelve 409 sin persistir")
    @SuppressWarnings("null")
    void ajusteNegativoExcesivoLanzaConflict() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(cafe));

        ApiException ex = catchThrowableOfType(() -> stockService.adjust(ID_ITEM, new BigDecimal("-10"),
                "Merma", "POS-1", ID_OPERADOR), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("negativo");
        assertThat(cafe.getStock()).isEqualByComparingTo("5");
        verify(itemRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un ajuste positivo incrementa el stock y registra el movimiento")
    @SuppressWarnings("null")
    void ajustePositivoIncrementaStockYRegistraMovimiento() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(cafe));
        when(userRepository.findById(ID_OPERADOR)).thenReturn(Optional.of(operador()));
        when(machineService.registerIfAbsent("POS-1"))
                .thenReturn(new MachineTerminal("POS-1", "Terminal POS-1"));
        when(movementRepository.save(any(StockMovement.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        StockMovementDTO dto = stockService.adjust(ID_ITEM, new BigDecimal("3"), "Reconteo", "POS-1", ID_OPERADOR);

        assertThat(cafe.getStock()).isEqualByComparingTo("8");
        assertThat(dto.itemId()).isEqualTo(ID_ITEM);
        assertThat(dto.movementType()).isEqualTo(MovementType.AJUSTE);
        assertThat(dto.quantity()).isEqualByComparingTo("3");
        assertThat(dto.note()).isEqualTo("Reconteo");
        assertThat(dto.username()).isEqualTo("operador");
        verify(itemRepository).save(Objects.requireNonNull(cafe));
    }

    @Test
    @DisplayName("Un ajuste negativo válido decrementa el stock con cantidad positiva")
    @SuppressWarnings("null")
    void ajusteNegativoValidoDecrementaStock() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(cafe));
        when(userRepository.findById(ID_OPERADOR)).thenReturn(Optional.of(operador()));
        when(machineService.registerIfAbsent("POS-1"))
                .thenReturn(new MachineTerminal("POS-1", "Terminal POS-1"));
        when(movementRepository.save(any(StockMovement.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        StockMovementDTO dto = stockService.adjust(ID_ITEM, new BigDecimal("-2"), "Merma", "POS-1", ID_OPERADOR);

        assertThat(cafe.getStock()).isEqualByComparingTo("3");
        assertThat(dto.movementType()).isEqualTo(MovementType.AJUSTE);
        assertThat(dto.quantity()).isEqualByComparingTo("2");
        verify(itemRepository).save(Objects.requireNonNull(cafe));
    }

    @Test
    @DisplayName("Un ajuste con operador inexistente devuelve 404")
    @SuppressWarnings("null")
    void ajusteConOperadorInexistenteLanzaNotFound() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(cafe));
        when(userRepository.findById(ID_OPERADOR)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(() -> stockService.adjust(ID_ITEM, new BigDecimal("3"),
                "Reconteo", "POS-1", ID_OPERADOR), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Usuario no encontrado");
        verify(movementRepository, never()).save(any());
    }
}
