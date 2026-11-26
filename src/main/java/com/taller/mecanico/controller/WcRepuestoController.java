package com.taller.mecanico.controller;

import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCategoria;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.service.WcRepuestoService;
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
public class WcRepuestoController implements WcRefrescable {
    private final WcRepuestoService wcService;
    private final WcNotificador wcNotificador;

    @FXML private VBox wcPanelFormulario;
    @FXML private Label wcLblTituloFormulario;
    @FXML private Label wcLblSubtituloFormulario;
    @FXML private TextField wcTxtNombre;
    @FXML private TextField wcTxtCodigoRepuesto;
    @FXML private TextArea wcTxtDescripcion;
    @FXML private TextField wcTxtPrecio;
    @FXML private ComboBox<WcCategoria> wcCmbCategoria;
    @FXML private TextField wcTxtStockMinimo;
    @FXML private Label wcLblStockActual;
    @FXML private TextField wcTxtBuscar;

    @FXML private TableView<WcRepuesto> wcTabla;
    @FXML private TableColumn<WcRepuesto, String> wcColId;
    @FXML private TableColumn<WcRepuesto, String> wcColNombre;
    @FXML private TableColumn<WcRepuesto, String> wcColCodigo;
    @FXML private TableColumn<WcRepuesto, BigDecimal> wcColPrecio;
    @FXML private TableColumn<WcRepuesto, String> wcColCategoria;
    @FXML private TableColumn<WcRepuesto, WcRepuesto> wcColStock;

    @FXML private Button wcBtnEditar;
    @FXML private Button wcBtnEliminar;
    @FXML private WcPaginador wcPaginador;

    private final ObservableList<WcRepuesto> wcDatos = FXCollections.observableArrayList();

    private Long wcIdEditando;

