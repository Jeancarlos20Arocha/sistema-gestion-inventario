package com.sgm.shared.constant;

import java.util.List;

public final class SecurityConstants {

    public static final String HEADER = "Authorization";
    public static final String PREFIX = "Bearer ";
    public static final String CLAIM_ROLE = "rol";
    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_MACHINE_ID = "mid";
    public static final String CLAIM_SCOPE = "scope";
    public static final String SCOPE_ACCESS = "ACCESS";
    public static final String SCOPE_PASSWORD_RESET = "PASSWORD_RESET";

    public static final int MAX_FAILED_ATTEMPTS = 3;
    public static final long LOCK_MINUTES = 15;

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";

    /**
     * Preguntas de seguridad estándar del sistema. Son fijas y obligatorias para
     * todos los usuarios: solo se almacena la respuesta de cada una.
     */
    public static final List<String> SECURITY_QUESTIONS = List.of(
            "El primer nombre de tu mascota",
            "Equipo de f\u00fatbol favorito",
            "\u00bfQu\u00e9 signo eres?");

    public static final int SECURITY_QUESTION_COUNT = SECURITY_QUESTIONS.size();

    private SecurityConstants() {
    }
}