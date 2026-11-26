package com.taller.mecanico.controller;

import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.model.WcVentaDetalle;
import com.taller.mecanico.service.WcComprobantePdfService;
import com.taller.mecanico.service.WcNumeroALetras;
import com.taller.mecanico.service.WcReporteExcelService;
import com.taller.mecanico.service.WcVentaService;
import com.taller.mecanico.service.WcVentaService.WcTotalizado;
import com.taller.mecanico.ui.WcExportador;
import com.taller.mecanico.ui.WcNotificador;
import com.taller.mecanico.ui.WcPaginador;
import com.taller.mecanico.ui.WcRefrescable;
import com.taller.mecanico.ui.WcTablas;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class WcVentasController implements WcRefrescable {
    private static final DateTimeFormatter WC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final PseudoClass WC_ANULADA = PseudoClass.getPseudoClass("anulada");

    private final WcVentaService wcService;
    private final WcComprobantePdfService wcPdfService;
    private final WcReporteExcelService wcExcelService;
    private final WcExportador wcExportador;
    private final WcNotificador wcNotificador;
    private final WcMainController wcMainController;

    @FXML private TextField wcTxtBuscar;
    @FXML private DatePicker wcDpDesde;
    @FXML private DatePicker wcDpHasta;
    @FXML private Button wcBtnVer;
    @FXML private Button wcBtnImprimir;
    @FXML private Button wcBtnAnular;
    @FXML private WcPaginador wcPaginador;

    @FXML private Label wcLblResComprobantes;
    @FXML private Label wcLblResBase;
    @FXML private Label wcLblResIgv;
    @FXML private Label wcLblResTotal;

    @FXML private TableView<WcVenta> wcTabla;
    @FXML private TableColumn<WcVenta, String> wcColComprobante;
    @FXML private TableColumn<WcVenta, String> wcColFecha;
    @FXML private TableColumn<WcVenta, String> wcColCliente;
    @FXML private TableColumn<WcVenta, String> wcColDni;
    @FXML private TableColumn<WcVenta, BigDecimal> wcColBase;
    @FXML private TableColumn<WcVenta, BigDecimal> wcColIgv;
    @FXML private TableColumn<WcVenta, BigDecimal> wcColTotal;
    @FXML private TableColumn<WcVenta, String> wcColEstado;

    @FXML private VBox wcPanelDetalle;
    @FXML private Label wcLblDetComprobante;
    @FXML private Label wcLblDetEstado;
    @FXML private Label wcLblDetSubtitulo;
    @FXML private Label wcLblDetIniciales;
    @FXML private Label wcLblDetCliente;
    @FXML private Label wcLblDetClienteDatos;
    @FXML private TableView<WcVentaDetalle> wcTablaDetalle;
    @FXML private TableColumn<WcVentaDetalle, String> wcColDetItem;
    @FXML private TableColumn<WcVentaDetalle, String> wcColDetRepuesto;
    @FXML private TableColumn<WcVentaDetalle, String> wcColDetCantidad;
    @FXML private TableColumn<WcVentaDetalle, BigDecimal> wcColDetPrecio;
    @FXML private TableColumn<WcVentaDetalle, BigDecimal> wcColDetImporte;
    @FXML private Label wcLblDetEtiquetaBase;
    @FXML private Label wcLblDetBase;
    @FXML private Label wcLblDetEtiquetaIgv;
    @FXML private Label wcLblDetIgv;
    @FXML private Label wcLblDetTotal;
    @FXML private Label wcLblDetLetras;
    @FXML private Label wcLblDetObservacion;

    private final ObservableList<WcVenta> wcDatos = FXCollections.observableArrayList();
    private WcVenta wcEnDetalle;

    @FXML
    public void initialize() {
        wcConfigurarColumnas();
        wcConfigurarTabla();
        wcConfigurarFiltros();
        wcConfigurarDetalle();
    }

    private void wcConfigurarColumnas() {
        wcColComprobante.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcComprobante()));
        wcColComprobante.setCellFactory(WcTablas.wcCeldaChip("—"));
        wcColFecha.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcFecha().format(WC_FECHA_HORA)));
        wcColCliente.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCliente().getWcNombreCompleto()));
        wcColCliente.setCellFactory(WcTablas.wcCeldaAvatar());
        wcColDni.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCliente().getWcDni()));
        wcColBase.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcBaseImponible()));
        wcColBase.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColIgv.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcIgv()));
        wcColIgv.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColTotal.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcTotal()));
        wcColTotal.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColEstado.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcEstado().name().replace("WC_", "")));
        wcColEstado.setCellFactory(WcTablas.wcCeldaEstado());
    }

    private void wcConfigurarTabla() {
        WcTablas.wcConfigurar(wcTabla, Feather.FILE_TEXT, "Sin ventas",
                "No hay comprobantes para el filtro seleccionado.", this::wcVerDetalle);
        wcTabla.setItems(wcDatos);

        wcTabla.setRowFactory(wcTv -> {
            TableRow<WcVenta> wcFila = new TableRow<>() {
                @Override
                protected void updateItem(WcVenta wcV, boolean wcVacio) {
                    super.updateItem(wcV, wcVacio);
                    pseudoClassStateChanged(WC_ANULADA,
                            !wcVacio && wcV != null && wcV.isWcAnulada());
                }
            };
            wcFila.setOnMouseClicked(wcE -> {
                if (wcE.getClickCount() == 2 && !wcFila.isEmpty()) {
                    wcVerDetalle(wcFila.getItem());
                }
            });
            return wcFila;
        });

        var wcSeleccion = wcTabla.getSelectionModel().selectedItemProperty();
        wcBtnVer.disableProperty().bind(wcSeleccion.isNull());
        wcBtnImprimir.disableProperty().bind(wcSeleccion.isNull());
        wcBtnAnular.disableProperty().bind(wcSeleccion.isNull());
    }

    private void wcConfigurarFiltros() {
        wcPaginador.setWcOnCambio(this::wcCargarTabla);
        wcTxtBuscar.textProperty().addListener((wcObs, wcA, wcB) -> wcPaginador.wcIrAPrimera());
        wcDpDesde.valueProperty().addListener((wcObs, wcA, wcB) -> wcPaginador.wcIrAPrimera());
        wcDpHasta.valueProperty().addListener((wcObs, wcA, wcB) -> wcPaginador.wcIrAPrimera());
    }

    private void wcConfigurarDetalle() {
        wcColDetItem.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcItem())));
        wcColDetItem.getStyleClass().add("celda-id");
        wcColDetRepuesto.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcNombreRepuesto()));
        wcColDetCantidad.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcCantidad())));
        wcColDetPrecio.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcPrecioUnitario()));
        wcColDetPrecio.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColDetImporte.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcImporte()));
        wcColDetImporte.setCellFactory(WcTablas.wcCeldaMoneda());
        wcTablaDetalle.getStyleClass().add("tabla");
        wcTablaDetalle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @Override
    public void wcRefrescar() {
        wcCargarTabla();
    }

    @FXML
    private void wcOnNuevaVenta() {
        wcMainController.wcIrA("punto-venta-view");
    }

    @FXML
    private void wcOnVer() {
        wcVerDetalle(wcTabla.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void wcOnImprimir() {
        wcImprimir(wcTabla.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void wcOnAnular() {
        WcVenta wcV = wcTabla.getSelectionModel().getSelectedItem();
        if (wcV == null) {
            return;
        }
        if (wcV.isWcAnulada()) {
            wcNotificador.wcAdvertencia("El comprobante " + wcV.getWcComprobante() + " ya esta anulado.");
            return;
        }
        wcNotificador.wcConfirmar("¿Anular el comprobante " + wcV.getWcComprobante() + "?",
                "La venta de " + WcTablas.WC_MONEDA.format(wcV.getWcTotal()) + " a " + wcV.getWcCliente().getWcNombreCompleto()
                        + " dejara de sumar en los reportes. El comprobante se conserva marcado como ANULADO.",
                "Anular",
                () -> {
                    try {
                        wcService.wcAnular(wcV.getWcId());
                        wcNotificador.wcExito("Comprobante " + wcV.getWcComprobante() + " anulado.");
                        wcCargarTabla();
                    } catch (WcNegocioException wcE) {
                        wcNotificador.wcError(wcE.getMessage());
                    }
                });
    }

    @FXML
    private void wcOnExportarExcel() {
        LocalDate wcHasta = wcDpHasta.getValue() != null ? wcDpHasta.getValue() : LocalDate.now();
        LocalDate wcDesde = wcDpDesde.getValue() != null ? wcDpDesde.getValue() : wcService.wcPrimeraFecha();
        if (wcDesde.isAfter(wcHasta)) {
            wcNotificador.wcAdvertencia("La fecha inicial no puede ser posterior a la final.");
            return;
        }
        wcExportador.wcGuardar(wcTabla.getScene().getWindow(), "Exportar reporte de ventas",
                "reporte-ventas_" + wcDesde + "_" + wcHasta, WcExportador.WcFormato.WC_EXCEL,
                wcDestino -> wcExcelService.wcGenerar(wcDesde, wcHasta, wcDestino));
    }

    @FXML
    private void wcOnCerrarDetalle() {
        wcNotificador.wcCerrarPanel();
    }

    @FXML
    private void wcOnImprimirDetalle() {
        wcImprimir(wcEnDetalle);
    }

    @FXML
    private void wcOnDescargarDetalle() {
        if (wcEnDetalle == null) {
            return;
        }
        Long wcId = wcEnDetalle.getWcId();
        wcExportador.wcGuardar(wcPanelDetalle.getScene().getWindow(), "Guardar comprobante",
                wcEnDetalle.getWcComprobante(), WcExportador.WcFormato.WC_PDF,
                wcDestino -> wcPdfService.wcGenerar(wcId, wcDestino));
    }

    private void wcImprimir(WcVenta wcV) {
        if (wcV == null) {
            return;
        }
        Long wcId = wcV.getWcId();
        wcExportador.wcAbrirTemporal(wcV.getWcComprobante(), WcExportador.WcFormato.WC_PDF,
                wcDestino -> wcPdfService.wcGenerar(wcId, wcDestino));
    }

    private void wcVerDetalle(WcVenta wcSeleccionada) {
        if (wcSeleccionada == null) {
            return;
        }
        WcVenta wcV = wcService.wcObtener(wcSeleccionada.getWcId());
        wcEnDetalle = wcV;

        wcLblDetComprobante.setText(wcV.getWcComprobante());
        wcLblDetEstado.setText(wcV.getWcEstado().name().replace("WC_", ""));
        wcLblDetEstado.getStyleClass().setAll("chip", wcV.isWcAnulada() ? "peligro" : "exito");
        wcLblDetSubtitulo.setText(wcV.getWcTipoComprobante().getWcDescripcion() + " · " + wcV.getWcFecha().format(WC_FECHA_HORA));

        wcLblDetIniciales.setText(WcTablas.wcIniciales(wcV.getWcCliente().getWcNombreCompleto()));
        wcLblDetCliente.setText(wcV.getWcCliente().getWcNombreCompleto());
        StringBuilder wcDatosCliente = new StringBuilder("DNI " + wcV.getWcCliente().getWcDni());
        if (wcV.getWcCliente().getWcEmail() != null) {
            wcDatosCliente.append("  ·  ").append(wcV.getWcCliente().getWcEmail());
        }
        if (wcV.getWcCliente().getWcTelefono() != null) {
            wcDatosCliente.append("  ·  ").append(wcV.getWcCliente().getWcTelefono());
        }
        wcLblDetClienteDatos.setText(wcDatosCliente.toString());

        wcTablaDetalle.getItems().setAll(wcV.getWcDetalles());

        wcLblDetEtiquetaBase.setText(wcV.isWcAplicaIgv() ? "Base imponible (Op. gravada)" : "Op. exonerada / inafecta");
        wcLblDetBase.setText(WcTablas.WC_MONEDA.format(wcV.getWcBaseImponible()));
        wcLblDetEtiquetaIgv.setText(wcV.isWcAplicaIgv()
                ? "IGV (18%) · " + (wcV.isWcPreciosIncluyenIgv() ? "incluido en precios" : "agregado al precio")
                : "IGV (no aplica)");
        wcLblDetIgv.setText(WcTablas.WC_MONEDA.format(wcV.getWcIgv()));
        wcLblDetTotal.setText(WcTablas.WC_MONEDA.format(wcV.getWcTotal()));
        wcLblDetLetras.setText("SON: " + WcNumeroALetras.wcImporte(wcV.getWcTotal()));
        boolean wcHayObs = wcV.getWcObservacion() != null;
        wcLblDetObservacion.setText(wcHayObs ? "Observacion: " + wcV.getWcObservacion() : "");
        wcLblDetObservacion.setVisible(wcHayObs);
        wcLblDetObservacion.setManaged(wcHayObs);

        wcNotificador.wcAbrirPanel(wcPanelDetalle);
    }

    private void wcCargarTabla() {
        Page<WcVenta> wcPagina = wcService.wcListar(wcTxtBuscar.getText(), wcDpDesde.getValue(), wcDpHasta.getValue(),
                wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);

        WcTotalizado wcT = wcService.wcTotalizar(wcTxtBuscar.getText(), wcDpDesde.getValue(), wcDpHasta.getValue());
        wcLblResComprobantes.setText(String.format(WcTablas.WC_LOCALE, "%,d", wcT.wcComprobantes()));
        wcLblResBase.setText(WcTablas.WC_MONEDA.format(wcT.wcBaseImponible()));
        wcLblResIgv.setText(WcTablas.WC_MONEDA.format(wcT.wcIgv()));
        wcLblResTotal.setText(WcTablas.WC_MONEDA.format(wcT.wcTotal()));
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }
}
