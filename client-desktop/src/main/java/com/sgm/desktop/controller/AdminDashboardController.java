package com.sgm.desktop.controller;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.sgm.desktop.client.ApiClientFactory;
import com.sgm.desktop.session.SessionManager;
import com.sgm.desktop.util.MensajeError;
import com.sgm.shared.dto.BatchDTO;
import com.sgm.shared.dto.ItemDTO;
import com.sgm.shared.dto.StockMovementDTO;
import com.sgm.shared.dto.UserDTO;
import com.sgm.shared.enums.Role;
import com.tuempresa.inventario.client.ApiClient;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

/**
 * Controlador del Panel de Administración (JavaFX).
 * <p>
 * Expone, mediante pestañas, las secciones clave para un administrador de
 * inventario/POS: gestión de inventario (ítems, stock, costos y precios),
 * control de lotes, movimientos de stock y control de usuarios con auditoría por
 * terminal ({@code machine_id}). Consume los endpoints {@code /api/admin/**} a
 * través del {@link ApiClient} autenticado y ejecuta las peticiones en hilos
 * secundarios para no bloquear la interfaz.
 */
public class AdminDashboardController {

    private static final Logger LOG = Logger.getLogger(AdminDashboardController.class.getName());

    private static final String RECURSO_LOGIN = "/fxml/login.fxml";
    private static final String RUTA_ITEMS = "/api/admin/items";
    private static final String RUTA_LOTES = "/api/admin/batches";
    private static final String RUTA_MOVIMIENTOS = "/api/admin/stock/movements";
    private static final String RUTA_USUARIOS = "/api/admin/users";

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ApiClient apiClient = ApiClientFactory.autenticado();

    @FXML
    private Label bienvenidaLabel;

    @FXML
    private Label usuarioLabel;

    @FXML
    private Label estadoLabel;

    @FXML
    private TableView<ItemDTO> itemsTable;

    @FXML
    private TableView<BatchDTO> batchesTable;

    @FXML
    private TableView<StockMovementDTO> movementsTable;

    @FXML
    private TableView<UserDTO> usersTable;

    /**
     * Configura la cabecera de usuario, las columnas de las tablas y lanza la
     * carga inicial de datos.
     */
    @FXML
    private void initialize() {
        SessionManager sesion = SessionManager.getInstancia();
        if (!sesion.isAutenticado()) {
            LOG.warning("Se abrió el panel de administración sin sesión activa.");
            actualizarEstado("Sesión no válida. Vuelva a iniciar sesión.");
            return;
        }

        String nombre = (sesion.getFullName() != null && !sesion.getFullName().isBlank())
                ? sesion.getFullName()
                : sesion.getUsername();
        bienvenidaLabel.setText("Bienvenido, " + nombre);
        usuarioLabel.setText("Usuario: " + sesion.getUsername()
                + "  |  Rol: " + formatearRol(sesion.getRole()));

        configurarColumnasItems();
        configurarColumnasLotes();
        configurarColumnasMovimientos();
        configurarColumnasUsuarios();

        onRefrescarInventario();
        onRefrescarLotes();
        onRefrescarMovimientos();
        onRefrescarUsuarios();
    }

    // ------------------------------------------------------------------
    // Inventario
    // ------------------------------------------------------------------

    @FXML
    private void onRefrescarInventario() {
        actualizarEstado("Cargando inventario...");
        ejecutarAsync(
                () -> apiClient.get(RUTA_ITEMS, RespuestaItems.class),
                respuesta -> {
                    itemsTable.getItems().setAll(respuesta.data() == null ? List.of() : respuesta.data());
                    actualizarEstado(itemsTable.getItems().size() + " artículo(s) cargado(s).");
                });
    }

    private void configurarColumnasItems() {
        itemsTable.getColumns().setAll(List.of(
                columnaTexto("Código de barras", i -> i.barcode()),
                columnaTexto("Nombre", i -> i.name()),
                columnaTexto("Categoría", i -> i.categoryName()),
                columnaTexto("Unidad", i -> i.unit()),
                columnaTexto("Stock", i -> decimal(i.stock())),
                columnaTexto("Stock mín.", i -> decimal(i.minStock())),
                columnaTexto("Costo", i -> decimal(i.cost())),
                columnaTexto("Precio", i -> decimal(i.price())),
                columnaTexto("Activo", i -> i.active() ? "Sí" : "No")));
    }

    // ------------------------------------------------------------------
    // Lotes
    // ------------------------------------------------------------------

    @FXML
    private void onRefrescarLotes() {
        actualizarEstado("Cargando lotes...");
        ejecutarAsync(
                () -> apiClient.get(RUTA_LOTES, RespuestaLotes.class),
                respuesta -> {
                    List<BatchDTO> lotes = respuesta.data();
                    batchesTable.getItems().setAll(lotes == null ? List.of() : lotes);
                    actualizarEstado(batchesTable.getItems().size() + " lote(s) cargado(s).");
                });
    }

    private void configurarColumnasLotes() {
        batchesTable.getColumns().setAll(List.of(
                columnaTexto("Código de lote", b -> b.batchCode()),
                columnaTexto("Ítem", b -> b.itemName()),
                columnaTexto("Código de barras", b -> b.itemBarcode()),
                columnaTexto("Proveedor", b -> b.provider()),
                columnaTexto("Cantidad", b -> decimal(b.quantity())),
                columnaTexto("Costo", b -> decimal(b.cost())),
                columnaTexto("Caducidad", b -> b.expiryDate() == null ? "-" : b.expiryDate().format(FORMATO_FECHA)),
                columnaTexto("Recepción", b -> b.receivedDate() == null ? "-" : b.receivedDate().format(FORMATO_FECHA))));
    }

