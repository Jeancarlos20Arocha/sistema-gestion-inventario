package com.sgm.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.sgm.backend.entity.MachineTerminal;
import com.sgm.backend.entity.User;
import com.sgm.backend.exception.AccountLockedException;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.UserRepository;
import com.sgm.backend.security.JwtService;
import com.sgm.shared.dto.LoginRequest;
import com.sgm.shared.dto.LoginResponse;
import com.sgm.shared.enums.Role;

/**
 * Pruebas unitarias del control de intentos fallidos y del bloqueo de cuentas.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long ID_ADMIN = Long.valueOf(1L);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private MachineService machineService;

    @InjectMocks
    private AuthService authService;

    private User admin;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(Objects.requireNonNull(authService), "maxFailedAttempts", Integer.valueOf(3));
        ReflectionTestUtils.setField(Objects.requireNonNull(authService), "lockMinutes", Long.valueOf(15L));
        ReflectionTestUtils.setField(Objects.requireNonNull(authService), "expirationMs", Long.valueOf(3600000L));
        admin = new User();
        admin.setId(ID_ADMIN);
        admin.setUsername("admin");
        admin.setPassword("hash");
        admin.setFullName("Administrador General");
        admin.setEmail("admin@sgm.com");
        admin.setRole(Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Login con usuario inexistente devuelve 400 con credenciales inválidas")
    void loginConUsuarioInexistenteLanzaBadRequest() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> authService.login(new LoginRequest("fantasma", "clave", "PC-1")), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("Credenciales inv\u00e1lidas");
        verify(userRepository, never()).incrementFailedAttempts(any());
    }

    @Test
    @DisplayName("Login con cuenta deshabilitada devuelve 403")
    void loginConCuentaDeshabilitadaLanzaForbidden() {
        admin.setEnabled(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        ApiException ex = catchThrowableOfType(
                () -> authService.login(new LoginRequest("admin", "clave", "PC-1")), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getMessage()).contains("deshabilitada");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Login con cuenta bloqueada vigente devuelve 423 sin consumir intentos")
    void loginConCuentaBloqueadaVigenteLanzaLocked() {
        admin.setAccountLocked(true);
        admin.setFailedAttempts(3);
        admin.setLockExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        AccountLockedException ex = catchThrowableOfType(
                () -> authService.login(new LoginRequest("admin", "clave", "PC-1")), AccountLockedException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.LOCKED);
        assertThat(ex.getAttempts()).isEqualTo(3);
        assertThat(ex.getUnlockAt()).isAfter(LocalDateTime.now());
        assertThat(ex.getMessage()).contains("Cuenta bloqueada temporalmente");
        verify(userRepository, never()).incrementFailedAttempts(any());
        verify(userRepository, never()).lockAccount(any(), any());
    }

    @Test
    @DisplayName("Login con bloqueo expirado reinicia contadores y permite el acceso")
    void loginConBloqueoExpiradoRecuperaElAcceso() {
        admin.setAccountLocked(true);
        admin.setFailedAttempts(3);
        admin.setLockExpiresAt(LocalDateTime.now().minusMinutes(5));

        User recuperado = new User();
        recuperado.setId(ID_ADMIN);
        recuperado.setUsername("admin");
        recuperado.setPassword("hash");
        recuperado.setFullName("Administrador General");
        recuperado.setEmail("admin@sgm.com");
        recuperado.setRole(Role.ROLE_ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin)).thenReturn(Optional.of(recuperado));
        doReturn(Boolean.TRUE).when(passwordEncoder).matches("clave", "hash");
        when(machineService.registerIfAbsent("PC-1"))
                .thenReturn(new MachineTerminal("PC-1", "Terminal PC-1"));
        when(jwtService.generateAccessToken("admin", ID_ADMIN, "ROLE_ADMIN", "PC-1")).thenReturn("jwt-token");

        LoginResponse respuesta = authService.login(new LoginRequest("admin", "clave", "PC-1"));

        assertThat(respuesta.token()).isEqualTo("jwt-token");
        assertThat(respuesta.tokenType()).isEqualTo("Bearer");
        assertThat(respuesta.username()).isEqualTo("admin");
        assertThat(respuesta.fullName()).isEqualTo("Administrador General");
        assertThat(respuesta.role()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(respuesta.expiresInMs()).isEqualTo(3600000L);
        verify(userRepository, times(2)).resetSecurityCounters(ID_ADMIN);
    }

    @Test
    @DisplayName("Login exitoso limpia contadores y devuelve el token de acceso")
    void loginExitosoLimpiaContadoresYDevuelveToken() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        doReturn(Boolean.TRUE).when(passwordEncoder).matches("clave", "hash");
        when(machineService.registerIfAbsent("PC-1"))
                .thenReturn(new MachineTerminal("PC-1", "Terminal PC-1"));
        when(jwtService.generateAccessToken("admin", ID_ADMIN, "ROLE_ADMIN", "PC-1")).thenReturn("jwt-token");

        LoginResponse respuesta = authService.login(new LoginRequest("admin", "clave", "PC-1"));

        assertThat(respuesta.token()).isEqualTo("jwt-token");
        verify(userRepository).resetSecurityCounters(ID_ADMIN);
        verify(userRepository, never()).incrementFailedAttempts(any());
    }

    @Test
    @DisplayName("Contraseña incorrecta cuenta el intento y avisa los intentos restantes")
    void contrasenaIncorrectaIncrementaIntentosFallidos() {
        admin.setFailedAttempts(0);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        doReturn(Boolean.FALSE).when(passwordEncoder).matches("mala", "hash");

        ApiException ex = catchThrowableOfType(
                () -> authService.login(new LoginRequest("admin", "mala", "PC-1")), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("Contrase\u00f1a incorrecta. Le quedan 2 intentos.");
        verify(userRepository).incrementFailedAttempts(ID_ADMIN);
        verify(userRepository, never()).lockAccount(any(), any());
    }

    @Test
    @DisplayName("El tercer intento fallido bloquea la cuenta por 15 minutos")
    void tercerIntentoFallidoBloqueaLaCuenta() {
        admin.setFailedAttempts(2);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        doReturn(Boolean.FALSE).when(passwordEncoder).matches("mala", "hash");

        AccountLockedException ex = catchThrowableOfType(
                () -> authService.login(new LoginRequest("admin", "mala", "PC-1")), AccountLockedException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.LOCKED);
        assertThat(ex.getAttempts()).isEqualTo(3);
        assertThat(ex.getMessage()).contains("3 intentos fallidos").contains("15 minutos");

        ArgumentCaptor<LocalDateTime> expiracionCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).incrementFailedAttempts(ID_ADMIN);
        verify(userRepository).lockAccount(eq(ID_ADMIN), expiracionCaptor.capture());
        assertThat(expiracionCaptor.getValue()).isAfter(LocalDateTime.now());
        assertThat(ex.getUnlockAt()).isEqualTo(expiracionCaptor.getValue());
    }
}
