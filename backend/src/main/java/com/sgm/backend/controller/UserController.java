package com.sgm.backend.controller;

import com.sgm.backend.service.UserService;
import com.sgm.shared.dto.ApiResponse;
import com.sgm.shared.dto.RegisterSecurityQuestionsRequest;
import com.sgm.shared.dto.UserDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list() {
        List<UserDTO> users = userService.findAll();
        return ResponseEntity.ok(ApiResponse.ok(users, users.size() + " usuario(s)"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody UserDTO.CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(userService.create(request), "Usuario creado"));
    }

    @PutMapping("/{id}/enabled")
    public ResponseEntity<ApiResponse> toggleEnabled(@PathVariable Long id,
                                                     @RequestBody UserDTO.ToggleEnabledRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.toggleEnabled(id, request.enabled()), "Estado actualizado"));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse> unlock(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.unlock(id), "Cuenta desbloqueada"));
    }

    @PutMapping("/{id}/security-questions")
    public ResponseEntity<ApiResponse> securityQuestions(
            @PathVariable Long id,
            @Valid @RequestBody RegisterSecurityQuestionsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.assignQuestions(id, request), "Preguntas de seguridad actualizadas"));
    }
}