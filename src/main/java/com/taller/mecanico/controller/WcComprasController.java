package com.taller.mecanico.controller;

import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCompra;
import com.taller.mecanico.model.WcCompraDetalle;
import com.taller.mecanico.service.WcCompraService;
import com.taller.mecanico.service.WcCompraService.WcTotalizado;
import com.taller.mecanico.service.WcNumeroALetras;
import com.taller.mecanico.service.WcReporteInventarioExcelService;
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
public class WcComprasController implements WcRefrescable {
    private static final DateTimeFormatter WC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter WC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final PseudoClass WC_ANULADA = PseudoClass.getPseudoClass("anulada");

    private final WcCompraService wcService;
    private final WcReporteInventarioExcelService wcExcelService;
    private final WcExportador wcExportador;
    private final WcNotificador wcNotificador;
    private final WcMainController wcMainController;

    @FXML private TextField wcTxtBuscar;
    @FXML private DatePicker wcDpDesde;
    @FXML private DatePicker wcDpHasta;
    @FXML private Button wcBtnVer;
    @FXML private Button wcBtnAnular;
    @FXML private WcPaginador wcPaginador;

    @FXML private Label wcLblResCompras;
    @FXML private Label wcLblResBase;
    @FXML private Label wcLblResIgv;
    @FXML private Label wcLblResTotal;

    @FXML private TableView<WcCompra> wcTabla;
    @FXML private TableColumn<WcCompra, String> wcColCodigo;
    @FXML private TableColumn<WcCompra, String> wcColFecha;
    @FXML private TableColumn<WcCompra, String> wcColProveedor;
    @FXML private TableColumn<WcCompra, String> wcColDocumento;
    @FXML private TableColumn<WcCompra, BigDecimal> wcColBase;
    @FXML private TableColumn<WcCompra, BigDecimal> wcColIgv;
    @FXML private TableColumn<WcCompra, BigDecimal> wcColTotal;
    @FXML private TableColumn<WcCompra, String> wcColEstado;

    @FXML private VBox wcPanelDetalle;
    @FXML private Label wcLblDetCodigo;
    @FXML private Label wcLblDetEstado;
    @FXML private Label wcLblDetSubtitulo;
    @FXML private Label wcLblDetProveedor;
    @FXML private Label wcLblDetProveedorDatos;
    @FXML private TableView<WcCompraDetalle> wcTablaDetalle;
    @FXML private TableColumn<WcCompraDetalle, String> wcColDetItem;
    @FXML private TableColumn<WcCompraDetalle, String> wcColDetRepuesto;
    @FXML private TableColumn<WcCompraDetalle, String> wcColDetCantidad;
    @FXML private TableColumn<WcCompraDetalle, BigDecimal> wcColDetCosto;
    @FXML private TableColumn<WcCompraDetalle, BigDecimal> wcColDetImporte;
    @FXML private Label wcLblDetEtiquetaBase;
    @FXML private Label wcLblDetBase;
    @FXML private Label wcLblDetEtiquetaIgv;
    @FXML private Label wcLblDetIgv;
    @FXML private Label wcLblDetTotal;
    @FXML private Label wcLblDetLetras;
    @FXML private Label wcLblDetObservacion;