    // ------------------------------------------------------------------
    // Movimientos de stock
    // ------------------------------------------------------------------

    @FXML
    private void onRefrescarMovimientos() {
        actualizarEstado("Cargando movimientos...");
        ejecutarAsync(
                () -> apiClient.get(RUTA_MOVIMIENTOS, RespuestaMovimientos.class),
                respuesta -> {
                    List<StockMovementDTO> movimientos = respuesta.data();
                    movementsTable.getItems().setAll(movimientos == null ? List.of() : movimientos);
                    actualizarEstado(movementsTable.getItems().size() + " movimiento(s) cargado(s).");
                });
    }

    private void configurarColumnasMovimientos() {
        movementsTable.getColumns().setAll(List.of(
                columnaTexto("Fecha", m -> m.createdAt() == null ? "-" : m.createdAt().format(FORMATO_FECHA_HORA)),
                columnaTexto("Ítem", m -> m.itemName()),
                columnaTexto("Tipo", m -> m.movementType() == null ? "-" : m.movementType().name()),
                columnaTexto("Cantidad", m -> decimal(m.quantity())),
                columnaTexto("Usuario", m -> m.username()),
                columnaTexto("Terminal (machine_id)", m -> m.machineId()),
                columnaTexto("Nota", m -> m.note())));
    }

    // ------------------------------------------------------------------
    // Usuarios y auditoría
    // ------------------------------------------------------------------

    @FXML
    private void onRefrescarUsuarios() {
        actualizarEstado("Cargando usuarios...");
        ejecutarAsync(
                () -> apiClient.get(RUTA_USUARIOS, RespuestaUsuarios.class),
                respuesta -> {
                    List<UserDTO> usuarios = respuesta.data();
                    usersTable.getItems().setAll(usuarios == null ? List.of() : usuarios);
                    actualizarEstado(usersTable.getItems().size() + " usuario(s) cargado(s).");
                });
    }

    private void configurarColumnasUsuarios() {
        usersTable.getColumns().setAll(List.of(
                columnaTexto("ID", u -> u.id() == null ? "-" : u.id().toString()),
                columnaTexto("Usuario", u -> u.username()),
                columnaTexto("Nombre completo", u -> u.fullName()),
                columnaTexto("Rol", u -> formatearRol(u.role())),
                columnaTexto("Activo", u -> u.enabled() ? "Sí" : "No"),
                columnaTexto("Bloqueado", u -> u.accountLocked() ? "Sí" : "No"),
                columnaTexto("Intentos fallidos", u -> Integer.toString(u.failedAttempts())),
                columnaTexto("Bloqueo hasta", u -> u.lockExpiresAt() == null ? "-" : u.lockExpiresAt().format(FORMATO_FECHA_HORA))));
    }

    // ------------------------------------------------------------------
    // Cierre de sesión y navegación
    // ------------------------------------------------------------------

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
        volverAlLogin();
    }

    private void volverAlLogin() {
        try {
            URL ubicacion = getClass().getResource(RECURSO_LOGIN);
            if (ubicacion == null) {
                LOG.severe(() -> "No se encontró el recurso " + RECURSO_LOGIN);
                return;
            }
            Parent raiz = new FXMLLoader(ubicacion).load();
            Stage escenario = (Stage) bienvenidaLabel.getScene().getWindow();
            escenario.setTitle("SGM - Escritorio");
            escenario.setScene(new Scene(raiz, 420, 480));
            escenario.show();
        } catch (IOException excepcion) {
            LOG.severe(() -> "No se pudo volver al login: " + excepcion.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Utilidades internas
    // ------------------------------------------------------------------

    /**
     * Ejecuta una llamada al backend en un hilo secundario y entrega el resultado
     * (o el error) en el hilo de JavaFX.
     */
    private <T> void ejecutarAsync(Callable<T> operacion, Consumer<T> alExito) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operacion.call();
            }
        };

        tarea.setOnSucceeded(evento -> alExito.accept(tarea.getValue()));
        tarea.setOnFailed(evento -> {
            Throwable causa = tarea.getException();
            LOG.warning(() -> "Error en el panel de administración: " + causa);
            actualizarEstado("Error: " + MensajeError.desde(causa));
        });

        Thread hilo = new Thread(tarea, "admin-api");
        hilo.setDaemon(true);
        hilo.start();
    }

    private <T> TableColumn<T, String> columnaTexto(String titulo, Function<T, String> extractor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(celda ->
                new ReadOnlyStringWrapper(extractor.apply(celda.getValue())));
        columna.setSortable(false);
        return columna;
    }

    private String decimal(java.math.BigDecimal valor) {
        return valor == null ? "-" : valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String formatearRol(Role rol) {
        if (rol == null) {
            return "Sin rol";
        }
        return switch (rol) {
            case ROLE_ADMIN -> "Administrador";
            case ROLE_USER -> "Usuario";
        };
    }

    private void actualizarEstado(String mensaje) {
        if (estadoLabel != null) {
            estadoLabel.setText(mensaje);
        }
    }

    // ------------------------------------------------------------------
    // Envoltorios tipados de las respuestas del backend (ApiResponse)
    // ------------------------------------------------------------------

    private record RespuestaItems(boolean success, int code, String message, String timestamp, List<ItemDTO> data) {
    }

    private record RespuestaLotes(boolean success, int code, String message, String timestamp, List<BatchDTO> data) {
    }

    private record RespuestaMovimientos(boolean success, int code, String message, String timestamp,
                                        List<StockMovementDTO> data) {
    }

    private record RespuestaUsuarios(boolean success, int code, String message, String timestamp, List<UserDTO> data) {
    }
}
