package com.taller.mecanico.controller;

import atlantafx.base.util.Animations;
import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCompra;
import com.taller.mecanico.model.WcProveedor;
import com.taller.mecanico.service.WcProveedorService;
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
import org.kordamp.ikonli.feather.Feather;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WcProveedorController implements WcRefrescable {

    private static final DateTimeFormatter WC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int WC_DIGITOS_RUC = 11;
    private static final int WC_DIGITOS_TELEFONO = 9;

    private final WcProveedorService wcService;
    private final WcNotificador wcNotificador;

    @FXML private VBox wcPanelFormulario;
    @FXML private Label wcLblTituloFormulario;
    @FXML private Label wcLblSubtituloFormulario;
    @FXML private TextField wcTxtRuc;
    @FXML private TextField wcTxtRazonSocial;
    @FXML private TextField wcTxtDireccion;
    @FXML private TextField wcTxtTelefono;
    @FXML private TextField wcTxtBuscar;

    @FXML private TableView<WcProveedor> wcTabla;
    @FXML private TableColumn<WcProveedor, String> wcColRuc;
    @FXML private TableColumn<WcProveedor, String> wcColRazonSocial;
    @FXML private TableColumn<WcProveedor, String> wcColDireccion;
    @FXML private TableColumn<WcProveedor, String> wcColTelefono;

    @FXML private VBox wcPanelCompras;
    @FXML private Label wcLblTituloCompras;
    @FXML private Label wcLblSubtituloCompras;
    @FXML private TableView<WcCompra> wcTablaCompras;
    @FXML private TableColumn<WcCompra, String> wcColCompraCodigo;
    @FXML private TableColumn<WcCompra, String> wcColCompraFecha;
    @FXML private TableColumn<WcCompra, String> wcColCompraDocumento;
    @FXML private TableColumn<WcCompra, BigDecimal> wcColCompraTotal;
    @FXML private TableColumn<WcCompra, String> wcColCompraEstado;

    @FXML private Button wcBtnCompras;
    @FXML private Button wcBtnEditar;
    @FXML private Button wcBtnEliminar;
    @FXML private WcPaginador wcPaginador;

    private final ObservableList<WcProveedor> wcDatos = FXCollections.observableArrayList();
    private Long wcIdEditando;

    @FXML
    public void initialize() {
        wcConfigurarTabla();
        wcConfigurarTablaCompras();
        wcPaginador.setWcOnCambio(this::wcCargarTabla);
        wcTxtBuscar.textProperty().addListener((wcObs, wcAnterior, wcActual) -> wcPaginador.wcIrAPrimera());
        WcFormularios.wcSoloDigitos(wcTxtRuc, WC_DIGITOS_RUC);
        WcFormularios.wcSoloDigitos(wcTxtTelefono, WC_DIGITOS_TELEFONO);
        WcFormularios.wcLimpiarAlEditar(wcTxtRuc, wcTxtRazonSocial, wcTxtDireccion, wcTxtTelefono);
    }

    @Override
    public void wcRefrescar() {
        wcCargarTabla();
    }

    private void wcConfigurarTabla() {
        wcColRuc.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcRuc()));
        wcColRuc.setCellFactory(WcTablas.wcCeldaChip("—"));
        wcColRazonSocial.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcRazonSocial()));
        wcColDireccion.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcDireccion()));
        wcColDireccion.setCellFactory(WcTablas.wcCeldaOpcional());
        wcColTelefono.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcTelefono()));
        wcColTelefono.setCellFactory(WcTablas.wcCeldaOpcional());

        WcTablas.wcConfigurar(wcTabla, "Registra un proveedor o cambia el texto de busqueda.", this::wcEditar);
        wcTabla.setItems(wcDatos);

        var wcSeleccion = wcTabla.getSelectionModel().selectedItemProperty();
        wcBtnCompras.disableProperty().bind(wcSeleccion.isNull());
        wcBtnEditar.disableProperty().bind(wcSeleccion.isNull());
        wcBtnEliminar.disableProperty().bind(wcSeleccion.isNull());
    }

    private void wcConfigurarTablaCompras() {
        wcColCompraCodigo.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcCodigo()));
        wcColCompraFecha.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcFecha().format(WC_FECHA)));
        wcColCompraDocumento.setCellValueFactory(wcC -> new SimpleStringProperty(
                wcC.getValue().getWcTipoDocumento().getWcDescripcion() + " " + wcC.getValue().getWcNumeroDocumento()));
        wcColCompraTotal.setCellValueFactory(wcC -> new SimpleObjectProperty<>(wcC.getValue().getWcTotal()));
        wcColCompraTotal.setCellFactory(WcTablas.wcCeldaMoneda());
        wcColCompraEstado.setCellValueFactory(wcC -> new SimpleStringProperty(wcC.getValue().getWcEstado().name().replace("WC_", "")));
        wcColCompraEstado.setCellFactory(WcTablas.wcCeldaEstado());
        WcTablas.wcConfigurar(wcTablaCompras, Feather.ARCHIVE, "Sin compras",
                "Aun no se registraron compras a este proveedor.", wcCompra -> { });
    }

    @FXML
    private void wcOnNuevo() {
        wcTabla.getSelectionModel().clearSelection();
        wcLimpiarFormulario();
        wcIdEditando = null;
        wcLblTituloFormulario.setText("Nuevo proveedor");
        wcLblSubtituloFormulario.setText("Empresa que vende repuestos al taller");
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnEditar() {
        wcEditar(wcTabla.getSelectionModel().getSelectedItem());
    }

    private void wcEditar(WcProveedor wcProveedor) {
        if (wcProveedor == null) {
            return;
        }
        wcLimpiarFormulario();
        wcIdEditando = wcProveedor.getWcId();
        wcTxtRuc.setText(wcProveedor.getWcRuc());
        wcTxtRazonSocial.setText(wcProveedor.getWcRazonSocial());
        wcTxtDireccion.setText(wcProveedor.getWcDireccion() == null ? "" : wcProveedor.getWcDireccion());
        wcTxtTelefono.setText(wcProveedor.getWcTelefono() == null ? "" : wcProveedor.getWcTelefono());
        wcLblTituloFormulario.setText("Editar proveedor");
        wcLblSubtituloFormulario.setText("RUC " + wcProveedor.getWcRuc());
        wcAbrirFormulario();
    }

    @FXML
    private void wcOnCompras() {
        WcProveedor wcProveedor = wcTabla.getSelectionModel().getSelectedItem();
        if (wcProveedor == null) {
            return;
        }
        List<WcCompra> wcCompras = wcService.wcCompras(wcProveedor.getWcId());
        wcLblTituloCompras.setText("Compras a " + wcProveedor.getWcRazonSocial());
        wcLblSubtituloCompras.setText("RUC " + wcProveedor.getWcRuc() + " · " + wcCompras.size() + " compras registradas");
        wcTablaCompras.getItems().setAll(wcCompras);
        wcNotificador.wcAbrirPanel(wcPanelCompras);
    }

    @FXML
    private void wcOnCerrarPanel() {
        wcNotificador.wcCerrarPanel();
    }

    @FXML
    private void wcOnGuardar() {
        WcFormularios.wcLimpiarErrores(wcTxtRuc, wcTxtRazonSocial, wcTxtDireccion, wcTxtTelefono);
        WcProveedor wcProveedor = WcProveedor.builder()
                .wcId(wcIdEditando)
                .wcRuc(wcTxtRuc.getText().trim())
                .wcRazonSocial(wcTxtRazonSocial.getText().trim().toUpperCase())
                .wcDireccion(wcTextoONulo(wcTxtDireccion))
                .wcTelefono(wcTextoONulo(wcTxtTelefono))
                .build();
        try {
            WcProveedor wcGuardado = wcService.wcGuardar(wcProveedor);
            wcNotificador.wcCerrarPanel();
            wcNotificador.wcExito(wcGuardado.getWcRazonSocial() + " se guardo correctamente.");
            wcCargarTabla();
        } catch (ConstraintViolationException wcE) {
            List<String> wcOtros = WcFormularios.wcMostrarViolaciones(wcE, Map.of(
                    "wcRuc", wcTxtRuc, "wcRazonSocial", wcTxtRazonSocial, "wcDireccion", wcTxtDireccion, "wcTelefono", wcTxtTelefono));
            wcOtros.forEach(wcNotificador::wcAdvertencia);
        } catch (WcNegocioException wcE) {
            WcFormularios.wcMarcarError(wcTxtRuc, wcE.getMessage());
        }
    }

    @FXML
    private void wcOnEliminar() {
        WcProveedor wcSeleccionado = wcTabla.getSelectionModel().getSelectedItem();
        if (wcSeleccionado == null) {
            return;
        }
        wcNotificador.wcConfirmar("¿Eliminar a " + wcSeleccionado.getWcRazonSocial() + "?",
                "Esta accion no se puede deshacer.",
                "Eliminar",
                () -> {
                    try {
                        wcService.wcEliminar(wcSeleccionado.getWcId());
                        wcNotificador.wcExito("Proveedor eliminado.");
                        wcCargarTabla();
                    } catch (WcNegocioException wcE) {
                        wcNotificador.wcError(wcE.getMessage());
                    }
                });
    }

    private void wcCargarTabla() {
        Page<WcProveedor> wcPagina = wcService.wcListar(wcTxtBuscar.getText(), wcPaginador.getWcPagina(), wcPaginador.getWcTamanio());
        wcDatos.setAll(wcPagina.getContent());
        wcPaginador.wcActualizar(wcPagina);
        Animations.fadeIn(wcTabla, Duration.millis(260)).play();
    }

    private void wcAbrirFormulario() {
        wcNotificador.wcAbrirPanel(wcPanelFormulario);
        Platform.runLater(wcTxtRuc::requestFocus);
    }

    private void wcLimpiarFormulario() {
        WcFormularios.wcLimpiarErrores(wcTxtRuc, wcTxtRazonSocial, wcTxtDireccion, wcTxtTelefono);
        wcTxtRuc.clear();
        wcTxtRazonSocial.clear();
        wcTxtDireccion.clear();
        wcTxtTelefono.clear();
    }

    private static String wcTextoONulo(TextField wcCampo) {
        String wcTexto = wcCampo.getText().trim();
        return wcTexto.isEmpty() ? null : wcTexto;
    }
}
