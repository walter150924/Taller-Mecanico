package com.taller.mecanico.controller;

import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCategoria;
import com.taller.mecanico.service.WcCategoriaService;
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
public class WcCategoriaController implements WcRefrescable {
    private final WcCategoriaService wcService;
    private final WcNotificador wcNotificador;

    @FXML private VBox wcPanelFormulario;
    @FXML private Label wcLblTituloFormulario;
    @FXML private Label wcLblSubtituloFormulario;
    @FXML private TextField wcTxtNombre;
    @FXML private TextField wcTxtCodigoCategoria;
    @FXML private TextArea wcTxtDescripcion;
    @FXML private TextField wcTxtBuscar;

    @FXML private TableView<WcCategoria> wcTabla;
    @FXML private TableColumn<WcCategoria, String> wcColId;
    @FXML private TableColumn<WcCategoria, String> wcColNombre;
    @FXML private TableColumn<WcCategoria, String> wcColCodigo;
    @FXML private TableColumn<WcCategoria, String> wcColDescripcion;

    @FXML private Button wcBtnEditar;
    @FXML private Button wcBtnEliminar;
    @FXML private WcPaginador wcPaginador;

    private final ObservableList<WcCategoria> wcDatos = FXCollections.observableArrayList();

    private Long wcIdEditando;

    @FXML
    public void initialize() {
        wcColId.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcId())));
        wcColId.setCellFactory(WcTablas.wcCeldaId());
        wcColNombre.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcNombre()));
        wcColCodigo.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCodigoCategoria()));
        wcColCodigo.setCellFactory(WcTablas.wcCeldaChip("—"));
        wcColDescripcion.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcDescripcion()));
        wcColDescripcion.setCellFactory(WcTablas.wcCeldaOpcional());

        WcTablas.wcConfigurar(wcTabla, "Registra una categoria o cambia el texto de busqueda.", this::wcEditar);
        wcTabla.setItems(wcDatos);

        var wcSeleccion = wcTabla.getSelectionModel().selectedItemProperty();
        wcBtnEditar.disableProperty().bind(wcSeleccion.isNull());
        wcBtnEliminar.disableProperty().bind(wcSeleccion.isNull());

        wcPaginador.setWcOnCambio(this::wcCargarTabla);
        wcTxtBuscar.textProperty().addListener((wcObs, wcA, wcB) -> wcPaginador.wcIrAPrimera());

        WcFormularios.wcLimpiarAlEditar(wcTxtNombre, wcTxtCodigoCategoria, wcTxtDescripcion);
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
        wcLblTituloFormulario.setText("Nueva categoria");
        wcLblSubtituloFormulario.setText("Agrupa tus repuestos por categoria");
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnEditar() {
        wcEditar(wcTabla.getSelectionModel().getSelectedItem());
    }

    private void wcEditar(WcCategoria wcC) {
        if (wcC == null) {
            return;
        }
        wcLimpiarFormulario();
        wcIdEditando = wcC.getWcId();
        wcTxtNombre.setText(wcC.getWcNombre());
        wcTxtCodigoCategoria.setText(wcC.getWcCodigoCategoria());
        wcTxtDescripcion.setText(wcTextoODefecto(wcC.getWcDescripcion()));
        wcLblTituloFormulario.setText("Editar categoria");
        wcLblSubtituloFormulario.setText("Registro #" + wcC.getWcId() + " · " + wcC.getWcNombre());
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnCerrarFormulario() {
        wcNotificador.wcCerrarPanel();
    }

    @FXML
    private void wcOnGuardar() {
        WcFormularios.wcLimpiarErrores(wcTxtNombre, wcTxtCodigoCategoria, wcTxtDescripcion);
        WcCategoria wcCategoria = WcCategoria.builder()
                .wcId(wcIdEditando)
                .wcNombre(wcTxtNombre.getText().trim())
                .wcCodigoCategoria(wcTxtCodigoCategoria.getText().trim())
                .wcDescripcion(wcTxtDescripcion.getText().trim())
                .build();

        try {
            boolean wcEsNueva = wcCategoria.getWcId() == null;
            WcCategoria wcGuardada = wcService.wcGuardar(wcCategoria);
            wcNotificador.wcCerrarPanel();
            wcNotificador.wcExito(wcEsNueva
                    ? "Categoria \"" + wcGuardada.getWcNombre() + "\" registrada."
                    : "Categoria \"" + wcGuardada.getWcNombre() + "\" actualizada.");
            wcCargarTabla();
        } catch (ConstraintViolationException wcE) {
            List<String> wcOtros = WcFormularios.wcMostrarViolaciones(wcE, Map.of(
                    "wcNombre", wcTxtNombre, "wcCodigoCategoria", wcTxtCodigoCategoria,
                    "wcDescripcion", wcTxtDescripcion));
            wcOtros.forEach(wcNotificador::wcAdvertencia);
        } catch (WcNegocioException wcE) {
            WcFormularios.wcMarcarError(wcTxtCodigoCategoria, wcE.getMessage());
        } catch (Exception wcE) {
            wcNotificador.wcError("Error inesperado: " + wcE.getMessage());
        }
    }

    @FXML
    private void wcOnEliminar() {
        WcCategoria wcSeleccionada = wcTabla.getSelectionModel().getSelectedItem();
        if (wcSeleccionada == null) {
            return;
        }
        wcNotificador.wcConfirmar("¿Eliminar la categoria " + wcSeleccionada.getWcNombre() + "?",
                "Esta accion no se puede deshacer. La categoria se eliminara permanentemente.",
                "Eliminar",
                () -> {
                    try {
                        wcService.wcEliminar(wcSeleccionada.getWcId());
                        wcNotificador.wcExito("Categoria eliminada.");
                        wcCargarTabla();
                    } catch (WcNegocioException wcE) {
                        wcNotificador.wcError(wcE.getMessage());
                    } catch (Exception wcE) {
                        wcNotificador.wcError("No se pudo eliminar: " + wcE.getMessage());
                    }
                });
    }

    private void wcCargarTabla() {
        Page<WcCategoria> wcPagina = wcService.wcListar(wcTxtBuscar.getText(), wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }

    private void wcAbrirFormulario() {
        wcNotificador.wcAbrirPanel(wcPanelFormulario);
        Platform.runLater(wcTxtCodigoCategoria::requestFocus);
    }

    private void wcLimpiarFormulario() {
        WcFormularios.wcLimpiarErrores(wcTxtNombre, wcTxtCodigoCategoria, wcTxtDescripcion);
        wcTxtNombre.clear();
        wcTxtCodigoCategoria.clear();
        wcTxtDescripcion.clear();
    }

    private String wcTextoODefecto(String wcValor) {
        return wcValor == null ? "" : wcValor;
    }
}
