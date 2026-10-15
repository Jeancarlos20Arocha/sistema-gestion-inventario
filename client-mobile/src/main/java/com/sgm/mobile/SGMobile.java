package com.sgm.mobile;

import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.Swatch;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.text.Font;

/**
 * Aplicación móvil (JavaFX/Gluon) del sistema SGM.
 * Define una vista inicial simple; luego servirá como pantalla POS ligera.
 * <p>
 * Nota: la API {@code com.gluonhq.charm.glisten} (Glisten) está marcada como
 * deprecada por el proveedor. Se conserva deliberadamente mientras este módulo
 * permanece como esqueleto, a la espera de migrar a la API vigente de Gluon.
 */
@SuppressWarnings("deprecation")
public class SGMobile extends MobileApplication {

    @Override
    public void init() {
        // Registra la vista principal (HOME)
        addViewFactory(HOME_VIEW, () -> {
            View view = new View();
            Label etiqueta = new Label("SGM Mobile - POS");
            etiqueta.setFont(Font.font(20));
            view.setCenter(etiqueta);
            return view;
        });
    }

    @Override
    public void postInit(Scene scene) {
        // Paleta de colores y título de la aplicación
        Swatch.BLUE.assignTo(scene);
        setTitle("SGM Mobile");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
