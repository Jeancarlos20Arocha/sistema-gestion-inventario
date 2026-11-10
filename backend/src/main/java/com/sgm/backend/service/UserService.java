package com.sgm.backend.service;

import com.sgm.backend.entity.User;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.UserRepository;
import com.sgm.shared.dto.ChangePasswordRequest;
import com.sgm.shared.dto.RegisterSecurityQuestionsRequest;
import com.sgm.shared.dto.UserDTO;
import com.sgm.shared.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecoveryService recoveryService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RecoveryService recoveryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.recoveryService = recoveryService;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return userRepository.findAll().stream().map(UserService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserDTO me(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
        return toDto(user);
    }

    @Transactional
    public UserDTO create(UserDTO.CreateUserRequest request) {
        if (!request.passwordsMatch()) {
            throw ApiException.badRequest("Las contrase\u00f1as no coinciden");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw ApiException.conflict("El nombre de usuario ya est\u00e1 registrado");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("El correo ya est\u00e1 registrado");
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setRole(request.role());
        user.setPassword(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        if (request.securityQuestions() != null) {
            recoveryService.configureQuestions(user.getId(), request.securityQuestions());
        }

        return toDto(user);
    }

    @Transactional
    public UserDTO toggleEnabled(Long userId, Boolean enabled) {
        User user = buscar(userId);
        boolean deshabilitar = !Boolean.TRUE.equals(enabled);
        if (user.getRole() == Role.ROLE_ADMIN && deshabilitar && esUltimoAdmin()) {
            throw ApiException.conflict("No puede deshabilitar al \u00faltimo administrador activo");
        }
        user.setEnabled(!deshabilitar);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDTO unlock(Long userId) {
        User user = buscar(userId);
        user.unlock();
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDTO assignQuestions(Long userId, RegisterSecurityQuestionsRequest request) {
        buscar(userId);
        recoveryService.configureQuestions(userId, request);
        return toDto(buscar(userId));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.passwordsMatch()) {
            throw ApiException.badRequest("Las contrase\u00f1as no coinciden");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("La contrase\u00f1a actual es incorrecta");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User buscar(Long userId) {
        return userRepository.findById(Objects.requireNonNull(userId, "El identificador del usuario es obligatorio"))
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    private boolean esUltimoAdmin() {
        long admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_ADMIN && u.isEnabled())
                .count();
        return admins <= 1;
    }

    public static UserDTO toDto(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.isAccountLocked(),
                user.getFailedAttempts(),
                user.getLockExpiresAt());
    }
}