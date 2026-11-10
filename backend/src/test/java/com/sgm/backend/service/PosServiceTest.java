package com.sgm.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.sgm.backend.entity.Item;
import com.sgm.backend.entity.MachineTerminal;
import com.sgm.backend.entity.Sale;
import com.sgm.backend.entity.StockMovement;
import com.sgm.backend.entity.User;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.ItemRepository;
import com.sgm.backend.repository.SaleRepository;
import com.sgm.backend.repository.StockMovementRepository;
import com.sgm.backend.repository.UserRepository;
import com.sgm.shared.dto.SaleDTO;
import com.sgm.shared.dto.SaleRequest;
import com.sgm.shared.enums.MovementType;

/**
 * Pruebas unitarias del punto de venta: validaciones de venta, descuento de
 * stock y registro de movimientos de salida.
 */
@ExtendWith(MockitoExtension.class)
class PosServiceTest {

    @NonNull
    private static final Long ID_CAJERO = Objects.requireNonNull(Long.valueOf(5L));
    @NonNull
    private static final Long ID_CAFE = Objects.requireNonNull(Long.valueOf(9L));

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MachineService machineService;

    @InjectMocks
    private PosService posService;

    private User cajero;
    private Item cafe;

    @BeforeEach
    void preparar() {
        cajero = new User();
        cajero.setId(ID_CAJERO);
        cajero.setUsername("cajero");
        cajero.setPassword("hash");
        cajero.setFullName("Cajero Uno");
        cajero.setEmail("cajero@sgm.com");

        cafe = new Item();
        cafe.setId(ID_CAFE);
        cafe.setBarcode("750100");
        cafe.setName("Caf\u00e9");
        cafe.setPrice(new BigDecimal("2.50"));
        cafe.setStock(new BigDecimal("10"));
        cafe.setActive(true);
    }

    private SaleRequest venta(BigDecimal cantidad, BigDecimal efectivo) {
        return new SaleRequest(List.of(new SaleRequest.SaleItem(ID_CAFE, cantidad)), efectivo, "POS-1");
    }

    @Test
    @DisplayName("Venta exitosa calcula totales, descuenta stock y registra la salida")
    @SuppressWarnings("null")
    void ventaExitosaDescuentaStockYRegistraSalida() {
        when(userRepository.findById(ID_CAJERO)).thenReturn(Optional.of(cajero));
        when(machineService.registerIfAbsent("POS-1"))
                .thenReturn(new MachineTerminal("POS-1", "Terminal POS-1"));
        when(itemRepository.findById(ID_CAFE)).thenReturn(Optional.of(cafe));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocacion -> {
            Sale venta = invocacion.getArgument(0);
            venta.setId(Long.valueOf(100L));
            return venta;
        });

        SaleDTO dto = posService.process(venta(new BigDecimal("2"), new BigDecimal("10.00")), ID_CAJERO);

