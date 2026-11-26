package com.taller.mecanico.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class WcAutocompletar<T> {
    private static final Duration WC_PAUSA = Duration.millis(180);
    private static final int WC_FILAS_VISIBLES = 7;
    private static final double WC_ALTO_FILA = 54;

    private final TextField wcCampo;
    private final Function<String, List<T>> wcBuscador;
    private final Consumer<T> wcAlSeleccionar;

    private Function<T, String> wcTitulo = String::valueOf;
    private Function<T, String> wcDetalle = wcT -> null;
    private Function<T, String> wcExtra = wcT -> null;
    private Function<T, Node> wcIcono = wcT -> new FontIcon(Feather.SEARCH);
    private int wcMinimoCaracteres = 1;

    private final Popup wcPopup = new Popup();
    private final ListView<T> wcLista = new ListView<>();
    private final PauseTransition wcEspera = new PauseTransition(WC_PAUSA);
    private boolean wcSuspendido;
    private String wcConsulta = "";

    public WcAutocompletar(TextField wcCampo, Function<String, List<T>> wcBuscador, Consumer<T> wcAlSeleccionar) {
        this.wcCampo = wcCampo;
        this.wcBuscador = wcBuscador;
        this.wcAlSeleccionar = wcAlSeleccionar;
        wcConstruirPopup();
        wcInstalarEventos();
    }

    public WcAutocompletar<T> wcPresentacion(Function<T, String> wcTitulo, Function<T, String> wcDetalle,
                                         Function<T, String> wcExtra) {
        this.wcTitulo = wcTitulo;
        this.wcDetalle = wcDetalle;
        this.wcExtra = wcExtra;
        return this;
    }

    public WcAutocompletar<T> wcIcono(Function<T, Node> wcIcono) {
        this.wcIcono = wcIcono;
        return this;
    }

    public WcAutocompletar<T> wcMinimoCaracteres(int wcMinimo) {
        this.wcMinimoCaracteres = wcMinimo;
        return this;
    }

    public void setWcTexto(String wcTexto) {
        wcSuspendido = true;
        wcCampo.setText(wcTexto);
        wcCampo.positionCaret(wcCampo.getLength());
        wcSuspendido = false;
        wcOcultar();
    }

    public void wcOcultar() {
        wcEspera.stop();
        wcPopup.hide();
    }

    private void wcConstruirPopup() {
        wcLista.getStyleClass().add("autocompletar-lista");
        wcLista.setCellFactory(wcLv -> new WcCelda());
        wcLista.setFocusTraversable(false);
        Label wcSinResultados = new Label("Sin coincidencias");
        wcSinResultados.getStyleClass().add("autocompletar-vacio");
        wcLista.setPlaceholder(wcSinResultados);
        wcLista.setOnMouseClicked(wcE -> wcElegir(wcLista.getSelectionModel().getSelectedItem()));

        VBox wcContenedor = new VBox(wcLista);
        wcContenedor.getStyleClass().add("autocompletar");

        wcContenedor.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());
        wcPopup.getContent().add(wcContenedor);
        wcPopup.setAutoHide(true);
        wcPopup.setAutoFix(true);
    }

    private void wcInstalarEventos() {
        wcEspera.setOnFinished(wcE -> wcBuscar());
        wcCampo.textProperty().addListener((wcObs, wcA, wcB) -> {
            if (!wcSuspendido) {
                wcEspera.playFromStart();
            }
        });
        wcCampo.focusedProperty().addListener((wcObs, wcA, wcEnfocado) -> {
            if (!wcEnfocado) {
                wcOcultar();
            }
        });
        wcCampo.addEventFilter(KeyEvent.KEY_PRESSED, wcE -> {
            switch (wcE.getCode()) {
                case DOWN -> {
                    if (!wcPopup.isShowing()) {
                        wcBuscar();
                    } else {
                        wcMover(1);
                    }
                    wcE.consume();
                }
                case UP -> {
                    wcMover(-1);
                    wcE.consume();
                }
                case ENTER -> {
                    if (wcPopup.isShowing() && wcLista.getSelectionModel().getSelectedItem() != null) {
                        wcElegir(wcLista.getSelectionModel().getSelectedItem());
                        wcE.consume();
                    }
                }
                case ESCAPE -> {
                    if (wcPopup.isShowing()) {
                        wcOcultar();
                        wcE.consume();
                    }
                }
                case TAB -> wcOcultar();
                default -> { }
            }
        });
    }

    private void wcBuscar() {
        String wcTexto = wcCampo.getText() == null ? "" : wcCampo.getText().trim();
        if (wcTexto.length() < wcMinimoCaracteres || wcCampo.getScene() == null) {
            wcPopup.hide();
            return;
        }
        wcConsulta = wcTexto;
        List<T> wcResultados = wcBuscador.apply(wcTexto);
        wcLista.getItems().setAll(wcResultados);
        wcLista.getSelectionModel().clearSelection();
        if (!wcResultados.isEmpty()) {
            wcLista.getSelectionModel().selectFirst();
        }
        wcLista.setPrefHeight(wcResultados.isEmpty()
                ? WC_ALTO_FILA
                : Math.min(wcResultados.size(), WC_FILAS_VISIBLES) * WC_ALTO_FILA + 6);
        wcMostrar();
    }

    private void wcMostrar() {
        Bounds wcB = wcCampo.localToScreen(wcCampo.getBoundsInLocal());
        if (wcB == null) {
            return;
        }
        wcLista.setPrefWidth(Math.max(wcCampo.getWidth(), 360));
        if (!wcPopup.isShowing()) {
            wcPopup.show(wcCampo, wcB.getMinX(), wcB.getMaxY() + 4);
        } else {
            wcPopup.setX(wcB.getMinX());
            wcPopup.setY(wcB.getMaxY() + 4);
        }
    }

    private void wcMover(int wcDelta) {
        if (!wcPopup.isShowing() || wcLista.getItems().isEmpty()) {
            return;
        }
        int wcActual = wcLista.getSelectionModel().getSelectedIndex();
        int wcNuevo = Math.max(0, Math.min(wcLista.getItems().size() - 1, wcActual + wcDelta));
        wcLista.getSelectionModel().select(wcNuevo);
        wcLista.scrollTo(Math.max(0, wcNuevo - WC_FILAS_VISIBLES + 1));
    }

    private void wcElegir(T wcItem) {
        if (wcItem == null) {
            return;
        }
        wcOcultar();
        wcAlSeleccionar.accept(wcItem);
    }

    private TextFlow wcResaltar(String wcTexto) {
        String wcMinusculas = wcTexto.toLowerCase(Locale.ROOT);
        boolean[] wcMarcado = new boolean[wcTexto.length()];
        for (String wcPalabra : wcConsulta.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (wcPalabra.isEmpty()) {
                continue;
            }
            int wcI = wcMinusculas.indexOf(wcPalabra);
            while (wcI >= 0) {
                for (int wcK = wcI; wcK < wcI + wcPalabra.length(); wcK++) {
                    wcMarcado[wcK] = true;
                }
                wcI = wcMinusculas.indexOf(wcPalabra, wcI + wcPalabra.length());
            }
        }

        List<Text> wcTramos = new ArrayList<>();
        int wcInicio = 0;
        for (int wcI = 1; wcI <= wcTexto.length(); wcI++) {
            if (wcI == wcTexto.length() || wcMarcado[wcI] != wcMarcado[wcInicio]) {
                Text wcT = new Text(wcTexto.substring(wcInicio, wcI));
                wcT.getStyleClass().add(wcMarcado[wcInicio] ? "autocompletar-coincidencia" : "autocompletar-texto");
                wcTramos.add(wcT);
                wcInicio = wcI;
            }
        }
        TextFlow wcFlujo = new TextFlow(wcTramos.toArray(Text[]::new));
        wcFlujo.getStyleClass().add("autocompletar-titulo");
        return wcFlujo;
    }

    private class WcCelda extends ListCell<T> {
        @Override
        protected void updateItem(T wcItem, boolean wcVacio) {
            super.updateItem(wcItem, wcVacio);
            if (wcVacio || wcItem == null) {
                setGraphic(null);
                return;
            }
            StackPane wcCaja = new StackPane(wcIcono.apply(wcItem));
            wcCaja.getStyleClass().add("autocompletar-icono");

            VBox wcTextos = new VBox(2, wcResaltar(wcTitulo.apply(wcItem)));
            String wcD = wcDetalle.apply(wcItem);
            if (wcD != null && !wcD.isBlank()) {
                TextFlow wcDetalleFlujo = wcResaltar(wcD);
                wcDetalleFlujo.getStyleClass().setAll("autocompletar-detalle");
                wcTextos.getChildren().add(wcDetalleFlujo);
            }
            HBox.setHgrow(wcTextos, Priority.ALWAYS);

            HBox wcFila = new HBox(12, wcCaja, wcTextos);
            wcFila.setAlignment(Pos.CENTER_LEFT);
            String wcValor = wcExtra.apply(wcItem);
            if (wcValor != null) {
                Label wcLblExtra = new Label(wcValor);
                wcLblExtra.getStyleClass().add("autocompletar-extra");
                wcLblExtra.setMinWidth(Label.USE_PREF_SIZE);
                wcFila.getChildren().add(wcLblExtra);
            }
            setGraphic(wcFila);
        }
    }
}
