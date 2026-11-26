package com.taller.mecanico.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.taller.mecanico.config.WcDatosEmpresa;
import com.taller.mecanico.model.WcLineaRepuesto;
import com.taller.mecanico.model.WcLineaServicio;
import com.taller.mecanico.model.WcOrdenServicio;
import com.taller.mecanico.model.WcVehiculo;
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
public class WcOrdenPdfService {
    private static final Color WC_ACENTO = new Color(234, 88, 12);
    private static final Color WC_TEXTO = new Color(31, 35, 40);
    private static final Color WC_TEXTO_SUAVE = new Color(101, 109, 118);
    private static final Color WC_BORDE = new Color(216, 222, 228);
    private static final Color WC_FONDO_SUAVE = new Color(246, 248, 250);
    private static final String WC_SIN_DATO = "—";

    private static final DateTimeFormatter WC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat WC_IMPORTE =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private final WcOrdenServicioService wcOrdenService;
    private final WcDatosEmpresa wcEmpresa;

    private final Font wcFuenteMarca = wcFuente(20, Font.BOLD, WC_ACENTO);
    private final Font wcFuenteTitulo = wcFuente(13, Font.BOLD, WC_TEXTO);
    private final Font wcFuenteNormal = wcFuente(9, Font.NORMAL, WC_TEXTO);
    private final Font wcFuenteNegrita = wcFuente(9, Font.BOLD, WC_TEXTO);
    private final Font wcFuenteSuave = wcFuente(8.5f, Font.NORMAL, WC_TEXTO_SUAVE);
    private final Font wcFuenteEtiqueta = wcFuente(7.5f, Font.BOLD, WC_TEXTO_SUAVE);
    private final Font wcFuenteCabecera = wcFuente(8, Font.BOLD, Color.WHITE);

    public void wcGenerar(Long wcOrdenId, File wcDestino) throws IOException {
        WcOrdenServicio wcOrden = wcOrdenService.wcObtener(wcOrdenId);
        try (OutputStream wcSalida = new FileOutputStream(wcDestino)) {
            wcEscribir(wcOrden, wcSalida);
        }
    }

    private void wcEscribir(WcOrdenServicio wcOrden, OutputStream wcSalida) {
        Document wcDocumento = new Document(PageSize.A4, 40, 40, 40, 50);
        try {
            PdfWriter.getInstance(wcDocumento, wcSalida);
            wcDocumento.addTitle("Orden de servicio " + wcOrden.getWcCodigo());
            wcDocumento.addAuthor(wcEmpresa.getWcRazonSocial());
            wcDocumento.open();

            wcDocumento.add(wcCabecera(wcOrden));
            wcDocumento.add(wcEspacio(12));
            wcDocumento.add(wcDatosGenerales(wcOrden));
            wcDocumento.add(wcEspacio(10));
            wcDocumento.add(wcBloqueTexto("PROBLEMA REPORTADO", wcOrden.getWcProblemaReportado()));
            wcDocumento.add(wcEspacio(6));
            wcDocumento.add(wcBloqueTexto("DIAGNOSTICO", wcOrden.getWcDiagnostico()));
            wcDocumento.add(wcEspacio(12));
            wcDocumento.add(wcTablaServicios(wcOrden));
            wcDocumento.add(wcEspacio(10));
            wcDocumento.add(wcTablaRepuestos(wcOrden));
            wcDocumento.add(wcEspacio(10));
            wcDocumento.add(wcTotales(wcOrden));
            wcDocumento.add(wcEspacio(10));
            wcDocumento.add(new Paragraph("SON: " + WcNumeroALetras.wcImporte(wcOrden.getWcTotal()), wcFuenteNegrita));
        } catch (DocumentException wcE) {
            throw new IllegalStateException("No se pudo generar el PDF: " + wcE.getMessage(), wcE);
        } finally {
            wcDocumento.close();
        }
    }

