package com.taller.mecanico.ui;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class WcVentana {
    private static final String WC_TITULO = "Taller Mecanico | Sistema de gestion";
    private static final String WC_HOJA_DE_ESTILOS = "/css/app.css";
    private static final double WC_ANCHO_MINIMO_PANEL = 1100;
    private static final double WC_ALTO_MINIMO_PANEL = 700;

    private final WcCargadorVistas wcCargador;
    private Stage wcEscenario;

    public void wcIniciar(Stage wcEscenario) {
        this.wcEscenario = wcEscenario;
        wcEscenario.setTitle(WC_TITULO);
        wcMostrarLogin();
        wcEscenario.show();
    }

    public void wcMostrarLogin() {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        wcEscenario.setMinWidth(0);
        wcEscenario.setMinHeight(0);
        wcMostrar(wcCargador.wcCargar("login-view").wcNodo());
        wcEscenario.setMaximized(false);
        wcEscenario.sizeToScene();
        wcEscenario.centerOnScreen();
    }

    public void wcMostrarPanel() {
        wcMostrar(wcCargador.wcCargar("main-view").wcNodo());
        wcEscenario.setMinWidth(WC_ANCHO_MINIMO_PANEL);
        wcEscenario.setMinHeight(WC_ALTO_MINIMO_PANEL);
        wcEscenario.setMaximized(true);
    }

    private void wcMostrar(Parent wcRaiz) {
        Scene wcEscena = new Scene(wcRaiz);
        wcEscena.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource(WC_HOJA_DE_ESTILOS)).toExternalForm());
        wcEscenario.setScene(wcEscena);
    }
}
