package com.sgm.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sgm.backend.entity.User;
import com.sgm.backend.exception.ApiException;
import com.sgm.backend.repository.UserRepository;
import com.sgm.shared.dto.ChangePasswordRequest;
import com.sgm.shared.dto.RegisterSecurityQuestionsRequest;
import com.sgm.shared.dto.UserDTO;
import com.sgm.shared.enums.Role;

/**
 * Pruebas unitarias de la gestión de usuarios: creación, protección del último
 * administrador activo, desbloqueo y cambio de contraseña.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @NonNull
    private static final Long ID_ADMIN = Objects.requireNonNull(Long.valueOf(1L));
    @NonNull
    private static final Long ID_USUARIO = Objects.requireNonNull(Long.valueOf(7L));

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RecoveryService recoveryService;

    @InjectMocks
    private UserService userService;

    private User admin;

    @BeforeEach
    void preparar() {
        admin = usuario(ID_ADMIN, Role.ROLE_ADMIN, true);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private User usuario(Long id, Role role, boolean enabled) {
        User usuario = new User();
        usuario.setId(id);
        usuario.setUsername("usuario" + id);
        usuario.setPassword("hash");
        usuario.setFullName("Usuario " + id);
        usuario.setEmail("usuario" + id + "@sgm.com");
        usuario.setRole(role);
        usuario.setEnabled(enabled);
        return usuario;
    }

    private void autenticarComo(String username) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, null));
    }

    @Test
    @DisplayName("Crear usuario con contraseñas distintas devuelve 400")
    void crearUsuarioConContrasenasNoCoincidenLanzaBadRequest() {
        UserDTO.CreateUserRequest request = new UserDTO.CreateUserRequest("juan", "Juan P\u00e9rez",
                "juan@mail.com", Role.ROLE_USER, "Clave123!", "Distinta123!", null);

        ApiException ex = catchThrowableOfType(() -> userService.create(request), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("Las contrase\u00f1as no coinciden");
        verifyNoInteractions(userRepository, passwordEncoder, recoveryService);
    }

    @Test
    @DisplayName("Crear usuario con nombre ya registrado devuelve 409")
    @SuppressWarnings("null")
    void crearUsuarioConNombreDuplicadoLanzaConflict() {
        UserDTO.CreateUserRequest request = new UserDTO.CreateUserRequest("juan", "Juan P\u00e9rez",
                "juan@mail.com", Role.ROLE_USER, "Clave123!", "Clave123!", null);
        doReturn(Boolean.TRUE).when(userRepository).existsByUsername("juan");

        ApiException ex = catchThrowableOfType(() -> userService.create(request), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("El nombre de usuario ya est\u00e1 registrado");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear usuario con correo ya registrado devuelve 409")
    @SuppressWarnings("null")
    void crearUsuarioConCorreoDuplicadoLanzaConflict() {
        UserDTO.CreateUserRequest request = new UserDTO.CreateUserRequest("juan", "Juan P\u00e9rez",
                "juan@mail.com", Role.ROLE_USER, "Clave123!", "Clave123!", null);
        doReturn(Boolean.FALSE).when(userRepository).existsByUsername("juan");
        doReturn(Boolean.TRUE).when(userRepository).existsByEmail("juan@mail.com");

        ApiException ex = catchThrowableOfType(() -> userService.create(request), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("El correo ya est\u00e1 registrado");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear usuario exitoso hashea la contraseña y normaliza el correo")
    @SuppressWarnings("null")
    void crearUsuarioExitosoHasheaYGuarda() {
        UserDTO.CreateUserRequest request = new UserDTO.CreateUserRequest("juan", "Juan P\u00e9rez",
                "Juan@Mail.COM", Role.ROLE_USER, "Clave123!", "Clave123!", null);
        doReturn(Boolean.FALSE).when(userRepository).existsByUsername("juan");
        doReturn(Boolean.FALSE).when(userRepository).existsByEmail("Juan@Mail.COM");
        when(passwordEncoder.encode("Clave123!")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocacion -> {
            User guardado = invocacion.getArgument(0);
            guardado.setId(ID_USUARIO);
            return guardado;
        });

        UserDTO dto = userService.create(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User guardado = userCaptor.getValue();
        assertThat(guardado.getUsername()).isEqualTo("juan");
        assertThat(guardado.getFullName()).isEqualTo("Juan P\u00e9rez");
        assertThat(guardado.getEmail()).isEqualTo("juan@mail.com");
        assertThat(guardado.getPassword()).isEqualTo("HASH");
        assertThat(guardado.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(dto.id()).isEqualTo(ID_USUARIO);
        assertThat(dto.enabled()).isTrue();
        verifyNoInteractions(recoveryService);
    }

    @Test
    @DisplayName("Crear usuario con preguntas de seguridad delega al servicio de recuperación")
    @SuppressWarnings("null")
    void crearUsuarioConPreguntasConfiguraSeguridad() {
        RegisterSecurityQuestionsRequest preguntas =
                new RegisterSecurityQuestionsRequest("Toby", "Barcelona", "Acuario");
        UserDTO.CreateUserRequest request = new UserDTO.CreateUserRequest("juan", "Juan P\u00e9rez",
                "juan@mail.com", Role.ROLE_USER, "Clave123!", "Clave123!", preguntas);
        doReturn(Boolean.FALSE).when(userRepository).existsByUsername("juan");
        doReturn(Boolean.FALSE).when(userRepository).existsByEmail("juan@mail.com");
        when(passwordEncoder.encode("Clave123!")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocacion -> {
            User guardado = invocacion.getArgument(0);
            guardado.setId(ID_USUARIO);
            return guardado;
        });

        userService.create(request);

        verify(recoveryService).configureQuestions(ID_USUARIO, preguntas);
    }

    @Test
    @DisplayName("Deshabilitar al último administrador activo devuelve 409")
    @SuppressWarnings("null")
    void deshabilitarUltimoAdminActivoLanzaConflict() {
        when(userRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(admin));

        ApiException ex = catchThrowableOfType(
                () -> userService.toggleEnabled(ID_ADMIN, Boolean.FALSE), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("No puede deshabilitar al \u00faltimo administrador activo");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deshabilitar al último administrador con otros inactivos devuelve 409")
    @SuppressWarnings("null")
    void deshabilitarUltimoAdminConOtrosInactivosLanzaConflict() {
        User otroAdmin = usuario(Long.valueOf(2L), Role.ROLE_ADMIN, false);
        when(userRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(admin, otroAdmin));

        ApiException ex = catchThrowableOfType(
                () -> userService.toggleEnabled(ID_ADMIN, Boolean.FALSE), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deshabilitar un administrador con otros activos está permitido")
    @SuppressWarnings("null")
    void deshabilitarAdminConOtrosActivosEsPermitido() {
        User otroAdmin = usuario(Long.valueOf(2L), Role.ROLE_ADMIN, true);
        when(userRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(admin, otroAdmin));
        when(userRepository.save(any(User.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UserDTO dto = userService.toggleEnabled(ID_ADMIN, Boolean.FALSE);

        assertThat(dto.enabled()).isFalse();
        assertThat(admin.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Habilitar al último administrador está permitido sin consultar admins")
    @SuppressWarnings("null")
    void habilitarUltimoAdminEsPermitido() {
        when(userRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UserDTO dto = userService.toggleEnabled(ID_ADMIN, Boolean.TRUE);

        assertThat(dto.enabled()).isTrue();
        verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("Deshabilitar un usuario normal está permitido")
    @SuppressWarnings("null")
    void deshabilitarUsuarioNormalEsPermitido() {
        User usuario = usuario(ID_USUARIO, Role.ROLE_USER, true);
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(userRepository.save(any(User.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UserDTO dto = userService.toggleEnabled(ID_USUARIO, Boolean.FALSE);

        assertThat(dto.enabled()).isFalse();
        assertThat(usuario.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Un estado nulo en toggleEnabled se interpreta como deshabilitar (null-safe)")
    @SuppressWarnings("null")
    void toggleEnabledConNullDeshabilitaAlUsuario() {
        User usuario = usuario(ID_USUARIO, Role.ROLE_USER, true);
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(userRepository.save(any(User.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UserDTO dto = userService.toggleEnabled(ID_USUARIO, null);

        assertThat(dto.enabled()).isFalse();
        assertThat(usuario.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Desbloquear restaura el estado de la cuenta")
    @SuppressWarnings("null")
    void desbloquearUsuarioRestauraElEstado() {
        admin.setAccountLocked(true);
        admin.setFailedAttempts(3);
        admin.setLockExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UserDTO dto = userService.unlock(ID_ADMIN);

        assertThat(dto.accountLocked()).isFalse();
        assertThat(dto.failedAttempts()).isZero();
        assertThat(dto.lockExpiresAt()).isNull();
        assertThat(admin.isAccountLocked()).isFalse();
        assertThat(admin.getFailedAttempts()).isZero();
        assertThat(admin.getLockExpiresAt()).isNull();
    }

    @Test
    @DisplayName("Desbloquear un usuario inexistente devuelve 404")
    void desbloquearUsuarioInexistenteLanzaNotFound() {
        when(userRepository.findById(ID_ADMIN)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(() -> userService.unlock(ID_ADMIN), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("Usuario no encontrado");
    }

    @Test
    @DisplayName("Consultar el perfil de un usuario inexistente devuelve 404")
    void perfilUsuarioInexistenteLanzaNotFound() {
        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(() -> userService.me("fantasma"), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Cambiar contraseña con la actual incorrecta devuelve 400")
    @SuppressWarnings("null")
    void cambiarContrasenaActualIncorrectaLanzaBadRequest() {
        autenticarComo("usuario1");
        when(userRepository.findByUsername("usuario1")).thenReturn(Optional.of(admin));
        doReturn(Boolean.FALSE).when(passwordEncoder).matches("MalaClave1!", "hash");
        ChangePasswordRequest request = new ChangePasswordRequest("MalaClave1!", "NuevaClave1!", "NuevaClave1!");

        ApiException ex = catchThrowableOfType(() -> userService.changePassword(request), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("La contrase\u00f1a actual es incorrecta");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambiar contraseña exitoso actualiza el hash almacenado")
    @SuppressWarnings("null")
    void cambiarContrasenaExitosoActualizaElHash() {
        autenticarComo("usuario1");
        when(userRepository.findByUsername("usuario1")).thenReturn(Optional.of(admin));
        doReturn(Boolean.TRUE).when(passwordEncoder).matches("Clave123!", "hash");
        when(passwordEncoder.encode("NuevaClave1!")).thenReturn("NUEVO-HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        ChangePasswordRequest request = new ChangePasswordRequest("Clave123!", "NuevaClave1!", "NuevaClave1!");

        userService.changePassword(request);

        assertThat(admin.getPassword()).isEqualTo("NUEVO-HASH");
        verify(userRepository).save(Objects.requireNonNull(admin));
    }
}
