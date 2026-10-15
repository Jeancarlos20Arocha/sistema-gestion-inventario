package com.sgm.desktop.controller;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import com.sgm.desktop.session.SessionManager;
import com.sgm.shared.enums.Role;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Controlador de la vista del menú principal (JavaFX).
 * <p>
 * Recupera la sesión activa desde {@link SessionManager} para dar la bienvenida
 * al usuario y exponer sus datos básicos. Además, centraliza la navegación hacia
 * los módulos del sistema de inventario (productos, stock, categorías y punto de
 * venta) y el cierre de sesión.
 * <p>
 * Nota: al igual que el resto del módulo {@code client-desktop}, no se usan
 * anotaciones de Spring porque la aplicación es JavaFX pura; se emplean las
 * anotaciones propias de JavaFX ({@code @FXML}).
 */
public class MenuController {

    private static final Logger LOG = Logger.getLogger(MenuController.class.getName());

    private static final String RECURSO_LOGIN = "/fxml/login.fxml";
    private static final String RECURSO_PRODUCTOS = "/fxml/productos.fxml";
    private static final String RECURSO_STOCK = "/fxml/stock.fxml";
    private static final String RECURSO_CATEGORIAS = "/fxml/categorias.fxml";
    private static final String RECURSO_PUNTO_DE_VENTA = "/fxml/punto-venta.fxml";

    @FXML
    private Label bienvenidaLabel;

    @FXML
    private Label usuarioLabel;

    @FXML
    private Label estadoLabel;

    @FXML
    private StackPane contenidoPane;

    /**
     * Inicializa la vista tras cargar el FXML. Verifica que exista una sesión
     * activa y pinta los datos del usuario autenticado.
     */
    @FXML
    private void initialize() {
        SessionManager sesion = SessionManager.getInstancia();

        if (!sesion.isAutenticado()) {
            LOG.warning("Se intentó abrir el menú principal sin una sesión activa.");
            redirigirALogin();
            return;
        }

        String nombre = (sesion.getFullName() != null && !sesion.getFullName().isBlank())
                ? sesion.getFullName()
                : sesion.getUsername();
        bienvenidaLabel.setText("Bienvenido, " + nombre);

        usuarioLabel.setText("Usuario: " + sesion.getUsername()
                + "  |  Rol: " + formatearRol(sesion.getRole()));

        actualizarEstado("Sesión iniciada. Seleccione un módulo para comenzar.");
        LOG.info(() -> "Menú principal cargado para el usuario: " + sesion.getUsername());
    }

    // ------------------------------------------------------------------
    // Navegación hacia los módulos
    // ------------------------------------------------------------------

    /**
     * Abre el módulo de gestión de productos.
     */
    @FXML
    private void onGestionProductos() {
        navegarA(RECURSO_PRODUCTOS, "Gestión de Productos");
    }

    /**
     * Abre el módulo de control de stock.
     */
    @FXML
    private void onGestionStock() {
        navegarA(RECURSO_STOCK, "Control de Stock");
    }

    /**
     * Abre el módulo de gestión de categorías.
     */
    @FXML
    private void onGestionCategorias() {
        navegarA(RECURSO_CATEGORIAS, "Gestión de Categorías");
    }

    /**
     * Abre el módulo de punto de venta.
     */
    @FXML
    private void onPuntoDeVenta() {
        navegarA(RECURSO_PUNTO_DE_VENTA, "Punto de Venta");
    }

    // ------------------------------------------------------------------
    // Cierre de sesión
    // ------------------------------------------------------------------

    /**
     * Cierra la sesión actual, previa confirmación, y regresa al inicio de sesión.
     */
    @FXML
    private void onCerrarSesion() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cerrar sesión");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Desea cerrar la sesión actual?");

        boolean confirmado = confirmacion.showAndWait()
                .filter(boton -> boton == ButtonType.OK)
                .isPresent();

        if (!confirmado) {
            return;
        }

        SessionManager.getInstancia().cerrarSesion();
        LOG.info("La sesión fue cerrada por el usuario.");
        redirigirALogin();
    }

    // ------------------------------------------------------------------
    // Utilidades internas
    // ------------------------------------------------------------------

    /**
     * Carga una vista en el panel central. Si el recurso todavía no existe,
     * informa al usuario sin interrumpir la aplicación, dejando preparada la
     * estructura para cuando el módulo se implemente.
     *
     * @param recurso ruta del FXML del módulo (por ejemplo {@code /fxml/productos.fxml})
     * @param titulo  título descriptivo del módulo
     */
    private void navegarA(String recurso, String titulo) {
        URL ubicacion = getClass().getResource(recurso);

        if (ubicacion == null) {
            LOG.info(() -> "El recurso " + recurso + " (" + titulo + ") aún no está implementado.");
            actualizarEstado("Módulo \"" + titulo + "\" pendiente de implementación.");
            return;
        }

        try {
            Parent vista = new FXMLLoader(ubicacion).load();
            contenidoPane.getChildren().setAll(vista);
            actualizarEscenario("SGM - " + titulo);
            actualizarEstado("Módulo abierto: " + titulo);
            LOG.info(() -> "Navegando al módulo: " + recurso);
        } catch (IOException excepcion) {
            LOG.severe(() -> "No se pudo cargar la vista " + recurso + ": " + excepcion.getMessage());
            actualizarEstado("No se pudo abrir el módulo \"" + titulo + "\".");
        }
    }

    /**
     * Vuelve a la vista de inicio de sesión. Si la escena todavía no está
     * enlazada a una ventana (por ejemplo, durante la inicialización), difiere
     * la operación al hilo de JavaFX.
     */
    private void redirigirALogin() {
        if (contenidoPane.getScene() == null || contenidoPane.getScene().getWindow() == null) {
            Platform.runLater(this::redirigirALogin);
            return;
        }

        try {
            URL ubicacion = getClass().getResource(RECURSO_LOGIN);
            if (ubicacion == null) {
                LOG.severe(() -> "No se encontró el recurso " + RECURSO_LOGIN);
                return;
            }

            Parent raiz = new FXMLLoader(ubicacion).load();
            Stage escenario = (Stage) contenidoPane.getScene().getWindow();
            escenario.setTitle("SGM - Escritorio");
            escenario.setScene(new Scene(raiz, 420, 480));
            escenario.show();
        } catch (IOException excepcion) {
            LOG.severe(() -> "No se pudo volver al inicio de sesión: " + excepcion.getMessage());
        }
    }

    /**
     * Traduce el rol devuelto por el backend a un texto legible.
     */
    private String formatearRol(Role rol) {
        if (rol == null) {
            return "Sin rol";
        }
        return switch (rol) {
            case ROLE_ADMIN -> "Administrador";
            case ROLE_USER -> "Usuario";
        };
    }

    /**
     * Actualiza el texto de la barra de estado inferior.
     */
    private void actualizarEstado(String mensaje) {
        if (estadoLabel != null) {
            estadoLabel.setText(mensaje);
        }
    }

    /**
     * Actualiza el título de la ventana principal, si la escena ya está lista.
     */
    private void actualizarEscenario(String titulo) {
        if (contenidoPane.getScene() != null && contenidoPane.getScene().getWindow() instanceof Stage escenario) {
            escenario.setTitle(titulo);
        }
    }
}
