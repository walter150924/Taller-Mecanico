package com.taller.mecanico.controller;

import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCliente;
import com.taller.mecanico.model.WcEstadoOrden;
import com.taller.mecanico.model.WcOrdenServicio;
import com.taller.mecanico.model.WcVehiculo;
import com.taller.mecanico.service.WcClienteService;
import com.taller.mecanico.service.WcVehiculoService;
import com.taller.mecanico.ui.WcAutocompletar;
import com.taller.mecanico.ui.WcFormularios;
import com.taller.mecanico.ui.WcNotificador;
import com.taller.mecanico.ui.WcPaginador;
import com.taller.mecanico.ui.WcRefrescable;
import com.taller.mecanico.ui.WcTablas;
import jakarta.validation.ConstraintViolationException;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WcVehiculoController implements WcRefrescable {
    private static final DateTimeFormatter WC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String WC_FORMATO_PLACA_PARCIAL = "[A-Z0-9]{0,3}(-[0-9]{0,3})?";
    private static final int WC_DIGITOS_ANIO = 4;
    private static final int WC_DIGITOS_KILOMETRAJE = 7;

    private final WcVehiculoService wcService;
    private final WcClienteService wcClienteService;
    private final WcNotificador wcNotificador;

    @FXML private VBox wcPanelFormulario;
    @FXML private Label wcLblTituloFormulario;
    @FXML private Label wcLblSubtituloFormulario;
    @FXML private TextField wcTxtCliente;
    @FXML private TextField wcTxtPlaca;
    @FXML private TextField wcTxtAnio;
    @FXML private TextField wcTxtMarca;
    @FXML private TextField wcTxtModelo;
    @FXML private TextField wcTxtColor;
    @FXML private TextField wcTxtKilometraje;
    @FXML private TextField wcTxtBuscar;

    @FXML private TableView<WcVehiculo> wcTabla;
    @FXML private TableColumn<WcVehiculo, String> wcColPlaca;
    @FXML private TableColumn<WcVehiculo, String> wcColVehiculo;
    @FXML private TableColumn<WcVehiculo, String> wcColPropietario;
    @FXML private TableColumn<WcVehiculo, String> wcColColor;
    @FXML private TableColumn<WcVehiculo, String> wcColKilometraje;

    @FXML private VBox wcPanelHistorial;
    @FXML private Label wcLblTituloHistorial;
    @FXML private Label wcLblSubtituloHistorial;
    @FXML private TableView<WcOrdenServicio> wcTablaHistorial;
    @FXML private TableColumn<WcOrdenServicio, String> wcColHistCodigo;
    @FXML private TableColumn<WcOrdenServicio, String> wcColHistFecha;
    @FXML private TableColumn<WcOrdenServicio, String> wcColHistKilometraje;
    @FXML private TableColumn<WcOrdenServicio, String> wcColHistDiagnostico;
    @FXML private TableColumn<WcOrdenServicio, WcEstadoOrden> wcColHistEstado;
    @FXML private TableColumn<WcOrdenServicio, BigDecimal> wcColHistTotal;

    @FXML private Button wcBtnHistorial;
    @FXML private Button wcBtnEditar;
    @FXML private Button wcBtnEliminar;
    @FXML private WcPaginador wcPaginador;

    private final ObservableList<WcVehiculo> wcDatos = FXCollections.observableArrayList();
    private WcAutocompletar<WcCliente> wcAutoCliente;
    private WcCliente wcPropietario;
    private Long wcIdEditando;

    @FXML
    public void initialize() {
        wcConfigurarTabla();
        wcConfigurarHistorial();
        wcConfigurarFormulario();
        wcPaginador.setWcOnCambio(this::wcCargarTabla);
        wcTxtBuscar.textProperty().addListener((wcObs, wcAnterior, wcActual) -> wcPaginador.wcIrAPrimera());
    }

    @Override
    public void wcRefrescar() {
        wcCargarTabla();
    }

    private void wcConfigurarTabla() {
        wcColPlaca.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcPlaca()));
        wcColPlaca.setCellFactory(WcTablas.wcCeldaChip("—"));
        wcColVehiculo.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcDescripcion()));
        wcColPropietario.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCliente().getWcNombreCompleto()));
        wcColPropietario.setCellFactory(WcTablas.wcCeldaAvatar());
        wcColColor.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcColor()));
        wcColColor.setCellFactory(WcTablas.wcCeldaOpcional());
        wcColKilometraje.setCellValueFactory(wcC -> new SimpleStringProperty(wcKilometros(wcC.getValue().getWcKilometraje())));

        WcTablas.wcConfigurar(wcTabla, "Registra un vehiculo o cambia el texto de busqueda.", this::wcEditar);
        wcTabla.setItems(wcDatos);

        var wcSeleccion = wcTabla.getSelectionModel().selectedItemProperty();
        wcBtnHistorial.disableProperty().bind(wcSeleccion.isNull());
        wcBtnEditar.disableProperty().bind(wcSeleccion.isNull());
        wcBtnEliminar.disableProperty().bind(wcSeleccion.isNull());
    }

    private void wcConfigurarHistorial() {
        wcColHistCodigo.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCodigo()));
        wcColHistFecha.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcFechaIngreso().format(WC_FECHA)));
        wcColHistKilometraje.setCellValueFactory(wcC -> new SimpleStringProperty(wcKilometros(wcC.getValue().getWcKilometrajeIngreso())));
        wcColHistDiagnostico.setCellValueFactory(wcC -> new SimpleStringProperty(wcResumenTrabajo(wcC.getValue())));
        wcColHistEstado.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcEstado()));
        wcColHistEstado.setCellFactory(WcTablas.wcCeldaEstadoOrden());
        wcColHistTotal.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcTotal()));
        wcColHistTotal.setCellFactory(WcTablas.wcCeldaMoneda());
        WcTablas.wcConfigurar(wcTablaHistorial, Feather.CLOCK, "Sin historial",
                "Este vehiculo aun no tiene ordenes de servicio.", wcOrden -> { });
    }

    private void wcConfigurarFormulario() {
        wcTxtPlaca.setTextFormatter(new TextFormatter<String>(wcCambio -> {
            wcCambio.setText(wcCambio.getText().toUpperCase());
            return wcCambio.getControlNewText().matches(WC_FORMATO_PLACA_PARCIAL) ? wcCambio : null;
        }));
        WcFormularios.wcSoloDigitos(wcTxtAnio, WC_DIGITOS_ANIO);
        WcFormularios.wcSoloDigitos(wcTxtKilometraje, WC_DIGITOS_KILOMETRAJE);
        WcFormularios.wcLimpiarAlEditar(wcTxtCliente, wcTxtPlaca, wcTxtAnio, wcTxtMarca, wcTxtModelo, wcTxtColor, wcTxtKilometraje);

        wcAutoCliente = new WcAutocompletar<>(wcTxtCliente, wcClienteService::wcAutocompletar, this::wcSeleccionarPropietario)
                .wcPresentacion(WcCliente::getWcNombreCompleto, wcC -> "DNI " + wcC.getWcDni(), wcC -> null)
                .wcIcono(this::wcAvatar)
                .wcMinimoCaracteres(2);
        wcTxtCliente.textProperty().addListener((wcObs, wcAnterior, wcActual) -> {
            if (wcPropietario != null && !wcActual.equals(wcPropietario.getWcNombreCompleto())) {
                wcPropietario = null;
            }
        });
    }

    private void wcSeleccionarPropietario(WcCliente wcCliente) {
        wcPropietario = wcCliente;
        wcAutoCliente.setWcTexto(wcCliente.getWcNombreCompleto());
        WcFormularios.wcLimpiarError(wcTxtCliente);
        wcTxtPlaca.requestFocus();
    }

    @FXML
    private void wcOnNuevo() {
        wcTabla.getSelectionModel().clearSelection();
        wcLimpiarFormulario();
        wcIdEditando = null;
        wcLblTituloFormulario.setText("Nuevo vehiculo");
        wcLblSubtituloFormulario.setText("Registra el vehiculo y su propietario");
        wcAbrirFormulario(wcTxtCliente);
    }

    @FXML
    private void wcOnEditar() {
        wcEditar(wcTabla.getSelectionModel().getSelectedItem());
    }

    private void wcEditar(WcVehiculo wcVehiculo) {
        if (wcVehiculo == null) {
            return;
        }
        wcLimpiarFormulario();
        wcIdEditando = wcVehiculo.getWcId();
        wcSeleccionarPropietario(wcVehiculo.getWcCliente());
        wcTxtPlaca.setText(wcVehiculo.getWcPlaca());
        wcTxtAnio.setText(String.valueOf(wcVehiculo.getWcAnio()));
        wcTxtMarca.setText(wcVehiculo.getWcMarca());
        wcTxtModelo.setText(wcVehiculo.getWcModelo());
        wcTxtColor.setText(wcVehiculo.getWcColor() == null ? "" : wcVehiculo.getWcColor());
        wcTxtKilometraje.setText(String.valueOf(wcVehiculo.getWcKilometraje()));
        wcLblTituloFormulario.setText("Editar vehiculo");
        wcLblSubtituloFormulario.setText(wcVehiculo.getWcPlaca() + " · " + wcVehiculo.getWcDescripcion());
        wcAbrirFormulario(wcTxtPlaca);
    }

    @FXML
    private void wcOnHistorial() {
        WcVehiculo wcVehiculo = wcTabla.getSelectionModel().getSelectedItem();
        if (wcVehiculo == null) {
            return;
        }
        List<WcOrdenServicio> wcOrdenes = wcService.wcHistorial(wcVehiculo.getWcId());
        wcLblTituloHistorial.setText("Historial de " + wcVehiculo.getWcPlaca());
        wcLblSubtituloHistorial.setText(wcVehiculo.getWcDescripcion() + " · " + wcOrdenes.size() + " ordenes registradas");
        wcTablaHistorial.getItems().setAll(wcOrdenes);
        wcNotificador.wcAbrirPanel(wcPanelHistorial);
    }

    @FXML
    private void wcOnCerrarFormulario() {
        wcNotificador.wcCerrarPanel();
    }

    @FXML
    private void wcOnGuardar() {
        WcFormularios.wcLimpiarErrores(wcTxtCliente, wcTxtPlaca, wcTxtAnio, wcTxtMarca, wcTxtModelo, wcTxtColor, wcTxtKilometraje);
        if (wcPropietario == null) {
            WcFormularios.wcMarcarError(wcTxtCliente, "Selecciona el propietario de la lista");
            return;
        }
        String wcColor = wcTxtColor.getText().trim();
        WcVehiculo wcVehiculo = WcVehiculo.builder()
                .wcId(wcIdEditando)
                .wcCliente(wcPropietario)
                .wcPlaca(wcTxtPlaca.getText().trim())
                .wcAnio(wcEnteroONulo(wcTxtAnio))
                .wcMarca(wcTxtMarca.getText().trim())
                .wcModelo(wcTxtModelo.getText().trim())
                .wcColor(wcColor.isEmpty() ? null : wcColor)
                .wcKilometraje(wcEnteroONulo(wcTxtKilometraje))
                .build();
        try {
            WcVehiculo wcGuardado = wcService.wcGuardar(wcVehiculo);
            wcNotificador.wcCerrarPanel();
            wcNotificador.wcExito("El vehiculo " + wcGuardado.getWcPlaca() + " se guardo correctamente.");
            wcCargarTabla();
        } catch (ConstraintViolationException wcE) {
            List<String> wcOtros = WcFormularios.wcMostrarViolaciones(wcE, Map.of(
                    "wcCliente", wcTxtCliente, "wcPlaca", wcTxtPlaca, "wcAnio", wcTxtAnio, "wcMarca", wcTxtMarca,
                    "wcModelo", wcTxtModelo, "wcColor", wcTxtColor, "wcKilometraje", wcTxtKilometraje));
            wcOtros.forEach(wcNotificador::wcAdvertencia);
        } catch (WcNegocioException wcE) {
            WcFormularios.wcMarcarError(wcTxtPlaca, wcE.getMessage());
        }
    }

    @FXML
    private void wcOnEliminar() {
        WcVehiculo wcSeleccionado = wcTabla.getSelectionModel().getSelectedItem();
        if (wcSeleccionado == null) {
            return;
        }
        wcNotificador.wcConfirmar("¿Eliminar el vehiculo " + wcSeleccionado.getWcPlaca() + "?",
                "Esta accion no se puede deshacer.",
                "Eliminar",
                () -> {
                    try {
                        wcService.wcEliminar(wcSeleccionado.getWcId());
                        wcNotificador.wcExito("Vehiculo eliminado.");
                        wcCargarTabla();
                    } catch (WcNegocioException wcE) {
                        wcNotificador.wcError(wcE.getMessage());
                    }
                });
    }

    private void wcCargarTabla() {
        Page<WcVehiculo> wcPagina = wcService.wcListar(wcTxtBuscar.getText(), wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }

    private void wcAbrirFormulario(TextField wcCampoInicial) {
        wcNotificador.wcAbrirPanel(wcPanelFormulario);
        Platform.runLater(wcCampoInicial::requestFocus);
    }

    private void wcLimpiarFormulario() {
        WcFormularios.wcLimpiarErrores(wcTxtCliente, wcTxtPlaca, wcTxtAnio, wcTxtMarca, wcTxtModelo, wcTxtColor, wcTxtKilometraje);
        wcPropietario = null;
        wcAutoCliente.setWcTexto("");
        wcTxtPlaca.clear();
        wcTxtAnio.clear();
        wcTxtMarca.clear();
        wcTxtModelo.clear();
        wcTxtColor.clear();
        wcTxtKilometraje.clear();
    }

    private StackPane wcAvatar(WcCliente wcCliente) {
        StackPane wcAvatar = new StackPane(new Label(WcTablas.wcIniciales(wcCliente.getWcNombreCompleto())));
        wcAvatar.getStyleClass().add("avatar");
        return wcAvatar;
    }

    private static Integer wcEnteroONulo(TextField wcCampo) {
        String wcTexto = wcCampo.getText().trim();
        return wcTexto.isEmpty() ? null : Integer.valueOf(wcTexto);
    }

    private static String wcKilometros(int wcKilometraje) {
        return String.format(WcTablas.WC_LOCALE, "%,d km", wcKilometraje);
    }

    private static String wcResumenTrabajo(WcOrdenServicio wcOrden) {
        return wcOrden.getWcDiagnostico() == null ? wcOrden.getWcProblemaReportado() : wcOrden.getWcDiagnostico();
    }
}
