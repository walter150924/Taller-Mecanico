package com.taller.mecanico.controller;

import atlantafx.base.util.Animations;
import com.taller.mecanico.model.WcMovimientoInventario;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.service.WcInventarioService;
import com.taller.mecanico.service.WcInventarioService.WcFiltroStock;
import com.taller.mecanico.service.WcInventarioService.WcResumenInventario;
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
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WcInventarioController implements WcRefrescable {
    private static final DateTimeFormatter WC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WcInventarioService wcService;
    private final WcReporteInventarioExcelService wcExcelService;
    private final WcExportador wcExportador;
    private final WcNotificador wcNotificador;
    private final WcMainController wcMainController;
    private final WcCompraFormController wcCompraForm;

    @FXML private Label wcLblResUnidades;
    @FXML private Label wcLblResRepuestos;
    @FXML private Label wcLblResCosto;
    @FXML private Label wcLblResVenta;
    @FXML private Label wcLblResMargen;
    @FXML private Label wcLblResBajo;
    @FXML private Label wcLblResSin;
    @FXML private VBox wcKpiBajo;
    @FXML private VBox wcKpiSin;

    @FXML private TextField wcTxtBuscar;
    @FXML private ComboBox<WcFiltroStock> wcCmbFiltro;
    @FXML private Button wcBtnKardex;
    @FXML private Button wcBtnReponer;
    @FXML private WcPaginador wcPaginador;

    @FXML private TableView<WcRepuesto> wcTabla;
    @FXML private TableColumn<WcRepuesto, String> wcColCodigo;
    @FXML private TableColumn<WcRepuesto, String> wcColNombre;
    @FXML private TableColumn<WcRepuesto, String> wcColCategoria;
    @FXML private TableColumn<WcRepuesto, WcRepuesto> wcColStock;
    @FXML private TableColumn<WcRepuesto, String> wcColMinimo;
    @FXML private TableColumn<WcRepuesto, BigDecimal> wcColCosto;
    @FXML private TableColumn<WcRepuesto, BigDecimal> wcColValor;
    @FXML private TableColumn<WcRepuesto, BigDecimal> wcColPrecio;

    @FXML private VBox wcPanelKardex;
    @FXML private Label wcLblKarRepuesto;
    @FXML private Label wcLblKarSubtitulo;
    @FXML private Label wcLblKarStock;
    @FXML private Label wcLblKarCosto;
    @FXML private Label wcLblKarValor;
    @FXML private Label wcLblKarEntradas;
    @FXML private Label wcLblKarSalidas;
    @FXML private TableView<WcMovimientoInventario> wcTablaKardex;
    @FXML private TableColumn<WcMovimientoInventario, String> wcColKarFecha;
    @FXML private TableColumn<WcMovimientoInventario, WcMovimientoInventario> wcColKarTipo;
    @FXML private TableColumn<WcMovimientoInventario, String> wcColKarDocumento;
    @FXML private TableColumn<WcMovimientoInventario, String> wcColKarEntrada;
    @FXML private TableColumn<WcMovimientoInventario, String> wcColKarSalida;
    @FXML private TableColumn<WcMovimientoInventario, String> wcColKarSaldo;

    private final ObservableList<WcRepuesto> wcDatos = FXCollections.observableArrayList();
    private WcRepuesto wcEnKardex;

    @FXML
    public void initialize() {
        wcConfigurarColumnas();
        wcConfigurarTabla();
        wcConfigurarFiltros();
        wcConfigurarKardex();
    }

    private void wcConfigurarColumnas() {
        wcColCodigo.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCodigoRepuesto()));
        wcColCodigo.setCellFactory(WcTablas.wcCeldaChip("—"));
        wcColNombre.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcNombre()));
        wcColCategoria.setCellValueFactory(wcC -> new SimpleStringProperty(
                wcC.getValue().getWcCategoria() != null ? wcC.getValue().getWcCategoria().getWcNombre() : null));
        wcColCategoria.setCellFactory(WcTablas.wcCeldaOpcional());
        wcColStock.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColStock.setCellFactory(WcTablas.wcCeldaStock());
        wcColMinimo.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcStockMinimo())));
        wcColMinimo.setCellFactory(WcTablas.wcCeldaOpcional());
        wcColCosto.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcCostoPromedio()));
        wcColCosto.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColValor.setCellValueFactory(wcC -> new SimpleObjectProperty<>(WcInventarioService.wcValorizado(wcC.getValue())));
        wcColValor.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColPrecio.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcPrecio()));
        wcColPrecio.setCellFactory(WcTablas.wcCeldaMoneda());
    }

    private void wcConfigurarTabla() {
        WcTablas.wcConfigurar(wcTabla, Feather.BOX, "Sin repuestos",
                "No hay repuestos para el filtro seleccionado.", this::wcVerKardex);
        wcTabla.setItems(wcDatos);

        var wcSeleccion = wcTabla.getSelectionModel().selectedItemProperty();
        wcBtnKardex.disableProperty().bind(wcSeleccion.isNull());
        wcBtnReponer.disableProperty().bind(wcSeleccion.isNull());
    }

    private void wcConfigurarFiltros() {
        wcCmbFiltro.getItems().setAll(WcFiltroStock.values());
        wcCmbFiltro.setValue(WcFiltroStock.WC_TODOS);
        wcCmbFiltro.valueProperty().addListener((wcObs, wcA, wcB) -> wcPaginador.wcIrAPrimera());
        wcTxtBuscar.textProperty().addListener((wcObs, wcA, wcB) -> wcPaginador.wcIrAPrimera());
        wcPaginador.setWcOnCambio(this::wcCargarTabla);

        wcKpiBajo.setOnMouseClicked(wcE -> wcCmbFiltro.setValue(WcFiltroStock.WC_BAJO));
        wcKpiSin.setOnMouseClicked(wcE -> wcCmbFiltro.setValue(WcFiltroStock.WC_SIN_STOCK));
    }

    private void wcConfigurarKardex() {
        wcColKarFecha.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcFecha().format(WC_FECHA_HORA)));
        wcColKarTipo.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColKarTipo.setCellFactory(wcCol -> new TableCell<>() {
            private final Label wcChip = new Label();

            @Override
            protected void updateItem(WcMovimientoInventario wcM, boolean wcVacio) {
                super.updateItem(wcM, wcVacio);
                if (wcVacio || wcM == null) {
                    setGraphic(null);
                    return;
                }
                wcChip.setText(wcM.getWcTipo().getWcDescripcion());
                wcChip.getStyleClass().setAll("chip", wcM.getWcTipo().isWcEntrada() ? "exito" : "advertencia");
                setGraphic(wcChip);
            }
        });
        wcColKarDocumento.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcDocumento()));
        wcColKarEntrada.setCellValueFactory(wcC -> new SimpleStringProperty(
                wcC.getValue().getWcEntrada() == 0 ? "" : "+" + wcC.getValue().getWcEntrada()));
        wcColKarEntrada.getStyleClass().add("kardex-entrada");
        wcColKarSalida.setCellValueFactory(wcC -> new SimpleStringProperty(
                wcC.getValue().getWcSalida() == 0 ? "" : "−" + wcC.getValue().getWcSalida()));
        wcColKarSalida.getStyleClass().add("kardex-salida");
        wcColKarSaldo.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcStockResultante())));
        wcColKarSaldo.getStyleClass().add("kardex-saldo");
        WcTablas.wcConfigurar(wcTablaKardex, Feather.LIST, "Sin movimientos",
                "Este repuesto aun no tiene compras ni ventas.", wcM -> { });
    }

    @Override
    public void wcRefrescar() {
        wcCargarResumen();
        wcCargarTabla();
    }

    @FXML
    private void wcOnNuevaCompra() {
        wcMainController.wcIrA("compra-form-view");
    }

    @FXML
    private void wcOnKardex() {
        wcVerKardex(wcTabla.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void wcOnReponer() {
        wcReponer(wcTabla.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void wcOnReponerDesdeKardex() {
        wcNotificador.wcCerrarPanel();
        wcReponer(wcEnKardex);
    }

    @FXML
    private void wcOnCerrarKardex() {
        wcNotificador.wcCerrarPanel();
    }

    @FXML
    private void wcOnExportarExcel() {
        wcExportador.wcGuardar(wcTabla.getScene().getWindow(), "Exportar inventario",
                "inventario_" + LocalDate.now(), WcExportador.WcFormato.WC_EXCEL, wcExcelService::wcGenerarInventario);
    }

    private void wcReponer(WcRepuesto wcP) {
        if (wcP == null) {
            return;
        }
        wcMainController.wcIrA("compra-form-view");
        int wcSugerida = Math.max(wcP.getWcStockMinimo(), wcP.getWcStockMinimo() * 2 - Math.max(0, wcP.getWcStock()));
        wcCompraForm.wcAgregarDesdeInventario(wcP, wcSugerida);
    }

    private void wcVerKardex(WcRepuesto wcP) {
        if (wcP == null) {
            return;
        }
        wcEnKardex = wcP;
        List<WcMovimientoInventario> wcMovimientos = wcService.wcKardex(wcP.getWcId());
        int wcEntradas = wcMovimientos.stream().mapToInt(WcMovimientoInventario::getWcEntrada).sum();
        int wcSalidas = wcMovimientos.stream().mapToInt(WcMovimientoInventario::getWcSalida).sum();

        wcLblKarRepuesto.setText(wcP.getWcNombre());
        wcLblKarSubtitulo.setText(wcP.getWcCodigoRepuesto() + " · Stock minimo " + wcP.getWcStockMinimo()
                + " · " + wcMovimientos.size() + " movimientos");
        wcLblKarStock.setText(wcP.getWcStock() + " u.");
        wcLblKarStock.getStyleClass().setAll("label", "resumen-filtro-valor", "stock-" + WcTablas.wcClaseStock(wcP));
        wcLblKarCosto.setText(wcP.getWcCostoPromedio() == null ? "—" : WcTablas.WC_MONEDA.format(wcP.getWcCostoPromedio()));
        wcLblKarValor.setText(WcTablas.WC_MONEDA.format(WcInventarioService.wcValorizado(wcP)));
        wcLblKarEntradas.setText(wcEntradas + " unidades ingresadas");
        wcLblKarSalidas.setText(wcSalidas + " unidades salieron");

        List<WcMovimientoInventario> wcRecientes = new ArrayList<>(wcMovimientos);
        Collections.reverse(wcRecientes);
        wcTablaKardex.getItems().setAll(wcRecientes);
        wcNotificador.wcAbrirPanel(wcPanelKardex);
    }

    private void wcCargarResumen() {
        WcResumenInventario wcR = wcService.wcResumen();
        wcLblResUnidades.setText(String.format(WcTablas.WC_LOCALE, "%,d", wcR.wcUnidades()));
        wcLblResRepuestos.setText(wcR.wcRepuestos() + " repuestos en catalogo");
        wcLblResCosto.setText(WcTablas.WC_MONEDA.format(wcR.wcValorCosto()));
        wcLblResVenta.setText(WcTablas.WC_MONEDA.format(wcR.wcValorVenta()));
        BigDecimal wcVentaSinIgv = wcR.wcValorVenta().divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        wcLblResMargen.setText(wcR.wcValorCosto().signum() == 0 ? "Sin costo registrado"
                : "Margen potencial " + WcTablas.WC_MONEDA.format(wcVentaSinIgv.subtract(wcR.wcValorCosto())) + " sin IGV");
        wcLblResBajo.setText(String.valueOf(wcR.wcStockBajo()));
        wcLblResSin.setText(String.valueOf(wcR.wcSinStock()));
    }

    private void wcCargarTabla() {
        Page<WcRepuesto> wcPagina = wcService.wcListar(wcTxtBuscar.getText(), wcCmbFiltro.getValue(),
                wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }
}
