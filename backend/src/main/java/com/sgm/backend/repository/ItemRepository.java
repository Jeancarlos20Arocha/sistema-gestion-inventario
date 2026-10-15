package com.sgm.backend.repository;

import com.sgm.backend.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    boolean existsByCategoryId(Long categoryId);

    List<Item> findAllByActiveTrueOrderByNameAsc();

    List<Item> findAllByOrderByNameAsc();

    List<Item> findByActiveTrueAndStockLessThanEqualOrderByNameAsc(BigDecimal minStock);
}