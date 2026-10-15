package com.sgm.backend.controller;

import com.sgm.backend.service.UserService;
import com.sgm.shared.dto.ApiResponse;
import com.sgm.shared.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/user")
public class UserSelfController {

    private final UserService userService;

    public UserSelfController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> me(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.me(principal.getName()), "Usuario autenticado"));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Contrase\u00f1a actualizada"));
    }
}