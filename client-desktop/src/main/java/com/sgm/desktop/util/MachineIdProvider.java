package com.sgm.desktop.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Obtiene un identificador de máquina estable para enviarlo al backend
 * ({@code machine_id}) en las operaciones de venta y de stock.
 * <p>
 * Se usa el nombre del host y, si no está disponible, el nombre de usuario del
 * sistema operativo. El valor se recorta a la longitud permitida por el backend.
 */
public final class MachineIdProvider {

    private static final int LONGITUD_MAXIMA = 64;

    private MachineIdProvider() {
        // Clase de utilidad: no instanciable
    }

    /**
     * Devuelve el identificador de máquina normalizado.
     *
     * @return identificador de hasta 64 caracteres
     */
    public static String obtener() {
        String identificador = null;
        try {
            identificador = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) {
            // Se usa el respaldo basado en el usuario del sistema operativo
        }

        if (identificador == null || identificador.isBlank()) {
            identificador = System.getProperty("user.name", "desktop-client");
        }

        return identificador.length() > LONGITUD_MAXIMA
                ? identificador.substring(0, LONGITUD_MAXIMA)
                : identificador;
    }
}
