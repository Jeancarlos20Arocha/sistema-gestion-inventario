package com.sgm.desktop.controller;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Logger;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgm.desktop.client.ApiClientFactory;
import com.sgm.desktop.session.SessionManager;
import com.sgm.desktop.util.MachineIdProvider;
import com.sgm.shared.dto.LoginRequest;
import com.sgm.shared.dto.LoginResponse;
import com.sgm.shared.enums.Role;
import com.tuempresa.inventario.client.ApiClient;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Controlador de la vista de inicio de sesión (JavaFX).
 * <p>
 * Envía las credenciales al endpoint {@code POST /api/auth/login} del backend a
 * través de {@link ApiClient}. Si la autenticación es correcta, almacena el
 * token en {@link SessionManager} y navega, con una transición de salida, a la
 * vista correspondiente según el rol del usuario (panel de administración o
 * punto de venta). Si falla, muestra al usuario el mensaje de error devuelto por
 * el servidor.
 * <p>
 * La vista incorpora animaciones de entrada (fade-in + slide-in), realce sutil
 * al enfocar los campos y al pasar el cursor sobre el botón, así como mensajes
 * de error animados. La llamada HTTP se ejecuta en un hilo secundario mediante
 * {@link Task} para no bloquear el hilo de la interfaz de JavaFX.
 * <p>
 * Nota: este módulo ({@code client-desktop}) es una aplicación JavaFX sin
 * contexto Spring, por lo que el controlador no lleva anotaciones de Spring. La
 * dependencia {@link ApiClient} se inyecta por constructor.
 */
public class LoginController {

    private static final Logger LOG = Logger.getLogger(LoginController.class.getName());

    private static final String RUTA_LOGIN = "/api/auth/login";
    private static final String RECURSO_MENU = "/fxml/menu.fxml";
    private static final String RECURSO_ADMIN_DASHBOARD = "/fxml/admin-dashboard.fxml";
    private static final String RECURSO_PUNTO_DE_VENTA = "/fxml/punto-venta.fxml";

    private static final Duration DURACION_ENTRADA = Duration.millis(450);
    private static final Duration DURACION_TRANSICION = Duration.millis(350);
    private static final Duration DURACION_REALCE = Duration.millis(140);

    private static final String ESTILO_INFO = "info";
    private static final String ESTILO_ERROR = "error";
    private static final String ESTILO_EXITO = "success";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ApiClient apiClient;

    @FXML
    private StackPane loginRoot;

    @FXML
    private VBox loginCard;

    @FXML
    private TextField usuarioField;

    @FXML
    private PasswordField claveField;

    @FXML
    private Button ingresarButton;

    @FXML
    private ProgressIndicator cargandoIndicator;

    @FXML
    private Label mensajeLabel;

    /**
     * Constructor sin argumentos requerido por {@link FXMLLoader}.
     * Crea un {@link ApiClient} apuntando a la URL base configurada.
     */
    public LoginController() {
        this(ApiClientFactory.sinAutenticacion());
    }

    /**
     * Constructor para inyección de dependencias (pruebas o configuración externa).
     *
     * @param apiClient cliente HTTP con el que se consumirá el backend
     */
    public LoginController(ApiClient apiClient) {
        this.apiClient = Objects.requireNonNull(apiClient, "apiClient no puede ser nulo");
    }

    /**
     * Inicializa la vista tras cargar el FXML: prepara la animación de entrada y
     * los efectos de realce de los controles.
     */
    @FXML
    private void initialize() {
        reproducirAnimacionEntrada();
        aplicarRealceEnfoque(usuarioField);
        aplicarRealceEnfoque(claveField);
        aplicarRealceBoton(ingresarButton);
    }

    /**
     * Acción disparada por el botón "Ingresar". Valida los campos y lanza la
     * autenticación de forma asíncrona.
     */
    @FXML
    private void onIngresar() {
        String usuario = usuarioField.getText() == null ? "" : usuarioField.getText().trim();
        String clave = claveField.getText() == null ? "" : claveField.getText();

        if (usuario.isEmpty() && clave.isEmpty()) {
            mostrarMensaje("Debe ingresar el usuario y la contraseña.", true);
            return;
        }
        if (usuario.isEmpty()) {
            mostrarMensaje("Debe ingresar el usuario.", true);
            return;
        }
        if (clave.isEmpty()) {
            mostrarMensaje("Debe ingresar la contraseña.", true);
            return;
        }

        autenticarAsync(usuario, clave);
    }

