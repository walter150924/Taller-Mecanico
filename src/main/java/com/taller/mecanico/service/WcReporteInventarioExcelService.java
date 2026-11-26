package com.taller.mecanico.service;

import com.taller.mecanico.config.WcDatosEmpresa;
import com.taller.mecanico.model.WcCompra;
import com.taller.mecanico.model.WcCompraDetalle;
import com.taller.mecanico.model.WcMovimientoInventario;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.repository.WcCompraRepository;
import com.taller.mecanico.repository.WcMovimientoInventarioRepository;
import com.taller.mecanico.service.WcInventarioService.WcResumenInventario;
import com.taller.mecanico.service.WcLibroExcel.WcEstilos;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(readOnly = true)
public class WcReporteInventarioExcelService {
    private static final DateTimeFormatter WC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter WC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final WcInventarioService wcInventarioService;
    private final WcMovimientoInventarioRepository wcMovimientoRepository;
    private final WcCompraRepository wcCompraRepository;
    private final WcDatosEmpresa wcEmpresa;

    public void wcGenerarInventario(File wcDestino) throws IOException {
        try (XSSFWorkbook wcLibro = new XSSFWorkbook(); OutputStream wcSalida = new FileOutputStream(wcDestino)) {
            WcEstilos wcE = new WcEstilos(wcLibro);
            wcHojaInventario(wcLibro, wcE, wcInventarioService.wcListarTodo(), wcInventarioService.wcResumen());
            wcHojaKardex(wcLibro, wcE, wcMovimientoRepository.findAll(Sort.by("wcRepuesto.wcCodigoRepuesto", "wcFecha", "wcId")));
            wcLibro.getProperties().getCoreProperties().setTitle("Inventario valorizado");
            wcLibro.getProperties().getCoreProperties().setCreator(wcEmpresa.getWcRazonSocial());
            wcLibro.write(wcSalida);
        }
    }

    private void wcHojaInventario(XSSFWorkbook wcLibro, WcEstilos wcE, List<WcRepuesto> wcRepuestos, WcResumenInventario wcR) {
        Sheet wcHoja = wcLibro.createSheet("Inventario");
        String[] wcTitulos = {"Codigo", "Repuesto", "Categoria", "Stock", "Stock minimo", "Estado",
                "Costo promedio", "Valor al costo", "Precio venta", "Valor a precio de venta"};
        int[] wcAnchos = {12, 42, 26, 10, 13, 13, 16, 17, 14, 22};
        wcPrepararTabla(wcHoja, wcE, wcTitulos, wcAnchos);

        int wcF = 1;
        for (WcRepuesto wcP : wcRepuestos) {
            Row wcFila = wcHoja.createRow(wcF++);
            wcTexto(wcFila, 0, wcP.getWcCodigoRepuesto(), wcE.wcCelda);
            wcTexto(wcFila, 1, wcP.getWcNombre(), wcE.wcCelda);
            wcTexto(wcFila, 2, wcP.getWcCategoria() == null ? "Sin categoria" : wcP.getWcCategoria().getWcNombre(), wcE.wcCelda);
            wcEntero(wcFila, 3, wcP.getWcStock(), wcE.wcEntero);
            wcEntero(wcFila, 4, wcP.getWcStockMinimo(), wcE.wcEntero);
            XSSFCellStyle wcEstado = wcP.isWcSinStock() ? wcE.wcEstadoAnulada : wcP.isWcStockBajo() ? wcE.wcEstadoAdvertencia : wcE.wcEstadoEmitida;
            wcTexto(wcFila, 5, wcP.isWcSinStock() ? "SIN STOCK" : wcP.isWcStockBajo() ? "BAJO" : "OK", wcEstado);
            wcNumero(wcFila, 6, wcP.getWcCostoPromedio() == null ? BigDecimal.ZERO : wcP.getWcCostoPromedio(), wcE.wcMoneda);
            wcFormula(wcFila, 7, "D" + wcF + "*G" + wcF, wcE.wcMonedaNegrita);
            wcNumero(wcFila, 8, wcP.getWcPrecio(), wcE.wcMoneda);
            wcFormula(wcFila, 9, "D" + wcF + "*I" + wcF, wcE.wcMoneda);
        }
        wcCerrarTabla(wcHoja, wcE, wcTitulos.length, wcF, new int[]{3}, new int[]{7, 9});

        wcBloqueResumen(wcHoja, wcE, wcR, wcTitulos.length + 1);
    }

