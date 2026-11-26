package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCliente;
import com.taller.mecanico.model.WcMetodoPago;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcTipoComprobante;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.repository.WcVentaRepository;
import com.taller.mecanico.service.WcVentaService.WcLineaCarrito;
import com.taller.mecanico.service.WcVentaService.WcNuevaVenta;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class WcVentasIntegracionTest {
    @DynamicPropertySource
    static void wcBaseTemporal(DynamicPropertyRegistry wcRegistro) throws Exception {
        File wcDb = Files.createTempFile("ventas-test", ".db").toFile();
        wcDb.deleteOnExit();
        wcRegistro.add("spring.datasource.url", () -> "jdbc:sqlite:" + wcDb.getAbsolutePath());
        wcRegistro.add("app.seeder.enabled", () -> "true");
        wcRegistro.add("spring.jpa.show-sql", () -> "false");
        wcRegistro.add("logging.level.org.hibernate.SQL", () -> "WARN");
    }

    @Autowired WcVentaService wcVentaService;
    @Autowired WcVentaRepository wcVentaRepository;
    @Autowired WcClienteService wcClienteService;
    @Autowired WcRepuestoService wcRepuestoService;
    @Autowired WcReporteVentasService wcReporteService;
    @Autowired WcComprobantePdfService wcPdfService;
    @Autowired WcReporteExcelService wcExcelService;
    @Autowired WcUsuarioService wcUsuarioService;

    @BeforeEach
    void wcIniciarSesionComoAdministrador() {
        wcUsuarioService.wcIniciarSesion("admin", "admin123");
    }

    @Test
    void wcSeederGeneraVentasConsistentes() {
        List<WcVenta> wcVentas = wcVentaRepository.findAll();
        assertThat(wcVentas).hasSizeGreaterThanOrEqualTo(120);

        Set<String> wcComprobantes = new HashSet<>();
        for (WcVenta wcV : wcVentas) {
            assertThat(wcComprobantes.add(wcV.getWcComprobante())).as("comprobante unico").isTrue();
            assertThat(wcV.getWcBaseImponible().add(wcV.getWcIgv())).isEqualByComparingTo(wcV.getWcTotal());
            if (!wcV.isWcAplicaIgv()) {
                assertThat(wcV.getWcIgv()).isEqualByComparingTo("0");
            }
        }
        assertThat(wcVentas).anyMatch(WcVenta::isWcAnulada);
    }

    @Test
    void wcAutocompletadoDeClientesYRepuestos() {
        WcCliente wcAlguien = wcClienteService.wcListar("", 0, 1).getContent().get(0);

        assertThat(wcClienteService.wcAutocompletar(wcAlguien.getWcDni().substring(0, 4)))
                .allMatch(wcP -> wcP.getWcDni().startsWith(wcAlguien.getWcDni().substring(0, 4)));

        String wcInverso = wcAlguien.getWcApellido().split(" ")[0] + " " + wcAlguien.getWcNombre();
        assertThat(wcClienteService.wcAutocompletar(wcInverso)).contains(wcAlguien);
        assertThat(wcClienteService.wcBuscarPorDni(wcAlguien.getWcDni())).contains(wcAlguien);

        assertThat(wcRepuestoService.wcAutocompletar("aceite")).isNotEmpty()
                .allMatch(wcP -> wcP.getWcNombre().toLowerCase().contains("aceite"));
        assertThat(wcRepuestoService.wcAutocompletar("REP-001")).extracting(WcRepuesto::getWcCodigoRepuesto)
                .contains("REP-001");
    }

    @Test
    void wcRegistraVentaConCorrelativoEIgv() {
        WcCliente wcCliente = wcClienteService.wcListar("", 0, 1).getContent().get(0);
        WcRepuesto wcRepuesto = wcRepuestoService.wcAutocompletar("REP-001").get(0);
        String wcEsperado = wcVentaService.wcSiguienteComprobante(WcTipoComprobante.WC_BOLETA);

        WcVenta wcVenta = wcVentaService.wcRegistrar(new WcNuevaVenta(WcTipoComprobante.WC_BOLETA, wcCliente.getWcId(),
                true, false, "Prueba", List.of(new WcLineaCarrito(wcRepuesto.getWcId(), 2, new BigDecimal("50.00"))), WcMetodoPago.WC_EFECTIVO));

        assertThat(wcVenta.getWcComprobante()).isEqualTo(wcEsperado);
        assertThat(wcVenta.getWcBaseImponible()).isEqualByComparingTo("100.00");
        assertThat(wcVenta.getWcIgv()).isEqualByComparingTo("18.00");
        assertThat(wcVenta.getWcTotal()).isEqualByComparingTo("118.00");

        assertThatThrownBy(() -> wcClienteService.wcEliminar(wcCliente.getWcId()))
                .isInstanceOf(WcNegocioException.class);
        assertThatThrownBy(() -> wcRepuestoService.wcEliminar(wcRepuesto.getWcId()))
                .isInstanceOf(WcNegocioException.class);
    }

    @Test
    void wcGeneraPdfYExcel() throws Exception {
        WcVenta wcVenta = wcVentaRepository.findAll().get(0);
        File wcPdf = Files.createTempFile("comprobante", ".pdf").toFile();
        wcPdfService.wcGenerar(wcVenta.getWcId(), wcPdf);
        assertThat(Files.readString(wcPdf.toPath(), StandardCharsets.ISO_8859_1)).startsWith("%PDF");

        LocalDate wcHasta = LocalDate.now();
        LocalDate wcDesde = wcHasta.minusDays(119);
        File wcXlsx = Files.createTempFile("reporte", ".xlsx").toFile();
        wcExcelService.wcGenerar(wcDesde, wcHasta, wcXlsx);
        try (XSSFWorkbook wcLibro = new XSSFWorkbook(new FileInputStream(wcXlsx))) {
            assertThat(wcLibro.getNumberOfSheets()).isEqualTo(5);
            assertThat(wcLibro.getSheet("Ventas").getLastRowNum()).isGreaterThan(200);
            assertThat(wcLibro.getSheet("Detalle").getLastRowNum()).isGreaterThan(wcLibro.getSheet("Ventas").getLastRowNum());
        }

        var wcDiario = wcReporteService.wcResumen(wcHasta.minusDays(89), wcHasta);
        assertThat(wcDiario.wcSeriePorMes()).isFalse();
        assertThat(wcDiario.wcSerie()).hasSize(90);
        assertThat(wcDiario.wcBaseImponible().add(wcDiario.wcIgv())).isEqualByComparingTo(wcDiario.wcTotal());

        var wcMensual = wcReporteService.wcResumen(wcDesde, wcHasta);
        assertThat(wcMensual.wcSeriePorMes()).isTrue();
        assertThat(wcMensual.wcComprobantes() + wcMensual.wcAnuladas()).isGreaterThanOrEqualTo(250);
    }
}
