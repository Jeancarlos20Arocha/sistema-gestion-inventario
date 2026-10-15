package com.sgm.desktop.client;

import com.sgm.desktop.session.SessionManager;
import com.tuempresa.inventario.client.ApiClient;

/**
 * Factoría de {@link ApiClient} para el cliente de escritorio.
 * <p>
 * Centraliza la resolución de la URL base del backend y la construcción del
 * cliente, ya sea sin autenticación (para el login) o con el token JWT de la
 * sesión activa (para el resto de módulos).
 */
public final class ApiClientFactory {

    private static final String URL_BASE_POR_DEFECTO = "http://localhost:8080";

    private ApiClientFactory() {
        // Clase de utilidad: no instanciable
    }

    /**
     * Resuelve la URL base del backend a partir de variables de entorno o
     * propiedades del sistema, con un valor por defecto para desarrollo local.
     *
     * @return URL base del backend
     */
    public static String urlBase() {
        String valor = System.getenv("SGM_API_URL");
        if (valor != null && !valor.isBlank()) {
            return valor;
        }
        valor = System.getProperty("sgm.api.url");
        return (valor != null && !valor.isBlank()) ? valor : URL_BASE_POR_DEFECTO;
    }

    /**
     * Crea un cliente sin cabecera de autenticación (por ejemplo, para el login).
     *
     * @return cliente HTTP apuntando al backend
     */
    public static ApiClient sinAutenticacion() {
        return new ApiClient(urlBase());
    }

    /**
     * Crea un cliente que envía el token JWT de la sesión activa en la cabecera
     * {@code Authorization}.
     *
     * @return cliente HTTP autenticado
     */
    public static ApiClient autenticado() {
        String token = SessionManager.getInstancia().getToken();
        return ApiClient.builder()
                .baseUrl(urlBase())
                .token(token)
                .build();
    }
}