    private PdfPTable wcCabecera(WcOrdenServicio wcOrden) {
        PdfPTable wcTabla = new PdfPTable(new float[]{62, 38});
        wcTabla.setWidthPercentage(100);

        PdfPCell wcEmisor = wcSinBorde();
        wcEmisor.addElement(new Paragraph(wcEmpresa.getWcNombreComercial(), wcFuenteMarca));
        wcEmisor.addElement(new Paragraph(wcEmpresa.getWcRazonSocial() + "  ·  RUC " + wcEmpresa.getWcRuc(), wcFuenteNegrita));
        wcEmisor.addElement(new Paragraph(wcEmpresa.getWcDireccion(), wcFuenteSuave));
        wcTabla.addCell(wcEmisor);

        PdfPCell wcCaja = new PdfPCell();
        wcCaja.setBorderColor(WC_ACENTO);
        wcCaja.setBorderWidth(1.2f);
        wcCaja.setPadding(10);
        Paragraph wcTitulo = new Paragraph("ORDEN DE SERVICIO", wcFuenteTitulo);
        wcTitulo.setAlignment(Element.ALIGN_CENTER);
        Paragraph wcCodigo = new Paragraph(wcOrden.getWcCodigo(), wcFuente(14, Font.BOLD, WC_ACENTO));
        wcCodigo.setAlignment(Element.ALIGN_CENTER);
        Paragraph wcEstado = new Paragraph(wcOrden.getWcEstado().getWcDescripcion().toUpperCase(), wcFuenteSuave);
        wcEstado.setAlignment(Element.ALIGN_CENTER);
        wcCaja.addElement(wcTitulo);
        wcCaja.addElement(wcCodigo);
        wcCaja.addElement(wcEstado);
        wcTabla.addCell(wcCaja);
        return wcTabla;
    }

    private PdfPTable wcDatosGenerales(WcOrdenServicio wcOrden) {
        WcVehiculo wcVehiculo = wcOrden.getWcVehiculo();
        PdfPTable wcTabla = new PdfPTable(new float[]{17, 33, 17, 33});
        wcTabla.setWidthPercentage(100);
        wcAgregarDato(wcTabla, "CLIENTE", wcOrden.getWcCliente().getWcNombreCompleto().toUpperCase());
        wcAgregarDato(wcTabla, "DNI", wcOrden.getWcCliente().getWcDni());
        wcAgregarDato(wcTabla, "VEHICULO", wcVehiculo.getWcDescripcion());
        wcAgregarDato(wcTabla, "PLACA", wcVehiculo.getWcPlaca());
        wcAgregarDato(wcTabla, "KILOMETRAJE", String.format(Locale.US, "%,d km", wcOrden.getWcKilometrajeIngreso()));
        wcAgregarDato(wcTabla, "MECANICO", wcOrden.getWcNombreMecanico());
        wcAgregarDato(wcTabla, "INGRESO", wcOrden.getWcFechaIngreso().format(WC_FECHA_HORA));
        wcAgregarDato(wcTabla, "ENTREGA", wcOrden.getWcFechaEntrega() == null ? WC_SIN_DATO : wcOrden.getWcFechaEntrega().format(WC_FECHA_HORA));
        return wcEnMarco(wcTabla);
    }

    private PdfPTable wcBloqueTexto(String wcEtiqueta, String wcContenido) {
        PdfPTable wcTabla = new PdfPTable(1);
        wcTabla.setWidthPercentage(100);
        Phrase wcFrase = new Phrase();
        wcFrase.add(new Chunk(wcEtiqueta + "\n", wcFuenteEtiqueta));
        wcFrase.add(new Chunk(wcContenido == null || wcContenido.isBlank() ? WC_SIN_DATO : wcContenido, wcFuenteNormal));
        PdfPCell wcCelda = new PdfPCell(wcFrase);
        wcCelda.setBorderColor(WC_BORDE);
        wcCelda.setPadding(8);
        wcTabla.addCell(wcCelda);
        return wcTabla;
    }

