package com.sgm.desktop;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Aplicación JavaFX del cliente de escritorio.
 * Carga la vista base (login) definida en /fxml/login.fxml.
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL vista = App.class.getResource("/fxml/login.fxml");
        FXMLLoader loader = new FXMLLoader(vista);
        Parent root = loader.load();

        Scene scene = new Scene(root, 420, 480);
        stage.setTitle("SGM - Escritorio");
        stage.setScene(scene);
        stage.show();
    }
}
