package com.sgm.backend.repository;

import com.sgm.backend.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    Optional<Batch> findByItemIdAndBatchCode(Long itemId, String batchCode);

    boolean existsByItemIdAndBatchCode(Long itemId, String batchCode);

    List<Batch> findByItemIdOrderByExpiryDateAsc(Long itemId);

    List<Batch> findAllByOrderByIdDesc();
}