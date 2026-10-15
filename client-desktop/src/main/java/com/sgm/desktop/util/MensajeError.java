package com.sgm.desktop.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Traduce las excepciones producidas por las llamadas HTTP del {@code ApiClient}
 * a mensajes legibles para el usuario final.
 */
public final class MensajeError {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MensajeError() {
        // Clase de utilidad: no instanciable
    }

    /**
     * Convierte una excepción en un mensaje amigable.
     *
     * @param causa excepción lanzada durante una llamada al backend
     * @return mensaje para mostrar en la interfaz
     */
    public static String desde(Throwable causa) {
        if (causa == null) {
            return "Error inesperado.";
        }
        if (causa instanceof RestClientResponseException excepcionRespuesta) {
            String delServidor = extraerMensajeServidor(excepcionRespuesta);
            if (delServidor != null) {
                return delServidor;
            }
            return "El servidor respondió con el código " + excepcionRespuesta.getStatusCode().value() + ".";
        }
        if (causa instanceof ResourceAccessException) {
            return "No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.";
        }
        if (causa instanceof RestClientException) {
            return "Error de comunicación con el servidor.";
        }

        String mensaje = causa.getMessage();
        return (mensaje != null && !mensaje.isBlank()) ? mensaje : "Error inesperado.";
    }

    /**
     * Extrae el campo {@code message} del cuerpo de error devuelto por el backend.
     */
    private static String extraerMensajeServidor(RestClientResponseException excepcion) {
        try {
            String cuerpo = excepcion.getResponseBodyAsString(StandardCharsets.UTF_8);
            if (cuerpo != null && !cuerpo.isBlank()) {
                JsonNode nodoMensaje = MAPPER.readTree(cuerpo).path("message");
                if (nodoMensaje.isTextual() && !nodoMensaje.asText().isBlank()) {
                    return nodoMensaje.asText();
                }
            }
        } catch (IOException ignored) {
            // Sin cuerpo interpretable: se usará el mensaje por código
        }
        return null;
    }
}
