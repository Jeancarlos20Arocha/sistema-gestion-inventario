package com.sgm.backend.repository;

import com.sgm.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("update User u set u.failedAttempts = u.failedAttempts + 1 where u.id = :userId")
    int incrementFailedAttempts(@Param("userId") Long userId);

    @Modifying
    @Query("update User u set u.failedAttempts = 0, u.accountLocked = false, u.lockExpiresAt = null where u.id = :userId")
    int resetSecurityCounters(@Param("userId") Long userId);

    @Modifying
    @Query("update User u set u.accountLocked = true, u.lockExpiresAt = :expiresAt where u.id = :userId")
    int lockAccount(@Param("userId") Long userId, @Param("expiresAt") java.time.LocalDateTime expiresAt);
}