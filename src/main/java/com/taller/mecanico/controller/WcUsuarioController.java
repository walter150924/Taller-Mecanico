package com.taller.mecanico.controller;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcRol;
import com.taller.mecanico.model.WcUsuario;
import com.taller.mecanico.service.WcUsuarioService;
import com.taller.mecanico.ui.WcFormularios;
import com.taller.mecanico.ui.WcNotificador;
import com.taller.mecanico.ui.WcPaginador;
import com.taller.mecanico.ui.WcRefrescable;
import com.taller.mecanico.ui.WcTablas;
import jakarta.validation.ConstraintViolationException;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
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
public class WcUsuarioController implements WcRefrescable {
    private final WcUsuarioService wcService;
    private final WcNotificador wcNotificador;

    @FXML private VBox wcPanelFormulario;
    @FXML private Label wcLblTituloFormulario;
    @FXML private Label wcLblSubtituloFormulario;
    @FXML private TextField wcTxtNombreCompleto;
    @FXML private ComboBox<WcRol> wcCmbRol;
    @FXML private TextField wcTxtNombreUsuario;
    @FXML private Label wcLblClave;
    @FXML private PasswordField wcTxtClave;
    @FXML private ToggleSwitch wcSwActivo;
    @FXML private TextField wcTxtBuscar;

    @FXML private TableView<WcUsuario> wcTabla;
    @FXML private TableColumn<WcUsuario, String> wcColId;
    @FXML private TableColumn<WcUsuario, String> wcColNombre;
    @FXML private TableColumn<WcUsuario, String> wcColUsuario;
    @FXML private TableColumn<WcUsuario, String> wcColRol;
    @FXML private TableColumn<WcUsuario, Boolean> wcColActivo;

    @FXML private Button wcBtnEditar;
    @FXML private Button wcBtnEliminar;
    @FXML private WcPaginador wcPaginador;

    private final ObservableList<WcUsuario> wcDatos = FXCollections.observableArrayList();
    private Long wcIdEditando;

