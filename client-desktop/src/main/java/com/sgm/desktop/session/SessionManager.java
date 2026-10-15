package com.sgm.desktop.session;

import com.sgm.shared.dto.LoginResponse;
import com.sgm.shared.enums.Role;

/**
 * Gestor de sesión en memoria del cliente de escritorio.
 * <p>
 * Almacena la información devuelta por el backend tras un inicio de sesión
 * exitoso (token JWT, usuario, rol, caducidad, etc.). El token se mantiene
 * únicamente en memoria y nunca se persiste en disco, lo que evita dejar las
 * credenciales de sesión expuestas en el sistema de archivos.
 * <p>
 * Es un singleton thread-safe: los campos son {@code volatile} y las
 * operaciones de escritura están sincronizadas, ya que el login se realiza en
 * un hilo secundario y la lectura puede ocurrir en el hilo de JavaFX.
 */
public final class SessionManager {

    private static final SessionManager INSTANCIA = new SessionManager();

    private static final String TIPO_POR_DEFECTO = "Bearer";

    private volatile String token;
    private volatile String tokenType;
    private volatile String username;
    private volatile String fullName;
    private volatile Role role;
    private volatile long expiraEn;

    private SessionManager() {
        // Constructor privado: uso exclusivo a través de getInstancia()
    }

    /**
     * Devuelve la única instancia del gestor de sesión.
     *
     * @return instancia única de {@link SessionManager}
     */
    public static SessionManager getInstancia() {
        return INSTANCIA;
    }

    /**
     * Registra los datos de la sesión a partir de la respuesta de autenticación.
     *
     * @param respuesta respuesta devuelta por el endpoint de login
     * @throws IllegalArgumentException si la respuesta es nula
     */
    public synchronized void iniciarSesion(LoginResponse respuesta) {
        if (respuesta == null) {
            throw new IllegalArgumentException("La respuesta de autenticación no puede ser nula");
        }

        this.token = respuesta.token();
        this.tokenType = (respuesta.tokenType() != null && !respuesta.tokenType().isBlank())
                ? respuesta.tokenType()
                : TIPO_POR_DEFECTO;
        this.username = respuesta.username();
        this.fullName = respuesta.fullName();
        this.role = respuesta.role();

        long expiracionMs = Math.max(respuesta.expiresInMs(), 0);
        this.expiraEn = (expiracionMs > 0) ? System.currentTimeMillis() + expiracionMs : 0;
    }

    /**
     * Elimina por completo los datos de la sesión actual.
     */
    public synchronized void cerrarSesion() {
        this.token = null;
        this.tokenType = null;
        this.username = null;
        this.fullName = null;
        this.role = null;
        this.expiraEn = 0;
    }

    /**
     * Indica si existe una sesión válida y no caducada.
     *
     * @return {@code true} si hay un token vigente
     */
    public boolean isAutenticado() {
        String tokenActual = this.token;
        if (tokenActual == null || tokenActual.isBlank()) {
            return false;
        }
        return expiraEn == 0 || System.currentTimeMillis() < expiraEn;
    }

    /**
     * Construye el valor de la cabecera {@code Authorization} listo para usarse
     * en peticiones autenticadas.
     *
     * @return por ejemplo {@code "Bearer eyJhbGci..."}, o {@code null} si no hay sesión
     */
    public String getAuthorizationHeader() {
        if (!isAutenticado()) {
            return null;
        }
        return tokenType + " " + token;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public long getExpiraEn() {
        return expiraEn;
    }
}
