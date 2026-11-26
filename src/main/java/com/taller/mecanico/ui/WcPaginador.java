package com.taller.mecanico.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;

public class WcPaginador extends HBox {
    private static final int WC_BOTONES_VISIBLES = 5;

    private final Label wcLblResumen = new Label();
    private final ComboBox<Integer> wcCmbTamanio =
            new ComboBox<>(FXCollections.observableArrayList(5, 10, 15, 25, 50));
    private final HBox wcBotones = new HBox(4);

    private int wcPagina = 0;
    private int wcTotalPaginas = 1;
    private Runnable wcOnCambio = () -> { };

    public WcPaginador() {
        getStyleClass().add("paginador");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);

        wcLblResumen.getStyleClass().add("paginador-resumen");

        Label wcLblFilas = new Label("Filas");
        wcLblFilas.getStyleClass().add("paginador-resumen");
        wcCmbTamanio.setValue(10);
        wcCmbTamanio.getStyleClass().add("paginador-tamanio");
        wcCmbTamanio.valueProperty().addListener((wcObs, wcA, wcB) -> wcIrAPrimera());

        wcBotones.setAlignment(Pos.CENTER);

        Region wcEspacio = new Region();
        HBox.setHgrow(wcEspacio, Priority.ALWAYS);

        getChildren().addAll(wcLblResumen, wcEspacio, wcLblFilas, wcCmbTamanio, wcBotones);
    }

    public void setWcOnCambio(Runnable wcOnCambio) {
        this.wcOnCambio = wcOnCambio;
    }

    public int getWcPagina() {
        return wcPagina;
    }

    public int getWcTamanio() {
        return wcCmbTamanio.getValue();
    }

    public void wcIrAPrimera() {
        wcIrA(0);
    }

    public void wcActualizar(Page<?> wcResultado) {
        wcTotalPaginas = Math.max(wcResultado.getTotalPages(), 1);
        if (wcPagina >= wcTotalPaginas) {
            wcIrA(wcTotalPaginas - 1);
            return;
        }

        long wcTotal = wcResultado.getTotalElements();
        long wcDesde = wcTotal == 0 ? 0 : (long) wcPagina * getWcTamanio() + 1;
        long wcHasta = (long) wcPagina * getWcTamanio() + wcResultado.getNumberOfElements();
        wcLblResumen.setText(wcTotal == 0
                ? "Sin registros"
                : "Mostrando " + wcDesde + "–" + wcHasta + " de " + wcTotal);

        wcConstruirBotones();
    }

    private void wcIrA(int wcNuevaPagina) {
        wcPagina = Math.max(0, Math.min(wcNuevaPagina, wcTotalPaginas - 1));
        wcOnCambio.run();
    }

    private void wcConstruirBotones() {
        wcBotones.getChildren().clear();
        wcBotones.getChildren().add(wcBotonIcono(Feather.CHEVRONS_LEFT, 0, wcPagina == 0));
        wcBotones.getChildren().add(wcBotonIcono(Feather.CHEVRON_LEFT, wcPagina - 1, wcPagina == 0));

        int wcInicio = Math.max(0, Math.min(wcPagina - WC_BOTONES_VISIBLES / 2, wcTotalPaginas - WC_BOTONES_VISIBLES));
        int wcFin = Math.min(wcTotalPaginas, wcInicio + WC_BOTONES_VISIBLES);

        if (wcInicio > 0) {
            wcBotones.getChildren().add(wcBotonNumero(0));
            if (wcInicio > 1) {
                wcBotones.getChildren().add(wcElipsis());
            }
        }
        for (int wcI = wcInicio; wcI < wcFin; wcI++) {
            wcBotones.getChildren().add(wcBotonNumero(wcI));
        }
        if (wcFin < wcTotalPaginas) {
            if (wcFin < wcTotalPaginas - 1) {
                wcBotones.getChildren().add(wcElipsis());
            }
            wcBotones.getChildren().add(wcBotonNumero(wcTotalPaginas - 1));
        }

        boolean wcEsUltima = wcPagina >= wcTotalPaginas - 1;
        wcBotones.getChildren().add(wcBotonIcono(Feather.CHEVRON_RIGHT, wcPagina + 1, wcEsUltima));
        wcBotones.getChildren().add(wcBotonIcono(Feather.CHEVRONS_RIGHT, wcTotalPaginas - 1, wcEsUltima));
    }

    private Button wcBotonNumero(int wcIndice) {
        Button wcB = new Button(String.valueOf(wcIndice + 1));
        wcB.getStyleClass().add("pagina-btn");
        if (wcIndice == wcPagina) {
            wcB.getStyleClass().add("activa");
        }
        wcB.setOnAction(wcE -> {
            if (wcIndice != wcPagina) {
                wcIrA(wcIndice);
            }
        });
        return wcB;
    }

    private Button wcBotonIcono(Feather wcIcono, int wcDestino, boolean wcDeshabilitado) {
        Button wcB = new Button(null, new FontIcon(wcIcono));
        wcB.getStyleClass().add("pagina-btn");
        wcB.setDisable(wcDeshabilitado);
        wcB.setOnAction(wcE -> wcIrA(wcDestino));
        return wcB;
    }

    private Label wcElipsis() {
        Label wcL = new Label("…");
        wcL.getStyleClass().add("paginador-resumen");
        return wcL;
    }
}