    private void wcBloqueResumen(Sheet wcHoja, WcEstilos wcE, WcResumenInventario wcR, int wcC) {
        wcHoja.setColumnWidth(wcC, 26 * 256);
        wcHoja.setColumnWidth(wcC + 1, 18 * 256);
        Object[][] wcKpis = {
                {"Repuestos", wcR.wcRepuestos()},
                {"Unidades en stock", wcR.wcUnidades()},
                {"Valor al costo (sin IGV)", wcR.wcValorCosto()},
                {"Valor a precio de venta", wcR.wcValorVenta()},
                {"Repuestos con stock bajo", wcR.wcStockBajo()},
                {"Repuestos sin stock", wcR.wcSinStock()},
        };
        Row wcTitulo = wcFila(wcHoja, 0);
        wcTexto(wcTitulo, wcC, "Resumen al " + LocalDate.now().format(WC_FECHA), wcE.wcCabecera);
        wcTexto(wcTitulo, wcC + 1, "", wcE.wcCabecera);
        for (int wcI = 0; wcI < wcKpis.length; wcI++) {
            Row wcFila = wcFila(wcHoja, wcI + 1);
            wcTexto(wcFila, wcC, (String) wcKpis[wcI][0], wcE.wcCeldaNegrita);
            if (wcKpis[wcI][1] instanceof BigDecimal wcB) {
                wcNumero(wcFila, wcC + 1, wcB, wcE.wcMoneda);
            } else {
                wcEntero(wcFila, wcC + 1, ((Number) wcKpis[wcI][1]).longValue(), wcE.wcEntero);
            }
        }
    }

    private void wcHojaKardex(XSSFWorkbook wcLibro, WcEstilos wcE, List<WcMovimientoInventario> wcMovimientos) {
        Sheet wcHoja = wcLibro.createSheet("Kardex");
        String[] wcTitulos = {"Codigo", "Repuesto", "Fecha", "Movimiento", "Documento", "Entrada", "Salida", "Saldo",
                "Costo unit. (sin IGV)"};
        int[] wcAnchos = {12, 40, 18, 22, 30, 10, 10, 10, 20};
        wcPrepararTabla(wcHoja, wcE, wcTitulos, wcAnchos);

        int wcF = 1;
        for (WcMovimientoInventario wcM : wcMovimientos) {
            Row wcFila = wcHoja.createRow(wcF++);
            wcTexto(wcFila, 0, wcM.getWcRepuesto().getWcCodigoRepuesto(), wcE.wcCelda);
            wcTexto(wcFila, 1, wcM.getWcRepuesto().getWcNombre(), wcE.wcCelda);
            wcFechaHora(wcFila, 2, wcM.getWcFecha(), wcE.wcFechaHora);
            wcTexto(wcFila, 3, wcM.getWcTipo().getWcDescripcion(), wcM.getWcTipo().isWcEntrada() ? wcE.wcEstadoEmitida : wcE.wcEstadoAdvertencia);
            wcTexto(wcFila, 4, wcM.getWcDocumento(), wcE.wcCelda);
            wcEntero(wcFila, 5, wcM.getWcEntrada(), wcE.wcEntero);
            wcEntero(wcFila, 6, wcM.getWcSalida(), wcE.wcEntero);
            wcEntero(wcFila, 7, wcM.getWcStockResultante(), wcE.wcEntero);
            if (wcM.getWcCostoUnitario() != null) {
                wcNumero(wcFila, 8, wcM.getWcCostoUnitario(), wcE.wcMoneda);
            } else {
                wcTexto(wcFila, 8, "", wcE.wcCelda);
            }
        }
        wcCerrarTabla(wcHoja, wcE, wcTitulos.length, wcF, new int[]{5, 6}, new int[]{});
    }

    public void wcGenerarCompras(LocalDate wcDesde, LocalDate wcHasta, File wcDestino) throws IOException {
        List<WcCompra> wcCompras = wcCompraRepository.findAll(Sort.by("wcFecha", "wcId")).stream()
                .filter(wcC -> !wcC.getWcFecha().isBefore(WcVentaService.wcInicio(wcDesde)) && !wcC.getWcFecha().isAfter(WcVentaService.wcFin(wcHasta)))
                .toList();
        try (XSSFWorkbook wcLibro = new XSSFWorkbook(); OutputStream wcSalida = new FileOutputStream(wcDestino)) {
            WcEstilos wcE = new WcEstilos(wcLibro);

            wcHojaCompras(wcLibro, wcE, wcCompras);
            wcHojaDetalleCompras(wcLibro, wcE, wcCompras);

            wcLibro.getProperties().getCoreProperties().setTitle("Compras del " + wcDesde.format(WC_FECHA) + " al " + wcHasta.format(WC_FECHA)
                    + " (generado " + LocalDateTime.now().format(WC_FECHA_HORA) + ")");
            wcLibro.getProperties().getCoreProperties().setCreator(wcEmpresa.getWcRazonSocial());
            wcLibro.write(wcSalida);
        }
    }

