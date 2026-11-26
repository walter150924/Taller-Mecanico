package com.taller.mecanico.service;

import com.taller.mecanico.config.WcDatosEmpresa;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.model.WcVentaDetalle;
import com.taller.mecanico.service.WcReporteVentasService.WcClienteFrecuente;
import com.taller.mecanico.service.WcReporteVentasService.WcDatosReporte;
import com.taller.mecanico.service.WcReporteVentasService.WcRepuestoVendido;
import com.taller.mecanico.service.WcReporteVentasService.WcPuntoSerie;
import com.taller.mecanico.service.WcReporteVentasService.WcResumenVentas;
import com.taller.mecanico.service.WcLibroExcel.WcEstilos;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.taller.mecanico.service.WcLibroExcel.*;

@Service
@RequiredArgsConstructor
public class WcReporteExcelService {
    private static final DateTimeFormatter WC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter WC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WcReporteVentasService wcReporteService;
    private final WcDatosEmpresa wcEmpresa;

    public void wcGenerar(LocalDate wcDesde, LocalDate wcHasta, File wcDestino) throws IOException {
        WcDatosReporte wcDatos = wcReporteService.wcDatosReporte(wcDesde, wcHasta);
        try (OutputStream wcSalida = new FileOutputStream(wcDestino)) {
            wcEscribir(wcDatos, wcSalida);
        }
    }

    void wcEscribir(WcDatosReporte wcDatos, OutputStream wcSalida) throws IOException {
        try (XSSFWorkbook wcLibro = new XSSFWorkbook()) {
            WcEstilos wcE = new WcEstilos(wcLibro);
            wcHojaResumen(wcLibro, wcE, wcDatos.wcResumen());
            wcHojaVentas(wcLibro, wcE, wcDatos.wcVentas());
            wcHojaDetalle(wcLibro, wcE, wcDatos.wcDetalles());
            wcHojaRepuestos(wcLibro, wcE, wcDatos.wcResumen().wcTopRepuestos());
            wcHojaClientes(wcLibro, wcE, wcDatos.wcResumen().wcTopClientes());
            wcLibro.getProperties().getCoreProperties().setTitle("Reporte de ventas");
            wcLibro.getProperties().getCoreProperties().setCreator(wcEmpresa.getWcRazonSocial());
            wcLibro.write(wcSalida);
        }
    }

    private void wcHojaResumen(XSSFWorkbook wcLibro, WcEstilos wcE, WcResumenVentas wcR) {
        Sheet wcHoja = wcLibro.createSheet("Resumen");
        wcHoja.setDisplayGridlines(false);
        wcHoja.setColumnWidth(0, 30 * 256);
        wcHoja.setColumnWidth(1, 20 * 256);
        wcHoja.setColumnWidth(2, 20 * 256);

        int wcF = 0;
        wcTexto(wcHoja.createRow(wcF++), 0, "Reporte de ventas", wcE.wcTitulo);
        wcTexto(wcHoja.createRow(wcF++), 0, wcEmpresa.getWcRazonSocial() + "  ·  RUC " + wcEmpresa.getWcRuc(), wcE.wcSubtitulo);
        wcTexto(wcHoja.createRow(wcF++), 0, "Periodo: " + wcR.wcDesde().format(WC_FECHA) + " al " + wcR.wcHasta().format(WC_FECHA)
                + "   ·   Generado: " + LocalDateTime.now().format(WC_FECHA_HORA), wcE.wcSubtitulo);
        wcF++;

        wcCabecera(wcHoja.createRow(wcF++), wcE, "Indicador", "Valor");
        Object[][] wcKpis = {
                {"Total vendido", wcR.wcTotal()},
                {"Base imponible", wcR.wcBaseImponible()},
                {"IGV", wcR.wcIgv()},
                {"Comprobantes emitidos", wcR.wcComprobantes()},
                {"Comprobantes anulados", wcR.wcAnuladas()},
                {"Ticket promedio", wcR.wcTicketPromedio()},
        };
        for (Object[] wcK : wcKpis) {
            Row wcFila = wcHoja.createRow(wcF++);
            wcTexto(wcFila, 0, (String) wcK[0], wcE.wcCeldaNegrita);
            if (wcK[1] instanceof BigDecimal wcB) {
                wcNumero(wcFila, 1, wcB, wcE.wcMoneda);
            } else {
                wcEntero(wcFila, 1, ((Number) wcK[1]).longValue(), wcE.wcEntero);
            }
        }
        wcF++;

        wcTexto(wcHoja.createRow(wcF++), 0, wcR.wcSeriePorMes() ? "Ventas por mes" : "Ventas por dia", wcE.wcSeccion);
        wcCabecera(wcHoja.createRow(wcF++), wcE, wcR.wcSeriePorMes() ? "Mes" : "Fecha", "Comprobantes", "Total");
        int wcPrimera = wcF;
        for (WcPuntoSerie wcP : wcR.wcSerie()) {
            Row wcFila = wcHoja.createRow(wcF++);
            wcFecha(wcFila, 0, wcP.wcFecha(), wcR.wcSeriePorMes() ? wcE.wcMes : wcE.wcFecha);
            wcEntero(wcFila, 1, wcP.wcComprobantes(), wcE.wcEntero);
            wcNumero(wcFila, 2, wcP.wcTotal(), wcE.wcMoneda);
        }
        wcFilaTotales(wcHoja.createRow(wcF), wcE, wcPrimera, wcF - 1, 0, new int[]{1}, new int[]{2});
    }

