package com.sgm.backend.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AccountLockedException extends ApiException {

    private static final long serialVersionUID = 1L;

    private final int attempts;
    private final LocalDateTime unlockAt;

    public AccountLockedException(int attempts, LocalDateTime unlockAt, String message) {
        super(org.springframework.http.HttpStatus.LOCKED, message);
        this.attempts = attempts;
        this.unlockAt = unlockAt;
    }
}