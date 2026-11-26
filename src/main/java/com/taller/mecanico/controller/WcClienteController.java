package com.taller.mecanico.controller;

import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCliente;
import com.taller.mecanico.service.WcClienteService;
import com.taller.mecanico.ui.WcFormularios;
import com.taller.mecanico.ui.WcNotificador;
import com.taller.mecanico.ui.WcPaginador;
import com.taller.mecanico.ui.WcRefrescable;
import com.taller.mecanico.ui.WcTablas;
import jakarta.validation.ConstraintViolationException;
import javafx.application.Platform;
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

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WcClienteController implements WcRefrescable {
    private final WcClienteService wcService;
    private final WcNotificador wcNotificador;

    @FXML private VBox wcPanelFormulario;
    @FXML private Label wcLblTituloFormulario;
    @FXML private Label wcLblSubtituloFormulario;
    @FXML private TextField wcTxtNombre;
    @FXML private TextField wcTxtApellido;
    @FXML private TextField wcTxtDni;
    @FXML private TextField wcTxtEmail;
    @FXML private TextField wcTxtTelefono;
    @FXML private TextField wcTxtBuscar;

    @FXML private TableView<WcCliente> wcTabla;
    @FXML private TableColumn<WcCliente, String> wcColId;
    @FXML private TableColumn<WcCliente, String> wcColNombre;
    @FXML private TableColumn<WcCliente, String> wcColDni;
    @FXML private TableColumn<WcCliente, String> wcColEmail;
    @FXML private TableColumn<WcCliente, String> wcColTelefono;

    @FXML private Button wcBtnEditar;
    @FXML private Button wcBtnEliminar;
    @FXML private WcPaginador wcPaginador;

    private final ObservableList<WcCliente> wcDatos = FXCollections.observableArrayList();

    private Long wcIdEditando;

    @FXML
    public void initialize() {
        wcColId.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcId())));
        wcColId.setCellFactory(WcTablas.wcCeldaId());
        wcColNombre.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcNombreCompleto()));
        wcColNombre.setCellFactory(WcTablas.wcCeldaAvatar());
        wcColDni.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcDni()));
        wcColEmail.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcEmail()));
        wcColEmail.setCellFactory(WcTablas.wcCeldaOpcional());
        wcColTelefono.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcTelefono()));
        wcColTelefono.setCellFactory(WcTablas.wcCeldaOpcional());

        WcTablas.wcConfigurar(wcTabla, "Registra un cliente o cambia el texto de busqueda.", this::wcEditar);
        wcTabla.setItems(wcDatos);

        var wcSeleccion = wcTabla.getSelectionModel().selectedItemProperty();
        wcBtnEditar.disableProperty().bind(wcSeleccion.isNull());
        wcBtnEliminar.disableProperty().bind(wcSeleccion.isNull());

        wcPaginador.setWcOnCambio(this::wcCargarTabla);
        wcTxtBuscar.textProperty().addListener((wcObs, wcA, wcB) -> wcPaginador.wcIrAPrimera());

        WcFormularios.wcSoloDigitos(wcTxtDni, 8);
        WcFormularios.wcSoloDigitos(wcTxtTelefono, 9);
        WcFormularios.wcLimpiarAlEditar(wcTxtNombre, wcTxtApellido, wcTxtDni, wcTxtEmail, wcTxtTelefono);
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
        wcLblTituloFormulario.setText("Nuevo cliente");
        wcLblSubtituloFormulario.setText("Completa los datos para registrarla");
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnEditar() {
        wcEditar(wcTabla.getSelectionModel().getSelectedItem());
    }

    private void wcEditar(WcCliente wcP) {
        if (wcP == null) {
            return;
        }
        wcLimpiarFormulario();
        wcIdEditando = wcP.getWcId();
        wcTxtNombre.setText(wcP.getWcNombre());
        wcTxtApellido.setText(wcP.getWcApellido());
        wcTxtDni.setText(wcP.getWcDni());
        wcTxtEmail.setText(wcTextoODefecto(wcP.getWcEmail()));
        wcTxtTelefono.setText(wcTextoODefecto(wcP.getWcTelefono()));
        wcLblTituloFormulario.setText("Editar cliente");
        wcLblSubtituloFormulario.setText("Registro #" + wcP.getWcId() + " · " + wcP.getWcNombreCompleto());
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnCerrarFormulario() {
        wcNotificador.wcCerrarPanel();
    }

    @FXML
    private void wcOnGuardar() {
        WcFormularios.wcLimpiarErrores(wcTxtNombre, wcTxtApellido, wcTxtDni, wcTxtEmail, wcTxtTelefono);
        WcCliente wcCliente = WcCliente.builder()
                .wcId(wcIdEditando)
                .wcNombre(wcTxtNombre.getText().trim())
                .wcApellido(wcTxtApellido.getText().trim())
                .wcDni(wcTxtDni.getText().trim())
                .wcEmail(wcTxtEmail.getText().trim())
                .wcTelefono(wcTxtTelefono.getText().trim())
                .build();

        try {
            boolean wcEsNuevo = wcCliente.getWcId() == null;
            WcCliente wcGuardado = wcService.wcGuardar(wcCliente);
            wcNotificador.wcCerrarPanel();
            wcNotificador.wcExito(wcEsNuevo
                    ? wcGuardado.getWcNombreCompleto() + " se registro correctamente."
                    : "Los cambios de " + wcGuardado.getWcNombreCompleto() + " se guardaron.");
            wcCargarTabla();
        } catch (ConstraintViolationException wcE) {
            List<String> wcOtros = WcFormularios.wcMostrarViolaciones(wcE, Map.of(
                    "wcNombre", wcTxtNombre, "wcApellido", wcTxtApellido, "wcDni", wcTxtDni,
                    "wcEmail", wcTxtEmail, "wcTelefono", wcTxtTelefono));
            wcOtros.forEach(wcNotificador::wcAdvertencia);
        } catch (WcNegocioException wcE) {
            WcFormularios.wcMarcarError(wcTxtDni, wcE.getMessage());
        } catch (Exception wcE) {
            wcNotificador.wcError("Error inesperado: " + wcE.getMessage());
        }
    }

    @FXML
    private void wcOnEliminar() {
        WcCliente wcSeleccionado = wcTabla.getSelectionModel().getSelectedItem();
        if (wcSeleccionado == null) {
            return;
        }
        wcNotificador.wcConfirmar("¿Eliminar a " + wcSeleccionado.getWcNombreCompleto() + "?",
                "Esta accion no se puede deshacer. El registro con DNI "
                        + wcSeleccionado.getWcDni() + " se eliminara permanentemente.",
                "Eliminar",
                () -> {
                    try {
                        wcService.wcEliminar(wcSeleccionado.getWcId());
                        wcNotificador.wcExito("Registro eliminado.");
                        wcCargarTabla();
                    } catch (WcNegocioException wcE) {
                        wcNotificador.wcError(wcE.getMessage());
                    }
                });
    }

    private void wcCargarTabla() {
        Page<WcCliente> wcPagina = wcService.wcListar(wcTxtBuscar.getText(), wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }

    private void wcAbrirFormulario() {
        wcNotificador.wcAbrirPanel(wcPanelFormulario);
        Platform.runLater(wcTxtDni::requestFocus);
    }

    private void wcLimpiarFormulario() {
        WcFormularios.wcLimpiarErrores(wcTxtNombre, wcTxtApellido, wcTxtDni, wcTxtEmail, wcTxtTelefono);
        wcTxtNombre.clear();
        wcTxtApellido.clear();
        wcTxtDni.clear();
        wcTxtEmail.clear();
        wcTxtTelefono.clear();
    }

    private String wcTextoODefecto(String wcValor) {
        return wcValor == null ? "" : wcValor;
    }
}