    private void wcHojaVentas(XSSFWorkbook wcLibro, WcEstilos wcE, List<WcVenta> wcVentas) {
        Sheet wcHoja = wcLibro.createSheet("Ventas");
        String[] wcTitulos = {"Comprobante", "Tipo", "Fecha", "Cliente", "DNI", "IGV",
                "Base imponible", "IGV (S/)", "Total", "Estado"};
        int[] wcAnchos = {16, 26, 18, 34, 12, 14, 16, 14, 16, 12};
        wcPrepararTabla(wcHoja, wcE, wcTitulos, wcAnchos);

        int wcF = 1;
        for (WcVenta wcV : wcVentas) {
            Row wcFila = wcHoja.createRow(wcF++);
            boolean wcAnulada = wcV.isWcAnulada();
            wcTexto(wcFila, 0, wcV.getWcComprobante(), wcAnulada ? wcE.wcAnulada : wcE.wcCelda);
            wcTexto(wcFila, 1, wcV.getWcTipoComprobante().getWcDescripcion(), wcAnulada ? wcE.wcAnulada : wcE.wcCelda);
            wcFechaHora(wcFila, 2, wcV.getWcFecha(), wcE.wcFechaHora);
            wcTexto(wcFila, 3, wcV.getWcCliente().getWcNombreCompleto(), wcAnulada ? wcE.wcAnulada : wcE.wcCelda);
            wcTexto(wcFila, 4, wcV.getWcCliente().getWcDni(), wcE.wcCelda);
            wcTexto(wcFila, 5, !wcV.isWcAplicaIgv() ? "Exonerado" : wcV.isWcPreciosIncluyenIgv() ? "Incluido" : "Agregado", wcE.wcCelda);

            wcNumero(wcFila, 6, wcAnulada ? BigDecimal.ZERO : wcV.getWcBaseImponible(), wcE.wcMoneda);
            wcNumero(wcFila, 7, wcAnulada ? BigDecimal.ZERO : wcV.getWcIgv(), wcE.wcMoneda);
            wcNumero(wcFila, 8, wcAnulada ? BigDecimal.ZERO : wcV.getWcTotal(), wcE.wcMonedaNegrita);
            wcTexto(wcFila, 9, wcV.getWcEstado().name().replace("WC_", ""), wcAnulada ? wcE.wcEstadoAnulada : wcE.wcEstadoEmitida);
        }
        wcCerrarTabla(wcHoja, wcE, wcTitulos.length, wcF, new int[]{}, new int[]{6, 7, 8});
    }