    /**
     * Ejecuta la llamada de autenticación en un hilo secundario.
     *
     * @param usuario nombre de usuario ingresado
     * @param clave   contraseña ingresada
     */
    private void autenticarAsync(String usuario, String clave) {
        actualizarMensaje("Autenticando...", ESTILO_INFO, false);
        setFormularioHabilitado(false);

        Task<LoginResponse> tarea = new Task<>() {
            @Override
            protected LoginResponse call() {
                return autenticar(usuario, clave);
            }
        };

        tarea.setOnSucceeded(evento -> {
            setFormularioHabilitado(true);
            LoginResponse respuesta = tarea.getValue();
            SessionManager.getInstancia().iniciarSesion(respuesta);
            LOG.info(() -> "Inicio de sesión exitoso para el usuario: " + respuesta.username());
            navegarSegunRol();
        });

        tarea.setOnFailed(evento -> {
            setFormularioHabilitado(true);
            Throwable causa = tarea.getException();
            LOG.warning(() -> "Fallo el inicio de sesión para el usuario '" + usuario + "': " + causa);
            mostrarMensaje(mensajeDeError(causa), true);
        });

        Thread hilo = new Thread(tarea, "login-api");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Realiza la petición de login contra el backend y devuelve el contenido útil
     * de la respuesta.
     *
     * @throws IllegalStateException si el servidor responde sin datos de sesión
     */
    private LoginResponse autenticar(String usuario, String clave) {
        LoginRequest solicitud = new LoginRequest(usuario, clave, obtenerMachineId());

        RespuestaApi respuesta = apiClient.post(RUTA_LOGIN, solicitud, RespuestaApi.class);

        if (respuesta == null || !respuesta.success() || respuesta.data() == null) {
            String mensaje = (respuesta != null && respuesta.message() != null)
                    ? respuesta.message()
                    : "Respuesta inválida del servidor";
            throw new IllegalStateException(mensaje);
        }

        return respuesta.data();
    }

    // ------------------------------------------------------------------
    // Navegación según el rol con transición de salida
    // ------------------------------------------------------------------

    /**
     * Aplica una transición de desvanecimiento de salida y, al terminar, abre la
     * vista correspondiente al rol del usuario: panel de administración para
     * {@code ROLE_ADMIN} o punto de venta para {@code ROLE_USER}. Si la vista
     * objetivo no existe todavía, se usa el menú principal como respaldo.
     */
    private void navegarSegunRol() {
        Role rol = SessionManager.getInstancia().getRole();

        String recurso = (rol == Role.ROLE_USER) ? RECURSO_PUNTO_DE_VENTA : RECURSO_ADMIN_DASHBOARD;
        String titulo = (rol == Role.ROLE_USER) ? "SGM - Punto de Venta" : "SGM - Panel de Administración";

        if (getClass().getResource(recurso) == null) {
            LOG.warning("No se encontró el recurso " + recurso + "; se usará el menú principal.");
            recurso = RECURSO_MENU;
            titulo = "SGM - Menú principal";
        }

        if (getClass().getResource(recurso) == null) {
            mostrarMensaje("Sesión iniciada correctamente. Falta la vista del sistema.", false);
            return;
        }

        final String recursoDestino = recurso;
        final String tituloDestino = titulo;

        FadeTransition salida = new FadeTransition(DURACION_TRANSICION, loginRoot);
        salida.setFromValue(1.0);
        salida.setToValue(0.0);
        salida.setInterpolator(Interpolator.EASE_IN);
        salida.setOnFinished(evento -> abrirVista(recursoDestino, tituloDestino));
        salida.play();
    }

    /**
     * Carga la vista destino reemplazando la escena actual y la muestra con un
     * suave fade-in. Si falla la carga, restaura la vista de login.
     */
    private void abrirVista(String recurso, String titulo) {
        URL ubicacion = getClass().getResource(recurso);
        if (ubicacion == null) {
            restaurarLogin();
            mostrarMensaje("No se encontró la vista solicitada.", true);
            return;
        }

        try {
            Parent raiz = new FXMLLoader(ubicacion).load();
            Stage escenario = (Stage) loginRoot.getScene().getWindow();
            raiz.setOpacity(0.0);
            escenario.setTitle(titulo);
            escenario.setScene(new Scene(raiz, 1024, 700));
            escenario.show();

            FadeTransition entrada = new FadeTransition(DURACION_TRANSICION, raiz);
            entrada.setFromValue(0.0);
            entrada.setToValue(1.0);
            entrada.setInterpolator(Interpolator.EASE_OUT);
            entrada.play();

            LOG.info(() -> "Navegando a la vista: " + recurso);
        } catch (IOException excepcion) {
            LOG.severe(() -> "Error al cargar la vista " + recurso + ": " + excepcion.getMessage());
            restaurarLogin();
            mostrarMensaje("No se pudo abrir la vista solicitada.", true);
        }
    }

    /**
     * Restaura la opacidad del formulario de login tras una transición fallida.
     */
    private void restaurarLogin() {
        if (loginRoot != null) {
            loginRoot.setOpacity(1.0);
        }
    }

    // ------------------------------------------------------------------
    // Animaciones
    // ------------------------------------------------------------------

    /**
     * Reproduce la animación de entrada del contenedor de login: un fade-in
     * combinado con un desplazamiento vertical (slide-in) suave.
     */
    private void reproducirAnimacionEntrada() {
        loginCard.setOpacity(0.0);
        loginCard.setTranslateY(28.0);

        FadeTransition fade = new FadeTransition(DURACION_ENTRADA, loginCard);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(DURACION_ENTRADA, loginCard);
        slide.setFromY(28.0);
        slide.setToY(0.0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }

    /**
     * Añade un realce sutil al enfocar/desenfocar un campo de texto mediante una
     * pequeña animación de escala (apoyada además por el estilo CSS {@code :focused}).
     */
    private void aplicarRealceEnfoque(TextField campo) {
        if (campo == null) {
            return;
        }

        ScaleTransition agrandar = new ScaleTransition(DURACION_REALCE, campo);
        agrandar.setToX(1.02);
        agrandar.setToY(1.02);
        agrandar.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition reducir = new ScaleTransition(DURACION_REALCE, campo);
        reducir.setToX(1.0);
        reducir.setToY(1.0);
        reducir.setInterpolator(Interpolator.EASE_OUT);

        campo.focusedProperty().addListener((observable, antes, enfocado) -> {
            if (Boolean.TRUE.equals(enfocado)) {
                reducir.stop();
                agrandar.playFromStart();
            } else {
                agrandar.stop();
                reducir.playFromStart();
            }
        });
    }

    /**
     * Añade un realce sutil al pasar el cursor sobre un botón mediante una
     * pequeña animación de escala (apoyada además por el estilo CSS {@code :hover}).
     */
    private void aplicarRealceBoton(Button boton) {
        if (boton == null) {
            return;
        }

        ScaleTransition agrandar = new ScaleTransition(DURACION_REALCE, boton);
        agrandar.setToX(1.03);
        agrandar.setToY(1.03);
        agrandar.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition reducir = new ScaleTransition(DURACION_REALCE, boton);
        reducir.setToX(1.0);
        reducir.setToY(1.0);
        reducir.setInterpolator(Interpolator.EASE_OUT);

        boton.setOnMouseEntered(evento -> {
            reducir.stop();
            agrandar.playFromStart();
        });
        boton.setOnMouseExited(evento -> {
            agrandar.stop();
            reducir.playFromStart();
        });
    }

    // ------------------------------------------------------------------
    // Mensajes
    // ------------------------------------------------------------------

    /**
     * Muestra un mensaje en la etiqueta de la vista con estilo de error o éxito.
     *
     * @param texto mensaje a mostrar
     * @param error {@code true} para mostrarlo como error, {@code false} como éxito
     */
    private void mostrarMensaje(String texto, boolean error) {
        actualizarMensaje(texto, error ? ESTILO_ERROR : ESTILO_EXITO, error);
    }

    /**
     * Actualiza el texto y el estilo del mensaje y reproduce una animación suave
     * de aparición (con un sutil "shake" cuando es un error).
     */
    private void actualizarMensaje(String texto, String estilo, boolean conSacudida) {
        mensajeLabel.setText(texto);
        mensajeLabel.getStyleClass().removeAll(ESTILO_INFO, ESTILO_ERROR, ESTILO_EXITO);
        mensajeLabel.getStyleClass().add(estilo);
        mensajeLabel.setOpacity(0.0);
        mensajeLabel.setTranslateX(0.0);

        FadeTransition fade = new FadeTransition(Duration.millis(250), mensajeLabel);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        if (!conSacudida) {
            fade.play();
            return;
        }

        TranslateTransition sacudida = new TranslateTransition(Duration.millis(60), mensajeLabel);
        sacudida.setFromX(-6.0);
        sacudida.setToX(6.0);
        sacudida.setCycleCount(4);
        sacudida.setAutoReverse(true);
        sacudida.setOnFinished(evento -> mensajeLabel.setTranslateX(0.0));

        new ParallelTransition(fade, sacudida).play();
    }

    /**
     * Traduce la excepción producida por la llamada HTTP a un mensaje legible.
     *
     * @param causa excepción lanzada durante la autenticación
     * @return mensaje para mostrar en la vista
     */
    private String mensajeDeError(Throwable causa) {
        if (causa instanceof RestClientResponseException excepcionRespuesta) {
            return extraerMensajeServidor(excepcionRespuesta);
        }
        if (causa instanceof ResourceAccessException) {
            return "No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.";
        }
        if (causa instanceof RestClientException) {
            return "Error de comunicación con el servidor.";
        }

        String mensaje = causa.getMessage();
        return (mensaje != null && !mensaje.isBlank()) ? mensaje : "Error inesperado al iniciar sesión.";
    }

    /**
     * Extrae el campo {@code message} del cuerpo de error devuelto por el backend
     * ({@code ApiResponse}). Si no es posible, usa un texto según el código HTTP.
     */
    private String extraerMensajeServidor(RestClientResponseException excepcion) {
        try {
            // La sobrecarga sin argumento decodifica el cuerpo como UTF-8 (comportamiento identico).
            String cuerpo = excepcion.getResponseBodyAsString();
            if (!cuerpo.isBlank()) {
                JsonNode nodoMensaje = MAPPER.readTree(cuerpo).path("message");
                if (!nodoMensaje.isMissingNode() && !nodoMensaje.isNull() && !nodoMensaje.asText().isBlank()) {
                    return nodoMensaje.asText();
                }
            }
        } catch (IOException excepcionParseo) {
            LOG.fine(() -> "No se pudo interpretar el cuerpo de error: " + excepcionParseo.getMessage());
        }

        int codigo = excepcion.getStatusCode().value();
        return switch (codigo) {
            case 400 -> "Usuario o contraseña incorrectos.";
            case 401 -> "Credenciales inválidas.";
            case 403 -> "La cuenta está deshabilitada.";
            case 423 -> "La cuenta está bloqueada temporalmente. Intente más tarde.";
            default -> "Error al iniciar sesión (código " + codigo + ").";
        };
    }

    /**
     * Obtiene un identificador de máquina estable para enviarlo al backend.
     *
     * @return identificador de máquina normalizado
     */
    private String obtenerMachineId() {
        return MachineIdProvider.obtener();
    }

    /**
     * Habilita o deshabilita los controles del formulario durante la llamada y
     * muestra u oculta el indicador de progreso.
     */
    private void setFormularioHabilitado(boolean habilitado) {
        usuarioField.setDisable(!habilitado);
        claveField.setDisable(!habilitado);
        if (ingresarButton != null) {
            ingresarButton.setDisable(!habilitado);
        }
        if (cargandoIndicator != null) {
            cargandoIndicator.setVisible(!habilitado);
            cargandoIndicator.setManaged(!habilitado);
        }
    }

    /**
     * Envoltorio tipado de la respuesta estándar del backend ({@code ApiResponse}).
     * Permite deserializar el campo {@code data} directamente como {@link LoginResponse}.
     */
    private record RespuestaApi(
            boolean success,
            int code,
            String message,
            String timestamp,
            LoginResponse data) {
    }
}
