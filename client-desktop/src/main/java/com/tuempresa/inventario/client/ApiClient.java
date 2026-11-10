package com.tuempresa.inventario.client;

import java.util.Map;
import java.util.Objects;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP genérico basado en {@link RestClient} de Spring.
 * <p>
 * Permite consumir servicios REST mediante peticiones GET, POST, PUT y DELETE,
 * configurando por defecto las cabeceras para trabajar con JSON en las peticiones
 * y respuestas.
 * <p>
 * Ejemplo de uso:
 * <pre>{@code
 * ApiClient client = ApiClient.builder()
 *         .baseUrl("http://localhost:8080")
 *         .token("mi-token-jwt")
 *         .build();
 *
 * ItemDTO item = client.get("/api/admin/items", ItemDTO.class);
 * ItemDTO creado = client.post("/api/admin/items", item, ItemDTO.class);
 * client.delete("/api/admin/items/1");
 * }</pre>
 *
 * @author SGM
 */
public class ApiClient {

    private final RestClient restClient;

    /**
     * Crea el cliente a partir de una URL base.
     *
     * @param baseUrl URL raíz del backend (por ejemplo {@code http://localhost:8080}); no puede ser {@code null}
     */
    public ApiClient(String baseUrl) {
        this(baseUrl, null);
    }

    /**
     * Crea el cliente a partir de una URL base y un token de autenticación opcional.
     *
     * @param baseUrl URL raíz del backend; no puede ser {@code null}
     * @param token   token que se enviará en la cabecera {@code Authorization} (puede ser {@code null})
     */
    public ApiClient(String baseUrl, @Nullable String token) {
        String url = Objects.requireNonNull(baseUrl, "La URL base (baseUrl) es obligatoria");
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(url)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (token != null && !token.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        this.restClient = builder.build();
    }

    /**
     * Crea el cliente a partir de un {@link RestClient} ya configurado.
     *
     * @param restClient instancia de RestClient a reutilizar; no puede ser {@code null}
     */
    public ApiClient(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "El RestClient es obligatorio");
    }

    /**
     * Devuelve una instancia del constructor fluido de {@link ApiClient}.
     *
     * @return el builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // ------------------------------------------------------------------
    // GET
    // ------------------------------------------------------------------

    /**
     * Realiza una petición GET y deserializa la respuesta al tipo indicado.
     */
    public <T> T get(@NonNull String path, @NonNull Class<T> responseType) {
        return restClient.get()
                .uri(path)
                .retrieve()
                .body(responseType);
    }

    /**
     * Realiza una petición GET con variables de plantilla de URI (por ejemplo {@code /api/admin/items/{id}}).
     */
    public <T> T get(@NonNull String path, @NonNull Class<T> responseType, Map<String, ?> uriVariables) {
        Map<String, ?> variables = Objects.requireNonNull(uriVariables, "Las variables de URI son obligatorias");
        return restClient.get()
                .uri(path, variables)
                .retrieve()
                .body(responseType);
    }

    /**
     * Realiza una petición GET para respuestas genéricas (listas, envoltorios, etc.).
     */
    public <T> T get(@NonNull String path, @NonNull ParameterizedTypeReference<T> responseType) {
        return restClient.get()
                .uri(path)
                .retrieve()
                .body(responseType);
    }

    /**
     * Realiza una petición GET genérica con variables de plantilla de URI.
     */
    public <T> T get(@NonNull String path, @NonNull ParameterizedTypeReference<T> responseType, Map<String, ?> uriVariables) {
        Map<String, ?> variables = Objects.requireNonNull(uriVariables, "Las variables de URI son obligatorias");
        return restClient.get()
                .uri(path, variables)
                .retrieve()
                .body(responseType);
    }

    // ------------------------------------------------------------------
    // POST
    // ------------------------------------------------------------------

    /**
     * Realiza una petición POST enviando {@code body} como JSON.
     */
    public <T> T post(@NonNull String path, @NonNull Object body, @NonNull Class<T> responseType) {
        return restClient.post()
                .uri(path)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    /**
     * Realiza una petición POST genérica enviando {@code body} como JSON.
     */
    public <T> T post(@NonNull String path, @NonNull Object body, @NonNull ParameterizedTypeReference<T> responseType) {
        return restClient.post()
                .uri(path)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    /**
     * Realiza una petición POST sin esperar cuerpo de respuesta (por ejemplo, un 201 Created vacío).
     */
    public void post(@NonNull String path, @NonNull Object body) {
        restClient.post()
                .uri(path)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    // ------------------------------------------------------------------
    // PUT
    // ------------------------------------------------------------------

    /**
     * Realiza una petición PUT enviando {@code body} como JSON.
     */
    public <T> T put(@NonNull String path, @NonNull Object body, @NonNull Class<T> responseType) {
        return restClient.put()
                .uri(path)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    /**
     * Realiza una petición PUT genérica enviando {@code body} como JSON.
     */
    public <T> T put(@NonNull String path, @NonNull Object body, @NonNull ParameterizedTypeReference<T> responseType) {
        return restClient.put()
                .uri(path)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    /**
     * Realiza una petición PUT sin esperar cuerpo de respuesta.
     */
    public void put(@NonNull String path, @NonNull Object body) {
        restClient.put()
                .uri(path)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    /**
     * Realiza una petición DELETE sin esperar cuerpo de respuesta.
     */
    public void delete(@NonNull String path) {
        restClient.delete()
                .uri(path)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Realiza una petición DELETE y deserializa la respuesta al tipo indicado.
     */
    public <T> T delete(@NonNull String path, @NonNull Class<T> responseType) {
        return restClient.delete()
                .uri(path)
                .retrieve()
                .body(responseType);
    }

    /**
     * Realiza una petición DELETE genérica y deserializa la respuesta.
     */
    public <T> T delete(@NonNull String path, @NonNull ParameterizedTypeReference<T> responseType) {
        return restClient.delete()
                .uri(path)
                .retrieve()
                .body(responseType);
    }

    /**
     * Expone el {@link RestClient} subyacente para casos avanzados que requieran
     * configurar la petición de forma más específica.
     *
     * @return el RestClient configurado
     */
    public RestClient restClient() {
        return this.restClient;
    }

    /**
     * Constructor fluido para {@link ApiClient}.
     */
    public static final class Builder {

        private String baseUrl;
        private String token;

        private Builder() {
        }

        /**
         * Define la URL raíz del backend.
         *
         * @param baseUrl URL base (por ejemplo {@code http://localhost:8080})
         * @return este builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Define el token JWT que se enviará en la cabecera {@code Authorization}.
         *
         * @param token token de autenticación
         * @return este builder
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Construye la instancia de {@link ApiClient}.
         *
         * @return el cliente configurado
         * @throws IllegalStateException si no se definió una URL base válida
         */
        public ApiClient build() {
            String url = baseUrl;
            if (url == null || url.isBlank()) {
                throw new IllegalStateException("La URL base (baseUrl) es obligatoria");
            }
            return new ApiClient(url, token);
        }
    }
}