    private void wcHojaDetalle(XSSFWorkbook wcLibro, WcEstilos wcE, List<WcVentaDetalle> wcDetalles) {
        Sheet wcHoja = wcLibro.createSheet("Detalle");
        String[] wcTitulos = {"Comprobante", "Fecha", "Cliente", "Item", "Codigo", "Repuesto",
                "Cantidad", "P. unitario", "Importe", "Estado"};
        int[] wcAnchos = {16, 18, 30, 7, 12, 40, 11, 14, 14, 12};
        wcPrepararTabla(wcHoja, wcE, wcTitulos, wcAnchos);

        int wcF = 1;
        for (WcVentaDetalle wcD : wcDetalles) {
            WcVenta wcV = wcD.getWcVenta();
            Row wcFila = wcHoja.createRow(wcF++);
            wcTexto(wcFila, 0, wcV.getWcComprobante(), wcE.wcCelda);
            wcFechaHora(wcFila, 1, wcV.getWcFecha(), wcE.wcFechaHora);
            wcTexto(wcFila, 2, wcV.getWcCliente().getWcNombreCompleto(), wcE.wcCelda);
            wcEntero(wcFila, 3, wcD.getWcItem(), wcE.wcEntero);
            wcTexto(wcFila, 4, wcD.getWcCodigoRepuesto(), wcE.wcCelda);
            wcTexto(wcFila, 5, wcD.getWcNombreRepuesto(), wcV.isWcAnulada() ? wcE.wcAnulada : wcE.wcCelda);
            wcEntero(wcFila, 6, wcD.getWcCantidad(), wcE.wcEntero);
            wcNumero(wcFila, 7, wcD.getWcPrecioUnitario(), wcE.wcMoneda);
            wcNumero(wcFila, 8, wcV.isWcAnulada() ? BigDecimal.ZERO : wcD.getWcImporte(), wcE.wcMonedaNegrita);
            wcTexto(wcFila, 9, wcV.getWcEstado().name().replace("WC_", ""), wcV.isWcAnulada() ? wcE.wcEstadoAnulada : wcE.wcEstadoEmitida);
        }
        wcCerrarTabla(wcHoja, wcE, wcTitulos.length, wcF, new int[]{6}, new int[]{8});
    }

    private void wcHojaRepuestos(XSSFWorkbook wcLibro, WcEstilos wcE, List<WcRepuestoVendido> wcRepuestos) {
        Sheet wcHoja = wcLibro.createSheet("Por repuesto");
        String[] wcTitulos = {"#", "Repuesto", "Unidades", "Importe", "% del total"};
        int[] wcAnchos = {6, 46, 12, 16, 13};
        wcPrepararTabla(wcHoja, wcE, wcTitulos, wcAnchos);

        int wcF = 1;
        int wcUltima = wcRepuestos.size();
        for (WcRepuestoVendido wcP : wcRepuestos) {
            Row wcFila = wcHoja.createRow(wcF);
            wcEntero(wcFila, 0, wcF, wcE.wcEntero);
            wcTexto(wcFila, 1, wcP.wcNombre(), wcE.wcCelda);
            wcEntero(wcFila, 2, wcP.wcUnidades(), wcE.wcEntero);
            wcNumero(wcFila, 3, wcP.wcImporte(), wcE.wcMonedaNegrita);
            wcFormula(wcFila, 4, "IFERROR(D" + (wcF + 1) + "/SUM($D$2:$D$" + (wcUltima + 1) + "),0)", wcE.wcPorcentaje);
            wcF++;
        }
        wcCerrarTabla(wcHoja, wcE, wcTitulos.length, wcF, new int[]{2}, new int[]{3});
    }

    private void wcHojaClientes(XSSFWorkbook wcLibro, WcEstilos wcE, List<WcClienteFrecuente> wcClientes) {
        Sheet wcHoja = wcLibro.createSheet("Por cliente");
        String[] wcTitulos = {"#", "Cliente", "DNI", "Compras", "Importe", "Ticket promedio"};
        int[] wcAnchos = {6, 36, 12, 11, 16, 16};
        wcPrepararTabla(wcHoja, wcE, wcTitulos, wcAnchos);

        int wcF = 1;
        for (WcClienteFrecuente wcC : wcClientes) {
            Row wcFila = wcHoja.createRow(wcF);
            wcEntero(wcFila, 0, wcF, wcE.wcEntero);
            wcTexto(wcFila, 1, wcC.wcNombre(), wcE.wcCelda);
            wcTexto(wcFila, 2, wcC.wcDni(), wcE.wcCelda);
            wcEntero(wcFila, 3, wcC.wcCompras(), wcE.wcEntero);
            wcNumero(wcFila, 4, wcC.wcImporte(), wcE.wcMonedaNegrita);
            wcFormula(wcFila, 5, "IFERROR(E" + (wcF + 1) + "/D" + (wcF + 1) + ",0)", wcE.wcMoneda);
            wcF++;
        }
        wcCerrarTabla(wcHoja, wcE, wcTitulos.length, wcF, new int[]{3}, new int[]{4});
    }
}
