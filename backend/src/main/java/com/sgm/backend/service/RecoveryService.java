package com.sgm.backend.service;

import com.sgm.backend.entity.SecurityQuestion;
import com.sgm.backend.entity.User;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.SecurityQuestionRepository;
import com.sgm.backend.repository.UserRepository;
import com.sgm.backend.security.JwtService;
import com.sgm.shared.constant.SecurityConstants;
import com.sgm.shared.dto.RecoverResponse;
import com.sgm.shared.dto.RegisterSecurityQuestionsRequest;
import com.sgm.shared.dto.ResetPasswordRequest;
import com.sgm.shared.dto.SecurityAnswerRequest;
import com.sgm.shared.dto.UserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RecoveryService {

    private final UserRepository userRepository;
    private final SecurityQuestionRepository securityQuestionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.reset-expiration-ms}")
    private long resetExpirationMs;

    public RecoveryService(UserRepository userRepository,
                           SecurityQuestionRepository securityQuestionRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.securityQuestionRepository = securityQuestionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Devuelve las 3 preguntas de seguridad estándar del sistema para el
     * usuario indicado, siempre que ya tenga registradas sus respuestas.
     */
    @Transactional(readOnly = true)
    public UserDTO.SecurityQuestionsResponse questionsFor(String username) {
        User user = findActiveUser(username);
        List<SecurityQuestion> preguntas = securityQuestionRepository.findByUserIdOrderByIdAsc(user.getId());

        if (preguntas.size() != SecurityConstants.SECURITY_QUESTION_COUNT) {
            throw ApiException.conflict(
                    "El usuario no tiene configuradas las 3 preguntas de seguridad. Contacte al administrador.");
        }

        return new UserDTO.SecurityQuestionsResponse(
                user.getUsername(), preguntasEstandar(), LocalDateTime.now());
    }

    /**
     * Valida las respuestas contra las 3 preguntas estándar y, si son correctas,
     * emite un token temporal de restablecimiento de contraseña.
     */
    @Transactional
    public RecoverResponse verifyAnswers(String username, List<SecurityAnswerRequest> answers) {
        User user = findActiveUser(username);
        List<SecurityQuestion> preguntas = securityQuestionRepository.findByUserIdOrderByIdAsc(user.getId());

        if (preguntas.size() != SecurityConstants.SECURITY_QUESTION_COUNT
                || answers.size() != SecurityConstants.SECURITY_QUESTION_COUNT) {
            throw ApiException.badRequest("Debe responder las 3 preguntas de seguridad configuradas.");
        }

        for (int i = 0; i < SecurityConstants.SECURITY_QUESTION_COUNT; i++) {
            SecurityQuestion almacenada = preguntas.get(i);
            String preguntaEstandar = SecurityConstants.SECURITY_QUESTIONS.get(i);

            // La pregunta almacenada debe ser exactamente la estándar en esa posición.
            if (!normalizar(preguntaEstandar).equals(normalizar(almacenada.getQuestion()))) {
                throw ApiException.conflict(
                        "La configuración de seguridad del usuario no coincide con las preguntas estándar del sistema.");
            }

            SecurityAnswerRequest respuesta = answers.get(i);
            if (!passwordEncoder.matches(normalizar(respuesta.answer()), almacenada.getAnswerHash())) {
                throw ApiException.badRequest("Respuesta de seguridad incorrecta en la pregunta: " + preguntaEstandar);
            }
        }

        String resetToken = jwtService.generateResetToken(user.getUsername(), user.getId());
        return new RecoverResponse(resetToken, "Bearer", resetExpirationMs);
    }

    /**
     * Restablece la contraseña usando el token emitido tras validar las
     * respuestas y limpia los contadores de bloqueo de la cuenta.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.passwordsMatch()) {
            throw ApiException.badRequest("Las contrase\u00f1as no coinciden");
        }
        if (!jwtService.isValid(request.resetToken(), SecurityConstants.SCOPE_PASSWORD_RESET)) {
            throw ApiException.badRequest("El token de restablecimiento es inv\u00e1lido o ha expirado.");
        }

        String username = jwtService.extractUsername(request.resetToken());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.unlock();
        userRepository.save(user);
    }

    /**
     * Registra (o reemplaza) las respuestas de las 3 preguntas estándar. Las
     * respuestas se normalizan y se cifran con BCrypt antes de persistirlas.
     */
    @Transactional
    public void configureQuestions(Long userId, RegisterSecurityQuestionsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        if (request == null || !request.allAnswered()) {
            throw ApiException.badRequest("Debe registrar las respuestas de las 3 preguntas de seguridad.");
        }

        securityQuestionRepository.deleteByUserId(userId);

        List<SecurityQuestion> preguntas = new ArrayList<>();
        for (int i = 0; i < SecurityConstants.SECURITY_QUESTION_COUNT; i++) {
            preguntas.add(new SecurityQuestion(
                    user,
                    SecurityConstants.SECURITY_QUESTIONS.get(i),
                    passwordEncoder.encode(normalizar(request.answer(i)))));
        }
        securityQuestionRepository.saveAll(preguntas);
    }

    private List<UserDTO.ApprovedQuestionDto> preguntasEstandar() {
        return SecurityConstants.SECURITY_QUESTIONS.stream()
                .map(UserDTO.ApprovedQuestionDto::new)
                .toList();
    }

    private User findActiveUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.badRequest("Usuario no encontrado"));
        if (!user.isEnabled()) {
            throw ApiException.forbidden("La cuenta est\u00e1 deshabilitada. Contacte al administrador.");
        }
        return user;
    }

    /**
     * Normaliza una respuesta: sin espacios al inicio/fin, en minúsculas y con
     * los espacios internos colapsados a uno solo.
     */
    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
