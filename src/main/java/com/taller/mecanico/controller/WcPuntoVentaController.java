package com.taller.mecanico.controller;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCliente;
import com.taller.mecanico.model.WcMetodoPago;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcTipoComprobante;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.service.WcCalculadoraIgv;
import com.taller.mecanico.service.WcCalculadoraIgv.WcTotales;
import com.taller.mecanico.service.WcComprobantePdfService;
import com.taller.mecanico.service.WcNumeroALetras;
import com.taller.mecanico.service.WcClienteService;
import com.taller.mecanico.service.WcRepuestoService;
import com.taller.mecanico.service.WcVentaService;
import com.taller.mecanico.service.WcVentaService.WcLineaCarrito;
import com.taller.mecanico.service.WcVentaService.WcNuevaVenta;
import com.taller.mecanico.ui.WcAutocompletar;
import com.taller.mecanico.ui.WcCeldasCarrito;
import com.taller.mecanico.ui.WcExportador;
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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class WcPuntoVentaController implements WcRefrescable {
    private static final DateTimeFormatter WC_FECHA_LARGA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", WcTablas.WC_LOCALE);

    private final WcVentaService wcVentaService;
    private final WcClienteService wcClienteService;
    private final WcRepuestoService wcRepuestoService;
    private final WcComprobantePdfService wcPdfService;
    private final WcExportador wcExportador;
    private final WcNotificador wcNotificador;

    @FXML private VBox wcRaiz;

    @FXML private TextField wcTxtDni;
    @FXML private TextField wcTxtNombreCliente;
    @FXML private HBox wcPanelSinCliente;
    @FXML private HBox wcPanelCliente;
    @FXML private Label wcLblIniciales;
    @FXML private Label wcLblClienteNombre;
    @FXML private Label wcLblClienteDni;
    @FXML private Label wcLblClienteEmail;
    @FXML private Label wcLblClienteTelefono;

    @FXML private TextField wcTxtRepuesto;
    @FXML private Spinner<Integer> wcSpnCantidad;
    @FXML private Label wcLblContadorItems;
    @FXML private TableView<WcItemCarrito> wcTablaCarrito;
    @FXML private TableColumn<WcItemCarrito, WcItemCarrito> wcColItem;
    @FXML private TableColumn<WcItemCarrito, String> wcColCodigo;
    @FXML private TableColumn<WcItemCarrito, String> wcColRepuesto;
    @FXML private TableColumn<WcItemCarrito, WcItemCarrito> wcColCantidad;
    @FXML private TableColumn<WcItemCarrito, WcItemCarrito> wcColPrecio;
    @FXML private TableColumn<WcItemCarrito, BigDecimal> wcColImporte;
    @FXML private TableColumn<WcItemCarrito, WcItemCarrito> wcColQuitar;

    @FXML private Label wcLblFecha;
    @FXML private ComboBox<WcTipoComprobante> wcCmbTipo;
    @FXML private Label wcLblNumero;
    @FXML private ToggleSwitch wcSwIgv;
    @FXML private ToggleSwitch wcSwIncluyeIgv;
    @FXML private Label wcLblModoIgv;
    @FXML private TextField wcTxtObservacion;
    @FXML private Label wcLblArticulos;
    @FXML private Label wcLblSubtotal;
    @FXML private Label wcLblEtiquetaBase;
    @FXML private Label wcLblBase;
    @FXML private Label wcLblEtiquetaIgv;
    @FXML private Label wcLblIgv;
    @FXML private Label wcLblTotal;
    @FXML private Label wcLblEnLetras;
    @FXML private Button wcBtnRegistrar;
    @FXML private ComboBox<WcMetodoPago> wcCmbMetodoPago;

    private final ObservableList<WcItemCarrito> wcCarrito =
            FXCollections.observableArrayList(wcI -> new Observable[]{wcI.wcCantidad, wcI.wcPrecio});
    private final ObjectProperty<WcCliente> wcCliente = new SimpleObjectProperty<>();

    private WcAutocompletar<WcCliente> wcAutoDni;
    private WcAutocompletar<WcCliente> wcAutoNombre;
    private WcAutocompletar<WcRepuesto> wcAutoRepuesto;

    @FXML
    public void initialize() {
        wcConfigurarCliente();
        wcConfigurarCarrito();
        wcConfigurarResumen();
        wcConfigurarPago();

        wcRaiz.addEventFilter(KeyEvent.KEY_PRESSED, wcE -> {
            if (wcE.getCode() == KeyCode.F9 && !wcBtnRegistrar.isDisabled()) {
                wcE.consume();
                wcOnRegistrar();
            }
        });
    }

    @Override
    public void wcRefrescar() {
        wcLblFecha.setText(wcCapitalizar(LocalDateTime.now().format(WC_FECHA_LARGA)));
        wcActualizarNumero();
        Platform.runLater(() -> (wcCliente.get() == null ? wcTxtDni : wcTxtRepuesto).requestFocus());
    }

    private void wcConfigurarPago() {
        wcCmbMetodoPago.getItems().setAll(WcMetodoPago.values());
        wcCmbMetodoPago.setValue(WcMetodoPago.WC_EFECTIVO);
    }

    private void wcConfigurarCliente() {
        WcFormularios.wcSoloDigitos(wcTxtDni, 8);
        WcFormularios.wcLimpiarAlEditar(wcTxtDni, wcTxtNombreCliente);

        wcAutoDni = new WcAutocompletar<>(wcTxtDni, wcClienteService::wcAutocompletar, this::wcSeleccionarCliente)
                .wcPresentacion(WcCliente::getWcDni, WcCliente::getWcNombreCompleto, wcT -> null)
                .wcIcono(this::wcAvatar)
                .wcMinimoCaracteres(2);
        wcAutoNombre = new WcAutocompletar<>(wcTxtNombreCliente, wcClienteService::wcAutocompletar, this::wcSeleccionarCliente)
                .wcPresentacion(WcCliente::getWcNombreCompleto, wcP -> "DNI " + wcP.getWcDni(), wcT -> null)
                .wcIcono(this::wcAvatar)
                .wcMinimoCaracteres(2);

        wcTxtDni.setOnAction(wcE -> wcOnBuscarDni());
    }

    @FXML
    private void wcOnBuscarDni() {
        String wcDni = wcTxtDni.getText().trim();
        wcAutoDni.wcOcultar();
        if (wcDni.length() != 8) {
            WcFormularios.wcMarcarError(wcTxtDni, "Ingresa los 8 digitos del DNI");
            return;
        }
        wcClienteService.wcBuscarPorDni(wcDni).ifPresentOrElse(this::wcSeleccionarCliente,
                () -> WcFormularios.wcMarcarError(wcTxtDni, "No hay ningun cliente con DNI " + wcDni));
    }

    private void wcSeleccionarCliente(WcCliente wcP) {
        wcCliente.set(wcP);
        WcFormularios.wcLimpiarErrores(wcTxtDni, wcTxtNombreCliente);
        wcAutoDni.setWcTexto(wcP.getWcDni());
        wcAutoNombre.setWcTexto(wcP.getWcNombreCompleto());

        wcLblIniciales.setText(WcTablas.wcIniciales(wcP.getWcNombreCompleto()));
        wcLblClienteNombre.setText(wcP.getWcNombreCompleto());
        wcLblClienteDni.setText("DNI " + wcP.getWcDni());
        wcMostrarDato(wcLblClienteEmail, wcP.getWcEmail());
        wcMostrarDato(wcLblClienteTelefono, wcP.getWcTelefono());

        wcAlternar(wcPanelCliente, wcPanelSinCliente);
        Animations.fadeIn(wcPanelCliente, Duration.millis(250)).play();
        Platform.runLater(wcTxtRepuesto::requestFocus);
    }

    @FXML
    private void wcOnQuitarCliente() {
        wcCliente.set(null);
        wcAutoDni.setWcTexto("");
        wcAutoNombre.setWcTexto("");
        wcAlternar(wcPanelSinCliente, wcPanelCliente);
        wcTxtDni.requestFocus();
    }

    private void wcConfigurarCarrito() {
        wcSpnCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));
        WcFormularios.wcSoloDigitos(wcSpnCantidad.getEditor(), 4);

        wcAutoRepuesto = new WcAutocompletar<>(wcTxtRepuesto, wcRepuestoService::wcAutocompletar, this::wcAgregarRepuesto)
                .wcPresentacion(WcRepuesto::getWcNombre,
                        wcP -> wcP.getWcCodigoRepuesto() + "  ·  " + (wcP.isWcSinStock() ? "Sin stock" : "Stock " + wcP.getWcStock()),
                        wcP -> WcTablas.WC_MONEDA.format(wcP.getWcPrecio()))
                .wcIcono(wcP -> new FontIcon(Feather.PACKAGE))
                .wcMinimoCaracteres(1);

        wcColItem.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColItem.setCellFactory(WcCeldasCarrito.wcNumeroFila());
        wcColCodigo.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().wcRepuesto.getWcCodigoRepuesto()));
        wcColCodigo.setCellFactory(WcTablas.wcCeldaChip("—"));
        wcColRepuesto.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().wcRepuesto.getWcNombre()));
        wcColCantidad.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColCantidad.setCellFactory(WcCeldasCarrito.wcCantidad(wcI -> wcI.wcCantidad, wcI -> wcI.wcRepuesto.getWcStock(),
                wcI -> "Solo hay " + wcI.wcRepuesto.getWcStock() + " unidades de " + wcI.wcRepuesto.getWcNombre() + " en stock.",
                wcNotificador::wcAdvertencia));
        wcColPrecio.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColPrecio.setCellFactory(WcCeldasCarrito.wcImporte(wcI -> wcI.wcPrecio, wcNotificador::wcAdvertencia));
        wcColImporte.setCellValueFactory(wcC -> wcC.getValue().wcImporte);
        wcColImporte.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColQuitar.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColQuitar.setCellFactory(WcCeldasCarrito.wcQuitar(wcCarrito::remove));

        WcTablas.wcConfigurar(wcTablaCarrito, Feather.SHOPPING_CART, "Carrito vacio",
                "Busca un repuesto arriba para agregarlo al carrito.", wcI -> { });
        wcTablaCarrito.setItems(wcCarrito);
        wcTablaCarrito.getStyleClass().add("tabla-carrito");

        wcCarrito.addListener((ListChangeListener<WcItemCarrito>) wcC -> wcRecalcular());
    }

    private void wcAgregarRepuesto(WcRepuesto wcP) {
        if (wcP.isWcSinStock()) {
            wcNotificador.wcAdvertencia(wcP.getWcNombre() + " no tiene stock disponible. Registra una compra para reponerlo.");
            wcAutoRepuesto.setWcTexto("");
            return;
        }
        int wcCantidad = wcSpnCantidad.getValue() == null ? 1 : wcSpnCantidad.getValue();
        WcItemCarrito wcExistente = wcCarrito.stream()
                .filter(wcI -> wcI.wcRepuesto.getWcId().equals(wcP.getWcId()))
                .findFirst().orElse(null);
        int wcEnCarrito = wcExistente == null ? 0 : wcExistente.wcCantidad.get();
        if (wcEnCarrito + wcCantidad > wcP.getWcStock()) {
            wcCantidad = wcP.getWcStock() - wcEnCarrito;
            wcNotificador.wcAdvertencia("Solo hay " + wcP.getWcStock() + " unidades de " + wcP.getWcNombre() + " en stock.");
            if (wcCantidad <= 0) {
                wcAutoRepuesto.setWcTexto("");
                return;
            }
        }
        if (wcExistente != null) {
            wcExistente.wcCantidad.set(wcEnCarrito + wcCantidad);
            wcTablaCarrito.getSelectionModel().select(wcExistente);
            wcTablaCarrito.scrollTo(wcExistente);
            wcNotificador.wcInfo(wcP.getWcNombre() + ": cantidad actualizada a " + wcExistente.wcCantidad.get());
        } else {
            WcItemCarrito wcNuevo = new WcItemCarrito(wcP, wcCantidad, wcP.getWcPrecio().setScale(2, RoundingMode.HALF_UP));
            wcCarrito.add(wcNuevo);
            wcTablaCarrito.getSelectionModel().select(wcNuevo);
            wcTablaCarrito.scrollTo(wcNuevo);
        }
        wcAutoRepuesto.setWcTexto("");
        wcSpnCantidad.getValueFactory().setValue(1);
        Platform.runLater(wcTxtRepuesto::requestFocus);
    }

    private void wcConfigurarResumen() {
        wcCmbTipo.getItems().setAll(WcTipoComprobante.values());
        wcCmbTipo.setValue(WcTipoComprobante.WC_BOLETA);
        wcCmbTipo.valueProperty().addListener((wcObs, wcA, wcB) -> wcActualizarNumero());

        wcSwIncluyeIgv.disableProperty().bind(wcSwIgv.selectedProperty().not());
        wcSwIgv.selectedProperty().addListener((wcObs, wcA, wcB) -> wcRecalcular());
        wcSwIncluyeIgv.selectedProperty().addListener((wcObs, wcA, wcB) -> wcRecalcular());

        wcBtnRegistrar.disableProperty().bind(Bindings.isEmpty(wcCarrito).or(wcCliente.isNull()));
        wcRecalcular();
    }

    private void wcRecalcular() {
        boolean wcAplica = wcSwIgv.isSelected();
        boolean wcIncluye = wcSwIncluyeIgv.isSelected();
        WcTotales wcT = WcCalculadoraIgv.wcCalcular(wcCarrito.stream().map(wcI -> wcI.wcImporte.get()).toList(), wcAplica, wcIncluye);
        BigDecimal wcSuma = wcCarrito.stream().map(wcI -> wcI.wcImporte.get()).reduce(BigDecimal.ZERO, BigDecimal::add);
        int wcArticulos = wcCarrito.stream().mapToInt(wcI -> wcI.wcCantidad.get()).sum();

        wcLblArticulos.setText(String.valueOf(wcArticulos));
        wcLblContadorItems.setText(wcCarrito.size() == 1 ? "1 item" : wcCarrito.size() + " items");
        wcLblSubtotal.setText(WcTablas.WC_MONEDA.format(wcSuma));
        wcLblEtiquetaBase.setText(wcAplica ? "Base imponible (Op. gravada)" : "Op. exonerada / inafecta");
        wcLblBase.setText(WcTablas.WC_MONEDA.format(wcT.wcBaseImponible()));
        wcLblEtiquetaIgv.setText(wcAplica ? "IGV (18%)" : "IGV (no aplica)");
        wcLblIgv.setText(WcTablas.WC_MONEDA.format(wcT.wcIgv()));
        wcLblTotal.setText(WcTablas.WC_MONEDA.format(wcT.wcTotal()));
        wcLblEnLetras.setText("SON: " + WcNumeroALetras.wcImporte(wcT.wcTotal()));
        wcLblModoIgv.setText(!wcAplica ? "Sin IGV: el total es la suma de importes"
                : wcIncluye ? "El IGV se extrae del precio (total ÷ 1.18)"
                : "El IGV (18%) se suma al precio");
    }

    private void wcActualizarNumero() {
        wcLblNumero.setText(wcVentaService.wcSiguienteComprobante(wcCmbTipo.getValue()));
    }

    @FXML
    private void wcOnRegistrar() {
        if (wcCliente.get() == null) {
            WcFormularios.wcMarcarError(wcTxtDni, "Selecciona un cliente");
            return;
        }
        if (wcCarrito.isEmpty()) {
            wcNotificador.wcAdvertencia("Agrega al menos un repuesto al carrito.");
            return;
        }
        List<WcLineaCarrito> wcLineas = wcCarrito.stream()
                .map(wcI -> new WcLineaCarrito(wcI.wcRepuesto.getWcId(), wcI.wcCantidad.get(), wcI.wcPrecio.get()))
                .toList();
        try {
            WcVenta wcVenta = wcVentaService.wcRegistrar(new WcNuevaVenta(
                    wcCmbTipo.getValue(), wcCliente.get().getWcId(),
                    wcSwIgv.isSelected(), wcSwIncluyeIgv.isSelected(),
                    wcTxtObservacion.getText(), wcLineas,
                    wcCmbMetodoPago.getValue()));

            Long wcId = wcVenta.getWcId();
            String wcComprobante = wcVenta.getWcComprobante();
            wcNotificador.wcExitoConAcciones("Venta registrada",
                    "Se emitio el comprobante " + wcComprobante + " por " + WcTablas.WC_MONEDA.format(wcVenta.getWcTotal())
                            + " a nombre de " + wcCliente.get().getWcNombreCompleto() + ".",
                    "Nueva venta", this::wcLimpiar,
                    "Imprimir comprobante", () -> {
                        wcExportador.wcAbrirTemporal(wcComprobante, WcExportador.WcFormato.WC_PDF,
                                wcDestino -> wcPdfService.wcGenerar(wcId, wcDestino));
                        wcLimpiar();
                    });
        } catch (WcNegocioException wcE) {
            wcNotificador.wcAdvertencia(wcE.getMessage());
        } catch (Exception wcE) {
            wcNotificador.wcError("No se pudo registrar la venta: " + wcE.getMessage());
        }
    }

    @FXML
    private void wcOnLimpiar() {
        if (wcCarrito.isEmpty() && wcCliente.get() == null) {
            wcLimpiar();
            return;
        }
        wcNotificador.wcConfirmar("¿Vaciar la venta en curso?",
                "Se quitaran el cliente y los " + wcCarrito.size() + " repuestos del carrito.",
                "Vaciar", this::wcLimpiar);
    }

    private void wcLimpiar() {
        wcCarrito.clear();
        wcOnQuitarCliente();
        wcTxtObservacion.clear();
        wcCmbMetodoPago.setValue(WcMetodoPago.WC_EFECTIVO);
        wcAutoRepuesto.setWcTexto("");
        wcSpnCantidad.getValueFactory().setValue(1);
        wcSwIgv.setSelected(true);
        wcSwIncluyeIgv.setSelected(true);
        wcActualizarNumero();
    }

    private StackPane wcAvatar(WcCliente wcP) {
        StackPane wcA = new StackPane(new Label(WcTablas.wcIniciales(wcP.getWcNombreCompleto())));
        wcA.getStyleClass().add("avatar");
        return wcA;
    }

    private static void wcMostrarDato(Label wcEtiqueta, String wcValor) {
        boolean wcHay = wcValor != null && !wcValor.isBlank();
        wcEtiqueta.setText(wcHay ? wcValor : "");
        wcEtiqueta.setVisible(wcHay);
        wcEtiqueta.setManaged(wcHay);
    }

    private static void wcAlternar(Node wcMostrar, Node wcOcultar) {
        wcMostrar.setVisible(true);
        wcMostrar.setManaged(true);
        wcOcultar.setVisible(false);
        wcOcultar.setManaged(false);
    }

    private static String wcCapitalizar(String wcTexto) {
        return wcTexto.isEmpty() ? wcTexto : wcTexto.substring(0, 1).toUpperCase(Locale.ROOT) + wcTexto.substring(1);
    }

    static final class WcItemCarrito {
        final WcRepuesto wcRepuesto;
        final IntegerProperty wcCantidad;
        final ObjectProperty<BigDecimal> wcPrecio;
        final ObjectBinding<BigDecimal> wcImporte;

        WcItemCarrito(WcRepuesto wcRepuesto, int wcCantidad, BigDecimal wcPrecio) {
            this.wcRepuesto = wcRepuesto;
            this.wcCantidad = new SimpleIntegerProperty(wcCantidad);
            this.wcPrecio = new SimpleObjectProperty<>(wcPrecio);
            this.wcImporte = Bindings.createObjectBinding(
                    () -> WcCalculadoraIgv.wcImporte(this.wcCantidad.get(), this.wcPrecio.get()),
                    this.wcCantidad, this.wcPrecio);
        }
    }
}
