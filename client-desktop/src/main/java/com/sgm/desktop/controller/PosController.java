package com.sgm.desktop.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.sgm.desktop.client.ApiClientFactory;
import com.sgm.desktop.session.SessionManager;
import com.sgm.desktop.util.MachineIdProvider;
import com.sgm.desktop.util.MensajeError;
import com.sgm.shared.dto.ItemDTO;
import com.sgm.shared.dto.SaleDTO;
import com.sgm.shared.dto.SaleRequest;
import com.sgm.shared.enums.Role;
import com.tuempresa.inventario.client.ApiClient;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controlador de la vista de Punto de Venta / Caja (JavaFX).
 * <p>
 * Interfaz ágil orientada al cajero ({@code ROLE_USER}): permite escanear o
 * buscar un artículo por código de barras, construir el carrito validando las
 * cantidades contra el stock disponible y procesar el cobro. La venta se envía
 * al backend ({@code POST /api/pos/sales}) a través del {@link ApiClient}
 * autenticado, incluyendo el {@code machine_id} de la terminal.
 */
public class PosController {

    private static final Logger LOG = Logger.getLogger(PosController.class.getName());

    private static final String RECURSO_LOGIN = "/fxml/login.fxml";
    private static final String RUTA_ITEM_POR_BARCODE = "/api/pos/items/{barcode}";
    private static final String RUTA_VENTA = "/api/pos/sales";

    private final ApiClient apiClient = ApiClientFactory.autenticado();
    private final ObservableList<LineaVenta> carrito = FXCollections.observableArrayList();

    @FXML
    private Label bienvenidaLabel;

    @FXML
    private Label usuarioLabel;

    @FXML
    private TextField barcodeField;

    @FXML
    private TextField cantidadField;

    @FXML
    private Button agregarButton;

    @FXML
    private Button quitarButton;

    @FXML
    private TableView<LineaVenta> carritoTable;

    @FXML
    private Label totalLabel;

    @FXML
    private Label articulosLabel;

    @FXML
    private TextField efectivoField;

    @FXML
    private Label cambioLabel;

    @FXML
    private Button procesarButton;

    @FXML
    private Label mensajeLabel;

    @FXML
    private Label estadoLabel;

    /**
     * Configura la cabecera, las columnas del carrito y los eventos de la vista.
     */
    @FXML
    private void initialize() {
        SessionManager sesion = SessionManager.getInstancia();
        if (!sesion.isAutenticado()) {
            LOG.warning("Se abrió el punto de venta sin sesión activa.");
            actualizarEstado("Sesión no válida. Vuelva a iniciar sesión.");
            return;
        }

        String nombre = (sesion.getFullName() != null && !sesion.getFullName().isBlank())
                ? sesion.getFullName()
                : sesion.getUsername();
        bienvenidaLabel.setText("Bienvenido, " + nombre);
        usuarioLabel.setText("Usuario: " + sesion.getUsername()
                + "  |  Rol: " + formatearRol(sesion.getRole()));

        configurarColumnasCarrito();
        carritoTable.setItems(carrito);

        // Actualiza el cambio cada vez que se modifica el efectivo recibido.
        efectivoField.textProperty().addListener((observable, antes, ahora) -> actualizarCambio());

        // Escanear/agregar con Enter desde el campo de código de barras.
        barcodeField.setOnAction(evento -> onAgregar());

        actualizarTotales();
        actualizarEstado("Listo para registrar ventas.");
    }

    // ------------------------------------------------------------------
    // Carrito
    // ------------------------------------------------------------------

    /**
     * Busca el artículo por código de barras y lo agrega al carrito validando el
     * stock disponible.
     */
    @FXML
    private void onAgregar() {
        String codigo = barcodeField.getText() == null ? "" : barcodeField.getText().trim();
        if (codigo.isEmpty()) {
            mostrarMensaje("Ingrese un código de barras.", true);
            return;
        }

        BigDecimal cantidad = parsearCantidad(cantidadField.getText());
        if (cantidad == null) {
            mostrarMensaje("La cantidad debe ser un número mayor a 0.", true);
            return;
        }

        setControlesHabilitados(false);
        ejecutarAsync(
                () -> apiClient.get(RUTA_ITEM_POR_BARCODE, RespuestaItem.class, Map.of("barcode", codigo)),
                respuesta -> {
                    setControlesHabilitados(true);
                    if (respuesta == null || !respuesta.success() || respuesta.data() == null) {
                        mostrarMensaje("Artículo no encontrado para el código " + codigo + ".", true);
                        return;
                    }
                    agregarAlCarrito(respuesta.data(), cantidad);
                });
    }

