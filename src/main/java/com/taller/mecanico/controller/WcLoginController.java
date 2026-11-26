package com.taller.mecanico.controller;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.service.WcUsuarioService;
import com.taller.mecanico.ui.WcVentana;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WcLoginController {
    private final WcUsuarioService wcUsuarioService;
    private final WcVentana wcVentana;

    @FXML private VBox wcTarjeta;
    @FXML private CustomTextField wcTxtUsuario;
    @FXML private PasswordField wcTxtClave;
    @FXML private Label wcLblError;

    @FXML
    public void initialize() {
        wcTxtUsuario.textProperty().addListener((wcObs, wcAnterior, wcActual) -> wcOcultarError());
        wcTxtClave.textProperty().addListener((wcObs, wcAnterior, wcActual) -> wcOcultarError());
        Animations.fadeInUp(wcTarjeta, Duration.millis(420)).play();
        Platform.runLater(wcTxtUsuario::requestFocus);
    }

    @FXML
    private void wcOnIngresar() {
        if (wcTxtUsuario.getText().isBlank() || wcTxtClave.getText().isBlank()) {
            wcMostrarError("Ingresa tu usuario y contraseña.");
            return;
        }
        try {
            wcUsuarioService.wcIniciarSesion(wcTxtUsuario.getText(), wcTxtClave.getText());
            wcVentana.wcMostrarPanel();
        } catch (WcNegocioException wcE) {
            wcTxtClave.clear();
            wcMostrarError(wcE.getMessage());
        }
    }

    private void wcMostrarError(String wcMensaje) {
        wcLblError.setText(wcMensaje);
        wcLblError.setVisible(true);
        wcLblError.setManaged(true);
        Animations.shakeX(wcTarjeta, 6).play();
    }

    private void wcOcultarError() {
        wcLblError.setVisible(false);
        wcLblError.setManaged(false);
    }
}
