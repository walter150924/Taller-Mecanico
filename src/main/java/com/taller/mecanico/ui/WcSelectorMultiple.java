package com.taller.mecanico.ui;

import atlantafx.base.theme.Styles;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Arrays;
import java.util.List;

public class WcSelectorMultiple {
    private static final String WC_SEPARADOR = ", ";

    private final ComboBox<String> wcOpciones;
    private final Button wcBotonAgregar;
    private final ListView<String> wcElegidos;
    private final BooleanProperty wcEditable = new SimpleBooleanProperty(true);
    private Runnable wcAlCambiar = () -> { };

    public WcSelectorMultiple(ComboBox<String> wcOpciones, Button wcBotonAgregar, ListView<String> wcElegidos,
                              List<String> wcCatalogo, String wcTextoVacio) {
        this.wcOpciones = wcOpciones;
        this.wcBotonAgregar = wcBotonAgregar;
        this.wcElegidos = wcElegidos;
        wcOpciones.getItems().setAll(wcCatalogo);
        wcBotonAgregar.setOnAction(wcE -> wcAgregar());
        wcElegidos.setPlaceholder(new Label(wcTextoVacio));
        wcElegidos.setCellFactory(wcLista -> new WcCeldaElegida());
    }

    public void setWcAlCambiar(Runnable wcAlCambiar) {
        this.wcAlCambiar = wcAlCambiar;
    }

    public String wcTexto() {
        return String.join(WC_SEPARADOR, wcElegidos.getItems());
    }

    public boolean wcEstaVacio() {
        return wcElegidos.getItems().isEmpty();
    }

    public void wcCargar(String wcTexto) {
        wcOpciones.setValue(null);
        if (wcTexto == null || wcTexto.isBlank()) {
            wcElegidos.getItems().clear();
            return;
        }
        wcElegidos.getItems().setAll(Arrays.stream(wcTexto.split(",\\s*")).filter(wcT -> !wcT.isBlank()).toList());
    }

    public void wcLimpiar() {
        wcCargar(null);
    }

    public void wcHabilitar(boolean wcSi) {
        wcEditable.set(wcSi);
        wcOpciones.setDisable(!wcSi);
        wcBotonAgregar.setDisable(!wcSi);
    }

    private void wcAgregar() {
        String wcElegido = wcOpciones.getValue();
        if (wcElegido == null) {
            return;
        }
        if (!wcElegidos.getItems().contains(wcElegido)) {
            wcElegidos.getItems().add(wcElegido);
            wcAlCambiar.run();
        }
        wcOpciones.setValue(null);
    }

    private void wcQuitar(String wcElegido) {
        wcElegidos.getItems().remove(wcElegido);
        wcAlCambiar.run();
    }

    private class WcCeldaElegida extends ListCell<String> {
        private final Label wcTexto = new Label();
        private final Button wcBotonQuitar = new Button(null, new FontIcon(Feather.TRASH_2));
        private final HBox wcCaja = new HBox(8, wcTexto, wcBotonQuitar);

        WcCeldaElegida() {
            wcCaja.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(wcTexto, Priority.ALWAYS);
            wcTexto.setMaxWidth(Double.MAX_VALUE);
            wcBotonQuitar.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER, "boton-mini");
            wcBotonQuitar.setTooltip(new Tooltip("Quitar"));
            wcBotonQuitar.visibleProperty().bind(wcEditable);
            wcBotonQuitar.setOnAction(wcE -> wcQuitar(getItem()));
        }

        @Override
        protected void updateItem(String wcItem, boolean wcVacio) {
            super.updateItem(wcItem, wcVacio);
            if (wcVacio || wcItem == null) {
                setGraphic(null);
                return;
            }
            wcTexto.setText(wcItem);
            setGraphic(wcCaja);
        }
    }
}