    /**
     * Agrega una línea al carrito (o incrementa la cantidad si ya existe),
     * verificando que no se supere el stock disponible.
     */
    private void agregarAlCarrito(ItemDTO item, BigDecimal cantidad) {
        if (!item.active()) {
            mostrarMensaje("El artículo \"" + item.name() + "\" está inactivo y no puede venderse.", true);
            return;
        }

        BigDecimal stockDisponible = item.stock() == null ? BigDecimal.ZERO : item.stock();
        BigDecimal enCarrito = carrito.stream()
                .filter(linea -> linea.item().id().equals(item.id()))
                .map(LineaVenta::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (enCarrito.add(cantidad).compareTo(stockDisponible) > 0) {
            mostrarMensaje("Stock insuficiente para \"" + item.name() + "\" (disponible: "
                    + decimal(stockDisponible) + ").", true);
            return;
        }

        LineaVenta existente = carrito.stream()
                .filter(linea -> linea.item().id().equals(item.id()))
                .findFirst()
                .orElse(null);

        if (existente != null) {
            existente.setCantidad(existente.cantidad().add(cantidad));
            carritoTable.refresh();
        } else {
            carrito.add(new LineaVenta(item, cantidad));
        }

        barcodeField.clear();
        cantidadField.setText("1");
        barcodeField.requestFocus();
        mostrarMensaje("Agregado: " + item.name() + ".", false);
        actualizarTotales();
    }

    /**
     * Quita del carrito la línea seleccionada.
     */
    @FXML
    private void onQuitar() {
        LineaVenta seleccionada = carritoTable.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Seleccione una línea del carrito para quitarla.", true);
            return;
        }
        carrito.remove(seleccionada);
        actualizarTotales();
    }

    private void configurarColumnasCarrito() {
        carritoTable.getColumns().setAll(List.of(
                columnaTexto("Código de barras", l -> l.item().barcode()),
                columnaTexto("Artículo", l -> l.item().name()),
                columnaTexto("Cantidad", l -> decimal(l.cantidad())),
                columnaTexto("Precio", l -> decimal(l.precio())),
                columnaTexto("Subtotal", l -> decimal(l.subtotal()))));
    }

    // ------------------------------------------------------------------
    // Cobro
    // ------------------------------------------------------------------

    /**
     * Procesa la venta: valida el carrito y el efectivo, y envía la solicitud al
     * {@code PosService} del backend incluyendo el {@code machine_id}.
     */
    @FXML
    private void onProcesar() {
        if (carrito.isEmpty()) {
            mostrarMensaje("El carrito está vacío.", true);
            return;
        }

        BigDecimal total = calcularTotal();
        BigDecimal efectivo = parsearMonto(efectivoField.getText());
        if (efectivo == null) {
            mostrarMensaje("Ingrese un efectivo recibido válido.", true);
            return;
        }
        if (efectivo.compareTo(total) < 0) {
            mostrarMensaje("El efectivo recibido es menor al total a pagar.", true);
            return;
        }

        List<SaleRequest.SaleItem> items = carrito.stream()
                .map(linea -> new SaleRequest.SaleItem(linea.item().id(), linea.cantidad()))
                .toList();
        SaleRequest solicitud = new SaleRequest(items, efectivo, MachineIdProvider.obtener());

        setControlesHabilitados(false);
        ejecutarAsync(
                () -> apiClient.post(RUTA_VENTA, solicitud, RespuestaVenta.class),
                respuesta -> {
                    setControlesHabilitados(true);
                    if (respuesta == null || !respuesta.success() || respuesta.data() == null) {
                        String mensaje = (respuesta != null && respuesta.message() != null)
                                ? respuesta.message()
                                : "No se pudo registrar la venta.";
                        mostrarMensaje(mensaje, true);
                        return;
                    }
                    SaleDTO venta = respuesta.data();
                    mostrarMensaje("Venta #" + venta.id() + " registrada. Cambio: "
                            + decimal(venta.changeAmount()) + ".", false);
                    actualizarEstado("Venta #" + venta.id() + " procesada correctamente.");
                    limpiarVenta();
                });
    }

