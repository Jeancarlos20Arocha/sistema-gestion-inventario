package com.sgm.backend.service;

import com.sgm.backend.entity.Sale;
import com.sgm.backend.entity.StockMovement;
import com.sgm.backend.repository.ItemRepository;
import com.sgm.backend.repository.SaleDetailRepository;
import com.sgm.backend.repository.SaleRepository;
import com.sgm.backend.repository.StockMovementRepository;
import com.sgm.shared.dto.ReportDTO;
import com.sgm.shared.dto.SaleDTO;
import com.sgm.shared.dto.StockMovementDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final SaleDetailRepository saleDetailRepository;
    private final StockMovementRepository movementRepository;
    private final ItemRepository itemRepository;

    public ReportService(SaleRepository saleRepository,
                         SaleDetailRepository saleDetailRepository,
                         StockMovementRepository movementRepository,
                         ItemRepository itemRepository) {
        this.saleRepository = saleRepository;
        this.saleDetailRepository = saleDetailRepository;
        this.movementRepository = movementRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public ReportDTO dailyReport(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Sale> sales = saleRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        long count = saleRepository.countByCreatedAtBetween(start, end);
        BigDecimal revenue = saleRepository.sumTotalBetween(start, end);
        BigDecimal itemsSold = saleDetailRepository.sumItemsSoldBetween(start, end);

        List<SaleDTO> saleDtos = sales.stream().map(PosService::toDto).toList();

        return new ReportDTO(date, (int) count,
                revenue == null ? BigDecimal.ZERO : revenue,
                itemsSold == null ? BigDecimal.ZERO : itemsSold,
                saleDtos);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> movements(Long itemId, LocalDate desde, LocalDate hasta) {
        List<StockMovement> movimientos;

        if (desde != null || hasta != null) {
            LocalDateTime start = desde != null ? desde.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime end = hasta != null ? hasta.plusDays(1).atStartOfDay() : LocalDateTime.now().plusDays(1);
            movimientos = movementRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        } else {
            movimientos = movementRepository.findAllByOrderByCreatedAtDesc();
        }

        return movimientos.stream()
                .filter(m -> itemId == null || m.getItem().getId().equals(itemId))
                .map(ReportService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.sgm.shared.dto.ItemDTO> stockAlert() {
        return itemRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(item -> item.getStock().compareTo(item.getMinStock()) <= 0)
                .map(InventoryService::toDto)
                .toList();
    }

    static StockMovementDTO toDto(StockMovement m) {
        return new StockMovementDTO(
                m.getId(),
                m.getItem().getId(),
                m.getItem().getName(),
                m.getItem().getBarcode(),
                m.getBatch() != null ? m.getBatch().getId() : null,
                m.getMachine() != null ? m.getMachine().getMachineId() : null,
                m.getUser().getUsername(),
                m.getMovementType(),
                m.getQuantity(),
                m.getNote(),
                m.getCreatedAt());
    }
}