    private PdfPTable wcTablaServicios(WcOrdenServicio wcOrden) {
        PdfPTable wcTabla = wcTablaConCabecera(new float[]{8, 72, 20}, "ÍTEM", "SERVICIO / MANO DE OBRA", "IMPORTE");
        int wcItem = 1;
        for (WcLineaServicio wcLinea : wcOrden.getWcServicios()) {
            wcTabla.addCell(wcCelda(String.valueOf(wcItem++), Element.ALIGN_CENTER, wcFuenteNormal));
            wcTabla.addCell(wcCelda(wcLinea.getWcServicio().getWcNombre(), Element.ALIGN_LEFT, wcFuenteNormal));
            wcTabla.addCell(wcCelda(WC_IMPORTE.format(wcLinea.getWcPrecio()), Element.ALIGN_RIGHT, wcFuenteNegrita));
        }
        wcAgregarFilaVaciaSiCorresponde(wcTabla, wcOrden.getWcServicios().isEmpty(), 3, "Sin servicios registrados");
        return wcTabla;
    }

    private PdfPTable wcTablaRepuestos(WcOrdenServicio wcOrden) {
        PdfPTable wcTabla = wcTablaConCabecera(new float[]{8, 14, 40, 10, 14, 14},
                "ÍTEM", "CÓDIGO", "REPUESTO", "CANT.", "P. UNIT.", "IMPORTE");
        int wcItem = 1;
        for (WcLineaRepuesto wcLinea : wcOrden.getWcRepuestos()) {
            wcTabla.addCell(wcCelda(String.valueOf(wcItem++), Element.ALIGN_CENTER, wcFuenteNormal));
            wcTabla.addCell(wcCelda(wcLinea.getWcRepuesto().getWcCodigoRepuesto(), Element.ALIGN_LEFT, wcFuenteSuave));
            wcTabla.addCell(wcCelda(wcLinea.getWcRepuesto().getWcNombre(), Element.ALIGN_LEFT, wcFuenteNormal));
            wcTabla.addCell(wcCelda(String.valueOf(wcLinea.getWcCantidad()), Element.ALIGN_CENTER, wcFuenteNormal));
            wcTabla.addCell(wcCelda(WC_IMPORTE.format(wcLinea.getWcPrecioUnitario()), Element.ALIGN_RIGHT, wcFuenteNormal));
            wcTabla.addCell(wcCelda(WC_IMPORTE.format(wcLinea.getWcImporte()), Element.ALIGN_RIGHT, wcFuenteNegrita));
        }
        wcAgregarFilaVaciaSiCorresponde(wcTabla, wcOrden.getWcRepuestos().isEmpty(), 6, "Sin repuestos registrados");
        return wcTabla;
    }

    private PdfPTable wcTotales(WcOrdenServicio wcOrden) {
        PdfPTable wcExterno = new PdfPTable(new float[]{55, 45});
        wcExterno.setWidthPercentage(100);
        PdfPCell wcNota = wcSinBorde();
        wcNota.addElement(new Paragraph("Los precios incluyen IGV.", wcFuenteSuave));
        wcNota.addElement(new Paragraph(wcOrden.getWcMetodoPago() == null
                ? "Pago: pendiente"
                : "Metodo de pago: " + wcOrden.getWcMetodoPago().getWcDescripcion(), wcFuenteNegrita));
        wcExterno.addCell(wcNota);

        PdfPTable wcTabla = new PdfPTable(new float[]{60, 40});
        wcTabla.setWidthPercentage(100);
        wcFilaTotal(wcTabla, "MANO DE OBRA", wcOrden.getWcTotalServicios(), wcFuenteNormal);
        wcFilaTotal(wcTabla, "REPUESTOS", wcOrden.getWcTotalRepuestos(), wcFuenteNormal);
        wcFilaTotal(wcTabla, "OP. GRAVADA", wcOrden.getWcBaseImponible(), wcFuenteNormal);
        wcFilaTotal(wcTabla, "IGV (18%)", wcOrden.getWcIgv(), wcFuenteNormal);
        wcFilaTotal(wcTabla, "TOTAL", wcOrden.getWcTotal(), wcFuenteNegrita);
        PdfPCell wcContenedor = wcSinBorde();
        wcContenedor.addElement(wcTabla);
        wcExterno.addCell(wcContenedor);
        return wcExterno;
    }

