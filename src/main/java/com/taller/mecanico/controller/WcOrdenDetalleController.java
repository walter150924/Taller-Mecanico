package com.taller.mecanico.controller;

import atlantafx.base.theme.Styles;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCatalogoFallas;
import com.taller.mecanico.model.WcEstadoOrden;
import com.taller.mecanico.model.WcLineaRepuesto;
import com.taller.mecanico.model.WcLineaServicio;
import com.taller.mecanico.model.WcMetodoPago;
import com.taller.mecanico.model.WcOrdenServicio;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcServicio;
import com.taller.mecanico.model.WcUsuario;
import com.taller.mecanico.seguridad.WcSesion;
import com.taller.mecanico.service.WcOrdenPdfService;
import com.taller.mecanico.service.WcOrdenServicioService;
import com.taller.mecanico.service.WcRepuestoService;
import com.taller.mecanico.service.WcServicioService;
import com.taller.mecanico.service.WcUsuarioService;
import com.taller.mecanico.ui.WcAutocompletar;
import com.taller.mecanico.ui.WcCeldasCarrito;
import com.taller.mecanico.ui.WcConvertidores;
import com.taller.mecanico.ui.WcExportador;
import com.taller.mecanico.ui.WcFormularios;
import com.taller.mecanico.ui.WcNotificador;
import com.taller.mecanico.ui.WcTablas;
import com.taller.mecanico.ui.WcSelectorMultiple;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class WcOrdenDetalleController {
    private static final DateTimeFormatter WC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int WC_CANTIDAD_MAXIMA = 999;
    private static final int WC_DIGITOS_CANTIDAD = 3;

    private final WcOrdenServicioService wcOrdenService;
    private final WcServicioService wcServicioService;
    private final WcRepuestoService wcRepuestoService;
    private final WcUsuarioService wcUsuarioService;
    private final WcOrdenPdfService wcPdfService;
    private final WcExportador wcExportador;
    private final WcNotificador wcNotificador;
    private final WcSesion wcSesion;

    @FXML private VBox wcRaiz;
    @FXML private Label wcLblCodigo;
    @FXML private Label wcLblEstado;
    @FXML private Label wcLblSubtitulo;
    @FXML private Label wcLblCliente;
    @FXML private Label wcLblVehiculo;
    @FXML private Label wcLblKilometraje;
    @FXML private Label wcLblProblema;
    @FXML private Label wcLblMecanico;
    @FXML private Label wcLblPago;
    @FXML private HBox wcBarraAsignacion;
    @FXML private ComboBox<WcUsuario> wcCmbMecanico;

    @FXML private ComboBox<String> wcCmbDiagnostico;
    @FXML private Button wcBtnAgregarDiagnostico;
    @FXML private ListView<String> wcLstDiagnostico;
    private WcSelectorMultiple wcDiagnostico;
    @FXML private Button wcBtnGuardarDiagnostico;

    @FXML private HBox wcBarraServicios;
    @FXML private ComboBox<WcServicio> wcCmbServicio;
    @FXML private TableView<WcLineaServicio> wcTablaServicios;
    @FXML private TableColumn<WcLineaServicio, String> wcColServicio;
    @FXML private TableColumn<WcLineaServicio, BigDecimal> wcColPrecioServicio;
    @FXML private TableColumn<WcLineaServicio, WcLineaServicio> wcColQuitarServicio;

    @FXML private HBox wcBarraRepuestos;
    @FXML private TextField wcTxtRepuesto;
    @FXML private Spinner<Integer> wcSpnCantidad;
    @FXML private TableView<WcLineaRepuesto> wcTablaRepuestos;
    @FXML private TableColumn<WcLineaRepuesto, String> wcColCodigoRepuesto;
    @FXML private TableColumn<WcLineaRepuesto, String> wcColRepuesto;
    @FXML private TableColumn<WcLineaRepuesto, String> wcColCantidad;
    @FXML private TableColumn<WcLineaRepuesto, BigDecimal> wcColPrecioRepuesto;
    @FXML private TableColumn<WcLineaRepuesto, BigDecimal> wcColImporteRepuesto;
    @FXML private TableColumn<WcLineaRepuesto, WcLineaRepuesto> wcColQuitarRepuesto;

    @FXML private Label wcLblTotalServicios;
    @FXML private Label wcLblTotalRepuestos;
    @FXML private Label wcLblBase;
    @FXML private Label wcLblIgv;
    @FXML private Label wcLblTotal;
    @FXML private HBox wcAccionesEstado;

    @FXML private VBox wcDialogoCobro;
    @FXML private Label wcLblCobroTotal;
    @FXML private ComboBox<WcMetodoPago> wcCmbMetodoPago;

    private WcAutocompletar<WcRepuesto> wcAutoRepuesto;
    private WcRepuesto wcRepuestoElegido;
    private WcOrdenServicio wcOrden;
    private Runnable wcAlCambiar = () -> { };

    @FXML
    public void initialize() {
        wcCmbMecanico.setConverter(WcConvertidores.wcMostrando(WcUsuario::getWcNombreCompleto, "Selecciona un mecanico"));
        wcCmbServicio.setConverter(WcConvertidores.wcMostrando(
                wcS -> wcS.getWcNombre() + "  ·  " + WcTablas.WC_MONEDA.format(wcS.getWcPrecio()), "Selecciona un servicio"));
        wcConfigurarTablaServicios();
        wcConfigurarTablaRepuestos();
        wcConfigurarBuscadorRepuestos();
        wcDiagnostico = new WcSelectorMultiple(wcCmbDiagnostico, wcBtnAgregarDiagnostico, wcLstDiagnostico,
                WcCatalogoFallas.WC_DIAGNOSTICOS, "Sin fallas agregadas");
        wcConfigurarCobro();
    }

    public void wcMostrar(Long wcOrdenId, Runnable wcAlCambiar) {
        this.wcAlCambiar = wcAlCambiar;
        wcPintar(wcOrdenService.wcObtener(wcOrdenId));
        wcCmbServicio.getItems().setAll(wcServicioService.wcActivos());
        wcCmbMecanico.getItems().setAll(wcUsuarioService.wcMecanicosActivos());
        wcLimpiarBuscadorRepuestos();
        wcNotificador.wcAbrirPanel(wcRaiz);
    }

    private void wcConfigurarTablaServicios() {
        wcColServicio.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcServicio().getWcNombre()));
        wcColPrecioServicio.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcPrecio()));
        wcColPrecioServicio.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColQuitarServicio.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColQuitarServicio.setCellFactory(WcCeldasCarrito.wcQuitar(wcLinea ->
                wcEjecutar(() -> wcOrdenService.wcQuitarServicio(wcOrden.getWcId(), wcLinea.getWcId()), null)));
        WcTablas.wcConfigurar(wcTablaServicios, Feather.TOOL, "Sin servicios",
                "Agrega la mano de obra realizada.", wcLinea -> { });
    }

    private void wcConfigurarTablaRepuestos() {
        wcColCodigoRepuesto.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcRepuesto().getWcCodigoRepuesto()));
        wcColCodigoRepuesto.setCellFactory(WcTablas.wcCeldaChip("—"));
        wcColRepuesto.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcRepuesto().getWcNombre()));
        wcColCantidad.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcCantidad())));
        wcColPrecioRepuesto.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcPrecioUnitario()));
        wcColPrecioRepuesto.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColImporteRepuesto.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcImporte()));
        wcColImporteRepuesto.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColQuitarRepuesto.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColQuitarRepuesto.setCellFactory(WcCeldasCarrito.wcQuitar(wcLinea ->
                wcEjecutar(() -> wcOrdenService.wcQuitarRepuesto(wcOrden.getWcId(), wcLinea.getWcId()),
                        wcLinea.getWcRepuesto().getWcNombre() + " se devolvio al inventario.")));
        WcTablas.wcConfigurar(wcTablaRepuestos, Feather.PACKAGE, "Sin repuestos",
                "Busca un repuesto arriba para registrarlo.", wcLinea -> { });
    }

    private void wcConfigurarBuscadorRepuestos() {
        wcSpnCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, WC_CANTIDAD_MAXIMA, 1));
        WcFormularios.wcSoloDigitos(wcSpnCantidad.getEditor(), WC_DIGITOS_CANTIDAD);
        wcAutoRepuesto = new WcAutocompletar<>(wcTxtRepuesto, wcRepuestoService::wcAutocompletar, this::wcElegirRepuesto)
                .wcPresentacion(WcRepuesto::getWcNombre,
                        wcR -> wcR.getWcCodigoRepuesto() + "  ·  " + (wcR.isWcSinStock() ? "Sin stock" : "Stock " + wcR.getWcStock()),
                        wcR -> WcTablas.WC_MONEDA.format(wcR.getWcPrecio()))
                .wcIcono(wcR -> new FontIcon(Feather.PACKAGE))
                .wcMinimoCaracteres(1);
        wcTxtRepuesto.textProperty().addListener((wcObs, wcAnterior, wcActual) -> {
            if (wcRepuestoElegido != null && !wcActual.equals(wcRepuestoElegido.getWcNombre())) {
                wcRepuestoElegido = null;
            }
        });
    }

    private void wcConfigurarCobro() {
        wcCmbMetodoPago.getItems().setAll(WcMetodoPago.values());
    }

    private void wcElegirRepuesto(WcRepuesto wcRepuesto) {
        wcRepuestoElegido = wcRepuesto;
        wcAutoRepuesto.setWcTexto(wcRepuesto.getWcNombre());
        wcSpnCantidad.requestFocus();
    }

    @FXML
    private void wcOnAsignarMecanico() {
        WcUsuario wcMecanico = wcCmbMecanico.getValue();
        if (wcMecanico == null) {
            wcNotificador.wcAdvertencia("Selecciona un mecanico.");
            return;
        }
        wcEjecutar(() -> wcOrdenService.wcAsignarMecanico(wcOrden.getWcId(), wcMecanico.getWcId()),
                "Orden asignada a " + wcMecanico.getWcNombreCompleto() + ".");
    }

    @FXML
    private void wcOnGuardarDiagnostico() {
        wcEjecutar(() -> wcOrdenService.wcGuardarDiagnostico(wcOrden.getWcId(), wcDiagnostico.wcTexto()),
                "Diagnostico guardado.");
    }

    @FXML
    private void wcOnAgregarServicio() {
        WcServicio wcServicio = wcCmbServicio.getValue();
        if (wcServicio == null) {
            wcNotificador.wcAdvertencia("Selecciona un servicio del catalogo.");
            return;
        }
        wcEjecutar(() -> wcOrdenService.wcAgregarServicio(wcOrden.getWcId(), wcServicio.getWcId()), null);
        wcCmbServicio.setValue(null);
    }

    @FXML
    private void wcOnAgregarRepuesto() {
        if (wcRepuestoElegido == null) {
            WcFormularios.wcMarcarError(wcTxtRepuesto, "Selecciona un repuesto de la lista");
            return;
        }
        WcRepuesto wcRepuesto = wcRepuestoElegido;
        int wcCantidad = wcSpnCantidad.getValue() == null ? 1 : wcSpnCantidad.getValue();
        boolean wcAgregado = wcEjecutar(() -> wcOrdenService.wcAgregarRepuesto(wcOrden.getWcId(), wcRepuesto.getWcId(), wcCantidad),
                wcCantidad + " x " + wcRepuesto.getWcNombre() + " registrado y descontado del inventario.");
        if (wcAgregado) {
            wcLimpiarBuscadorRepuestos();
        }
    }

    @FXML
    private void wcOnImprimir() {
        Long wcOrdenId = wcOrden.getWcId();
        wcExportador.wcAbrirTemporal(wcOrden.getWcCodigo(), WcExportador.WcFormato.WC_PDF,
                wcDestino -> wcPdfService.wcGenerar(wcOrdenId, wcDestino));
    }

    @FXML
    private void wcOnCerrar() {
        wcNotificador.wcCerrarPanel();
    }

    private void wcAbrirCobro() {
        wcLblCobroTotal.setText("Total a cobrar por la orden " + wcOrden.getWcCodigo() + ": "
                + WcTablas.WC_MONEDA.format(wcOrden.getWcTotal()));
        wcCmbMetodoPago.setValue(WcMetodoPago.WC_EFECTIVO);
        wcNotificador.wcMostrarDialogo(wcDialogoCobro);
    }

    @FXML
    private void wcOnConfirmarCobro() {
        WcMetodoPago wcMetodo = wcCmbMetodoPago.getValue();
        boolean wcEntregada = wcEjecutar(() -> wcOrdenService.wcEntregar(wcOrden.getWcId(), wcMetodo),
                "Orden " + wcOrden.getWcCodigo() + " cobrada con " + wcMetodo + " y entregada.");
        if (wcEntregada) {
            wcNotificador.wcCerrarDialogo();
        }
    }

    @FXML
    private void wcOnCancelarCobro() {
        wcNotificador.wcCerrarDialogo();
    }

    private void wcCambiarEstado(WcEstadoOrden wcDestino) {
        if (wcDestino == WcEstadoOrden.WC_CANCELADO) {
            wcNotificador.wcConfirmar("¿Cancelar la orden " + wcOrden.getWcCodigo() + "?",
                    "Los repuestos registrados se devolveran al inventario. Esta accion no se puede deshacer.",
                    "Cancelar orden",
                    () -> wcEjecutar(() -> wcOrdenService.wcCambiarEstado(wcOrden.getWcId(), wcDestino), "Orden cancelada."));
            return;
        }
        wcEjecutar(() -> wcOrdenService.wcCambiarEstado(wcOrden.getWcId(), wcDestino),
                "La orden " + wcOrden.getWcCodigo() + " ahora esta: " + wcDestino.getWcDescripcion() + ".");
    }

    private boolean wcEjecutar(Supplier<WcOrdenServicio> wcOperacion, String wcMensajeExito) {
        try {
            wcPintar(wcOperacion.get());
            wcAlCambiar.run();
            if (wcMensajeExito != null) {
                wcNotificador.wcExito(wcMensajeExito);
            }
            return true;
        } catch (WcNegocioException wcE) {
            wcNotificador.wcAdvertencia(wcE.getMessage());
            return false;
        }
    }

    private void wcPintar(WcOrdenServicio wcActualizada) {
        wcOrden = wcActualizada;
        wcLblCodigo.setText("Orden " + wcOrden.getWcCodigo());
        WcTablas.wcAplicarEstilo(wcLblEstado, wcOrden.getWcEstado());
        wcLblSubtitulo.setText("Ingreso el " + wcOrden.getWcFechaIngreso().format(WC_FECHA_HORA)
                + (wcOrden.getWcFechaEntrega() == null ? "" : " · Entregado el " + wcOrden.getWcFechaEntrega().format(WC_FECHA_HORA)));
        wcLblCliente.setText(wcOrden.getWcCliente().getWcNombreCompleto() + " · DNI " + wcOrden.getWcCliente().getWcDni());
        wcLblVehiculo.setText(wcOrden.getWcVehiculo().getWcPlaca() + " · " + wcOrden.getWcVehiculo().getWcDescripcion());
        wcLblKilometraje.setText(String.format(WcTablas.WC_LOCALE, "%,d km", wcOrden.getWcKilometrajeIngreso()));
        wcLblProblema.setText(wcOrden.getWcProblemaReportado());
        wcLblMecanico.setText(wcOrden.getWcNombreMecanico());
        wcLblPago.setText(wcOrden.getWcMetodoPago() == null ? "Pendiente" : wcOrden.getWcMetodoPago().getWcDescripcion());
        wcDiagnostico.wcCargar(wcOrden.getWcDiagnostico());

        wcTablaServicios.getItems().setAll(wcOrden.getWcServicios());
        wcTablaRepuestos.getItems().setAll(wcOrden.getWcRepuestos());
        wcLblTotalServicios.setText(WcTablas.WC_MONEDA.format(wcOrden.getWcTotalServicios()));
        wcLblTotalRepuestos.setText(WcTablas.WC_MONEDA.format(wcOrden.getWcTotalRepuestos()));
        wcLblBase.setText(WcTablas.WC_MONEDA.format(wcOrden.getWcBaseImponible()));
        wcLblIgv.setText(WcTablas.WC_MONEDA.format(wcOrden.getWcIgv()));
        wcLblTotal.setText(WcTablas.WC_MONEDA.format(wcOrden.getWcTotal()));

        wcAplicarPermisos();
        wcConstruirAccionesDeEstado();
    }

    private void wcAplicarPermisos() {
        boolean wcEditable = wcOrden.getWcEstado().wcEsEditable() && wcSesion.wcTiene(WcPermiso.WC_TRABAJAR_ORDENES);
        boolean wcPuedeAsignar = wcOrden.getWcEstado().wcEstaEnTaller() && wcSesion.wcTiene(WcPermiso.WC_CREAR_ORDENES);
        wcMostrarSi(wcBarraAsignacion, wcPuedeAsignar);
        wcMostrarSi(wcBarraServicios, wcEditable);
        wcMostrarSi(wcBarraRepuestos, wcEditable);
        wcMostrarSi(wcBtnGuardarDiagnostico, wcEditable);
        wcDiagnostico.wcHabilitar(wcEditable);
        wcColQuitarServicio.setVisible(wcEditable);
        wcColQuitarRepuesto.setVisible(wcEditable);
    }

    private void wcConstruirAccionesDeEstado() {
        wcAccionesEstado.getChildren().clear();
        wcOrden.getWcEstado().wcSiguientes().stream()
                .filter(wcDestino -> wcSesion.wcTiene(wcDestino.getWcPermisoRequerido()))
                .map(this::wcBotonDeEstado)
                .forEach(wcAccionesEstado.getChildren()::add);
    }

    private Button wcBotonDeEstado(WcEstadoOrden wcDestino) {
        Button wcBoton = new Button(wcDestino.getWcAccion());
        wcBoton.getStyleClass().add(wcDestino == WcEstadoOrden.WC_CANCELADO ? Styles.DANGER : Styles.ACCENT);
        if (wcDestino == WcEstadoOrden.WC_CANCELADO) {
            wcBoton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        }
        wcBoton.setOnAction(wcE -> {
            if (wcDestino == WcEstadoOrden.WC_ENTREGADO) {
                wcAbrirCobro();
            } else {
                wcCambiarEstado(wcDestino);
            }
        });
        return wcBoton;
    }

    private void wcLimpiarBuscadorRepuestos() {
        wcRepuestoElegido = null;
        wcAutoRepuesto.setWcTexto("");
        wcSpnCantidad.getValueFactory().setValue(1);
        WcFormularios.wcLimpiarError(wcTxtRepuesto);
    }

    private static void wcMostrarSi(Node wcNodo, boolean wcVisible) {
        wcNodo.setVisible(wcVisible);
        wcNodo.setManaged(wcVisible);
    }
}
