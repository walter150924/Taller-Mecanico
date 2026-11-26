package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCompra;
import com.taller.mecanico.model.WcMetodoPago;
import com.taller.mecanico.model.WcMovimientoInventario;
import com.taller.mecanico.model.WcCliente;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcTipoComprobante;
import com.taller.mecanico.model.WcTipoDocumentoCompra;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.repository.WcCompraRepository;
import com.taller.mecanico.repository.WcRepuestoRepository;
import com.taller.mecanico.service.WcCompraService.WcLineaCompra;
import com.taller.mecanico.service.WcCompraService.WcNuevaCompra;
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
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class WcInventarioIntegracionTest {
    @DynamicPropertySource
    static void wcBaseTemporal(DynamicPropertyRegistry wcRegistro) throws Exception {
        File wcDb = Files.createTempFile("inventario-test", ".db").toFile();
        wcDb.deleteOnExit();
        wcRegistro.add("spring.datasource.url", () -> "jdbc:sqlite:" + wcDb.getAbsolutePath());
        wcRegistro.add("app.seeder.enabled", () -> "true");
        wcRegistro.add("spring.jpa.show-sql", () -> "false");
        wcRegistro.add("logging.level.org.hibernate.SQL", () -> "WARN");
    }

    @Autowired WcInventarioService wcInventarioService;
    @Autowired WcCompraService wcCompraService;
    @Autowired WcVentaService wcVentaService;
    @Autowired WcRepuestoService wcRepuestoService;
    @Autowired WcClienteService wcClienteService;
    @Autowired WcRepuestoRepository wcRepuestoRepository;
    @Autowired WcCompraRepository wcCompraRepository;
    @Autowired WcReporteInventarioExcelService wcExcelService;
    @Autowired WcUsuarioService wcUsuarioService;

    @BeforeEach
    void wcIniciarSesionComoAdministrador() {
        wcUsuarioService.wcIniciarSesion("admin", "admin123");
    }

    @Test
    void wcExportaInventarioYComprasAExcel() throws Exception {
        File wcInventario = Files.createTempFile("inventario", ".xlsx").toFile();
        wcExcelService.wcGenerarInventario(wcInventario);
        try (XSSFWorkbook wcLibro = new XSSFWorkbook(new FileInputStream(wcInventario))) {
            assertThat(wcLibro.getSheet("Inventario").getLastRowNum()).isGreaterThanOrEqualTo(100);
            assertThat(wcLibro.getSheet("Kardex").getLastRowNum()).isGreaterThan(100);
        }
        File wcCompras = Files.createTempFile("compras", ".xlsx").toFile();
        wcExcelService.wcGenerarCompras(LocalDate.now().minusYears(1), LocalDate.now(), wcCompras);
        try (XSSFWorkbook wcLibro = new XSSFWorkbook(new FileInputStream(wcCompras))) {
            assertThat(wcLibro.getSheet("Compras").getLastRowNum()).isGreaterThan(5);
            assertThat(wcLibro.getSheet("Detalle").getLastRowNum()).isGreaterThan(100);
        }
    }

    @Test
    void wcElKardexCuadraConElStockDeCadaRepuesto() {
        assertThat(wcCompraRepository.count()).isGreaterThan(5);
        for (WcRepuesto wcP : wcRepuestoRepository.findAll()) {
            List<WcMovimientoInventario> wcKardex = wcInventarioService.wcKardex(wcP.getWcId());
            int wcSaldo = 0;
            for (WcMovimientoInventario wcM : wcKardex) {
                wcSaldo += wcM.getWcEntrada() - wcM.getWcSalida();
                assertThat(wcSaldo).as("saldo nunca negativo en %s", wcP.getWcCodigoRepuesto()).isGreaterThanOrEqualTo(0);
                assertThat(wcM.getWcStockResultante()).as("saldo cronologico de %s", wcP.getWcCodigoRepuesto()).isEqualTo(wcSaldo);
            }
            assertThat(wcP.getWcStock()).as("stock de %s", wcP.getWcCodigoRepuesto()).isEqualTo(wcSaldo);
            if (!wcKardex.isEmpty()) {
                assertThat(wcP.getWcCostoPromedio()).isPositive();
            }
        }
    }

    @Test
    void wcCompraSumaVentaRestaYAnulacionesRevierten() {
        WcRepuesto wcP = wcRepuestoService.wcAutocompletar("REP-010").get(0);
        WcCliente wcCliente = wcClienteService.wcListar("", 0, 1).getContent().get(0);
        int wcInicial = wcStock(wcP);

        WcCompra wcCompra = wcCompraService.wcRegistrar(new WcNuevaCompra("20100055237", "ALICORP S.A.A.",
                WcTipoDocumentoCompra.WC_FACTURA, "F001-999001", LocalDate.now(), true, true, null,
                List.of(new WcLineaCompra(wcP.getWcId(), 10, new BigDecimal("11.80")))));
        assertThat(wcStock(wcP)).isEqualTo(wcInicial + 10);
        assertThat(wcCompra.getWcBaseImponible()).isEqualByComparingTo("100.00");
        assertThat(wcCompra.getWcIgv()).isEqualByComparingTo("18.00");

        WcVenta wcVenta = wcVentaService.wcRegistrar(new WcNuevaVenta(WcTipoComprobante.WC_BOLETA, wcCliente.getWcId(), true, true,
                null, List.of(new WcLineaCarrito(wcP.getWcId(), 4, wcP.getWcPrecio())), WcMetodoPago.WC_EFECTIVO));
        assertThat(wcStock(wcP)).isEqualTo(wcInicial + 6);

        assertThatThrownBy(() -> wcVentaService.wcRegistrar(new WcNuevaVenta(WcTipoComprobante.WC_BOLETA, wcCliente.getWcId(),
                true, true, null, List.of(new WcLineaCarrito(wcP.getWcId(), wcInicial + 1000, wcP.getWcPrecio())), WcMetodoPago.WC_EFECTIVO)))
                .isInstanceOf(WcNegocioException.class).hasMessageContaining("Stock insuficiente");
        assertThat(wcStock(wcP)).as("una venta rechazada no toca el stock").isEqualTo(wcInicial + 6);

        wcVentaService.wcAnular(wcVenta.getWcId());
        assertThat(wcStock(wcP)).isEqualTo(wcInicial + 10);

        wcCompraService.wcAnular(wcCompra.getWcId());
        assertThat(wcStock(wcP)).isEqualTo(wcInicial);

        assertThatThrownBy(() -> wcCompraService.wcRegistrar(new WcNuevaCompra("20100055237", "ALICORP S.A.A.",
                WcTipoDocumentoCompra.WC_FACTURA, "F001-999001", LocalDate.now().plusDays(3), true, true, null,
                List.of(new WcLineaCompra(wcP.getWcId(), 1, BigDecimal.ONE)))))
                .isInstanceOf(WcNegocioException.class);
    }

    @Test
    void wcNoSePuedeAnularUnaCompraYaVendida() {
        WcRepuesto wcP = wcRepuestoService.wcAutocompletar("REP-020").get(0);
        WcCliente wcCliente = wcClienteService.wcListar("", 0, 1).getContent().get(0);
        int wcInicial = wcStock(wcP);

        WcCompra wcCompra = wcCompraService.wcRegistrar(new WcNuevaCompra("20611122233", "PET FOOD DISTRIBUCIONES S.A.C.",
                WcTipoDocumentoCompra.WC_FACTURA, "F002-000777", LocalDate.now(), true, false, null,
                List.of(new WcLineaCompra(wcP.getWcId(), 5, new BigDecimal("2.00")))));
        wcVentaService.wcRegistrar(new WcNuevaVenta(WcTipoComprobante.WC_BOLETA, wcCliente.getWcId(), true, true, null,
                List.of(new WcLineaCarrito(wcP.getWcId(), wcInicial + 2, wcP.getWcPrecio())), WcMetodoPago.WC_EFECTIVO));

        assertThatThrownBy(() -> wcCompraService.wcAnular(wcCompra.getWcId()))
                .isInstanceOf(WcNegocioException.class).hasMessageContaining("ya se vendio");
        assertThat(wcStock(wcP)).isEqualTo(3);
    }

    @Test
    void wcEditarRepuestoConservaElStockYProveedorNuevoSeRegistra() {
        WcRepuesto wcP = wcRepuestoService.wcAutocompletar("REP-030").get(0);
        int wcStock = wcStock(wcP);
        wcP.setWcNombre(wcP.getWcNombre() + " (editado)");
        wcP.setWcStock(0);
        wcRepuestoService.wcGuardar(wcP);
        assertThat(wcStock(wcP)).isEqualTo(wcStock);

        wcCompraService.wcRegistrar(new WcNuevaCompra("20999888771", "Proveedor Nuevo S.A.C.",
                WcTipoDocumentoCompra.WC_BOLETA, "B001-12", LocalDate.now(), false, true, null,
                List.of(new WcLineaCompra(wcP.getWcId(), 2, new BigDecimal("5.00")))));
        assertThat(wcStock(wcP)).isEqualTo(wcStock + 2);
    }

    @Test
    void wcCostoPromedioPonderado() {
        assertThat(WcInventarioService.wcCostoPromedio(new BigDecimal("10"), 10, new BigDecimal("16"), 5))
                .isEqualByComparingTo("12.0000");
        assertThat(WcInventarioService.wcCostoPromedio(null, 0, new BigDecimal("7.5"), 3))
                .isEqualByComparingTo("7.5");
    }

    private int wcStock(WcRepuesto wcP) {
        return wcRepuestoRepository.findById(wcP.getWcId()).orElseThrow().getWcStock();
    }
}
