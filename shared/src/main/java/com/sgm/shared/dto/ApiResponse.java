package com.sgm.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(
        boolean success,
        int code,
        String message,
        LocalDateTime timestamp,
        Object data) {

    public static ApiResponse ok(Object data, String message) {
        return new ApiResponse(true, 200, message, LocalDateTime.now(), data);
    }

    public static ApiResponse ok(Object data) {
        return ok(data, "OK");
    }

    public static ApiResponse error(int code, String message) {
        return new ApiResponse(false, code, message, LocalDateTime.now(), null);
    }
}