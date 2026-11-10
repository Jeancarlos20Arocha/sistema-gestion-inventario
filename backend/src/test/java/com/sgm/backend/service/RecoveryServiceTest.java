package com.sgm.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.sgm.backend.entity.SecurityQuestion;
import com.sgm.backend.entity.User;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.SecurityQuestionRepository;
import com.sgm.backend.repository.UserRepository;
import com.sgm.backend.security.JwtService;
import com.sgm.shared.constant.SecurityConstants;
import com.sgm.shared.dto.RecoverResponse;
import com.sgm.shared.dto.ResetPasswordRequest;
import com.sgm.shared.dto.SecurityAnswerRequest;
import com.sgm.shared.enums.Role;

/**
 * Pruebas unitarias de la recuperación de cuenta mediante preguntas de
 * seguridad.
 */
@ExtendWith(MockitoExtension.class)
class RecoveryServiceTest {

    private static final Long ID_ANA = Long.valueOf(4L);

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityQuestionRepository securityQuestionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RecoveryService recoveryService;

    private User ana;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(Objects.requireNonNull(recoveryService), "resetExpirationMs", Long.valueOf(600000L));
        ana = new User();
        ana.setId(ID_ANA);
        ana.setUsername("ana");
        ana.setPassword("hash");
        ana.setFullName("Ana P\u00e9rez");
        ana.setEmail("ana@sgm.com");
        ana.setRole(Role.ROLE_USER);
    }

    private SecurityQuestion preguntaAlmacenada(int indice) {
        return new SecurityQuestion(ana, SecurityConstants.SECURITY_QUESTIONS.get(indice), "hash-" + indice);
    }

    @Test
    @DisplayName("Pedir las preguntas de un usuario sin las 3 configuradas devuelve 409")
    void preguntasDeUsuarioSinLasTresConfiguradasLanzaConflict() {
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(securityQuestionRepository.findByUserIdOrderByIdAsc(ID_ANA))
                .thenReturn(List.of(preguntaAlmacenada(0), preguntaAlmacenada(1)));

        ApiException ex = catchThrowableOfType(() -> recoveryService.questionsFor("ana"), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("3 preguntas");
    }

    @Test
    @DisplayName("Pedir las preguntas de un usuario deshabilitado devuelve 403")
    void preguntasDeUsuarioDeshabilitadoLanzaForbidden() {
        ana.setEnabled(false);
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));

        ApiException ex = catchThrowableOfType(() -> recoveryService.questionsFor("ana"), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getMessage()).contains("deshabilitada");
    }

    @Test
    @DisplayName("Verificar respuestas correctas devuelve el token de restablecimiento")
    void verificarRespuestasCorrectasDevuelveToken() {
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(securityQuestionRepository.findByUserIdOrderByIdAsc(ID_ANA))
                .thenReturn(List.of(preguntaAlmacenada(0), preguntaAlmacenada(1), preguntaAlmacenada(2)));
        doReturn(Boolean.TRUE).when(passwordEncoder).matches("toby", "hash-0");
        doReturn(Boolean.TRUE).when(passwordEncoder).matches("barcelona", "hash-1");
        doReturn(Boolean.TRUE).when(passwordEncoder).matches("acuario", "hash-2");
        when(jwtService.generateResetToken("ana", ID_ANA)).thenReturn("reset-token");

        RecoverResponse respuesta = recoveryService.verifyAnswers("ana",
                List.of(new SecurityAnswerRequest(" Toby "), new SecurityAnswerRequest("Barcelona"),
                        new SecurityAnswerRequest("ACUARIO")));

        assertThat(respuesta.resetToken()).isEqualTo("reset-token");
        assertThat(respuesta.tokenType()).isEqualTo("Bearer");
        assertThat(respuesta.expiresInMs()).isEqualTo(600000L);
    }

    @Test
    @DisplayName("Verificar con una respuesta incorrecta devuelve 400")
    void verificarRespuestaIncorrectaLanzaBadRequest() {
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(securityQuestionRepository.findByUserIdOrderByIdAsc(ID_ANA))
                .thenReturn(List.of(preguntaAlmacenada(0), preguntaAlmacenada(1), preguntaAlmacenada(2)));
        doReturn(Boolean.FALSE).when(passwordEncoder).matches("toby", "hash-0");

        ApiException ex = catchThrowableOfType(() -> recoveryService.verifyAnswers("ana",
                List.of(new SecurityAnswerRequest("toby"), new SecurityAnswerRequest("barcelona"),
                        new SecurityAnswerRequest("acuario"))), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).contains("Respuesta de seguridad incorrecta");
        verify(jwtService, never()).generateResetToken(any(), any());
    }

    @Test
    @DisplayName("Verificar con preguntas almacenadas no estándar devuelve 409")
    void verificarConConfiguracionCorruptaLanzaConflict() {
        SecurityQuestion alterada = new SecurityQuestion(ana, "Pregunta alterada", "hash-0");
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(securityQuestionRepository.findByUserIdOrderByIdAsc(ID_ANA))
                .thenReturn(List.of(alterada, preguntaAlmacenada(1), preguntaAlmacenada(2)));

        ApiException ex = catchThrowableOfType(() -> recoveryService.verifyAnswers("ana",
                List.of(new SecurityAnswerRequest("toby"), new SecurityAnswerRequest("barcelona"),
                        new SecurityAnswerRequest("acuario"))), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).contains("preguntas est\u00e1ndar");
    }

    @Test
    @DisplayName("Restablecer contraseña con token inválido devuelve 400")
    void restablecerConTokenInvalidoLanzaBadRequest() {
        doReturn(Boolean.FALSE).when(jwtService).isValid("token-malo", SecurityConstants.SCOPE_PASSWORD_RESET);
        ResetPasswordRequest request = new ResetPasswordRequest("token-malo", "NuevaClave1!", "NuevaClave1!");

        ApiException ex = catchThrowableOfType(() -> recoveryService.resetPassword(request), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("El token de restablecimiento es inv\u00e1lido o ha expirado.");
    }
}