    private PdfPTable wcTablaConCabecera(float[] wcAnchos, String... wcTitulos) {
        PdfPTable wcTabla = new PdfPTable(wcAnchos);
        wcTabla.setWidthPercentage(100);
        wcTabla.setHeaderRows(1);
        for (String wcTitulo : wcTitulos) {
            PdfPCell wcCelda = new PdfPCell(new Phrase(wcTitulo, wcFuenteCabecera));
            wcCelda.setBackgroundColor(WC_ACENTO);
            wcCelda.setBorder(Rectangle.NO_BORDER);
            wcCelda.setPadding(6);
            wcTabla.addCell(wcCelda);
        }
        return wcTabla;
    }

    private void wcAgregarFilaVaciaSiCorresponde(PdfPTable wcTabla, boolean wcVacia, int wcColumnas, String wcTexto) {
        if (wcVacia) {
            PdfPCell wcCelda = wcCelda(wcTexto, Element.ALIGN_CENTER, wcFuenteSuave);
            wcCelda.setColspan(wcColumnas);
            wcTabla.addCell(wcCelda);
        }
    }

    private void wcFilaTotal(PdfPTable wcTabla, String wcEtiqueta, BigDecimal wcValor, Font wcFuente) {
        wcTabla.addCell(wcCelda(wcEtiqueta, Element.ALIGN_LEFT, wcFuente));
        wcTabla.addCell(wcCelda("S/ " + WC_IMPORTE.format(wcValor), Element.ALIGN_RIGHT, wcFuente));
    }

    private void wcAgregarDato(PdfPTable wcTabla, String wcEtiqueta, String wcValor) {
        PdfPCell wcCeldaEtiqueta = wcSinBorde();
        wcCeldaEtiqueta.setPhrase(new Phrase(wcEtiqueta, wcFuenteEtiqueta));
        wcCeldaEtiqueta.setPaddingBottom(5);
        wcTabla.addCell(wcCeldaEtiqueta);
        PdfPCell wcCeldaValor = wcSinBorde();
        wcCeldaValor.setPhrase(new Phrase(wcValor, wcFuenteNormal));
        wcCeldaValor.setPaddingBottom(5);
        wcTabla.addCell(wcCeldaValor);
    }

    private PdfPTable wcEnMarco(PdfPTable wcContenido) {
        PdfPTable wcMarco = new PdfPTable(1);
        wcMarco.setWidthPercentage(100);
        PdfPCell wcCelda = new PdfPCell(wcContenido);
        wcCelda.setBorderColor(WC_BORDE);
        wcCelda.setPadding(8);
        wcCelda.setBackgroundColor(WC_FONDO_SUAVE);
        wcMarco.addCell(wcCelda);
        return wcMarco;
    }

    private PdfPCell wcCelda(String wcTexto, int wcAlineacion, Font wcFuente) {
        PdfPCell wcCelda = new PdfPCell(new Phrase(wcTexto, wcFuente));
        wcCelda.setHorizontalAlignment(wcAlineacion);
        wcCelda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        wcCelda.setBorder(Rectangle.BOTTOM);
        wcCelda.setBorderColor(WC_BORDE);
        wcCelda.setPadding(6);
        return wcCelda;
    }

    private static PdfPCell wcSinBorde() {
        PdfPCell wcCelda = new PdfPCell();
        wcCelda.setBorder(Rectangle.NO_BORDER);
        wcCelda.setPadding(0);
        return wcCelda;
    }

    private static Paragraph wcEspacio(float wcAlto) {
        Paragraph wcParrafo = new Paragraph(" ");
        wcParrafo.setLeading(wcAlto);
        return wcParrafo;
    }

    private static Font wcFuente(float wcTamanio, int wcEstilo, Color wcColor) {
        return new Font(Font.HELVETICA, wcTamanio, wcEstilo, wcColor);
    }
}