        assertThat(dto.total()).isEqualByComparingTo("5.00");
        assertThat(dto.cashReceived()).isEqualByComparingTo("10.00");
        assertThat(dto.changeAmount()).isEqualByComparingTo("5.00");
        assertThat(dto.status()).isEqualTo("COMPLETADA");
        assertThat(dto.username()).isEqualTo("cajero");
        assertThat(dto.machineId()).isEqualTo("POS-1");
        assertThat(dto.details()).hasSize(1);
        assertThat(dto.details().get(0).itemId()).isEqualTo(ID_CAFE);
        assertThat(dto.details().get(0).itemName()).isEqualTo("Caf\u00e9");
        assertThat(dto.details().get(0).quantity()).isEqualByComparingTo("2");
        assertThat(dto.details().get(0).subtotal()).isEqualByComparingTo("5.00");
        assertThat(cafe.getStock()).isEqualByComparingTo("8");

        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(movementCaptor.capture());
        StockMovement movement = movementCaptor.getValue();
        assertThat(movement.getMovementType()).isEqualTo(MovementType.SALIDA);
        assertThat(movement.getQuantity()).isEqualByComparingTo("2");
        assertThat(movement.getNote()).isEqualTo("Venta #100");
        assertThat(movement.getUser().getUsername()).isEqualTo("cajero");
        assertThat(movement.getItem().getId()).isEqualTo(ID_CAFE);
    }

    @Test
    @DisplayName("Las líneas duplicadas del mismo artículo se agrupan en un solo detalle")
    @SuppressWarnings("null")
    void lineasDuplicadasSeAgrupanEnUnSoloDetalle() {
        SaleRequest request = new SaleRequest(
                List.of(new SaleRequest.SaleItem(ID_CAFE, new BigDecimal("2")),
                        new SaleRequest.SaleItem(ID_CAFE, new BigDecimal("3"))),
                new BigDecimal("13.00"), "POS-1");
        when(userRepository.findById(ID_CAJERO)).thenReturn(Optional.of(cajero));
        when(machineService.registerIfAbsent("POS-1"))
                .thenReturn(new MachineTerminal("POS-1", "Terminal POS-1"));
        when(itemRepository.findById(ID_CAFE)).thenReturn(Optional.of(cafe));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocacion -> {
            Sale venta = invocacion.getArgument(0);
            venta.setId(Long.valueOf(101L));
            return venta;
        });

        SaleDTO dto = posService.process(request, ID_CAJERO);

        assertThat(dto.total()).isEqualByComparingTo("12.50");
        assertThat(dto.changeAmount()).isEqualByComparingTo("0.50");
        assertThat(dto.details()).hasSize(1);
        assertThat(dto.details().get(0).quantity()).isEqualByComparingTo("5");
        assertThat(cafe.getStock()).isEqualByComparingTo("5");
        verify(movementRepository, times(1)).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Stock insuficiente rechaza la venta sin modificar el stock")
    @SuppressWarnings("null")
    void stockInsuficienteRechazaLaVenta() {
        cafe.setStock(new BigDecimal("3"));
        when(userRepository.findById(ID_CAJERO)).thenReturn(Optional.of(cajero));
        when(machineService.registerIfAbsent("POS-1"))
                .thenReturn(new MachineTerminal("POS-1", "Terminal POS-1"));
        when(itemRepository.findById(ID_CAFE)).thenReturn(Optional.of(cafe));

        ApiException ex = catchThrowableOfType(
                () -> posService.process(venta(new BigDecimal("5"), new BigDecimal("100.00")), ID_CAJERO),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("Stock insuficiente").contains("Caf\u00e9");
        assertThat(cafe.getStock()).isEqualByComparingTo("3");
        verify(saleRepository, never()).save(any());
        verify(itemRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un artículo deshabilitado rechaza la venta")
    @SuppressWarnings("null")
    void articuloDeshabilitadoRechazaLaVenta() {
        cafe.setActive(false);
        when(userRepository.findById(ID_CAJERO)).thenReturn(Optional.of(cajero));
        when(machineService.registerIfAbsent("POS-1"))
                .thenReturn(new MachineTerminal("POS-1", "Terminal POS-1"));
        when(itemRepository.findById(ID_CAFE)).thenReturn(Optional.of(cafe));

        ApiException ex = catchThrowableOfType(
                () -> posService.process(venta(new BigDecimal("2"), new BigDecimal("100.00")), ID_CAJERO),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("deshabilitado");
        verify(saleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un artículo inexistente devuelve 404")
    void articuloInexistenteLanzaNotFound() {
        when(userRepository.findById(ID_CAJERO)).thenReturn(Optional.of(cajero));
        when(machineService.registerIfAbsent("POS-1"))
                .thenReturn(new MachineTerminal("POS-1", "Terminal POS-1"));
        when(itemRepository.findById(ID_CAFE)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> posService.process(venta(new BigDecimal("2"), new BigDecimal("100.00")), ID_CAJERO),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Art\u00edculo no encontrado (id 9)");
    }

    @Test
    @DisplayName("Efectivo insuficiente rechaza la venta antes de persistir")
    @SuppressWarnings("null")
    void efectivoInsuficienteRechazaLaVenta() {
        when(userRepository.findById(ID_CAJERO)).thenReturn(Optional.of(cajero));
        when(machineService.registerIfAbsent("POS-1"))
                .thenReturn(new MachineTerminal("POS-1", "Terminal POS-1"));
        when(itemRepository.findById(ID_CAFE)).thenReturn(Optional.of(cafe));

        ApiException ex = catchThrowableOfType(
                () -> posService.process(venta(new BigDecimal("2"), new BigDecimal("4.99")), ID_CAJERO),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("Efectivo recibido insuficiente");
        assertThat(cafe.getStock()).isEqualByComparingTo("10");
        verify(saleRepository, never()).save(any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un cajero inexistente devuelve 404")
    @SuppressWarnings("null")
    void cajeroInexistenteLanzaNotFound() {
        when(userRepository.findById(ID_CAJERO)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> posService.process(venta(new BigDecimal("2"), new BigDecimal("10.00")), ID_CAJERO),
                ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Usuario no encontrado");
        verify(itemRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
        verify(saleRepository, never()).save(any());
    }
}
