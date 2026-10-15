package com.sgm.backend.repository;

import com.sgm.backend.entity.SaleDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleDetailRepository extends JpaRepository<SaleDetail, Long> {

    @Query("select coalesce(sum(d.quantity), 0) from SaleDetail d join d.sale s " +
            "where s.createdAt between :start and :end and s.status = 'COMPLETADA'")
    BigDecimal sumItemsSoldBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<SaleDetail> findBySaleId(Long saleId);
}