    private void wcHojaCompras(XSSFWorkbook wcLibro, WcEstilos wcE, List<WcCompra> wcCompras) {
        Sheet wcHoja = wcLibro.createSheet("Compras");
        String[] wcTitulos = {"Codigo", "Fecha", "Proveedor", "RUC", "Documento", "Fecha doc.",
                "Base imponible", "IGV", "Total", "Estado"};
        int[] wcAnchos = {14, 18, 42, 13, 26, 12, 16, 13, 15, 13};
        wcPrepararTabla(wcHoja, wcE, wcTitulos, wcAnchos);
        int wcF = 1;
        for (WcCompra wcC : wcCompras) {
            boolean wcAnulada = wcC.isWcAnulada();
            Row wcFila = wcHoja.createRow(wcF++);
            wcTexto(wcFila, 0, wcC.getWcCodigo(), wcAnulada ? wcE.wcAnulada : wcE.wcCelda);
            wcFechaHora(wcFila, 1, wcC.getWcFecha(), wcE.wcFechaHora);
            wcTexto(wcFila, 2, wcC.getWcProveedor().getWcRazonSocial(), wcAnulada ? wcE.wcAnulada : wcE.wcCelda);
            wcTexto(wcFila, 3, wcC.getWcProveedor().getWcRuc(), wcE.wcCelda);
            wcTexto(wcFila, 4, wcC.getWcTipoDocumento().getWcDescripcion() + " " + wcC.getWcNumeroDocumento(), wcE.wcCelda);
            wcFecha(wcFila, 5, wcC.getWcFechaDocumento(), wcE.wcFecha);
            wcNumero(wcFila, 6, wcAnulada ? BigDecimal.ZERO : wcC.getWcBaseImponible(), wcE.wcMoneda);
            wcNumero(wcFila, 7, wcAnulada ? BigDecimal.ZERO : wcC.getWcIgv(), wcE.wcMoneda);
            wcNumero(wcFila, 8, wcAnulada ? BigDecimal.ZERO : wcC.getWcTotal(), wcE.wcMonedaNegrita);
            wcTexto(wcFila, 9, wcC.getWcEstado().name().replace("WC_", ""), wcAnulada ? wcE.wcEstadoAnulada : wcE.wcEstadoEmitida);
        }
        wcCerrarTabla(wcHoja, wcE, wcTitulos.length, wcF, new int[]{}, new int[]{6, 7, 8});
    }

    private void wcHojaDetalleCompras(XSSFWorkbook wcLibro, WcEstilos wcE, List<WcCompra> wcCompras) {
        Sheet wcDetalle = wcLibro.createSheet("Detalle");
        String[] wcTDet = {"Codigo", "Fecha", "Proveedor", "Item", "Cod. repuesto", "Repuesto", "Cantidad",
                "Costo unitario", "Importe", "Estado"};
        int[] wcADet = {14, 18, 34, 7, 13, 40, 11, 15, 14, 13};
        wcPrepararTabla(wcDetalle, wcE, wcTDet, wcADet);
        int wcF = 1;
        for (WcCompra wcC : wcCompras) {
            for (WcCompraDetalle wcD : wcC.getWcDetalles()) {
                Row wcFila = wcDetalle.createRow(wcF++);
                wcTexto(wcFila, 0, wcC.getWcCodigo(), wcE.wcCelda);
                wcFechaHora(wcFila, 1, wcC.getWcFecha(), wcE.wcFechaHora);
                wcTexto(wcFila, 2, wcC.getWcProveedor().getWcRazonSocial(), wcE.wcCelda);
                wcEntero(wcFila, 3, wcD.getWcItem(), wcE.wcEntero);
                wcTexto(wcFila, 4, wcD.getWcCodigoRepuesto(), wcE.wcCelda);
                wcTexto(wcFila, 5, wcD.getWcNombreRepuesto(), wcC.isWcAnulada() ? wcE.wcAnulada : wcE.wcCelda);
                wcEntero(wcFila, 6, wcC.isWcAnulada() ? 0 : wcD.getWcCantidad(), wcE.wcEntero);
                wcNumero(wcFila, 7, wcD.getWcCostoUnitario(), wcE.wcMoneda);
                wcNumero(wcFila, 8, wcC.isWcAnulada() ? BigDecimal.ZERO : wcD.getWcImporte(), wcE.wcMonedaNegrita);
                wcTexto(wcFila, 9, wcC.getWcEstado().name().replace("WC_", ""), wcC.isWcAnulada() ? wcE.wcEstadoAnulada : wcE.wcEstadoEmitida);
            }
        }
        wcCerrarTabla(wcDetalle, wcE, wcTDet.length, wcF, new int[]{6}, new int[]{8});
    }

    private static Row wcFila(Sheet wcHoja, int wcIndice) {
        Row wcR = wcHoja.getRow(wcIndice);
        return wcR != null ? wcR : wcHoja.createRow(wcIndice);
    }
}