    private void limpiarVenta() {
        carrito.clear();
        efectivoField.clear();
        cantidadField.setText("1");
        actualizarTotales();
        barcodeField.requestFocus();
    }

    private void actualizarTotales() {
        BigDecimal total = calcularTotal();
        totalLabel.setText(decimal(total));
        articulosLabel.setText(Integer.toString(carrito.size()));
        actualizarCambio();
        if (procesarButton != null) {
            procesarButton.setDisable(carrito.isEmpty());
        }
    }

    private void actualizarCambio() {
        BigDecimal total = calcularTotal();
        BigDecimal efectivo = parsearMonto(efectivoField.getText());
        if (efectivo == null || efectivo.compareTo(total) < 0) {
            cambioLabel.setText(decimal(BigDecimal.ZERO));
            return;
        }
        cambioLabel.setText(decimal(efectivo.subtract(total)));
    }

    private BigDecimal calcularTotal() {
        return carrito.stream()
                .map(LineaVenta::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------
    // Cierre de sesión
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

    private <T> void ejecutarAsync(Callable<T> operacion, Consumer<T> alExito) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operacion.call();
            }
        };

        tarea.setOnSucceeded(evento -> alExito.accept(tarea.getValue()));
        tarea.setOnFailed(evento -> {
            setControlesHabilitados(true);
            Throwable causa = tarea.getException();
            LOG.warning(() -> "Error en el punto de venta: " + causa);
            mostrarMensaje(MensajeError.desde(causa), true);
        });

        Thread hilo = new Thread(tarea, "pos-api");
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

    private BigDecimal parsearCantidad(String texto) {
        BigDecimal valor = parsearMonto(texto);
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return valor;
    }

    private BigDecimal parsearMonto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim().replace(',', '.'));
        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    private String decimal(BigDecimal valor) {
        return valor == null ? "0.00" : valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void setControlesHabilitados(boolean habilitado) {
        if (agregarButton != null) {
            agregarButton.setDisable(!habilitado);
        }
        if (quitarButton != null) {
            quitarButton.setDisable(!habilitado);
        }
        if (procesarButton != null) {
            procesarButton.setDisable(!habilitado || carrito.isEmpty());
        }
        if (barcodeField != null) {
            barcodeField.setDisable(!habilitado);
        }
        if (cantidadField != null) {
            cantidadField.setDisable(!habilitado);
        }
        if (efectivoField != null) {
            efectivoField.setDisable(!habilitado);
        }
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

    private void mostrarMensaje(String texto, boolean error) {
        mensajeLabel.setText(texto);
        mensajeLabel.getStyleClass().removeAll("error", "success");
        mensajeLabel.getStyleClass().add(error ? "error" : "success");
    }

    private void actualizarEstado(String mensaje) {
        if (estadoLabel != null) {
            estadoLabel.setText(mensaje);
        }
    }

    /**
     * Línea del carrito de venta: artículo, precio unitario y cantidad.
     */
    public static final class LineaVenta {

        private final ItemDTO item;
        private final BigDecimal precio;
        private BigDecimal cantidad;

        LineaVenta(ItemDTO item, BigDecimal cantidad) {
            this.item = item;
            this.precio = item.price();
            this.cantidad = cantidad;
        }

        public ItemDTO item() {
            return item;
        }

        public BigDecimal precio() {
            return precio;
        }

        public BigDecimal cantidad() {
            return cantidad;
        }

        public void setCantidad(BigDecimal cantidad) {
            this.cantidad = cantidad;
        }

        public BigDecimal subtotal() {
            return precio.multiply(cantidad);
        }
    }

    // ------------------------------------------------------------------
    // Envoltorios tipados de las respuestas del backend (ApiResponse)
    // ------------------------------------------------------------------

    private record RespuestaItem(boolean success, int code, String message, String timestamp, ItemDTO data) {
    }

    private record RespuestaVenta(boolean success, int code, String message, String timestamp, SaleDTO data) {
    }
}
