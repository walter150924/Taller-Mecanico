package com.taller.mecanico.service;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

final class WcLibroExcel {
    private WcLibroExcel() {
    }

    static void wcPrepararTabla(Sheet wcHoja, WcEstilos wcE, String[] wcTitulos, int[] wcAnchos) {
        wcCabecera(wcHoja.createRow(0), wcE, wcTitulos);
        for (int wcI = 0; wcI < wcAnchos.length; wcI++) {
            wcHoja.setColumnWidth(wcI, wcAnchos[wcI] * 256);
        }
        wcHoja.createFreezePane(0, 1);
    }

    static void wcCerrarTabla(Sheet wcHoja, WcEstilos wcE, int wcColumnas, int wcSiguienteFila, int[] wcEnteros, int[] wcImportes) {
        if (wcSiguienteFila > 1) {
            wcHoja.setAutoFilter(new CellRangeAddress(0, wcSiguienteFila - 1, 0, wcColumnas - 1));
        }
        Row wcFila = wcHoja.createRow(wcSiguienteFila);
        for (int wcC = 0; wcC < wcColumnas; wcC++) {
            wcFila.createCell(wcC).setCellStyle(wcE.wcTotal);
        }
        wcFilaTotales(wcFila, wcE, 1, wcSiguienteFila - 1, 0, wcEnteros, wcImportes);
    }

    static void wcFilaTotales(Row wcFila, WcEstilos wcE, int wcDesde, int wcHasta, int wcColEtiqueta, int[] wcEnteros, int[] wcImportes) {
        wcTexto(wcFila, wcColEtiqueta, "TOTAL", wcE.wcTotal);
        for (int wcC : wcEnteros) {
            wcFormula(wcFila, wcC, wcSubtotal(wcC, wcDesde, wcHasta), wcE.wcTotalEntero);
        }
        for (int wcC : wcImportes) {
            wcFormula(wcFila, wcC, wcSubtotal(wcC, wcDesde, wcHasta), wcE.wcTotalMoneda);
        }
    }

    static String wcSubtotal(int wcColumna, int wcDesde, int wcHasta) {
        if (wcHasta < wcDesde) {
            return "0";
        }
        String wcCol = CellReference.convertNumToColString(wcColumna);
        return "SUBTOTAL(109," + wcCol + (wcDesde + 1) + ":" + wcCol + (wcHasta + 1) + ")";
    }

    static void wcCabecera(Row wcFila, WcEstilos wcE, String... wcTitulos) {
        wcFila.setHeightInPoints(22);
        for (int wcI = 0; wcI < wcTitulos.length; wcI++) {
            wcTexto(wcFila, wcI, wcTitulos[wcI], wcE.wcCabecera);
        }
    }

    static void wcTexto(Row wcFila, int wcCol, String wcValor, CellStyle wcEstilo) {
        Cell wcC = wcFila.createCell(wcCol);
        wcC.setCellValue(wcValor == null ? "" : wcValor);
        wcC.setCellStyle(wcEstilo);
    }

    static void wcNumero(Row wcFila, int wcCol, BigDecimal wcValor, CellStyle wcEstilo) {
        Cell wcC = wcFila.createCell(wcCol);
        wcC.setCellValue(wcValor == null ? 0 : wcValor.doubleValue());
        wcC.setCellStyle(wcEstilo);
    }

    static void wcEntero(Row wcFila, int wcCol, long wcValor, CellStyle wcEstilo) {
        Cell wcC = wcFila.createCell(wcCol);
        wcC.setCellValue(wcValor);
        wcC.setCellStyle(wcEstilo);
    }