    @FXML
    public void initialize() {
        wcColId.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcId())));
        wcColId.setCellFactory(WcTablas.wcCeldaId());
        wcColNombre.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcNombreCompleto()));
        wcColNombre.setCellFactory(WcTablas.wcCeldaAvatar());
        wcColUsuario.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcNombreUsuario()));
        wcColRol.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcRol().getWcDescripcion()));
        wcColRol.setCellFactory(WcTablas.wcCeldaChip("—"));
        wcColActivo.setCellValueFactory(wcC -> new SimpleBooleanProperty(wcC.getValue().isWcActivo()).asObject());
        wcColActivo.setCellFactory(WcTablas.wcCeldaActivo());

        WcTablas.wcConfigurar(wcTabla, "Registra un usuario o cambia el texto de busqueda.", this::wcEditar);
        wcTabla.setItems(wcDatos);

        var wcSeleccion = wcTabla.getSelectionModel().selectedItemProperty();
        wcBtnEditar.disableProperty().bind(wcSeleccion.isNull());
        wcBtnEliminar.disableProperty().bind(wcSeleccion.isNull());

        wcCmbRol.getItems().setAll(WcRol.values());
        wcCmbRol.valueProperty().addListener((wcObs, wcAnterior, wcActual) -> WcFormularios.wcLimpiarError(wcCmbRol));
        wcPaginador.setWcOnCambio(this::wcCargarTabla);
        wcTxtBuscar.textProperty().addListener((wcObs, wcAnterior, wcActual) -> wcPaginador.wcIrAPrimera());
        wcTxtNombreUsuario.setTextFormatter(new TextFormatter<String>(wcCambio -> {
            wcCambio.setText(wcCambio.getText().toLowerCase());
            return wcCambio;
        }));
        WcFormularios.wcLimpiarAlEditar(wcTxtNombreCompleto, wcTxtNombreUsuario, wcTxtClave);
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
        wcLblTituloFormulario.setText("Nuevo usuario");
        wcLblSubtituloFormulario.setText("Completa los datos de acceso");
        wcLblClave.setText("Contraseña *");
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnEditar() {
        wcEditar(wcTabla.getSelectionModel().getSelectedItem());
    }

    private void wcEditar(WcUsuario wcUsuario) {
        if (wcUsuario == null) {
            return;
        }
        wcLimpiarFormulario();
        wcIdEditando = wcUsuario.getWcId();
        wcTxtNombreCompleto.setText(wcUsuario.getWcNombreCompleto());
        wcCmbRol.setValue(wcUsuario.getWcRol());
        wcTxtNombreUsuario.setText(wcUsuario.getWcNombreUsuario());
        wcSwActivo.setSelected(wcUsuario.isWcActivo());
        wcLblTituloFormulario.setText("Editar usuario");
        wcLblSubtituloFormulario.setText("Usuario #" + wcUsuario.getWcId() + " · " + wcUsuario.getWcNombreUsuario());
        wcLblClave.setText("Nueva contraseña (dejala vacia para mantener la actual)");
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnCerrarFormulario() {
        wcNotificador.wcCerrarPanel();
    }

    @FXML
    private void wcOnGuardar() {
        WcFormularios.wcLimpiarErrores(wcTxtNombreCompleto, wcCmbRol, wcTxtNombreUsuario, wcTxtClave);
        WcUsuario wcUsuario = WcUsuario.builder()
                .wcId(wcIdEditando)
                .wcNombreCompleto(wcTxtNombreCompleto.getText().trim())
                .wcRol(wcCmbRol.getValue())
                .wcNombreUsuario(wcTxtNombreUsuario.getText().trim())
                .wcActivo(wcSwActivo.isSelected())
                .build();
        try {
            boolean wcEsNuevo = wcIdEditando == null;
            WcUsuario wcGuardado = wcService.wcGuardar(wcUsuario, wcTxtClave.getText());
            wcNotificador.wcCerrarPanel();
            wcNotificador.wcExito(wcEsNuevo
                    ? wcGuardado.getWcNombreCompleto() + " ya puede iniciar sesion."
                    : "Los cambios de " + wcGuardado.getWcNombreCompleto() + " se guardaron.");
            wcCargarTabla();
        } catch (ConstraintViolationException wcE) {
            List<String> wcOtros = WcFormularios.wcMostrarViolaciones(wcE, Map.of(
                    "wcNombreCompleto", wcTxtNombreCompleto, "wcRol", wcCmbRol, "wcNombreUsuario", wcTxtNombreUsuario));
            wcOtros.forEach(wcNotificador::wcAdvertencia);
        } catch (WcNegocioException wcE) {
            wcNotificador.wcAdvertencia(wcE.getMessage());
        }
    }

    @FXML
    private void wcOnEliminar() {
        WcUsuario wcSeleccionado = wcTabla.getSelectionModel().getSelectedItem();
        if (wcSeleccionado == null) {
            return;
        }
        wcNotificador.wcConfirmar("¿Eliminar a " + wcSeleccionado.getWcNombreCompleto() + "?",
                "El usuario " + wcSeleccionado.getWcNombreUsuario() + " ya no podra ingresar al sistema.",
                "Eliminar",
                () -> {
                    try {
                        wcService.wcEliminar(wcSeleccionado.getWcId());
                        wcNotificador.wcExito("Usuario eliminado.");
                        wcCargarTabla();
                    } catch (WcNegocioException wcE) {
                        wcNotificador.wcError(wcE.getMessage());
                    }
                });
    }

    private void wcCargarTabla() {
        Page<WcUsuario> wcPagina = wcService.wcListar(wcTxtBuscar.getText(), wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }

    private void wcAbrirFormulario() {
        wcNotificador.wcAbrirPanel(wcPanelFormulario);
        Platform.runLater(wcTxtNombreCompleto::requestFocus);
    }

    private void wcLimpiarFormulario() {
        WcFormularios.wcLimpiarErrores(wcTxtNombreCompleto, wcCmbRol, wcTxtNombreUsuario, wcTxtClave);
        wcTxtNombreCompleto.clear();
        wcCmbRol.setValue(null);
        wcTxtNombreUsuario.clear();
        wcTxtClave.clear();
        wcSwActivo.setSelected(true);
    }
}
