package com.sgm.backend.service;

import com.sgm.backend.entity.Item;
import com.sgm.backend.entity.MachineTerminal;
import com.sgm.backend.entity.Sale;
import com.sgm.backend.entity.SaleDetail;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PosService {

    private final SaleRepository saleRepository;
    private final ItemRepository itemRepository;
    private final StockMovementRepository movementRepository;
    private final UserRepository userRepository;
    private final MachineService machineService;

    public PosService(SaleRepository saleRepository,
                      ItemRepository itemRepository,
                      StockMovementRepository movementRepository,
                      UserRepository userRepository,
                      MachineService machineService) {
        this.saleRepository = saleRepository;
        this.itemRepository = itemRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
        this.machineService = machineService;
    }

    @Transactional
    public SaleDTO process(SaleRequest request, Long operatorId) {
        User user = userRepository.findById(operatorId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        MachineTerminal terminal = machineService.registerIfAbsent(request.machineId());

        Map<Long, BigDecimal> cantidades = new HashMap<>();
        request.items().forEach(linea -> cantidades.merge(linea.itemId(), linea.quantity(), BigDecimal::add));

        Sale sale = new Sale();
        sale.setUser(user);
        sale.setMachine(terminal);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : cantidades.entrySet()) {
            Item item = itemRepository.findById(entry.getKey())
                    .orElseThrow(() -> ApiException.notFound("Art\u00edculo no encontrado (id " + entry.getKey() + ")"));
            if (!item.isActive()) {
                throw ApiException.conflict("El art\u00edculo \u00ab" + item.getName() + "\u00bb est\u00e1 deshabilitado");
            }
            if (item.getStock().compareTo(entry.getValue()) < 0) {
                throw ApiException.conflict("Stock insuficiente para \u00ab" + item.getName()
                        + "\u00bb (disponible: " + item.getStock() + ")");
            }

            BigDecimal subtotal = item.getPrice().multiply(entry.getValue());
            total = total.add(subtotal);
            sale.addDetail(new SaleDetail(item, entry.getValue(), item.getPrice()));
        }

        if (request.cashReceived().compareTo(total) < 0) {
            throw ApiException.badRequest("Efectivo recibido insuficiente");
        }

        sale.setTotal(total);
        sale.setCashReceived(request.cashReceived());
        sale.setChangeAmount(request.cashReceived().subtract(total));
        sale = saleRepository.save(sale);

        for (SaleDetail detail : sale.getDetails()) {
            Item item = detail.getItem();
            item.subtractStock(detail.getQuantity());
            itemRepository.save(item);

            StockMovement movement = new StockMovement();
            movement.setItem(item);
            movement.setUser(user);
            movement.setMachine(terminal);
            movement.setMovementType(MovementType.SALIDA);
            movement.setQuantity(detail.getQuantity());
            movement.setNote("Venta #" + sale.getId());
            movementRepository.save(movement);
        }

        return toDto(sale);
    }

    static SaleDTO toDto(Sale sale) {
        String username = sale.getUser().getUsername();
        String machineId = sale.getMachine() != null ? sale.getMachine().getMachineId() : null;

        var detalles = sale.getDetails().stream()
                .map(d -> new SaleDTO.SaleDetail(
                        d.getItem().getId(),
                        d.getItem().getName(),
                        d.getItem().getBarcode(),
                        d.getQuantity(),
                        d.getPrice(),
                        d.getSubtotal()))
                .toList();

        return new SaleDTO(
                sale.getId(),
                username,
                machineId,
                sale.getTotal(),
                sale.getCashReceived(),
                sale.getChangeAmount(),
                sale.getStatus(),
                sale.getCreatedAt(),
                detalles);
    }
}