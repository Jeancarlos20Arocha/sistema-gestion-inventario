package com.sgm.backend.service;

import com.sgm.backend.entity.MachineTerminal;
import com.sgm.backend.entity.User;
import com.sgm.backend.exception.AccountLockedException;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.UserRepository;
import com.sgm.backend.security.JwtService;
import com.sgm.shared.dto.LoginRequest;
import com.sgm.shared.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MachineService machineService;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-minutes}")
    private long lockMinutes;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       MachineService machineService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.machineService = machineService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> ApiException.badRequest("Credenciales inv\u00e1lidas"));

        if (!user.isEnabled()) {
            throw ApiException.forbidden("La cuenta est\u00e1 deshabilitada. Contacte al administrador.");
        }

        if (user.isAccountLocked() && cuentaSigueBloqueada(user)) {
            long restantes = minutosRestantes(user.getLockExpiresAt());
            throw new AccountLockedException(
                    user.getFailedAttempts(), user.getLockExpiresAt(),
                    "Cuenta bloqueada temporalmente. Reintente en " + restantes + " min.");
        }

        if (user.isAccountLocked()) {
            userRepository.resetSecurityCounters(user.getId());
            user = userRepository.findByUsername(request.username()).orElseThrow();
        }

        if (passwordEncoder.matches(request.password(), user.getPassword())) {
            userRepository.resetSecurityCounters(user.getId());
            MachineTerminal terminal = machineService.registerIfAbsent(request.machineId());

            String token = jwtService.generateAccessToken(
                    user.getUsername(), user.getId(), user.getRole().name(), terminal.getMachineId());

            return new LoginResponse(token, "Bearer", user.getUsername(),
                    user.getFullName(), user.getRole(), expirationMs);
        }

        userRepository.incrementFailedAttempts(user.getId());
        int intentos = user.getFailedAttempts() + 1;

        if (intentos >= maxFailedAttempts) {
            LocalDateTime unlockAt = LocalDateTime.now().plusMinutes(lockMinutes);
            userRepository.lockAccount(user.getId(), unlockAt);
            throw new AccountLockedException(intentos, unlockAt,
                    "Ha excedido " + maxFailedAttempts + " intentos fallidos. Cuenta bloqueada por " + lockMinutes + " minutos.");
        }

        throw ApiException.badRequest("Contrase\u00f1a incorrecta. Le quedan "
                + (maxFailedAttempts - intentos) + " intentos.");
    }

    private boolean cuentaSigueBloqueada(User user) {
        return user.getLockExpiresAt() == null || user.getLockExpiresAt().isAfter(LocalDateTime.now());
    }

    private long minutosRestantes(LocalDateTime unlockAt) {
        if (unlockAt == null) {
            return 0;
        }
        long minutos = java.time.Duration.between(LocalDateTime.now(), unlockAt).toMinutes();
        return Math.max(1, minutos);
    }
}