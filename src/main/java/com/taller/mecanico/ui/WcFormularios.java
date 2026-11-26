package com.taller.mecanico.ui;

import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class WcFormularios {
    private static final Pattern WC_DECIMAL = Pattern.compile("\\d{0,8}([.,]\\d{0,2})?");

    private WcFormularios() {
    }

    public static void wcMarcarError(Control wcCampo, String wcMensaje) {
        wcCampo.pseudoClassStateChanged(Styles.STATE_DANGER, true);
        Label wcError = wcEtiquetaError(wcCampo);
        if (wcError != null) {
            wcError.setText(wcMensaje);
            if (!wcError.isVisible()) {
                wcError.setVisible(true);
                wcError.setManaged(true);
                Animations.fadeInDown(wcError, Duration.millis(220)).play();
            }
        }
        Animations.shakeX(wcCampo, 4).play();
    }

    public static void wcLimpiarError(Control wcCampo) {
        wcCampo.pseudoClassStateChanged(Styles.STATE_DANGER, false);
        Label wcError = wcEtiquetaError(wcCampo);
        if (wcError != null) {
            wcError.setVisible(false);
            wcError.setManaged(false);
        }
    }

    public static void wcLimpiarErrores(Control... wcCampos) {
        for (Control wcCampo : wcCampos) {
            wcLimpiarError(wcCampo);
        }
    }

    public static void wcLimpiarAlEditar(TextInputControl... wcCampos) {
        for (TextInputControl wcCampo : wcCampos) {
            wcCampo.textProperty().addListener((wcObs, wcA, wcB) -> wcLimpiarError(wcCampo));
        }
    }

    public static List<String> wcMostrarViolaciones(ConstraintViolationException wcE,
                                                  Map<String, ? extends Control> wcCampos) {
        List<String> wcSinCampo = new ArrayList<>();
        for (ConstraintViolation<?> wcV : wcE.getConstraintViolations()) {
            Control wcCampo = wcCampos.get(wcUltimoNodo(wcV.getPropertyPath()));
            if (wcCampo != null) {
                wcMarcarError(wcCampo, wcV.getMessage());
            } else {
                wcSinCampo.add(wcV.getMessage());
            }
        }
        return wcSinCampo;
    }

    public static void wcSoloDigitos(TextInputControl wcCampo, int wcMaximo) {
        wcCampo.setTextFormatter(new TextFormatter<>(wcCambio ->
                wcCambio.getControlNewText().matches("\\d{0," + wcMaximo + "}") ? wcCambio : null));
    }

    public static void wcSoloDecimal(TextInputControl wcCampo) {
        wcCampo.setTextFormatter(new TextFormatter<>(wcCambio ->
                WC_DECIMAL.matcher(wcCambio.getControlNewText()).matches() ? wcCambio : null));
    }

    private static Label wcEtiquetaError(Control wcCampo) {
        Parent wcContenedor = wcCampo.getParent();
        while (wcContenedor != null && !wcContenedor.getStyleClass().contains("campo")) {
            wcContenedor = wcContenedor.getParent();
        }
        if (wcContenedor == null) {
            return null;
        }
        Node wcNodo = wcContenedor.lookup(".campo-error");
        return wcNodo instanceof Label wcL ? wcL : null;
    }

    private static String wcUltimoNodo(Path wcRuta) {
        String wcNombre = null;
        for (Path.Node wcNodo : wcRuta) {
            wcNombre = wcNodo.getName();
        }
        return wcNombre;
    }
}
