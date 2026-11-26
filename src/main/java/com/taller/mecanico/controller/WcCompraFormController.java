package com.taller.mecanico.controller;

import atlantafx.base.controls.ToggleSwitch;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCompra;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcProveedor;
import com.taller.mecanico.model.WcTipoDocumentoCompra;
import com.taller.mecanico.service.WcCalculadoraIgv;
import com.taller.mecanico.service.WcCalculadoraIgv.WcTotales;
import com.taller.mecanico.service.WcCompraService;
import com.taller.mecanico.service.WcCompraService.WcLineaCompra;
import com.taller.mecanico.service.WcCompraService.WcNuevaCompra;
import com.taller.mecanico.service.WcNumeroALetras;
import com.taller.mecanico.service.WcRepuestoService;
import com.taller.mecanico.service.WcProveedorService;
import com.taller.mecanico.ui.WcAutocompletar;
import com.taller.mecanico.ui.WcCeldasCarrito;
import com.taller.mecanico.ui.WcFormularios;
import com.taller.mecanico.ui.WcNotificador;
import com.taller.mecanico.ui.WcRefrescable;
import com.taller.mecanico.ui.WcTablas;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class WcCompraFormController implements WcRefrescable {
    private static final DateTimeFormatter WC_FECHA_LARGA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", WcTablas.WC_LOCALE);
    private static final BigDecimal WC_FACTOR_IGV = BigDecimal.ONE.add(WcCalculadoraIgv.WC_TASA_IGV);

    private final WcCompraService wcCompraService;
    private final WcProveedorService wcProveedorService;
    private final WcRepuestoService wcRepuestoService;
    private final WcNotificador wcNotificador;
    private final WcMainController wcMainController;

    @FXML private VBox wcRaiz;
    @FXML private TextField wcTxtRuc;
    @FXML private TextField wcTxtRazonSocial;
    @FXML private Label wcLblEstadoProveedor;
    @FXML private ComboBox<WcTipoDocumentoCompra> wcCmbTipoDocumento;
    @FXML private TextField wcTxtNumeroDocumento;
    @FXML private DatePicker wcDpFechaDocumento;

    @FXML private TextField wcTxtRepuesto;
    @FXML private Spinner<Integer> wcSpnCantidad;
    @FXML private Label wcLblContadorItems;
    @FXML private TableView<WcItemCompra> wcTablaItems;
    @FXML private TableColumn<WcItemCompra, WcItemCompra> wcColItem;
    @FXML private TableColumn<WcItemCompra, String> wcColRepuesto;
    @FXML private TableColumn<WcItemCompra, String> wcColStock;
    @FXML private TableColumn<WcItemCompra, WcItemCompra> wcColCantidad;
    @FXML private TableColumn<WcItemCompra, WcItemCompra> wcColCosto;
    @FXML private TableColumn<WcItemCompra, BigDecimal> wcColImporte;
    @FXML private TableColumn<WcItemCompra, WcItemCompra> wcColQuitar;

    @FXML private Label wcLblFecha;
    @FXML private Label wcLblCodigo;
    @FXML private ToggleSwitch wcSwIgv;
    @FXML private ToggleSwitch wcSwIncluyeIgv;
    @FXML private Label wcLblModoIgv;
    @FXML private TextField wcTxtObservacion;
    @FXML private Label wcLblUnidades;
    @FXML private Label wcLblEtiquetaBase;
    @FXML private Label wcLblBase;
    @FXML private Label wcLblEtiquetaIgv;
    @FXML private Label wcLblIgv;
    @FXML private Label wcLblTotal;
    @FXML private Label wcLblEnLetras;
    @FXML private Button wcBtnRegistrar;

    private final ObservableList<WcItemCompra> wcItems =
            FXCollections.observableArrayList(wcI -> new Observable[]{wcI.wcCantidad, wcI.wcCosto});

    private WcAutocompletar<WcProveedor> wcAutoRuc;
    private WcAutocompletar<WcProveedor> wcAutoRazon;
    private WcAutocompletar<WcRepuesto> wcAutoRepuesto;

    private WcProveedor wcProveedor;

    @FXML
    public void initialize() {
        wcConfigurarProveedor();
        wcConfigurarItems();
        wcConfigurarResumen();

        wcRaiz.addEventFilter(KeyEvent.KEY_PRESSED, wcE -> {
            if (wcE.getCode() == KeyCode.F9 && !wcBtnRegistrar.isDisabled()) {
                wcE.consume();
                wcOnRegistrar();
            }
        });
    }

    @Override
    public void wcRefrescar() {
        String wcHoy = LocalDateTime.now().format(WC_FECHA_LARGA);
        wcLblFecha.setText(wcHoy.substring(0, 1).toUpperCase(Locale.ROOT) + wcHoy.substring(1));
        wcLblCodigo.setText(wcCompraService.wcSiguienteCodigo());
        if (wcDpFechaDocumento.getValue() == null) {
            wcDpFechaDocumento.setValue(LocalDate.now());
        }
        Platform.runLater(() -> (wcTxtRuc.getText().isBlank() ? wcTxtRuc : wcTxtRepuesto).requestFocus());
    }

    private void wcConfigurarProveedor() {
        WcFormularios.wcSoloDigitos(wcTxtRuc, 11);
        WcFormularios.wcLimpiarAlEditar(wcTxtRuc, wcTxtRazonSocial, wcTxtNumeroDocumento);

        wcAutoRuc = new WcAutocompletar<>(wcTxtRuc, wcProveedorService::wcAutocompletar, this::wcSeleccionarProveedor)
                .wcPresentacion(WcProveedor::getWcRuc, WcProveedor::getWcRazonSocial, wcP -> null)
                .wcIcono(wcP -> new FontIcon(Feather.TRUCK))
                .wcMinimoCaracteres(2);
        wcAutoRazon = new WcAutocompletar<>(wcTxtRazonSocial, wcProveedorService::wcAutocompletar, this::wcSeleccionarProveedor)
                .wcPresentacion(WcProveedor::getWcRazonSocial, wcP -> "RUC " + wcP.getWcRuc(), wcP -> null)
                .wcIcono(wcP -> new FontIcon(Feather.TRUCK))
                .wcMinimoCaracteres(2);

        wcTxtRuc.textProperty().addListener((wcObs, wcA, wcB) -> {
            if (wcProveedor != null && !wcProveedor.getWcRuc().equals(wcB)) {
                wcProveedor = null;
            }
            wcActualizarEstadoProveedor();
        });
        wcTxtRuc.setOnAction(wcE -> wcBuscarRucExacto());
        wcActualizarEstadoProveedor();

        wcCmbTipoDocumento.getItems().setAll(WcTipoDocumentoCompra.values());
        wcCmbTipoDocumento.setValue(WcTipoDocumentoCompra.WC_FACTURA);
        wcDpFechaDocumento.setValue(LocalDate.now());
        wcDpFechaDocumento.setDayCellFactory(wcDp -> new DateCell() {
            @Override
            public void updateItem(LocalDate wcFecha, boolean wcVacio) {
                super.updateItem(wcFecha, wcVacio);
                setDisable(wcVacio || wcFecha.isAfter(LocalDate.now()));
            }
        });
    }

    private void wcBuscarRucExacto() {
        wcAutoRuc.wcOcultar();
        wcProveedorService.wcBuscarPorRuc(wcTxtRuc.getText()).ifPresentOrElse(this::wcSeleccionarProveedor, () -> {
            if (wcTxtRuc.getText().length() == 11) {
                wcTxtRazonSocial.requestFocus();
            }
        });
    }

    private void wcSeleccionarProveedor(WcProveedor wcP) {
        wcProveedor = wcP;
        wcAutoRuc.setWcTexto(wcP.getWcRuc());
        wcAutoRazon.setWcTexto(wcP.getWcRazonSocial());
        WcFormularios.wcLimpiarErrores(wcTxtRuc, wcTxtRazonSocial);
        wcActualizarEstadoProveedor();
        Platform.runLater(wcTxtNumeroDocumento::requestFocus);
    }

    private void wcActualizarEstadoProveedor() {
        boolean wcVacio = wcTxtRuc.getText() == null || wcTxtRuc.getText().isBlank();
        wcLblEstadoProveedor.setText(wcProveedor != null ? "Proveedor registrado" : wcVacio ? "Sin proveedor" : "Proveedor nuevo");
        wcLblEstadoProveedor.getStyleClass().setAll("chip", wcProveedor != null ? "exito" : wcVacio ? "neutro" : "advertencia");
    }

    private void wcConfigurarItems() {
        wcSpnCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));
        WcFormularios.wcSoloDigitos(wcSpnCantidad.getEditor(), 4);

        wcAutoRepuesto = new WcAutocompletar<>(wcTxtRepuesto, wcRepuestoService::wcAutocompletar, this::wcAgregarRepuesto)
                .wcPresentacion(WcRepuesto::getWcNombre,
                        wcP -> wcP.getWcCodigoRepuesto() + "  ·  Stock " + wcP.getWcStock()
                                + (wcP.getWcCostoPromedio() != null ? "  ·  Costo prom. " + WcTablas.WC_MONEDA.format(wcP.getWcCostoPromedio()) : ""),
                        wcP -> null)
                .wcIcono(wcP -> new FontIcon(Feather.PACKAGE))
                .wcMinimoCaracteres(1);

        wcColItem.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColItem.setCellFactory(WcCeldasCarrito.wcNumeroFila());
        wcColRepuesto.setCellValueFactory(wcC -> new SimpleStringProperty(
                wcC.getValue().wcRepuesto.getWcCodigoRepuesto() + "  " + wcC.getValue().wcRepuesto.getWcNombre()));
        wcColStock.setCellValueFactory(wcC -> Bindings.createStringBinding(
                () -> wcC.getValue().wcRepuesto.getWcStock() + " → " + (wcC.getValue().wcRepuesto.getWcStock() + wcC.getValue().wcCantidad.get()),
                wcC.getValue().wcCantidad));
        wcColStock.getStyleClass().add("columna-stock");
        wcColCantidad.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColCantidad.setCellFactory(WcCeldasCarrito.wcCantidad(wcI -> wcI.wcCantidad, wcI -> 9999, wcI -> "", wcNotificador::wcAdvertencia));
        wcColCosto.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColCosto.setCellFactory(WcCeldasCarrito.wcImporte(wcI -> wcI.wcCosto, wcNotificador::wcAdvertencia));
        wcColImporte.setCellValueFactory(wcC -> wcC.getValue().wcImporte);
        wcColImporte.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColQuitar.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColQuitar.setCellFactory(WcCeldasCarrito.wcQuitar(wcItems::remove));

        WcTablas.wcConfigurar(wcTablaItems, Feather.PACKAGE, "Sin repuestos",
                "Busca un repuesto arriba para agregarlo a la compra.", wcI -> { });
        wcTablaItems.setItems(wcItems);
        wcTablaItems.getStyleClass().add("tabla-carrito");
        wcItems.addListener((ListChangeListener<WcItemCompra>) wcC -> wcRecalcular());
    }

    public void wcAgregarDesdeInventario(WcRepuesto wcP, int wcCantidad) {
        wcSpnCantidad.getValueFactory().setValue(Math.min(9999, Math.max(1, wcCantidad)));
        wcAgregarRepuesto(wcP);
        wcNotificador.wcInfo("Se agrego " + wcP.getWcNombre() + " a la compra. Completa el proveedor y el documento.");
        Platform.runLater(wcTxtRuc::requestFocus);
    }

    private void wcAgregarRepuesto(WcRepuesto wcP) {
        int wcCantidad = wcSpnCantidad.getValue() == null ? 1 : wcSpnCantidad.getValue();
        WcItemCompra wcExistente = wcItems.stream().filter(wcI -> wcI.wcRepuesto.getWcId().equals(wcP.getWcId())).findFirst().orElse(null);
        if (wcExistente != null) {
            wcExistente.wcCantidad.set(Math.min(9999, wcExistente.wcCantidad.get() + wcCantidad));
            wcTablaItems.getSelectionModel().select(wcExistente);
        } else {
            WcItemCompra wcNuevo = new WcItemCompra(wcP, wcCantidad, wcCostoSugerido(wcP));
            wcItems.add(wcNuevo);
            wcTablaItems.getSelectionModel().select(wcNuevo);
            wcTablaItems.scrollTo(wcNuevo);
        }
        wcAutoRepuesto.setWcTexto("");
        wcSpnCantidad.getValueFactory().setValue(1);
        Platform.runLater(wcTxtRepuesto::requestFocus);
    }

    private BigDecimal wcCostoSugerido(WcRepuesto wcP) {
        BigDecimal wcSinIgv = wcP.getWcCostoPromedio() != null
                ? wcP.getWcCostoPromedio()
                : wcP.getWcPrecio().divide(WC_FACTOR_IGV, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("0.70"));
        BigDecimal wcCosto = wcSwIgv.isSelected() && wcSwIncluyeIgv.isSelected() ? wcSinIgv.multiply(WC_FACTOR_IGV) : wcSinIgv;
        return wcCosto.setScale(2, RoundingMode.HALF_UP).max(new BigDecimal("0.01"));
    }

    private void wcConfigurarResumen() {
        wcSwIncluyeIgv.disableProperty().bind(wcSwIgv.selectedProperty().not());
        wcSwIgv.selectedProperty().addListener((wcObs, wcA, wcB) -> wcRecalcular());
        wcSwIncluyeIgv.selectedProperty().addListener((wcObs, wcA, wcB) -> wcRecalcular());
        wcBtnRegistrar.disableProperty().bind(Bindings.isEmpty(wcItems));
        wcRecalcular();
    }

    private void wcRecalcular() {
        boolean wcAplica = wcSwIgv.isSelected();
        boolean wcIncluye = wcSwIncluyeIgv.isSelected();
        WcTotales wcT = WcCalculadoraIgv.wcCalcular(wcItems.stream().map(wcI -> wcI.wcImporte.get()).toList(), wcAplica, wcIncluye);
        int wcUnidades = wcItems.stream().mapToInt(wcI -> wcI.wcCantidad.get()).sum();

        wcLblUnidades.setText(String.valueOf(wcUnidades));
        wcLblContadorItems.setText(wcItems.size() == 1 ? "1 item" : wcItems.size() + " items");
        wcLblEtiquetaBase.setText(wcAplica ? "Base imponible (Op. gravada)" : "Op. no gravada");
        wcLblBase.setText(WcTablas.WC_MONEDA.format(wcT.wcBaseImponible()));
        wcLblEtiquetaIgv.setText(wcAplica ? "IGV (18%) · credito fiscal" : "IGV (no aplica)");
        wcLblIgv.setText(WcTablas.WC_MONEDA.format(wcT.wcIgv()));
        wcLblTotal.setText(WcTablas.WC_MONEDA.format(wcT.wcTotal()));
        wcLblEnLetras.setText("SON: " + WcNumeroALetras.wcImporte(wcT.wcTotal()));
        wcLblModoIgv.setText(!wcAplica ? "Sin IGV: el total es la suma de importes"
                : wcIncluye ? "El IGV se extrae del costo (total ÷ 1.18)"
                : "El IGV (18%) se suma a los costos");
    }

    private boolean wcFormularioValido() {
        WcFormularios.wcLimpiarErrores(wcTxtRuc, wcTxtRazonSocial, wcTxtNumeroDocumento, wcDpFechaDocumento);
        boolean wcValido = true;
        if (!wcTxtRuc.getText().matches("(10|15|17|20)\\d{9}")) {
            WcFormularios.wcMarcarError(wcTxtRuc, "RUC de 11 digitos (inicia en 10, 15, 17 o 20)");
            wcValido = false;
        }
        if (wcTxtRazonSocial.getText().isBlank()) {
            WcFormularios.wcMarcarError(wcTxtRazonSocial, "Ingresa la razon social");
            wcValido = false;
        }
        if (wcTxtNumeroDocumento.getText().isBlank()) {
            WcFormularios.wcMarcarError(wcTxtNumeroDocumento, "Ingresa la serie y numero del documento");
            wcValido = false;
        }
        if (wcDpFechaDocumento.getValue() == null || wcDpFechaDocumento.getValue().isAfter(LocalDate.now())) {
            WcFormularios.wcMarcarError(wcDpFechaDocumento, "Fecha invalida");
            wcValido = false;
        }
        return wcValido;
    }

    @FXML
    private void wcOnRegistrar() {
        if (!wcFormularioValido()) {
            return;
        }

        List<WcLineaCompra> wcLineas = wcItems.stream()
                .map(wcI -> new WcLineaCompra(wcI.wcRepuesto.getWcId(), wcI.wcCantidad.get(), wcI.wcCosto.get()))
                .toList();
        try {
            WcCompra wcCompra = wcCompraService.wcRegistrar(new WcNuevaCompra(
                    wcTxtRuc.getText(), wcTxtRazonSocial.getText(), wcCmbTipoDocumento.getValue(),
                    wcTxtNumeroDocumento.getText(), wcDpFechaDocumento.getValue(),
                    wcSwIgv.isSelected(), wcSwIncluyeIgv.isSelected(), wcTxtObservacion.getText(), wcLineas));
            int wcUnidades = wcLineas.stream().mapToInt(WcLineaCompra::wcCantidad).sum();
            wcNotificador.wcExitoConAcciones("Compra registrada",
                    "Se registro " + wcCompra.getWcCodigo() + " (" + wcCompra.getWcNumeroDocumento() + ") por "
                            + WcTablas.WC_MONEDA.format(wcCompra.getWcTotal()) + ". Ingresaron " + wcUnidades
                            + " unidades al inventario.",
                    "Nueva compra", this::wcLimpiar,
                    "Ver inventario", () -> {
                        wcLimpiar();
                        wcMainController.wcIrA("inventario-view");
                    });
        } catch (WcNegocioException wcE) {
            wcNotificador.wcAdvertencia(wcE.getMessage());
        } catch (Exception wcE) {
            wcNotificador.wcError("No se pudo registrar la compra: " + wcE.getMessage());
        }
    }

    @FXML
    private void wcOnLimpiar() {
        if (wcItems.isEmpty() && wcTxtRuc.getText().isBlank()) {
            wcLimpiar();
            return;
        }
        wcNotificador.wcConfirmar("¿Vaciar la compra en curso?",
                "Se quitaran el proveedor, el documento y los " + wcItems.size() + " repuestos.",
                "Vaciar", this::wcLimpiar);
    }

    private void wcLimpiar() {
        wcItems.clear();
        wcProveedor = null;
        wcAutoRuc.setWcTexto("");
        wcAutoRazon.setWcTexto("");
        wcAutoRepuesto.setWcTexto("");
        wcTxtNumeroDocumento.clear();
        wcTxtObservacion.clear();
        wcCmbTipoDocumento.setValue(WcTipoDocumentoCompra.WC_FACTURA);
        wcDpFechaDocumento.setValue(LocalDate.now());
        wcSwIgv.setSelected(true);
        wcSwIncluyeIgv.setSelected(false);
        WcFormularios.wcLimpiarErrores(wcTxtRuc, wcTxtRazonSocial, wcTxtNumeroDocumento, wcDpFechaDocumento);
        wcActualizarEstadoProveedor();
        wcRefrescar();
    }

    static final class WcItemCompra {
        final WcRepuesto wcRepuesto;
        final IntegerProperty wcCantidad;
        final ObjectProperty<BigDecimal> wcCosto;
        final ObjectBinding<BigDecimal> wcImporte;

        WcItemCompra(WcRepuesto wcRepuesto, int wcCantidad, BigDecimal wcCosto) {
            this.wcRepuesto = wcRepuesto;
            this.wcCantidad = new SimpleIntegerProperty(wcCantidad);
            this.wcCosto = new SimpleObjectProperty<>(wcCosto);
            this.wcImporte = Bindings.createObjectBinding(
                    () -> WcCalculadoraIgv.wcImporte(this.wcCantidad.get(), this.wcCosto.get()), this.wcCantidad, this.wcCosto);
        }
    }
}