    private final ObservableList<WcCompra> wcDatos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        wcConfigurarColumnas();
        wcConfigurarTabla();
        wcConfigurarFiltros();
        wcConfigurarDetalle();
    }

    private void wcConfigurarColumnas() {
        wcColCodigo.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCodigo()));
        wcColCodigo.setCellFactory(WcTablas.wcCeldaChip("—"));
        wcColFecha.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcFecha().format(WC_FECHA_HORA)));
        wcColProveedor.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcProveedor().getWcRazonSocial()));
        wcColDocumento.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcNumeroDocumento()));
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
        WcTablas.wcConfigurar(wcTabla, Feather.TRUCK, "Sin compras",
                "No hay compras para el filtro seleccionado.", this::wcVerDetalle);
        wcTabla.setItems(wcDatos);
        wcTabla.setRowFactory(wcTv -> {
            TableRow<WcCompra> wcFila = new TableRow<>() {
                @Override
                protected void updateItem(WcCompra wcC, boolean wcVacio) {
                    super.updateItem(wcC, wcVacio);
                    pseudoClassStateChanged(WC_ANULADA, !wcVacio && wcC != null && wcC.isWcAnulada());
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
        wcColDetRepuesto.setCellValueFactory(wcC -> new SimpleStringProperty(
                wcC.getValue().getWcCodigoRepuesto() + "  " + wcC.getValue().getWcNombreRepuesto()));
        wcColDetCantidad.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcCantidad())));
        wcColDetCosto.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcCostoUnitario()));
        wcColDetCosto.setCellFactory(WcTablas.wcCeldaMoneda());
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
    private void wcOnNuevaCompra() {
        wcMainController.wcIrA("compra-form-view");
    }

    @FXML
    private void wcOnVer() {
        wcVerDetalle(wcTabla.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void wcOnAnular() {
        WcCompra wcC = wcTabla.getSelectionModel().getSelectedItem();
        if (wcC == null) {
            return;
        }
        if (wcC.isWcAnulada()) {
            wcNotificador.wcAdvertencia("La compra " + wcC.getWcCodigo() + " ya esta anulada.");
            return;
        }
        wcNotificador.wcConfirmar("¿Anular la compra " + wcC.getWcCodigo() + "?",
                "Se retiraran del inventario las unidades que ingresaron con el documento "
                        + wcC.getWcNumeroDocumento() + " de " + wcC.getWcProveedor().getWcRazonSocial() + ".",
                "Anular",
                () -> {
                    try {
                        wcService.wcAnular(wcC.getWcId());
                        wcNotificador.wcExito("Compra " + wcC.getWcCodigo() + " anulada; el stock fue actualizado.");
                        wcCargarTabla();
                    } catch (WcNegocioException wcE) {
                        wcNotificador.wcError(wcE.getMessage());
                    }
                });
    }

    @FXML
    private void wcOnExportarExcel() {
        LocalDate wcHasta = wcDpHasta.getValue() != null ? wcDpHasta.getValue() : LocalDate.now();
        LocalDate wcDesde = wcDpDesde.getValue() != null ? wcDpDesde.getValue() : wcHasta.minusYears(1);
        if (wcDesde.isAfter(wcHasta)) {
            wcNotificador.wcAdvertencia("La fecha inicial no puede ser posterior a la final.");
            return;
        }
        wcExportador.wcGuardar(wcTabla.getScene().getWindow(), "Exportar compras",
                "compras_" + wcDesde + "_" + wcHasta, WcExportador.WcFormato.WC_EXCEL,
                wcDestino -> wcExcelService.wcGenerarCompras(wcDesde, wcHasta, wcDestino));
    }

    @FXML
    private void wcOnCerrarDetalle() {
        wcNotificador.wcCerrarPanel();
    }

    private void wcVerDetalle(WcCompra wcSeleccionada) {
        if (wcSeleccionada == null) {
            return;
        }
        WcCompra wcC = wcService.wcObtener(wcSeleccionada.getWcId());

        wcLblDetCodigo.setText(wcC.getWcCodigo());
        wcLblDetEstado.setText(wcC.getWcEstado().name().replace("WC_", ""));
        wcLblDetEstado.getStyleClass().setAll("chip", wcC.isWcAnulada() ? "peligro" : "exito");
        wcLblDetSubtitulo.setText(wcC.getWcTipoDocumento().getWcDescripcion() + " " + wcC.getWcNumeroDocumento()
                + " del " + wcC.getWcFechaDocumento().format(WC_FECHA) + " · registrada " + wcC.getWcFecha().format(WC_FECHA_HORA));
        wcLblDetProveedor.setText(wcC.getWcProveedor().getWcRazonSocial());
        wcLblDetProveedorDatos.setText("RUC " + wcC.getWcProveedor().getWcRuc()
                + (wcC.getWcProveedor().getWcTelefono() != null ? "  ·  " + wcC.getWcProveedor().getWcTelefono() : ""));

        wcTablaDetalle.getItems().setAll(wcC.getWcDetalles());

        wcLblDetEtiquetaBase.setText(wcC.isWcAplicaIgv() ? "Base imponible (Op. gravada)" : "Op. no gravada");
        wcLblDetBase.setText(WcTablas.WC_MONEDA.format(wcC.getWcBaseImponible()));
        wcLblDetEtiquetaIgv.setText(wcC.isWcAplicaIgv()
                ? "IGV (18%) · " + (wcC.isWcPreciosIncluyenIgv() ? "incluido en costos" : "agregado a los costos")
                : "IGV (no aplica)");
        wcLblDetIgv.setText(WcTablas.WC_MONEDA.format(wcC.getWcIgv()));
        wcLblDetTotal.setText(WcTablas.WC_MONEDA.format(wcC.getWcTotal()));
        wcLblDetLetras.setText("SON: " + WcNumeroALetras.wcImporte(wcC.getWcTotal()));
        boolean wcHayObs = wcC.getWcObservacion() != null;
        wcLblDetObservacion.setText(wcHayObs ? "Observacion: " + wcC.getWcObservacion() : "");
        wcLblDetObservacion.setVisible(wcHayObs);
        wcLblDetObservacion.setManaged(wcHayObs);

        wcNotificador.wcAbrirPanel(wcPanelDetalle);
    }

    private void wcCargarTabla() {
        Page<WcCompra> wcPagina = wcService.wcListar(wcTxtBuscar.getText(), wcDpDesde.getValue(), wcDpHasta.getValue(),
                wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);

        WcTotalizado wcT = wcService.wcTotalizar(wcTxtBuscar.getText(), wcDpDesde.getValue(), wcDpHasta.getValue());
        wcLblResCompras.setText(String.format(WcTablas.WC_LOCALE, "%,d", wcT.wcCompras()));
        wcLblResBase.setText(WcTablas.WC_MONEDA.format(wcT.wcBaseImponible()));
        wcLblResIgv.setText(WcTablas.WC_MONEDA.format(wcT.wcIgv()));
        wcLblResTotal.setText(WcTablas.WC_MONEDA.format(wcT.wcTotal()));
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }
}
