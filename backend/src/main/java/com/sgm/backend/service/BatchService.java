package com.sgm.backend.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BatchService {

    private final BatchRepository batchRepository;
    private final ItemRepository itemRepository;
    private final StockMovementRepository movementRepository;
    private final UserRepository userRepository;
    private final MachineService machineService;

    public BatchService(BatchRepository batchRepository,
                        ItemRepository itemRepository,
                        StockMovementRepository movementRepository,
                        UserRepository userRepository,
                        MachineService machineService) {
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
        this.machineService = machineService;
    }

    @Transactional
    public BatchDTO register(BatchDTO.CreateBatchRequest request, String machineId, Long operatorId) {
        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> ApiException.notFound("Art\u00edculo no encontrado"));
        if (!item.isActive()) {
            throw ApiException.conflict("El art\u00edculo est\u00e1 deshabilitado");
        }
        if (batchRepository.existsByItemIdAndBatchCode(request.itemId(), request.batchCode().trim())) {
            throw ApiException.conflict("El lote proporcionado ya existe para este art\u00edculo");
        }

        Batch batch = new Batch();
        batch.setItem(item);
        batch.setBatchCode(request.batchCode().trim());
        batch.setProvider(request.provider());
        batch.setQuantity(request.quantity());
        batch.setCost(request.cost());
        batch.setExpiryDate(request.expiryDate());

        batch = batchRepository.save(batch);
        item.addStock(request.quantity());
        itemRepository.save(item);

        MachineTerminal terminal = machineService.registerIfAbsent(machineId);
        User user = userRepository.findById(operatorId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        StockMovement movement = new StockMovement();
        movement.setItem(item);
        movement.setBatch(batch);
        movement.setUser(user);
        movement.setMachine(terminal);
        movement.setMovementType(MovementType.ENTRADA);
        movement.setQuantity(request.quantity());
        movement.setNote("Ingreso de lote " + batch.getBatchCode());
        movementRepository.save(movement);

        return toDto(batch);
    }

    @Transactional(readOnly = true)
    public List<BatchDTO> findAll() {
        return batchRepository.findAllByOrderByIdDesc().stream()
                .map(BatchService::toDto)
                .toList();
    }

    static BatchDTO toDto(Batch batch) {
        return new BatchDTO(
                batch.getId(),
                batch.getItem().getId(),
                batch.getItem().getName(),
                batch.getItem().getBarcode(),
                batch.getBatchCode(),
                batch.getProvider(),
                batch.getQuantity(),
                batch.getCost(),
                batch.getExpiryDate(),
                batch.getReceivedDate());
    }
}