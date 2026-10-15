package com.sgm.backend.controller;

import com.sgm.backend.service.AuthService;
import com.sgm.backend.service.RecoveryService;
import com.sgm.shared.dto.ApiResponse;
import com.sgm.shared.dto.LoginRequest;
import com.sgm.shared.dto.RecoverResponse;
import com.sgm.shared.dto.ResetPasswordRequest;
import com.sgm.shared.dto.VerifyAnswersRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RecoveryService recoveryService;

    public AuthController(AuthService authService,
                          RecoveryService recoveryService) {
        this.authService = authService;
        this.recoveryService = recoveryService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.login(request), "Autenticaci\u00f3n exitosa"));
    }

    @GetMapping("/recover/questions/{username}")
    public ResponseEntity<ApiResponse> questions(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.ok(
                recoveryService.questionsFor(username), "Preguntas de seguridad"));
    }

    @PostMapping("/recover/verify")
    public ResponseEntity<ApiResponse> verify(@Valid @RequestBody VerifyAnswersRequest request) {
        RecoverResponse response = recoveryService.verifyAnswers(
                request.username(), request.answers());
        return ResponseEntity.ok(ApiResponse.ok(response, "Respuestas correctas. Puede restablecer su contrase\u00f1a."));
    }

    @PostMapping("/recover/reset")
    public ResponseEntity<ApiResponse> reset(@Valid @RequestBody ResetPasswordRequest request) {
        recoveryService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Contrase\u00f1a restablecida correctamente"));
    }
}
