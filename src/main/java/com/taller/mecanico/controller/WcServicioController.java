package com.taller.mecanico.controller;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcServicio;
import com.taller.mecanico.service.WcServicioService;
import com.taller.mecanico.ui.WcFormularios;
import com.taller.mecanico.ui.WcNotificador;
import com.taller.mecanico.ui.WcPaginador;
import com.taller.mecanico.ui.WcRefrescable;
import com.taller.mecanico.ui.WcTablas;
import jakarta.validation.ConstraintViolationException;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WcServicioController implements WcRefrescable {
    private final WcServicioService wcService;
    private final WcNotificador wcNotificador;

    @FXML private VBox wcPanelFormulario;
    @FXML private Label wcLblTituloFormulario;
    @FXML private Label wcLblSubtituloFormulario;
    @FXML private TextField wcTxtNombre;
    @FXML private TextField wcTxtDescripcion;
    @FXML private TextField wcTxtPrecio;
    @FXML private ToggleSwitch wcSwActivo;
    @FXML private TextField wcTxtBuscar;

    @FXML private TableView<WcServicio> wcTabla;
    @FXML private TableColumn<WcServicio, String> wcColId;
    @FXML private TableColumn<WcServicio, String> wcColNombre;
    @FXML private TableColumn<WcServicio, String> wcColDescripcion;
    @FXML private TableColumn<WcServicio, BigDecimal> wcColPrecio;
    @FXML private TableColumn<WcServicio, Boolean> wcColActivo;

    @FXML private Button wcBtnEditar;
    @FXML private Button wcBtnEliminar;
    @FXML private WcPaginador wcPaginador;

    private final ObservableList<WcServicio> wcDatos = FXCollections.observableArrayList();
    private Long wcIdEditando;

    @FXML
    public void initialize() {
        wcColId.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcId())));
        wcColId.setCellFactory(WcTablas.wcCeldaId());
        wcColNombre.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcNombre()));
        wcColDescripcion.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcDescripcion()));
        wcColDescripcion.setCellFactory(WcTablas.wcCeldaOpcional());
        wcColPrecio.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcPrecio()));
        wcColPrecio.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColActivo.setCellValueFactory(wcC -> new SimpleBooleanProperty(wcC.getValue().isWcActivo()).asObject());
        wcColActivo.setCellFactory(WcTablas.wcCeldaActivo());

        WcTablas.wcConfigurar(wcTabla, "Registra un servicio o cambia el texto de busqueda.", this::wcEditar);
        wcTabla.setItems(wcDatos);

        var wcSeleccion = wcTabla.getSelectionModel().selectedItemProperty();
        wcBtnEditar.disableProperty().bind(wcSeleccion.isNull());
        wcBtnEliminar.disableProperty().bind(wcSeleccion.isNull());

        wcPaginador.setWcOnCambio(this::wcCargarTabla);
        wcTxtBuscar.textProperty().addListener((wcObs, wcAnterior, wcActual) -> wcPaginador.wcIrAPrimera());
        WcFormularios.wcSoloDecimal(wcTxtPrecio);
        WcFormularios.wcLimpiarAlEditar(wcTxtNombre, wcTxtDescripcion, wcTxtPrecio);
    }

    @Override
    public void wcRefrescar() {
        wcCargarTabla();
    }

    @FXML
    private void wcOnNuevo() {
        wcTabla.getSelectionModel().clearSelection();
        wcLimpiarFormulario();
        wcIdEditando = null;
        wcLblTituloFormulario.setText("Nuevo servicio");
        wcLblSubtituloFormulario.setText("Mano de obra que ofrece el taller");
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnEditar() {
        wcEditar(wcTabla.getSelectionModel().getSelectedItem());
    }

    private void wcEditar(WcServicio wcServicio) {
        if (wcServicio == null) {
            return;
        }
        wcLimpiarFormulario();
        wcIdEditando = wcServicio.getWcId();
        wcTxtNombre.setText(wcServicio.getWcNombre());
        wcTxtDescripcion.setText(wcServicio.getWcDescripcion() == null ? "" : wcServicio.getWcDescripcion());
        wcTxtPrecio.setText(wcServicio.getWcPrecio().toPlainString());
        wcSwActivo.setSelected(wcServicio.isWcActivo());
        wcLblTituloFormulario.setText("Editar servicio");
        wcLblSubtituloFormulario.setText("Servicio #" + wcServicio.getWcId() + " · " + wcServicio.getWcNombre());
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnCerrarFormulario() {
        wcNotificador.wcCerrarPanel();
    }

    @FXML
    private void wcOnGuardar() {
        WcFormularios.wcLimpiarErrores(wcTxtNombre, wcTxtDescripcion, wcTxtPrecio);
        if (wcTxtPrecio.getText().isBlank()) {
            WcFormularios.wcMarcarError(wcTxtPrecio, "Ingresa el precio de mano de obra");
            return;
        }
        String wcDescripcion = wcTxtDescripcion.getText().trim();
        WcServicio wcServicio = WcServicio.builder()
                .wcId(wcIdEditando)
                .wcNombre(wcTxtNombre.getText().trim())
                .wcDescripcion(wcDescripcion.isEmpty() ? null : wcDescripcion)
                .wcPrecio(new BigDecimal(wcTxtPrecio.getText().replace(",", ".")))
                .wcActivo(wcSwActivo.isSelected())
                .build();
        try {
            WcServicio wcGuardado = wcService.wcGuardar(wcServicio);
            wcNotificador.wcCerrarPanel();
            wcNotificador.wcExito(wcGuardado.getWcNombre() + " se guardo correctamente.");
            wcCargarTabla();
        } catch (ConstraintViolationException wcE) {
            List<String> wcOtros = WcFormularios.wcMostrarViolaciones(wcE, Map.of(
                    "wcNombre", wcTxtNombre, "wcDescripcion", wcTxtDescripcion, "wcPrecio", wcTxtPrecio));
            wcOtros.forEach(wcNotificador::wcAdvertencia);
        } catch (WcNegocioException wcE) {
            WcFormularios.wcMarcarError(wcTxtNombre, wcE.getMessage());
        }
    }

    @FXML
    private void wcOnEliminar() {
        WcServicio wcSeleccionado = wcTabla.getSelectionModel().getSelectedItem();
        if (wcSeleccionado == null) {
            return;
        }
        wcNotificador.wcConfirmar("¿Eliminar " + wcSeleccionado.getWcNombre() + "?",
                "El servicio dejara de aparecer en el catalogo.",
                "Eliminar",
                () -> {
                    try {
                        wcService.wcEliminar(wcSeleccionado.getWcId());
                        wcNotificador.wcExito("Servicio eliminado.");
                        wcCargarTabla();
                    } catch (WcNegocioException wcE) {
                        wcNotificador.wcError(wcE.getMessage());
                    }
                });
    }

    private void wcCargarTabla() {
        Page<WcServicio> wcPagina = wcService.wcListar(wcTxtBuscar.getText(), wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }

    private void wcAbrirFormulario() {
        wcNotificador.wcAbrirPanel(wcPanelFormulario);
        Platform.runLater(wcTxtNombre::requestFocus);
    }

    private void wcLimpiarFormulario() {
        WcFormularios.wcLimpiarErrores(wcTxtNombre, wcTxtDescripcion, wcTxtPrecio);
        wcTxtNombre.clear();
        wcTxtDescripcion.clear();
        wcTxtPrecio.clear();
        wcSwActivo.setSelected(true);
    }
}
