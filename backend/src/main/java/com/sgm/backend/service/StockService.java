package com.sgm.backend.service;

import com.sgm.backend.entity.Item;
import com.sgm.backend.entity.StockMovement;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.ItemRepository;
import com.sgm.backend.repository.StockMovementRepository;
import com.sgm.backend.repository.UserRepository;
import com.sgm.shared.dto.StockMovementDTO;
import com.sgm.shared.enums.MovementType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class StockService {

    private final ItemRepository itemRepository;
    private final StockMovementRepository movementRepository;
    private final UserRepository userRepository;
    private final MachineService machineService;

    public StockService(ItemRepository itemRepository,
                        StockMovementRepository movementRepository,
                        UserRepository userRepository,
                        MachineService machineService) {
        this.itemRepository = itemRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
        this.machineService = machineService;
    }

    @Transactional
    public StockMovementDTO adjust(Long itemId, BigDecimal delta, String note,
                                   String machineId, Long operatorId) {
        if (delta == null || delta.signum() == 0) {
            throw ApiException.badRequest("El ajuste debe ser distinto de cero");
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Art\u00edculo no encontrado"));

        BigDecimal resultado = item.getStock().add(delta);
        if (resultado.signum() < 0) {
            throw ApiException.conflict("El ajuste deja el stock en negativo (stock actual: " + item.getStock() + ")");
        }

        item.setStock(resultado);
        itemRepository.save(item);

        StockMovement movement = new StockMovement();
        movement.setItem(item);
        movement.setUser(userRepository.findById(operatorId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado")));
        movement.setMachine(machineService.registerIfAbsent(machineId));
        movement.setMovementType(MovementType.AJUSTE);
        movement.setQuantity(delta.abs());
        movement.setNote(note);
        movementRepository.save(movement);

        return ReportService.toDto(movement);
    }
}