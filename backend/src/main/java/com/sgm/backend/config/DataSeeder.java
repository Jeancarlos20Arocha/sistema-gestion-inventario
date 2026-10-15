package com.sgm.backend.config;

import com.sgm.backend.entity.User;
import com.sgm.backend.repository.SecurityQuestionRepository;
import com.sgm.backend.repository.UserRepository;
import com.sgm.backend.service.RecoveryService;
import com.sgm.shared.dto.RegisterSecurityQuestionsRequest;
import com.sgm.shared.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicializador automático de datos críticos.
 * <p>
 * Al arrancar la aplicación garantiza el usuario administrador inicial con su
 * contraseña cifrada mediante BCrypt y sus 3 preguntas de seguridad estándar
 * (con respuestas normalizadas y cifradas). El proceso es idempotente: si el
 * usuario o sus preguntas ya existen, no se modifican ni se sobrescriben.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String ADMIN_FULL_NAME = "Administrador del Sistema";
    private static final String ADMIN_EMAIL = "admin@sgm.local";

    /** Respuestas iniciales de las 3 preguntas estándar del administrador. */
    private static final String ADMIN_ANSWER_1 = "Toby";
    private static final String ADMIN_ANSWER_2 = "Barcelona";
    private static final String ADMIN_ANSWER_3 = "Acuario";

    private final UserRepository userRepository;
    private final SecurityQuestionRepository securityQuestionRepository;
    private final RecoveryService recoveryService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-username:ADMIN}")
    private String adminUsername;

    @Value("${app.seed.admin-password:123456}")
    private String adminPassword;

    public DataSeeder(UserRepository userRepository,
                      SecurityQuestionRepository securityQuestionRepository,
                      RecoveryService recoveryService,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.securityQuestionRepository = securityQuestionRepository;
        this.recoveryService = recoveryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            User admin = userRepository.findByUsername(adminUsername).orElse(null);

            if (admin == null) {
                admin = crearAdministrador();
                log.warn("Usuario administrador '{}' creado. Cambie la contraseña por defecto por seguridad.",
                        adminUsername);
            } else {
                log.info("El usuario administrador '{}' ya existe; no se modifica.", adminUsername);
            }

            asegurarPreguntasSeguridad(admin);
        } catch (DataAccessException excepcion) {
            log.error("No se pudo inicializar el usuario administrador '{}': {}",
                    adminUsername, excepcion.getMessage(), excepcion);
            throw excepcion;
        }
    }

    private User crearAdministrador() {
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFullName(ADMIN_FULL_NAME);
        admin.setEmail(ADMIN_EMAIL);
        admin.setRole(Role.ROLE_ADMIN);
        admin.setEnabled(true);
        admin.setAccountLocked(false);
        admin.setFailedAttempts(0);
        return userRepository.save(admin);
    }

    /**
     * Verifica si el administrador ya tiene configuradas sus preguntas de
     * seguridad y, si no las tiene, inserta las 3 preguntas estándar con sus
     * respuestas iniciales normalizadas y cifradas (vía {@link RecoveryService}).
     */
    private void asegurarPreguntasSeguridad(User admin) {
        if (securityQuestionRepository.existsByUserId(admin.getId())) {
            log.info("El administrador '{}' ya tiene preguntas de seguridad configuradas.", adminUsername);
            return;
        }

        recoveryService.configureQuestions(admin.getId(),
                new RegisterSecurityQuestionsRequest(ADMIN_ANSWER_1, ADMIN_ANSWER_2, ADMIN_ANSWER_3));
        log.warn("Preguntas de seguridad estándar asignadas al administrador '{}'. "
                + "Cambie las respuestas por defecto por seguridad.", adminUsername);
    }
}
