package com.taller.mecanico.controller;

import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCatalogoFallas;
import com.taller.mecanico.model.WcEstadoOrden;
import com.taller.mecanico.model.WcOrdenServicio;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.model.WcUsuario;
import com.taller.mecanico.model.WcVehiculo;
import com.taller.mecanico.seguridad.WcSesion;
import com.taller.mecanico.service.WcOrdenPdfService;
import com.taller.mecanico.service.WcOrdenServicioService;
import com.taller.mecanico.service.WcOrdenServicioService.WcNuevaOrden;
import com.taller.mecanico.service.WcUsuarioService;
import com.taller.mecanico.service.WcVehiculoService;
import com.taller.mecanico.ui.WcAutocompletar;
import com.taller.mecanico.ui.WcCargadorVistas;
import com.taller.mecanico.ui.WcConvertidores;
import com.taller.mecanico.ui.WcExportador;
import com.taller.mecanico.ui.WcFormularios;
import com.taller.mecanico.ui.WcNotificador;
import com.taller.mecanico.ui.WcPaginador;
import com.taller.mecanico.ui.WcRefrescable;
import com.taller.mecanico.ui.WcSelectorMultiple;
import com.taller.mecanico.ui.WcTablas;
import javafx.application.Platform;
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
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WcOrdenesController implements WcRefrescable {
    private static final DateTimeFormatter WC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int WC_DIGITOS_KILOMETRAJE = 7;

    private final WcOrdenServicioService wcOrdenService;
    private final WcVehiculoService wcVehiculoService;
    private final WcUsuarioService wcUsuarioService;
    private final WcOrdenPdfService wcPdfService;
    private final WcCargadorVistas wcCargador;
    private final WcExportador wcExportador;
    private final WcNotificador wcNotificador;
    private final WcSesion wcSesion;

    @FXML private Label wcLblTitulo;
    @FXML private Label wcLblSubtitulo;
    @FXML private Button wcBtnNuevaOrden;
    @FXML private TextField wcTxtBuscar;
    @FXML private ComboBox<WcEstadoOrden> wcCmbEstado;
    @FXML private Button wcBtnImprimir;
    @FXML private Button wcBtnAbrir;

    @FXML private TableView<WcOrdenServicio> wcTabla;
    @FXML private TableColumn<WcOrdenServicio, String> wcColCodigo;
    @FXML private TableColumn<WcOrdenServicio, String> wcColIngreso;
    @FXML private TableColumn<WcOrdenServicio, String> wcColVehiculo;
    @FXML private TableColumn<WcOrdenServicio, String> wcColCliente;
    @FXML private TableColumn<WcOrdenServicio, String> wcColMecanico;
    @FXML private TableColumn<WcOrdenServicio, WcEstadoOrden> wcColEstado;
    @FXML private TableColumn<WcOrdenServicio, BigDecimal> wcColTotal;
    @FXML private WcPaginador wcPaginador;

    @FXML private VBox wcPanelNuevaOrden;
    @FXML private TextField wcTxtVehiculo;
    @FXML private Label wcLblPropietario;
    @FXML private TextField wcTxtKilometraje;
    @FXML private ComboBox<String> wcCmbProblema;
    @FXML private Button wcBtnAgregarProblema;
    @FXML private ListView<String> wcLstProblemas;
    private WcSelectorMultiple wcProblemas;
    @FXML private ComboBox<WcUsuario> wcCmbMecanico;

    private final ObservableList<WcOrdenServicio> wcDatos = FXCollections.observableArrayList();
    private WcAutocompletar<WcVehiculo> wcAutoVehiculo;
    private WcVehiculo wcVehiculoElegido;
    private WcOrdenDetalleController wcDetalle;

    @FXML
    public void initialize() {
        wcDetalle = null;
        wcConfigurarEncabezado();
        wcConfigurarTabla();
        wcConfigurarFiltros();
        wcConfigurarNuevaOrden();
    }

    @Override
    public void wcRefrescar() {
        wcCargarTabla();
    }

    private void wcConfigurarEncabezado() {
        boolean wcPuedeCrear = wcSesion.wcTiene(WcPermiso.WC_CREAR_ORDENES);
        wcBtnNuevaOrden.setVisible(wcPuedeCrear);
        wcBtnNuevaOrden.setManaged(wcPuedeCrear);
        if (!wcSesion.wcTiene(WcPermiso.WC_VER_TODAS_LAS_ORDENES)) {
            wcLblTitulo.setText("Mis ordenes de servicio");
            wcLblSubtitulo.setText("Vehiculos que tienes asignados para diagnosticar y reparar");
        }
    }

    private void wcConfigurarTabla() {
        wcColCodigo.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCodigo()));
        wcColCodigo.setCellFactory(WcTablas.wcCeldaChip("—"));
        wcColIngreso.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcFechaIngreso().format(WC_FECHA_HORA)));
        wcColVehiculo.setCellValueFactory(wcC -> new SimpleStringProperty(
                wcC.getValue().getWcVehiculo().getWcPlaca() + " · " + wcC.getValue().getWcVehiculo().getWcDescripcion()));
        wcColCliente.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCliente().getWcNombreCompleto()));
        wcColCliente.setCellFactory(WcTablas.wcCeldaAvatar());
        wcColMecanico.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcNombreMecanico()));
        wcColEstado.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcEstado()));
        wcColEstado.setCellFactory(WcTablas.wcCeldaEstadoOrden());
        wcColTotal.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcTotal()));
        wcColTotal.setCellFactory(WcTablas.wcCeldaMoneda());

        WcTablas.wcConfigurar(wcTabla, Feather.CLIPBOARD, "Sin ordenes",
                "No hay ordenes que coincidan con el filtro.", this::wcAbrirDetalle);
        wcTabla.setItems(wcDatos);

        var wcSeleccion = wcTabla.getSelectionModel().selectedItemProperty();
        wcBtnAbrir.disableProperty().bind(wcSeleccion.isNull());
        wcBtnImprimir.disableProperty().bind(wcSeleccion.isNull());
        wcPaginador.setWcOnCambio(this::wcCargarTabla);
    }

    private void wcConfigurarFiltros() {
        List<WcEstadoOrden> wcOpciones = new ArrayList<>();
        wcOpciones.add(null);
        wcOpciones.addAll(Arrays.asList(WcEstadoOrden.values()));
        wcCmbEstado.getItems().setAll(wcOpciones);
        wcCmbEstado.setConverter(WcConvertidores.wcMostrando(WcEstadoOrden::getWcDescripcion, "Todos los estados"));
        wcCmbEstado.valueProperty().addListener((wcObs, wcAnterior, wcActual) -> wcPaginador.wcIrAPrimera());
        wcTxtBuscar.textProperty().addListener((wcObs, wcAnterior, wcActual) -> wcPaginador.wcIrAPrimera());
    }

    private void wcConfigurarNuevaOrden() {
        WcFormularios.wcSoloDigitos(wcTxtKilometraje, WC_DIGITOS_KILOMETRAJE);
        WcFormularios.wcLimpiarAlEditar(wcTxtVehiculo, wcTxtKilometraje);
        wcConfigurarProblemas();
        wcCmbMecanico.setConverter(WcConvertidores.wcMostrando(WcUsuario::getWcNombreCompleto, "Sin asignar"));

        wcAutoVehiculo = new WcAutocompletar<>(wcTxtVehiculo, wcVehiculoService::wcAutocompletar, this::wcSeleccionarVehiculo)
                .wcPresentacion(WcVehiculo::getWcPlaca,
                        wcV -> wcV.getWcDescripcion() + " · " + wcV.getWcCliente().getWcNombreCompleto(), wcV -> null)
                .wcIcono(wcV -> new FontIcon(Feather.TRUCK))
                .wcMinimoCaracteres(2);
        wcTxtVehiculo.textProperty().addListener((wcObs, wcAnterior, wcActual) -> {
            if (wcVehiculoElegido != null && !wcActual.equals(wcVehiculoElegido.getWcPlaca())) {
                wcElegirVehiculo(null);
            }
        });
    }

    private void wcSeleccionarVehiculo(WcVehiculo wcVehiculo) {
        wcElegirVehiculo(wcVehiculo);
        wcAutoVehiculo.setWcTexto(wcVehiculo.getWcPlaca());
        wcTxtKilometraje.setText(String.valueOf(wcVehiculo.getWcKilometraje()));
        WcFormularios.wcLimpiarError(wcTxtVehiculo);
        wcTxtKilometraje.requestFocus();
    }

    private void wcElegirVehiculo(WcVehiculo wcVehiculo) {
        wcVehiculoElegido = wcVehiculo;
        boolean wcHayVehiculo = wcVehiculo != null;
        wcLblPropietario.setVisible(wcHayVehiculo);
        wcLblPropietario.setManaged(wcHayVehiculo);
        wcLblPropietario.setText(wcHayVehiculo
                ? wcVehiculo.getWcDescripcion() + " · Propietario: " + wcVehiculo.getWcCliente().getWcNombreCompleto()
                : "");
    }

    @FXML
    private void wcOnNuevaOrden() {
        wcLimpiarNuevaOrden();
        wcCmbMecanico.getItems().setAll(wcUsuarioService.wcMecanicosActivos());
        wcNotificador.wcAbrirPanel(wcPanelNuevaOrden);
        Platform.runLater(wcTxtVehiculo::requestFocus);
    }

    @FXML
    private void wcOnCerrarFormulario() {
        wcNotificador.wcCerrarPanel();
    }

    @FXML
    private void wcOnRegistrar() {
        WcFormularios.wcLimpiarErrores(wcTxtVehiculo, wcTxtKilometraje, wcLstProblemas);
        if (!wcFormularioCompleto()) {
            return;
        }
        WcUsuario wcMecanico = wcCmbMecanico.getValue();
        try {
            WcOrdenServicio wcOrden = wcOrdenService.wcRegistrar(new WcNuevaOrden(
                    wcVehiculoElegido.getWcId(),
                    wcMecanico == null ? null : wcMecanico.getWcId(),
                    Integer.valueOf(wcTxtKilometraje.getText()),
                    wcProblemas.wcTexto()));
            wcNotificador.wcCerrarPanel();
            wcCargarTabla();
            wcNotificador.wcExitoConAcciones("Orden registrada",
                    "Se registro la orden " + wcOrden.getWcCodigo() + " para el vehiculo "
                            + wcVehiculoElegido.getWcPlaca() + ".",
                    "Cerrar", null,
                    "Abrir orden", () -> wcAbrirDetalle(wcOrden));
        } catch (WcNegocioException wcE) {
            wcNotificador.wcAdvertencia(wcE.getMessage());
        }
    }

    private boolean wcFormularioCompleto() {
        boolean wcCompleto = true;
        if (wcVehiculoElegido == null) {
            WcFormularios.wcMarcarError(wcTxtVehiculo, "Selecciona el vehiculo de la lista");
            wcCompleto = false;
        }
        if (wcTxtKilometraje.getText().isBlank()) {
            WcFormularios.wcMarcarError(wcTxtKilometraje, "Ingresa el kilometraje actual");
            wcCompleto = false;
        }
        if (wcProblemas.wcEstaVacio()) {
            WcFormularios.wcMarcarError(wcLstProblemas, "Agrega al menos un problema");
            wcCompleto = false;
        }
        return wcCompleto;
    }

    @FXML
    private void wcOnAbrir() {
        wcAbrirDetalle(wcTabla.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void wcOnImprimir() {
        WcOrdenServicio wcOrden = wcTabla.getSelectionModel().getSelectedItem();
        if (wcOrden != null) {
            wcExportador.wcAbrirTemporal(wcOrden.getWcCodigo(), WcExportador.WcFormato.WC_PDF,
                    wcDestino -> wcPdfService.wcGenerar(wcOrden.getWcId(), wcDestino));
        }
    }

    private void wcAbrirDetalle(WcOrdenServicio wcOrden) {
        if (wcOrden == null) {
            return;
        }
        try {
            wcDetalle().wcMostrar(wcOrden.getWcId(), this::wcCargarTabla);
        } catch (WcNegocioException wcE) {
            wcNotificador.wcAdvertencia(wcE.getMessage());
        }
    }

    private WcOrdenDetalleController wcDetalle() {
        if (wcDetalle == null) {
            wcDetalle = (WcOrdenDetalleController) wcCargador.wcCargar("orden-detalle-view").wcControlador();
        }
        return wcDetalle;
    }

    private void wcCargarTabla() {
        Page<WcOrdenServicio> wcPagina = wcOrdenService.wcListar(wcTxtBuscar.getText(), wcCmbEstado.getValue(),
                wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }

    private void wcLimpiarNuevaOrden() {
        WcFormularios.wcLimpiarErrores(wcTxtVehiculo, wcTxtKilometraje, wcLstProblemas);
        wcAutoVehiculo.setWcTexto("");
        wcElegirVehiculo(null);
        wcTxtKilometraje.clear();
        wcProblemas.wcLimpiar();
        wcCmbMecanico.setValue(null);
    }

    private void wcConfigurarProblemas() {
        wcProblemas = new WcSelectorMultiple(wcCmbProblema, wcBtnAgregarProblema, wcLstProblemas,
                WcCatalogoFallas.WC_PROBLEMAS, "Sin problemas agregados");
        wcProblemas.setWcAlCambiar(() -> WcFormularios.wcLimpiarErrores(wcLstProblemas));
    }
}
