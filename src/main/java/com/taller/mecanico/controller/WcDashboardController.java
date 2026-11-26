package com.taller.mecanico.controller;

import atlantafx.base.theme.Styles;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.service.WcComprobantePdfService;
import com.taller.mecanico.service.WcDashboardService;
import com.taller.mecanico.service.WcOrdenServicioService;
import com.taller.mecanico.service.WcOrdenServicioService.WcResumenTaller;
import com.taller.mecanico.service.WcPeriodo;
import com.taller.mecanico.service.WcReporteExcelService;
import com.taller.mecanico.service.WcReporteVentasService;
import com.taller.mecanico.service.WcReporteVentasService.WcClienteFrecuente;
import com.taller.mecanico.service.WcReporteVentasService.WcRepuestoVendido;
import com.taller.mecanico.service.WcReporteVentasService.WcPuntoSerie;
import com.taller.mecanico.service.WcReporteVentasService.WcResumenVentas;
import com.taller.mecanico.service.WcDashboardService.WcRangoPrecio;
import com.taller.mecanico.service.WcDashboardService.WcResumen;
import com.taller.mecanico.service.WcDashboardService.WcSerieCategoria;
import com.taller.mecanico.ui.WcExportador;
import com.taller.mecanico.ui.WcRefrescable;
import com.taller.mecanico.ui.WcTablas;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class WcDashboardController implements WcRefrescable {
    private static final int WC_MAX_CATEGORIAS = 6;

    private static final int WC_MAX_BARRAS = 8;
    private static final Duration WC_DURACION = Duration.millis(900);
    private static final Interpolator WC_SUAVE = Interpolator.SPLINE(0.25, 0.1, 0.25, 1);
    private static final DateTimeFormatter WC_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter WC_DIA = DateTimeFormatter.ofPattern("dd MMM", WcTablas.WC_LOCALE);
    private static final DateTimeFormatter WC_MES = DateTimeFormatter.ofPattern("MMM yy", WcTablas.WC_LOCALE);
    private static final DateTimeFormatter WC_FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final int WC_MAX_TOP = 5;

    private final WcDashboardService wcService;
    private final WcOrdenServicioService wcOrdenService;
    private final WcReporteVentasService wcReporteVentas;
    private final WcReporteExcelService wcExcelService;
    private final WcComprobantePdfService wcPdfService;
    private final WcExportador wcExportador;
    private final WcMainController wcMainController;

    @FXML private ComboBox<WcPeriodo> wcCmbPeriodo;
    @FXML private Label wcKpiEnTaller;
    @FXML private Label wcKpiListos;
    @FXML private Label wcKpiOrdenesMes;
    @FXML private Label wcKpiFacturadoTaller;
    @FXML private Label wcKpiVentas;
    @FXML private Label wcKpiVentasVariacion;
    @FXML private Label wcKpiVentasDetalle;
    @FXML private Label wcKpiComprobantes;
    @FXML private Label wcKpiComprobantesDetalle;
    @FXML private Label wcKpiTicket;
    @FXML private Label wcKpiTicketDetalle;
    @FXML private Label wcKpiIgv;
    @FXML private Label wcKpiIgvDetalle;
    @FXML private Label wcLblSerieSubtitulo;
    @FXML private AreaChart<Number, Number> wcGraficoVentas;
    @FXML private NumberAxis wcEjeFechas;
    @FXML private NumberAxis wcEjeVentas;
    @FXML private VBox wcListaTopVendidos;
    @FXML private VBox wcListaUltimas;
    @FXML private VBox wcListaClientes;

    private List<String> wcEtiquetasSerie = List.of();

    @FXML private Label wcLblActualizado;
    @FXML private FontIcon wcIconoActualizar;

    @FXML private Label wcKpiClientes;
    @FXML private Label wcKpiClientesDetalle;
    @FXML private Label wcKpiCategorias;
    @FXML private Label wcKpiCategoriasDetalle;
    @FXML private Label wcKpiRepuestos;
    @FXML private Label wcKpiRepuestosDetalle;
    @FXML private Label wcKpiValor;
    @FXML private Label wcKpiValorDetalle;

    @FXML private StackPane wcContenedorDona;
    @FXML private PieChart wcGraficoCategorias;
    @FXML private Circle wcHuecoDona;
    @FXML private Label wcLblTotalDona;
    @FXML private VBox wcLeyendaDona;

    @FXML private BarChart<Number, String> wcGraficoPromedio;
    @FXML private NumberAxis wcEjePromedio;
    @FXML private BarChart<String, Number> wcGraficoRangos;
    @FXML private NumberAxis wcEjeRangos;

    @FXML private VBox wcListaTop;
    @FXML private HBox wcMedidoresContacto;

    @FXML
    public void initialize() {
        wcHuecoDona.radiusProperty().bind(Bindings.min(
                wcGraficoCategorias.widthProperty(), wcGraficoCategorias.heightProperty()).multiply(0.5 * 0.62));

        for (NumberAxis wcEje : List.of(wcEjePromedio, wcEjeRangos)) {
            wcEje.setAutoRanging(false);
            wcEje.setLowerBound(0);
            wcEje.setAnimated(false);
        }
        wcGraficoPromedio.getYAxis().setAnimated(false);
        wcGraficoRangos.getXAxis().setAnimated(false);
        NumberFormat wcEnteros = NumberFormat.getIntegerInstance(WcTablas.WC_LOCALE);
        wcEjePromedio.setTickLabelFormatter(wcFormato(wcV -> "S/ " + wcEnteros.format(wcV)));

        wcCmbPeriodo.getItems().setAll(WcPeriodo.values());
        wcCmbPeriodo.setValue(WcPeriodo.WC_ULTIMOS_30);
        wcCmbPeriodo.valueProperty().addListener((wcObs, wcA, wcB) -> wcMostrarVentas());

        for (NumberAxis wcEje : List.of(wcEjeFechas, wcEjeVentas)) {
            wcEje.setAutoRanging(false);
            wcEje.setAnimated(false);
        }
        wcEjeVentas.setLowerBound(0);
        wcEjeVentas.setTickLabelFormatter(wcFormato(wcV -> "S/ " + wcEnteros.format(wcV)));

        wcEjeFechas.setTickLabelFormatter(wcFormato(wcV -> {
            int wcI = (int) Math.round(wcV.doubleValue());
            return Math.abs(wcV.doubleValue() - wcI) < 0.01 && wcI >= 0 && wcI < wcEtiquetasSerie.size()
                    ? wcEtiquetasSerie.get(wcI) : "";
        }));
    }

    @Override
    public void wcRefrescar() {
        wcMostrarTaller(wcOrdenService.wcResumen());
        WcResumen wcR = wcService.wcObtenerResumen();
        wcMostrarIndicadores(wcR);
        wcMostrarDona(wcR);
        wcMostrarPromedios(wcR.wcPorCategoria());
        wcMostrarRangos(wcR.wcRangosPrecio());
        wcMostrarTop(wcR.wcMasCaros());
        wcMostrarContacto(wcR);
        wcMostrarVentas();
        wcLblActualizado.setText("Resumen general · actualizado a las " + LocalTime.now().format(WC_HORA));
    }

    @FXML
    private void wcOnActualizar() {
        RotateTransition wcGiro = new RotateTransition(Duration.millis(600), wcIconoActualizar);
        wcGiro.setByAngle(360);
        wcGiro.setInterpolator(WC_SUAVE);
        wcGiro.play();
        wcRefrescar();
    }

    @FXML
    private void wcOnExportarExcel() {
        LocalDate wcHasta = LocalDate.now();
        LocalDate wcDesde = wcCmbPeriodo.getValue().wcDesde(wcHasta);
        wcExportador.wcGuardar(wcCmbPeriodo.getScene().getWindow(), "Exportar reporte de ventas",
                "reporte-ventas_" + wcDesde + "_" + wcHasta, WcExportador.WcFormato.WC_EXCEL,
                wcDestino -> wcExcelService.wcGenerar(wcDesde, wcHasta, wcDestino));
    }

    @FXML
    private void wcOnVerVentas() {
        wcMainController.wcIrA("ventas-view");
    }

    private void wcMostrarTaller(WcResumenTaller wcR) {
        wcAnimarNumero(wcKpiEnTaller, wcR.wcEnTaller(), wcV -> String.format(WcTablas.WC_LOCALE, "%,.0f", wcV));
        wcAnimarNumero(wcKpiListos, wcR.wcListasParaEntrega(), wcV -> String.format(WcTablas.WC_LOCALE, "%,.0f", wcV));
        wcAnimarNumero(wcKpiOrdenesMes, wcR.wcIngresadasEnElMes(), wcV -> String.format(WcTablas.WC_LOCALE, "%,.0f", wcV));
        wcAnimarNumero(wcKpiFacturadoTaller, wcR.wcFacturadoEnElMes().doubleValue(), wcV -> WcTablas.WC_MONEDA.format(wcV));
    }

    private void wcMostrarVentas() {
        LocalDate wcHoy = LocalDate.now();
        WcResumenVentas wcR = wcReporteVentas.wcResumen(wcCmbPeriodo.getValue().wcDesde(wcHoy), wcHoy);

        wcAnimarNumero(wcKpiVentas, wcR.wcTotal().doubleValue(), wcV -> WcTablas.WC_MONEDA.format(wcV));
        wcAnimarNumero(wcKpiComprobantes, wcR.wcComprobantes(), wcV -> String.format(WcTablas.WC_LOCALE, "%,.0f", wcV));
        wcAnimarNumero(wcKpiTicket, wcR.wcTicketPromedio().doubleValue(), wcV -> WcTablas.WC_MONEDA.format(wcV));
        wcAnimarNumero(wcKpiIgv, wcR.wcIgv().doubleValue(), wcV -> WcTablas.WC_MONEDA.format(wcV));

        if (wcR.wcVariacionTotal() == null) {
            wcKpiVentasVariacion.setVisible(false);
            wcKpiVentasVariacion.setManaged(false);
            wcKpiVentasDetalle.setText("Sin ventas en el periodo anterior");
        } else {
            double var = wcR.wcVariacionTotal();
            wcKpiVentasVariacion.setVisible(true);
            wcKpiVentasVariacion.setManaged(true);
            wcKpiVentasVariacion.setText(String.format(WcTablas.WC_LOCALE, "%s %.1f%%", var >= 0 ? "▲" : "▼", Math.abs(var)));
            wcKpiVentasVariacion.getStyleClass().setAll("label", "variacion", var >= 0 ? "positiva" : "negativa");
            wcKpiVentasDetalle.setText("vs. periodo anterior");
        }
        wcKpiComprobantesDetalle.setText(wcR.wcAnuladas() == 0
                ? "Ningun comprobante anulado"
                : wcR.wcAnuladas() + (wcR.wcAnuladas() == 1 ? " anulado" : " anulados"));
        wcKpiTicketDetalle.setText("Promedio por comprobante emitido");
        wcKpiIgvDetalle.setText("Base imponible " + WcTablas.WC_MONEDA.format(wcR.wcBaseImponible()));

        wcMostrarSerie(wcR);
        wcMostrarTopVendidos(wcR.wcTopRepuestos());
        wcMostrarUltimas(wcR.wcUltimas());
        wcMostrarClientes(wcR.wcTopClientes());
    }

    private void wcMostrarSerie(WcResumenVentas wcR) {
        List<WcPuntoSerie> wcPuntos = wcR.wcSerie();
        DateTimeFormatter wcFormato = wcR.wcSeriePorMes() ? WC_MES : WC_DIA;
        wcEtiquetasSerie = wcPuntos.stream().map(wcP -> wcP.wcFecha().format(wcFormato)).toList();
        wcLblSerieSubtitulo.setText(wcR.wcSeriePorMes() ? "Total vendido por mes (S/)" : "Total vendido por dia (S/)");

        XYChart.Series<Number, Number> wcSerie = new XYChart.Series<>();
        for (int wcI = 0; wcI < wcPuntos.size(); wcI++) {
            wcSerie.getData().add(new XYChart.Data<>(wcI, 0.0));
        }
        wcGraficoVentas.getData().setAll(List.of(wcSerie));

        int wcN = wcPuntos.size();
        wcEjeFechas.setLowerBound(0);
        wcEjeFechas.setUpperBound(Math.max(1, wcN - 1));
        wcEjeFechas.setTickUnit(Math.max(1, Math.ceil((wcN - 1) / 7.0)));
        double wcMaximo = wcPuntos.stream().mapToDouble(wcP -> wcP.wcTotal().doubleValue()).max().orElse(0);
        wcAjustarEje(wcEjeVentas, wcMaximo, false);

        Timeline wcCrecimiento = new Timeline();
        for (int wcI = 0; wcI < wcN; wcI++) {
            XYChart.Data<Number, Number> wcD = wcSerie.getData().get(wcI);
            WcPuntoSerie wcP = wcPuntos.get(wcI);
            wcCrecimiento.getKeyFrames().add(new KeyFrame(WC_DURACION,
                    new KeyValue(wcD.YValueProperty(), wcP.wcTotal().doubleValue(), WC_SUAVE)));
            wcInstalarTooltip(wcD.getNode(), wcP.wcFecha().format(wcR.wcSeriePorMes() ? WC_MES : DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + "\n" + WcTablas.WC_MONEDA.format(wcP.wcTotal()) + " · " + wcP.wcComprobantes()
                    + (wcP.wcComprobantes() == 1 ? " comprobante" : " comprobantes"));
        }
        wcCrecimiento.play();
    }

    private void wcMostrarTopVendidos(List<WcRepuestoVendido> wcRepuestos) {
        wcListaTopVendidos.getChildren().clear();
        if (wcRepuestos.isEmpty()) {
            wcListaTopVendidos.getChildren().add(wcTextoVacio("Sin ventas en el periodo."));
            return;
        }
        double wcMaximo = wcRepuestos.get(0).wcImporte().doubleValue();
        int wcPosicion = 1;
        for (WcRepuestoVendido wcP : wcRepuestos.stream().limit(WC_MAX_TOP).toList()) {
            wcListaTopVendidos.getChildren().add(wcFilaRanking(wcPosicion++, wcP.wcNombre(),
                    wcP.wcUnidades() + (wcP.wcUnidades() == 1 ? " unidad" : " unidades"),
                    WcTablas.WC_MONEDA.format(wcP.wcImporte()), wcMaximo == 0 ? 0 : wcP.wcImporte().doubleValue() / wcMaximo));
        }
    }

    private void wcMostrarClientes(List<WcClienteFrecuente> wcClientes) {
        wcListaClientes.getChildren().clear();
        if (wcClientes.isEmpty()) {
            wcListaClientes.getChildren().add(wcTextoVacio("Sin ventas en el periodo."));
            return;
        }
        double wcMaximo = wcClientes.get(0).wcImporte().doubleValue();
        int wcPosicion = 1;
        for (WcClienteFrecuente wcC : wcClientes.stream().limit(WC_MAX_TOP).toList()) {
            wcListaClientes.getChildren().add(wcFilaRanking(wcPosicion++, wcC.wcNombre(),
                    "DNI " + wcC.wcDni() + " · " + wcC.wcCompras() + (wcC.wcCompras() == 1 ? " compra" : " compras"),
                    WcTablas.WC_MONEDA.format(wcC.wcImporte()), wcMaximo == 0 ? 0 : wcC.wcImporte().doubleValue() / wcMaximo));
        }
    }

    private void wcMostrarUltimas(List<WcVenta> wcVentas) {
        wcListaUltimas.getChildren().clear();
        if (wcVentas.isEmpty()) {
            wcListaUltimas.getChildren().add(wcTextoVacio("Aun no hay ventas registradas."));
            return;
        }
        for (WcVenta wcV : wcVentas) {
            StackPane wcIcono = new StackPane(new FontIcon(Feather.FILE_TEXT));
            wcIcono.getStyleClass().add("venta-icono");

            Label wcComprobante = new Label(wcV.getWcComprobante());
            wcComprobante.getStyleClass().add("ranking-nombre");
            Label wcCliente = new Label(wcV.getWcCliente().getWcNombreCompleto() + " · " + wcCuando(wcV.getWcFecha()));
            wcCliente.getStyleClass().add("ranking-detalle");
            VBox wcTextos = new VBox(1, wcComprobante, wcCliente);
            HBox.setHgrow(wcTextos, Priority.ALWAYS);

            Label wcTotal = new Label(WcTablas.WC_MONEDA.format(wcV.getWcTotal()));
            wcTotal.getStyleClass().add("ranking-precio");
            Label wcEstado = new Label(wcV.getWcEstado().name().replace("WC_", ""));
            wcEstado.getStyleClass().addAll("chip", "chip-mini", wcV.isWcAnulada() ? "peligro" : "exito");
            VBox wcDerecha = new VBox(3, wcTotal, wcEstado);
            wcDerecha.setAlignment(Pos.CENTER_RIGHT);

            Button wcImprimir = new Button(null, new FontIcon(Feather.PRINTER));
            wcImprimir.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
            wcInstalarTooltip(wcImprimir, "Imprimir comprobante (PDF)");
            Long wcId = wcV.getWcId();
            wcImprimir.setOnAction(wcE -> wcExportador.wcAbrirTemporal(wcV.getWcComprobante(), WcExportador.WcFormato.WC_PDF,
                    wcDestino -> wcPdfService.wcGenerar(wcId, wcDestino)));

            HBox wcFila = new HBox(12, wcIcono, wcTextos, wcDerecha, wcImprimir);
            wcFila.setAlignment(Pos.CENTER_LEFT);
            wcFila.getStyleClass().add("venta-fila");
            if (wcV.isWcAnulada()) {
                wcFila.getStyleClass().add("anulada");
            }
            wcListaUltimas.getChildren().add(wcFila);
        }
    }

    private Node wcFilaRanking(int wcPosicion, String wcTitulo, String wcDetalle, String wcValor, double wcFraccion) {
        Label wcPos = new Label(String.valueOf(wcPosicion));
        wcPos.getStyleClass().add("ranking-posicion");
        Label wcNombre = new Label(wcTitulo);
        wcNombre.getStyleClass().add("ranking-nombre");
        Label wcSub = new Label(wcDetalle);
        wcSub.getStyleClass().add("ranking-detalle");
        VBox wcTextos = new VBox(1, wcNombre, wcSub);
        HBox.setHgrow(wcTextos, Priority.ALWAYS);
        Label wcImporte = new Label(wcValor);
        wcImporte.getStyleClass().add("ranking-precio");
        wcImporte.setMinWidth(Region.USE_PREF_SIZE);

        HBox wcCabecera = new HBox(12, wcPos, wcTextos, wcImporte);
        wcCabecera.setAlignment(Pos.CENTER_LEFT);
        ProgressBar wcBarra = new ProgressBar(0);
        wcBarra.getStyleClass().add("barra-fina");
        wcBarra.setMaxWidth(Double.MAX_VALUE);
        wcAnimarProgreso(wcBarra, wcFraccion);

        VBox wcFila = new VBox(8, wcCabecera, wcBarra);
        wcFila.getStyleClass().add("ranking-fila");
        return wcFila;
    }

    private static Label wcTextoVacio(String wcTexto) {
        Label wcVacio = new Label(wcTexto);
        wcVacio.getStyleClass().add("texto-vacio");
        return wcVacio;
    }

    private static String wcCuando(LocalDateTime wcFecha) {
        LocalDateTime wcAhora = LocalDateTime.now();
        long wcMinutos = java.time.Duration.between(wcFecha, wcAhora).toMinutes();
        if (wcMinutos >= 0 && wcMinutos < 60) {
            return wcMinutos <= 1 ? "hace un momento" : "hace " + wcMinutos + " min";
        }
        if (wcFecha.toLocalDate().equals(wcAhora.toLocalDate())) {
            return "hoy " + wcFecha.format(WC_HORA);
        }
        if (wcFecha.toLocalDate().equals(wcAhora.toLocalDate().minusDays(1))) {
            return "ayer " + wcFecha.format(WC_HORA);
        }
        return wcFecha.format(WC_FECHA_CORTA);
    }

    private void wcMostrarIndicadores(WcResumen wcR) {
        wcAnimarNumero(wcKpiClientes, wcR.wcClientes(), wcV -> String.format(WcTablas.WC_LOCALE, "%,.0f", wcV));
        wcAnimarNumero(wcKpiCategorias, wcR.wcCategorias(), wcV -> String.format(WcTablas.WC_LOCALE, "%,.0f", wcV));
        wcAnimarNumero(wcKpiRepuestos, wcR.wcRepuestos(), wcV -> String.format(WcTablas.WC_LOCALE, "%,.0f", wcV));
        wcAnimarNumero(wcKpiValor, wcR.wcValorCatalogo().doubleValue(), wcV -> WcTablas.WC_MONEDA.format(wcV));

        wcKpiClientesDetalle.setText(wcR.wcClientesContactoCompleto() + " con contacto completo");
        wcKpiCategoriasDetalle.setText(wcR.wcCategorias() == 0
                ? "Sin categorias registradas"
                : String.format(WcTablas.WC_LOCALE, "%.1f repuestos por categoria", (double) wcR.wcRepuestos() / wcR.wcCategorias()));
        long wcSinCategoria = wcR.wcPorCategoria().stream()
                .filter(wcS -> wcS.wcNombre().equals("Sin categoria"))
                .mapToLong(WcSerieCategoria::wcRepuestos).sum();
        wcKpiRepuestosDetalle.setText(wcSinCategoria == 0
                ? "Todos tienen categoria"
                : wcSinCategoria + " sin categoria asignada");
        wcKpiValorDetalle.setText("Precio promedio " + WcTablas.WC_MONEDA.format(wcR.wcPrecioPromedio()));
    }

    private void wcMostrarDona(WcResumen wcR) {
        List<WcSerieCategoria> wcPrincipales = new ArrayList<>(
                wcR.wcPorCategoria().subList(0, Math.min(WC_MAX_CATEGORIAS, wcR.wcPorCategoria().size())));
        long wcOtras = wcR.wcPorCategoria().stream().skip(WC_MAX_CATEGORIAS).mapToLong(WcSerieCategoria::wcRepuestos).sum();

        wcPrincipales.sort(Comparator.comparing((WcSerieCategoria wcS) -> wcS.wcNombre().equals("Sin categoria"))
                .thenComparing(WcSerieCategoria::wcNombre, String.CASE_INSENSITIVE_ORDER));

        List<PieChart.Data> wcDatos = new ArrayList<>();
        List<String> wcColores = new ArrayList<>();
        for (int wcI = 0; wcI < wcPrincipales.size(); wcI++) {
            wcDatos.add(new PieChart.Data(wcPrincipales.get(wcI).wcNombre(), wcPrincipales.get(wcI).wcRepuestos()));
            wcColores.add("-serie-" + (wcI + 1));
        }
        if (wcOtras > 0) {
            wcDatos.add(new PieChart.Data("Otras", wcOtras));
            wcColores.add("-serie-otros");
        }

        wcGraficoCategorias.setData(FXCollections.observableArrayList(wcDatos));
        wcLeyendaDona.getChildren().clear();
        wcLblTotalDona.setText(String.valueOf(wcR.wcRepuestos()));

        if (wcDatos.isEmpty()) {
            Label wcVacio = new Label("Aun no hay repuestos registrados.");
            wcVacio.getStyleClass().add("texto-vacio");
            wcLeyendaDona.getChildren().add(wcVacio);
            return;
        }

        for (int wcI = 0; wcI < wcDatos.size(); wcI++) {
            PieChart.Data wcD = wcDatos.get(wcI);
            String wcColor = wcColores.get(wcI);
            double wcPorcentaje = 100.0 * wcD.getPieValue() / wcR.wcRepuestos();
            wcD.getNode().setStyle("-fx-pie-color: " + wcColor + ";");
            wcInstalarTooltip(wcD.getNode(), wcD.getName() + "\n"
                    + (long) wcD.getPieValue() + " repuestos · " + String.format(WcTablas.WC_LOCALE, "%.0f%%", wcPorcentaje));
            wcLeyendaDona.getChildren().add(wcFilaLeyenda(wcColor, wcD.getName(), (long) wcD.getPieValue(), wcPorcentaje));
        }
    }

    private HBox wcFilaLeyenda(String wcColor, String wcTexto, long wcValor, double wcPorcentaje) {
        Region wcPunto = new Region();
        wcPunto.getStyleClass().add("leyenda-punto");
        wcPunto.setStyle("-fx-background-color: " + wcColor + ";");
        Label wcNombre = new Label(wcTexto);
        wcNombre.getStyleClass().add("leyenda-nombre");
        wcNombre.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wcNombre, Priority.ALWAYS);
        Label wcCantidad = new Label(String.valueOf(wcValor));
        wcCantidad.getStyleClass().add("leyenda-valor");
        wcCantidad.setMinWidth(Region.USE_PREF_SIZE);
        Label wcPct = new Label(String.format(WcTablas.WC_LOCALE, "%.0f%%", wcPorcentaje));
        wcPct.getStyleClass().add("leyenda-porcentaje");
        wcPct.setMinWidth(Region.USE_PREF_SIZE);
        HBox wcFila = new HBox(10, wcPunto, wcNombre, wcCantidad, wcPct);
        wcFila.setAlignment(Pos.CENTER_LEFT);
        wcFila.getStyleClass().add("leyenda-fila");
        return wcFila;
    }


    private void wcMostrarPromedios(List<WcSerieCategoria> wcCategorias) {
        List<WcSerieCategoria> wcVisibles = wcCategorias.stream()
                .limit(WC_MAX_BARRAS)
                .sorted(Comparator.comparingDouble(WcSerieCategoria::wcPrecioPromedio))
                .toList();

        XYChart.Series<Number, String> wcSerie = new XYChart.Series<>();
        for (WcSerieCategoria wcC : wcVisibles) {
            wcSerie.getData().add(new XYChart.Data<>(0.0, wcC.wcNombre()));
        }
        wcGraficoPromedio.getData().setAll(List.of(wcSerie));

        double wcMaximo = wcVisibles.stream().mapToDouble(WcSerieCategoria::wcPrecioPromedio).max().orElse(0);
        wcAjustarEje(wcEjePromedio, wcMaximo, false);

        Timeline wcCrecimiento = new Timeline();
        for (int wcI = 0; wcI < wcVisibles.size(); wcI++) {
            XYChart.Data<Number, String> wcD = wcSerie.getData().get(wcI);
            WcSerieCategoria wcC = wcVisibles.get(wcI);
            wcCrecimiento.getKeyFrames().add(new KeyFrame(WC_DURACION,
                    new KeyValue(wcD.XValueProperty(), wcC.wcPrecioPromedio(), WC_SUAVE)));
            wcInstalarTooltip(wcD.getNode(), wcC.wcNombre() + "\nPromedio " + WcTablas.WC_MONEDA.format(wcC.wcPrecioPromedio())
                    + " · " + wcC.wcRepuestos() + " repuestos");
        }
        wcCrecimiento.play();
    }

    private void wcMostrarRangos(List<WcRangoPrecio> wcRangos) {
        XYChart.Series<String, Number> wcSerie = new XYChart.Series<>();
        for (WcRangoPrecio wcR : wcRangos) {
            wcSerie.getData().add(new XYChart.Data<>(wcR.wcEtiqueta(), 0.0));
        }
        wcGraficoRangos.getData().setAll(List.of(wcSerie));

        double wcMaximo = wcRangos.stream().mapToLong(WcRangoPrecio::wcCantidad).max().orElse(0);
        wcAjustarEje(wcEjeRangos, wcMaximo, true);

        Timeline wcCrecimiento = new Timeline();
        for (int wcI = 0; wcI < wcRangos.size(); wcI++) {
            XYChart.Data<String, Number> wcD = wcSerie.getData().get(wcI);
            WcRangoPrecio wcR = wcRangos.get(wcI);
            wcCrecimiento.getKeyFrames().add(new KeyFrame(WC_DURACION,
                    new KeyValue(wcD.YValueProperty(), (double) wcR.wcCantidad(), WC_SUAVE)));
            wcInstalarTooltip(wcD.getNode(), "S/ " + wcR.wcEtiqueta() + "\n" + wcR.wcCantidad() + " repuestos");
        }
        wcCrecimiento.play();
    }

    private void wcMostrarTop(List<WcRepuesto> wcRepuestos) {
        wcListaTop.getChildren().clear();
        if (wcRepuestos.isEmpty()) {
            Label wcVacio = new Label("Aun no hay repuestos registrados.");
            wcVacio.getStyleClass().add("texto-vacio");
            wcListaTop.getChildren().add(wcVacio);
            return;
        }

        double wcMaximo = wcRepuestos.get(0).getWcPrecio().doubleValue();
        int wcPosicion = 1;
        for (WcRepuesto wcP : wcRepuestos) {
            Label wcPos = new Label(String.valueOf(wcPosicion++));
            wcPos.getStyleClass().add("ranking-posicion");

            Label wcNombre = new Label(wcP.getWcNombre());
            wcNombre.getStyleClass().add("ranking-nombre");
            Label wcCategoria = new Label(wcP.getWcCategoria() != null ? wcP.getWcCategoria().getWcNombre() : "Sin categoria");
            wcCategoria.getStyleClass().add("ranking-detalle");
            VBox wcTextos = new VBox(1, wcNombre, wcCategoria);
            HBox.setHgrow(wcTextos, Priority.ALWAYS);

            Label wcPrecio = new Label(WcTablas.WC_MONEDA.format(wcP.getWcPrecio()));
            wcPrecio.getStyleClass().add("ranking-precio");

            HBox wcCabecera = new HBox(12, wcPos, wcTextos, wcPrecio);
            wcCabecera.setAlignment(Pos.CENTER_LEFT);

            ProgressBar wcBarra = new ProgressBar(0);
            wcBarra.getStyleClass().add("barra-fina");
            wcBarra.setMaxWidth(Double.MAX_VALUE);
            wcAnimarProgreso(wcBarra, wcMaximo == 0 ? 0 : wcP.getWcPrecio().doubleValue() / wcMaximo);

            VBox wcFila = new VBox(8, wcCabecera, wcBarra);
            wcFila.getStyleClass().add("ranking-fila");
            wcListaTop.getChildren().add(wcFila);
        }
    }

    private void wcMostrarContacto(WcResumen wcR) {
        wcMedidoresContacto.getChildren().setAll(
                wcMedidor("Con correo electronico", wcR.wcClientesConEmail(), wcR.wcClientes()),
                wcMedidor("Con telefono", wcR.wcClientesConTelefono(), wcR.wcClientes()),
                wcMedidor("Contacto completo", wcR.wcClientesContactoCompleto(), wcR.wcClientes()));
    }

    private Node wcMedidor(String wcTitulo, long wcCantidad, long wcTotal) {
        double wcFraccion = wcTotal == 0 ? 0 : (double) wcCantidad / wcTotal;

        Label wcLblTitulo = new Label(wcTitulo);
        wcLblTitulo.getStyleClass().add("medidor-titulo");
        Label wcLblPct = new Label("0%");
        wcLblPct.getStyleClass().add("medidor-valor");
        wcAnimarNumero(wcLblPct, wcFraccion * 100, wcV -> String.format(WcTablas.WC_LOCALE, "%.0f%%", wcV));

        ProgressBar wcBarra = new ProgressBar(0);
        wcBarra.getStyleClass().add("barra-fina");
        wcBarra.setMaxWidth(Double.MAX_VALUE);
        wcAnimarProgreso(wcBarra, wcFraccion);

        Label wcDetalle = new Label(wcCantidad + " de " + wcTotal + " clientes");
        wcDetalle.getStyleClass().add("medidor-detalle");

        VBox wcCaja = new VBox(6, wcLblTitulo, wcLblPct, wcBarra, wcDetalle);
        wcCaja.getStyleClass().add("medidor");
        HBox.setHgrow(wcCaja, Priority.ALWAYS);
        wcCaja.setMaxWidth(Double.MAX_VALUE);
        return wcCaja;
    }

    private void wcAnimarNumero(Label wcEtiqueta, double wcDestino, DoubleFunction<String> wcFormato) {
        DoubleProperty wcValor = new SimpleDoubleProperty(0);
        wcValor.addListener((wcObs, wcA, wcB) -> wcEtiqueta.setText(wcFormato.apply(wcB.doubleValue())));
        wcEtiqueta.setText(wcFormato.apply(0));
        new Timeline(new KeyFrame(WC_DURACION, new KeyValue(wcValor, wcDestino, WC_SUAVE))).play();
    }

    private void wcAnimarProgreso(ProgressBar wcBarra, double wcDestino) {
        new Timeline(new KeyFrame(WC_DURACION, new KeyValue(wcBarra.progressProperty(), wcDestino, WC_SUAVE))).play();
    }

    private static StringConverter<Number> wcFormato(Function<Number, String> wcF) {
        return new StringConverter<>() {
            @Override
            public String toString(Number wcValor) {
                return wcF.apply(wcValor);
            }

            @Override
            public Number fromString(String wcTexto) {
                return null;
            }
        };
    }

    private void wcInstalarTooltip(Node wcNodo, String wcTexto) {
        Tooltip wcT = new Tooltip(wcTexto);
        wcT.setShowDelay(Duration.millis(80));
        wcT.setHideDelay(Duration.millis(80));
        Tooltip.install(wcNodo, wcT);
    }

    private static void wcAjustarEje(NumberAxis wcEje, double wcMaximo, boolean wcEnteros) {
        double wcSuperior;
        if (wcMaximo <= 0) {
            wcSuperior = wcEnteros ? 5 : 100;
        } else {
            double wcEscala = Math.pow(10, Math.floor(Math.log10(wcMaximo)));
            double wcF = wcMaximo / wcEscala;
            double wcRedondo = wcF <= 1 ? 1 : wcF <= 2 ? 2 : wcF <= 2.5 ? 2.5 : wcF <= 5 ? 5 : 10;
            wcSuperior = wcRedondo * wcEscala;
        }
        if (wcEnteros) {
            wcSuperior = Math.max(5, Math.ceil(wcSuperior));
            if (wcSuperior % 5 != 0) {
                wcSuperior = Math.ceil(wcSuperior / 5) * 5;
            }
        }
        wcEje.setUpperBound(wcSuperior);
        wcEje.setTickUnit(wcSuperior / 5);
    }
}
