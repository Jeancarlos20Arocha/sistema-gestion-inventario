package com.sgm.backend.repository;

import com.sgm.backend.entity.SecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, Long> {

    List<SecurityQuestion> findByUserIdOrderByIdAsc(Long userId);

    boolean existsByUserId(Long userId);

    @Modifying
    void deleteByUserId(Long userId);
}