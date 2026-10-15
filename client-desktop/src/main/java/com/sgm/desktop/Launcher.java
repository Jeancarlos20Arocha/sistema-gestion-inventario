package com.sgm.desktop;

import javafx.application.Application;

/**
 * Punto de entrada del cliente de escritorio.
 * Se separa de la clase JavaFX para poder arrancar la aplicación
 * desde el classpath sin el error "JavaFX runtime components are missing".
 */
public class Launcher {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