    @FXML
    public void initialize() {
        wcColId.setCellValueFactory(wcC -> new SimpleStringProperty(String.valueOf(wcC.getValue().getWcId())));
        wcColId.setCellFactory(WcTablas.wcCeldaId());
        wcColNombre.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcNombre()));
        wcColCodigo.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCodigoRepuesto()));
        wcColPrecio.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcPrecio()));
        wcColPrecio.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColCategoria.setCellValueFactory(wcC -> new SimpleStringProperty(
                wcC.getValue().getWcCategoria() != null ? wcC.getValue().getWcCategoria().getWcNombre() : null));
        wcColCategoria.setCellFactory(WcTablas.wcCeldaChip("Sin categoria"));
        wcColStock.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue()));
        wcColStock.setCellFactory(WcTablas.wcCeldaStock());

        WcTablas.wcConfigurar(wcTabla, "Registra un repuesto o cambia el texto de busqueda.", this::wcEditar);
        wcTabla.setItems(wcDatos);

        var wcSeleccion = wcTabla.getSelectionModel().selectedItemProperty();
        wcBtnEditar.disableProperty().bind(wcSeleccion.isNull());
        wcBtnEliminar.disableProperty().bind(wcSeleccion.isNull());

        wcPaginador.setWcOnCambio(this::wcCargarTabla);
        wcTxtBuscar.textProperty().addListener((wcObs, wcA, wcB) -> wcPaginador.wcIrAPrimera());

        wcCmbCategoria.setCellFactory(wcLv -> new WcCeldaCategoria());
        wcCmbCategoria.setButtonCell(new WcCeldaCategoria());

        WcFormularios.wcSoloDecimal(wcTxtPrecio);
        WcFormularios.wcSoloDigitos(wcTxtStockMinimo, 5);
        WcFormularios.wcLimpiarAlEditar(wcTxtNombre, wcTxtCodigoRepuesto, wcTxtDescripcion, wcTxtPrecio, wcTxtStockMinimo);
    }

    @Override
    public void wcRefrescar() {
        wcCargarCategorias();
        wcCargarTabla();
    }

    @FXML
    private void wcOnNuevo() {
        wcTabla.getSelectionModel().clearSelection();
        wcLimpiarFormulario();
        wcIdEditando = null;
        wcLblTituloFormulario.setText("Nuevo repuesto");
        wcLblSubtituloFormulario.setText("Agrega un repuesto al catalogo");
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnEditar() {
        wcEditar(wcTabla.getSelectionModel().getSelectedItem());
    }

    private void wcEditar(WcRepuesto wcP) {
        if (wcP == null) {
            return;
        }
        wcLimpiarFormulario();
        wcIdEditando = wcP.getWcId();
        wcTxtNombre.setText(wcP.getWcNombre());
        wcTxtCodigoRepuesto.setText(wcP.getWcCodigoRepuesto());
        wcTxtDescripcion.setText(wcTextoODefecto(wcP.getWcDescripcion()));
        wcTxtPrecio.setText(wcP.getWcPrecio() != null ? wcP.getWcPrecio().toPlainString() : "");
        wcCmbCategoria.setValue(wcP.getWcCategoria());
        wcTxtStockMinimo.setText(String.valueOf(wcP.getWcStockMinimo()));
        wcLblStockActual.setText(wcP.getWcStock() + " unidades");
        wcLblTituloFormulario.setText("Editar repuesto");
        wcLblSubtituloFormulario.setText("Registro #" + wcP.getWcId() + " · " + wcP.getWcNombre());
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnCerrarFormulario() {
        wcNotificador.wcCerrarPanel();
    }

    @FXML
    private void wcOnGuardar() {
        WcFormularios.wcLimpiarErrores(wcTxtNombre, wcTxtCodigoRepuesto, wcTxtDescripcion, wcTxtPrecio, wcTxtStockMinimo);

        BigDecimal wcPrecio = null;
        String wcTextoPrecio = wcTxtPrecio.getText().trim().replace(",", ".");
        if (!wcTextoPrecio.isEmpty()) {
            try {
                wcPrecio = new BigDecimal(wcTextoPrecio);
            } catch (NumberFormatException wcE) {
                WcFormularios.wcMarcarError(wcTxtPrecio, "El precio debe ser un numero valido");
                return;
            }
        }

        WcRepuesto wcRepuesto = WcRepuesto.builder()
                .wcId(wcIdEditando)
                .wcNombre(wcTxtNombre.getText().trim())
                .wcCodigoRepuesto(wcTxtCodigoRepuesto.getText().trim())
                .wcDescripcion(wcTxtDescripcion.getText().trim())
                .wcPrecio(wcPrecio)
                .wcCategoria(wcCmbCategoria.getValue())
                .wcStockMinimo(wcTxtStockMinimo.getText().isBlank() ? 5 : Integer.parseInt(wcTxtStockMinimo.getText()))
                .build();

        try {
            boolean wcEsNuevo = wcRepuesto.getWcId() == null;
            WcRepuesto wcGuardado = wcService.wcGuardar(wcRepuesto);
            wcNotificador.wcCerrarPanel();
            wcNotificador.wcExito(wcEsNuevo
                    ? "Repuesto \"" + wcGuardado.getWcNombre() + "\" registrado."
                    : "Repuesto \"" + wcGuardado.getWcNombre() + "\" actualizado.");
            wcCargarTabla();
        } catch (ConstraintViolationException wcE) {
            List<String> wcOtros = WcFormularios.wcMostrarViolaciones(wcE, Map.of(
                    "wcNombre", wcTxtNombre, "wcCodigoRepuesto", wcTxtCodigoRepuesto,
                    "wcDescripcion", wcTxtDescripcion, "wcPrecio", wcTxtPrecio, "wcStockMinimo", wcTxtStockMinimo));
            wcOtros.forEach(wcNotificador::wcAdvertencia);
        } catch (WcNegocioException wcE) {
            WcFormularios.wcMarcarError(wcTxtCodigoRepuesto, wcE.getMessage());
        } catch (Exception wcE) {
            wcNotificador.wcError("Error inesperado: " + wcE.getMessage());
        }
    }

    @FXML
    private void wcOnEliminar() {
        WcRepuesto wcSeleccionado = wcTabla.getSelectionModel().getSelectedItem();
        if (wcSeleccionado == null) {
            return;
        }
        wcNotificador.wcConfirmar("¿Eliminar " + wcSeleccionado.getWcNombre() + "?",
                "Esta accion no se puede deshacer. El repuesto con codigo "
                        + wcSeleccionado.getWcCodigoRepuesto() + " se eliminara permanentemente.",
                "Eliminar",
                () -> {
                    try {
                        wcService.wcEliminar(wcSeleccionado.getWcId());
                        wcNotificador.wcExito("Repuesto eliminado.");
                        wcCargarTabla();
                    } catch (WcNegocioException wcE) {
                        wcNotificador.wcError(wcE.getMessage());
                    }
                });
    }

    private void wcCargarCategorias() {
        ObservableList<WcCategoria> wcCategorias = FXCollections.observableArrayList();
        wcCategorias.add(null);
        wcCategorias.addAll(wcService.wcListarCategorias());
        wcCmbCategoria.setItems(wcCategorias);
    }

    private void wcCargarTabla() {
        Page<WcRepuesto> wcPagina = wcService.wcListar(wcTxtBuscar.getText(), wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }

    private void wcAbrirFormulario() {
        wcNotificador.wcAbrirPanel(wcPanelFormulario);
        Platform.runLater(wcTxtCodigoRepuesto::requestFocus);
    }

    private void wcLimpiarFormulario() {
        WcFormularios.wcLimpiarErrores(wcTxtNombre, wcTxtCodigoRepuesto, wcTxtDescripcion, wcTxtPrecio, wcTxtStockMinimo);
        wcTxtNombre.clear();
        wcTxtCodigoRepuesto.clear();
        wcTxtDescripcion.clear();
        wcTxtPrecio.clear();
        wcCmbCategoria.setValue(null);
        wcTxtStockMinimo.setText("5");
        wcLblStockActual.setText("0 unidades (nuevo)");
    }

    private String wcTextoODefecto(String wcValor) {
        return wcValor == null ? "" : wcValor;
    }

    private static class WcCeldaCategoria extends ListCell<WcCategoria> {
        @Override
        protected void updateItem(WcCategoria wcCategoria, boolean wcVacio) {
            super.updateItem(wcCategoria, wcVacio);
            if (wcVacio) {
                setText(null);
            } else {
                setText(wcCategoria == null ? "Sin categoria" : wcCategoria.getWcNombre());
            }
        }
    }
}