    static void wcFecha(Row wcFila, int wcCol, LocalDate wcValor, CellStyle wcEstilo) {
        Cell wcC = wcFila.createCell(wcCol);
        wcC.setCellValue(Date.from(wcValor.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        wcC.setCellStyle(wcEstilo);
    }

    static void wcFechaHora(Row wcFila, int wcCol, LocalDateTime wcValor, CellStyle wcEstilo) {
        Cell wcC = wcFila.createCell(wcCol);
        wcC.setCellValue(wcValor);
        wcC.setCellStyle(wcEstilo);
    }

    static void wcFormula(Row wcFila, int wcCol, String wcFormula, CellStyle wcEstilo) {
        Cell wcC = wcFila.createCell(wcCol);
        wcC.setCellFormula(wcFormula);
        wcC.setCellStyle(wcEstilo);
    }

    static final class WcEstilos {
        final XSSFCellStyle wcTitulo, wcSubtitulo, wcSeccion, wcCabecera, wcCelda, wcCeldaNegrita, wcAnulada;
        final XSSFCellStyle wcEntero, wcMoneda, wcMonedaNegrita, wcPorcentaje, wcFecha, wcMes, wcFechaHora;
        final XSSFCellStyle wcEstadoEmitida, wcEstadoAnulada, wcEstadoAdvertencia, wcTotal, wcTotalEntero, wcTotalMoneda;

        WcEstilos(XSSFWorkbook wcLibro) {
            XSSFColor wcAcento = wcColor(234, 88, 12);
            XSSFColor wcBordeColor = wcColor(216, 222, 228);
            XSSFColor wcFondoTotal = wcColor(234, 242, 252);
            short wcFmtMoneda = wcLibro.createDataFormat().getFormat("\"S/\" #,##0.00");
            short wcFmtEntero = wcLibro.createDataFormat().getFormat("#,##0");

            wcTitulo = wcEstilo(wcLibro, wcFuente(wcLibro, 18, true, wcColor(31, 35, 40)), null, null);
            wcSubtitulo = wcEstilo(wcLibro, wcFuente(wcLibro, 10, false, wcColor(101, 109, 118)), null, null);
            wcSeccion = wcEstilo(wcLibro, wcFuente(wcLibro, 12, true, wcAcento), null, null);

            wcCabecera = wcEstilo(wcLibro, wcFuente(wcLibro, 10, true, wcColor(255, 255, 255)), wcAcento, null);
            wcCabecera.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFFont wcNormal = wcFuente(wcLibro, 10, false, wcColor(31, 35, 40));
            XSSFFont wcNegrita = wcFuente(wcLibro, 10, true, wcColor(31, 35, 40));
            wcCelda = wcEstilo(wcLibro, wcNormal, null, wcBordeColor);
            wcCeldaNegrita = wcEstilo(wcLibro, wcNegrita, null, wcBordeColor);

            XSSFFont wcTachada = wcFuente(wcLibro, 10, false, wcColor(140, 149, 159));
            wcTachada.setStrikeout(true);
            wcAnulada = wcEstilo(wcLibro, wcTachada, null, wcBordeColor);

            wcEntero = wcEstilo(wcLibro, wcNormal, null, wcBordeColor);
            wcEntero.setDataFormat(wcFmtEntero);
            wcEntero.setAlignment(HorizontalAlignment.CENTER);
            wcMoneda = wcEstilo(wcLibro, wcNormal, null, wcBordeColor);
            wcMoneda.setDataFormat(wcFmtMoneda);
            wcMonedaNegrita = wcEstilo(wcLibro, wcNegrita, null, wcBordeColor);
            wcMonedaNegrita.setDataFormat(wcFmtMoneda);
            wcPorcentaje = wcEstilo(wcLibro, wcNormal, null, wcBordeColor);
            wcPorcentaje.setDataFormat(wcLibro.createDataFormat().getFormat("0.0%"));
            wcFecha = wcEstilo(wcLibro, wcNormal, null, wcBordeColor);
            wcFecha.setDataFormat(wcLibro.createDataFormat().getFormat("dd/mm/yyyy"));
            wcMes = wcEstilo(wcLibro, wcNormal, null, wcBordeColor);
            wcMes.setDataFormat(wcLibro.createDataFormat().getFormat("mmm yyyy"));
            wcFechaHora = wcEstilo(wcLibro, wcNormal, null, wcBordeColor);
            wcFechaHora.setDataFormat(wcLibro.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

            wcEstadoEmitida = wcEstilo(wcLibro, wcFuente(wcLibro, 9, true, wcColor(26, 127, 55)), wcColor(218, 251, 225), wcBordeColor);
            wcEstadoEmitida.setAlignment(HorizontalAlignment.CENTER);
            wcEstadoAnulada = wcEstilo(wcLibro, wcFuente(wcLibro, 9, true, wcColor(207, 34, 46)), wcColor(255, 235, 233), wcBordeColor);
            wcEstadoAnulada.setAlignment(HorizontalAlignment.CENTER);
            wcEstadoAdvertencia = wcEstilo(wcLibro, wcFuente(wcLibro, 9, true, wcColor(154, 103, 0)), wcColor(255, 248, 197), wcBordeColor);
            wcEstadoAdvertencia.setAlignment(HorizontalAlignment.CENTER);

            XSSFFont wcFuenteTotal = wcFuente(wcLibro, 10, true, wcAcento);
            wcTotal = wcEstilo(wcLibro, wcFuenteTotal, wcFondoTotal, null);
            wcTotal.setBorderTop(BorderStyle.MEDIUM);
            wcTotal.setTopBorderColor(wcAcento);
            wcTotalEntero = wcLibro.createCellStyle();
            wcTotalEntero.cloneStyleFrom(wcTotal);
            wcTotalEntero.setDataFormat(wcFmtEntero);
            wcTotalEntero.setAlignment(HorizontalAlignment.CENTER);
            wcTotalMoneda = wcLibro.createCellStyle();
            wcTotalMoneda.cloneStyleFrom(wcTotal);
            wcTotalMoneda.setDataFormat(wcFmtMoneda);
        }

        private static XSSFCellStyle wcEstilo(XSSFWorkbook wcLibro, XSSFFont wcFuente, XSSFColor wcFondo, XSSFColor wcBorde) {
            XSSFCellStyle wcS = wcLibro.createCellStyle();
            wcS.setFont(wcFuente);
            wcS.setVerticalAlignment(VerticalAlignment.CENTER);
            if (wcFondo != null) {
                wcS.setFillForegroundColor(wcFondo);
                wcS.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (wcBorde != null) {
                wcS.setBorderBottom(BorderStyle.THIN);
                wcS.setBottomBorderColor(wcBorde);
            }
            return wcS;
        }

        private static XSSFFont wcFuente(XSSFWorkbook wcLibro, int wcPuntos, boolean wcNegrita, XSSFColor wcColor) {
            XSSFFont wcF = wcLibro.createFont();
            wcF.setFontName("Calibri");
            wcF.setFontHeightInPoints((short) wcPuntos);
            wcF.setBold(wcNegrita);
            wcF.setColor(wcColor);
            return wcF;
        }

        private static XSSFColor wcColor(int wcR, int wcG, int wcB) {
            return new XSSFColor(new byte[]{(byte) wcR, (byte) wcG, (byte) wcB}, null);
        }
    }
}
