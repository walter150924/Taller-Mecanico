package com.taller.mecanico.service;

import com.taller.mecanico.config.WcDatosEmpresa;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.model.WcVentaDetalle;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WcComprobantePdfService {
    private static final Color WC_ACENTO = new Color(234, 88, 12);
    private static final Color WC_ACENTO_SUAVE = new Color(255, 241, 230);
    private static final Color WC_TEXTO = new Color(31, 35, 40);
    private static final Color WC_TEXTO_SUAVE = new Color(101, 109, 118);
    private static final Color WC_BORDE = new Color(216, 222, 228);
    private static final Color WC_FILA_ALTERNA = new Color(246, 248, 250);
    private static final Color WC_PELIGRO = new Color(207, 34, 46);

    private static final DateTimeFormatter WC_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter WC_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DecimalFormat WC_IMPORTE =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private final WcVentaService wcVentaService;
    private final WcDatosEmpresa wcEmpresa;

    private final Font wcFNormal = wcFuente(9, Font.NORMAL, WC_TEXTO);
    private final Font wcFNegrita = wcFuente(9, Font.BOLD, WC_TEXTO);
    private final Font wcFSuave = wcFuente(8.5f, Font.NORMAL, WC_TEXTO_SUAVE);
    private final Font wcFEtiqueta = wcFuente(7.5f, Font.BOLD, WC_TEXTO_SUAVE);
    private final Font wcFCabecera = wcFuente(8, Font.BOLD, Color.WHITE);
    private final Font wcFCajaRuc = wcFuente(11, Font.BOLD, WC_TEXTO);
    private final Font wcFCajaTipo = wcFuente(10.5f, Font.BOLD, Color.WHITE);
    private final Font wcFCajaNumero = wcFuente(13, Font.BOLD, WC_ACENTO);
    private final Font wcFTotal = wcFuente(11, Font.BOLD, Color.WHITE);

    public void wcGenerar(Long wcVentaId, File wcDestino) throws IOException {
        WcVenta wcVenta = wcVentaService.wcObtener(wcVentaId);
        try (OutputStream wcSalida = new FileOutputStream(wcDestino)) {
            wcEscribir(wcVenta, wcSalida);
        }
    }

    void wcEscribir(WcVenta wcVenta, OutputStream wcSalida) {
        Document wcDoc = new Document(PageSize.A4, 40, 40, 40, 50);
        try {
            PdfWriter wcWriter = PdfWriter.getInstance(wcDoc, wcSalida);
            wcWriter.setPageEvent(new WcPiePagina(wcVenta));
            wcDoc.addTitle(wcVenta.getWcTipoComprobante().getWcDescripcion() + " " + wcVenta.getWcComprobante());
            wcDoc.addAuthor(wcEmpresa.getWcRazonSocial());
            wcDoc.open();

            wcDoc.add(wcCabecera(wcVenta));
            wcDoc.add(wcEspacio(14));
            wcDoc.add(wcDatosCliente(wcVenta));
            wcDoc.add(wcEspacio(14));
            wcDoc.add(wcTablaItems(wcVenta));
            wcDoc.add(wcEspacio(10));
            wcDoc.add(wcTotales(wcVenta));
            wcDoc.add(wcEspacio(12));
            wcDoc.add(wcImporteEnLetras(wcVenta));
            if (wcVenta.getWcMetodoPago() != null) {
                wcDoc.add(wcEspacio(8));
                Paragraph wcPago = new Paragraph();
                wcPago.add(new Chunk("METODO DE PAGO: ", wcFEtiqueta));
                wcPago.add(new Chunk(wcVenta.getWcMetodoPago().getWcDescripcion(), wcFNormal));
                wcDoc.add(wcPago);
            }
            if (wcVenta.getWcObservacion() != null) {
                wcDoc.add(wcEspacio(8));
                Paragraph wcObs = new Paragraph();
                wcObs.add(new Chunk("OBSERVACIONES: ", wcFEtiqueta));
                wcObs.add(new Chunk(wcVenta.getWcObservacion(), wcFNormal));
                wcDoc.add(wcObs);
            }
        } catch (DocumentException wcE) {
            throw new IllegalStateException("No se pudo generar el PDF: " + wcE.getMessage(), wcE);
        } finally {
            wcDoc.close();
        }
    }

    private PdfPTable wcCabecera(WcVenta wcVenta) {
        PdfPTable wcTabla = new PdfPTable(new float[]{62, 38});
        wcTabla.setWidthPercentage(100);

        PdfPCell wcEmisor = wcSinBorde();
        wcEmisor.setVerticalAlignment(Element.ALIGN_MIDDLE);
        wcEmisor.addElement(new Paragraph(wcEmpresa.getWcNombreComercial(), wcFuente(20, Font.BOLD, WC_ACENTO)));
        wcEmisor.addElement(new Paragraph(wcEmpresa.getWcRazonSocial(), wcFNegrita));
        wcEmisor.addElement(new Paragraph(wcEmpresa.getWcDireccion(), wcFSuave));
        StringBuilder wcContacto = new StringBuilder();
        if (!wcEmpresa.getWcTelefono().isBlank()) {
            wcContacto.append("Telf. ").append(wcEmpresa.getWcTelefono());
        }
        if (!wcEmpresa.getWcEmail().isBlank()) {
            wcContacto.append(wcContacto.isEmpty() ? "" : "  ·  ").append(wcEmpresa.getWcEmail());
        }
        if (!wcContacto.isEmpty()) {
            wcEmisor.addElement(new Paragraph(wcContacto.toString(), wcFSuave));
        }
        wcTabla.addCell(wcEmisor);

        PdfPTable wcCaja = new PdfPTable(1);
        wcCaja.setWidthPercentage(100);
        wcCaja.addCell(wcCeldaCaja(new Phrase("R.U.C. " + wcEmpresa.getWcRuc(), wcFCajaRuc), Color.WHITE, 10, 4));
        wcCaja.addCell(wcCeldaCaja(new Phrase(wcVenta.getWcTipoComprobante().getWcDescripcion().toUpperCase(), wcFCajaTipo),
                WC_ACENTO, 7, 7));
        wcCaja.addCell(wcCeldaCaja(new Phrase(wcVenta.getWcComprobante(), wcFCajaNumero), Color.WHITE, 6, 10));

        PdfPCell wcContenedor = new PdfPCell(wcCaja);
        wcContenedor.setBorderColor(WC_ACENTO);
        wcContenedor.setBorderWidth(1.2f);
        wcContenedor.setPadding(0);
        wcTabla.addCell(wcContenedor);
        return wcTabla;
    }

    private PdfPTable wcDatosCliente(WcVenta wcVenta) {
        PdfPTable wcTabla = new PdfPTable(new float[]{17, 43, 17, 23});
        wcTabla.setWidthPercentage(100);

        wcAgregarDato(wcTabla, "CLIENTE", wcVenta.getWcCliente().getWcNombreCompleto().toUpperCase());
        wcAgregarDato(wcTabla, "FECHA DE EMISIÓN", wcVenta.getWcFecha().format(WC_FECHA));
        wcAgregarDato(wcTabla, "DNI", wcVenta.getWcCliente().getWcDni());
        wcAgregarDato(wcTabla, "HORA", wcVenta.getWcFecha().format(WC_HORA));
        wcAgregarDato(wcTabla, "CORREO", wcVenta.getWcCliente().getWcEmail() == null ? "—" : wcVenta.getWcCliente().getWcEmail());
        wcAgregarDato(wcTabla, "MONEDA", "SOLES (PEN)");
        wcAgregarDato(wcTabla, "TELÉFONO", wcVenta.getWcCliente().getWcTelefono() == null ? "—" : wcVenta.getWcCliente().getWcTelefono());
        wcAgregarDato(wcTabla, "CONDICIÓN", "CONTADO");

        PdfPTable wcMarco = new PdfPTable(1);
        wcMarco.setWidthPercentage(100);
        PdfPCell wcCelda = new PdfPCell(wcTabla);
        wcCelda.setBorderColor(WC_BORDE);
        wcCelda.setPadding(8);
        wcCelda.setBackgroundColor(WC_FILA_ALTERNA);
        wcMarco.addCell(wcCelda);
        return wcMarco;
    }

    private PdfPTable wcTablaItems(WcVenta wcVenta) {
        PdfPTable wcTabla = new PdfPTable(new float[]{7, 13, 44, 9, 13, 14});
        wcTabla.setWidthPercentage(100);
        wcTabla.setHeaderRows(1);

        String[] wcTitulos = {"ÍTEM", "CÓDIGO", "DESCRIPCIÓN", "CANT.", "P. UNIT.", "IMPORTE"};
        int[] wcAlineacion = {Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_LEFT,
                Element.ALIGN_CENTER, Element.ALIGN_RIGHT, Element.ALIGN_RIGHT};
        for (int wcI = 0; wcI < wcTitulos.length; wcI++) {
            PdfPCell wcC = new PdfPCell(new Phrase(wcTitulos[wcI], wcFCabecera));
            wcC.setBackgroundColor(WC_ACENTO);
            wcC.setBorder(Rectangle.NO_BORDER);
            wcC.setHorizontalAlignment(wcAlineacion[wcI]);
            wcC.setPaddingTop(7);
            wcC.setPaddingBottom(7);
            wcC.setPaddingLeft(6);
            wcC.setPaddingRight(6);
            wcTabla.addCell(wcC);
        }

        int wcFila = 0;
        for (WcVentaDetalle wcD : wcVenta.getWcDetalles()) {
            Color wcFondo = wcFila++ % 2 == 0 ? Color.WHITE : WC_FILA_ALTERNA;
            wcTabla.addCell(wcCeldaItem(String.valueOf(wcD.getWcItem()), Element.ALIGN_CENTER, wcFondo, wcFNormal));
            wcTabla.addCell(wcCeldaItem(wcD.getWcCodigoRepuesto(), Element.ALIGN_LEFT, wcFondo, wcFSuave));
            wcTabla.addCell(wcCeldaItem(wcD.getWcNombreRepuesto(), Element.ALIGN_LEFT, wcFondo, wcFNormal));
            wcTabla.addCell(wcCeldaItem(String.valueOf(wcD.getWcCantidad()), Element.ALIGN_CENTER, wcFondo, wcFNormal));
            wcTabla.addCell(wcCeldaItem(WC_IMPORTE.format(wcD.getWcPrecioUnitario()), Element.ALIGN_RIGHT, wcFondo, wcFNormal));
            wcTabla.addCell(wcCeldaItem(WC_IMPORTE.format(wcD.getWcImporte()), Element.ALIGN_RIGHT, wcFondo, wcFNegrita));
        }
        return wcTabla;
    }

    private PdfPTable wcTotales(WcVenta wcVenta) {
        PdfPTable wcExterno = new PdfPTable(new float[]{55, 45});
        wcExterno.setWidthPercentage(100);

        PdfPCell wcNota = wcSinBorde();
        String wcModalidad = !wcVenta.isWcAplicaIgv()
                ? "Operación inafecta / exonerada de IGV."
                : wcVenta.isWcPreciosIncluyenIgv()
                ? "Los precios unitarios incluyen IGV."
                : "Los precios unitarios no incluyen IGV.";
        wcNota.addElement(new Paragraph(wcModalidad, wcFSuave));
        wcNota.addElement(new Paragraph("Artículos: " + wcVenta.getWcDetalles().stream()
                .mapToInt(WcVentaDetalle::getWcCantidad).sum(), wcFSuave));
        wcExterno.addCell(wcNota);

        PdfPTable wcT = new PdfPTable(new float[]{60, 40});
        wcT.setWidthPercentage(100);
        String wcEtiquetaBase = wcVenta.isWcAplicaIgv() ? "OP. GRAVADA" : "OP. EXONERADA";
        wcFilaTotal(wcT, wcEtiquetaBase, "S/ " + WC_IMPORTE.format(wcVenta.getWcBaseImponible()), false);
        String wcTasa = wcVenta.getWcTasaIgv().multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
        wcFilaTotal(wcT, "IGV (" + wcTasa + "%)", "S/ " + WC_IMPORTE.format(wcVenta.getWcIgv()), false);
        wcFilaTotal(wcT, "IMPORTE TOTAL", "S/ " + WC_IMPORTE.format(wcVenta.getWcTotal()), true);

        PdfPCell wcCelda = new PdfPCell(wcT);
        wcCelda.setBorder(Rectangle.NO_BORDER);
        wcCelda.setPadding(0);
        wcExterno.addCell(wcCelda);
        return wcExterno;
    }

    private PdfPTable wcImporteEnLetras(WcVenta wcVenta) {
        PdfPTable wcT = new PdfPTable(1);
        wcT.setWidthPercentage(100);
        Phrase wcFrase = new Phrase();
        wcFrase.add(new Chunk("SON: ", wcFEtiqueta));
        wcFrase.add(new Chunk(WcNumeroALetras.wcImporte(wcVenta.getWcTotal()), wcFNegrita));
        PdfPCell wcC = new PdfPCell(wcFrase);
        wcC.setBorderColor(WC_BORDE);
        wcC.setBackgroundColor(WC_ACENTO_SUAVE);
        wcC.setPadding(8);
        wcT.addCell(wcC);
        return wcT;
    }

    private void wcAgregarDato(PdfPTable wcTabla, String wcEtiqueta, String wcValor) {
        PdfPCell wcE = wcSinBorde();
        wcE.setPhrase(new Phrase(wcEtiqueta, wcFEtiqueta));
        wcE.setPaddingBottom(5);
        wcTabla.addCell(wcE);
        PdfPCell wcV = wcSinBorde();
        wcV.setPhrase(new Phrase(wcValor, wcFNormal));
        wcV.setPaddingBottom(5);
        wcTabla.addCell(wcV);
    }

    private PdfPCell wcCeldaCaja(Phrase wcTexto, Color wcFondo, float wcArriba, float wcAbajo) {
        PdfPCell wcC = new PdfPCell(wcTexto);
        wcC.setHorizontalAlignment(Element.ALIGN_CENTER);
        wcC.setBorder(Rectangle.NO_BORDER);
        wcC.setBackgroundColor(wcFondo);
        wcC.setPaddingTop(wcArriba);
        wcC.setPaddingBottom(wcAbajo);
        return wcC;
    }

    private PdfPCell wcCeldaItem(String wcTexto, int wcAlineacion, Color wcFondo, Font wcFuente) {
        PdfPCell wcC = new PdfPCell(new Phrase(wcTexto, wcFuente));
        wcC.setHorizontalAlignment(wcAlineacion);
        wcC.setVerticalAlignment(Element.ALIGN_MIDDLE);
        wcC.setBackgroundColor(wcFondo);
        wcC.setBorder(Rectangle.BOTTOM);
        wcC.setBorderColor(WC_BORDE);
        wcC.setPaddingTop(6);
        wcC.setPaddingBottom(7);
        wcC.setPaddingLeft(6);
        wcC.setPaddingRight(6);
        return wcC;
    }

    private void wcFilaTotal(PdfPTable wcT, String wcEtiqueta, String wcValor, boolean wcDestacado) {
        Font wcFe = wcDestacado ? wcFTotal : wcFNegrita;
        Font wcFv = wcDestacado ? wcFTotal : wcFNormal;
        PdfPCell wcE = new PdfPCell(new Phrase(wcEtiqueta, wcFe));
        PdfPCell wcV = new PdfPCell(new Phrase(wcValor, wcFv));
        for (PdfPCell wcC : new PdfPCell[]{wcE, wcV}) {
            wcC.setBorder(Rectangle.BOTTOM);
            wcC.setBorderColor(WC_BORDE);
            wcC.setPadding(6);
            if (wcDestacado) {
                wcC.setBackgroundColor(WC_ACENTO);
                wcC.setBorder(Rectangle.NO_BORDER);
                wcC.setPaddingTop(8);
                wcC.setPaddingBottom(9);
            }
        }
        wcV.setHorizontalAlignment(Element.ALIGN_RIGHT);
        wcT.addCell(wcE);
        wcT.addCell(wcV);
    }

    private static PdfPCell wcSinBorde() {
        PdfPCell wcC = new PdfPCell();
        wcC.setBorder(Rectangle.NO_BORDER);
        wcC.setPadding(0);
        return wcC;
    }

    private static Paragraph wcEspacio(float wcAlto) {
        Paragraph wcP = new Paragraph(" ");
        wcP.setLeading(wcAlto);
        return wcP;
    }

    private static Font wcFuente(float wcTamanio, int wcEstilo, Color wcColor) {
        return new Font(Font.HELVETICA, wcTamanio, wcEstilo, wcColor);
    }

    private class WcPiePagina extends PdfPageEventHelper {
        private final WcVenta wcVenta;

        WcPiePagina(WcVenta wcVenta) {
            this.wcVenta = wcVenta;
        }

        @Override
        public void onEndPage(PdfWriter wcWriter, Document wcDocument) {
            PdfContentByte wcCb = wcWriter.getDirectContent();
            float wcCentro = (wcDocument.left() + wcDocument.right()) / 2;
            float wcY = wcDocument.bottom() - 22;

            wcCb.setColorStroke(WC_BORDE);
            wcCb.setLineWidth(0.6f);
            wcCb.moveTo(wcDocument.left(), wcY + 12);
            wcCb.lineTo(wcDocument.right(), wcY + 12);
            wcCb.stroke();

            ColumnText.showTextAligned(wcCb, Element.ALIGN_CENTER,
                    new Phrase("Representación impresa de la " + wcVenta.getWcTipoComprobante().getWcDescripcion()
                            + "  ·  Gracias por su compra", wcFSuave), wcCentro, wcY, 0);
            ColumnText.showTextAligned(wcCb, Element.ALIGN_RIGHT,
                    new Phrase("Pág. " + wcWriter.getPageNumber(), wcFSuave), wcDocument.right(), wcY, 0);

            if (wcVenta.isWcAnulada()) {
                PdfContentByte wcDebajo = wcWriter.getDirectContentUnder();
                wcDebajo.saveState();
                PdfGState wcEstado = new PdfGState();
                wcEstado.setFillOpacity(0.12f);
                wcDebajo.setGState(wcEstado);
                try {
                    wcDebajo.beginText();
                    wcDebajo.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false), 110);
                    wcDebajo.setColorFill(WC_PELIGRO);
                    wcDebajo.showTextAligned(Element.ALIGN_CENTER, "ANULADO",
                            wcCentro, (wcDocument.top() + wcDocument.bottom()) / 2, 40);
                    wcDebajo.endText();
                } catch (DocumentException | IOException wcE) {
                    throw new IllegalStateException(wcE);
                }
                wcDebajo.restoreState();
            }
        }
    }
}
