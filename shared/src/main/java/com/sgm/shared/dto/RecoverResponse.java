package com.sgm.shared.dto;

public record RecoverResponse(
        String resetToken,
        String tokenType,
        long expiresInMs) {
}