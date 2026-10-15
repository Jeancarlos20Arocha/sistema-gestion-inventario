package com.sgm.shared.dto;

import com.sgm.shared.enums.Role;

public record LoginResponse(
        String token,
        String tokenType,
        String username,
        String fullName,
        Role role,
        long expiresInMs) {
}