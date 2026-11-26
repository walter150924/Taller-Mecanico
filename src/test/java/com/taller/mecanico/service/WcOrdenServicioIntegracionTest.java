package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcEstadoOrden;
import com.taller.mecanico.model.WcMetodoPago;
import com.taller.mecanico.model.WcOrdenServicio;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcServicio;
import com.taller.mecanico.model.WcUsuario;
import com.taller.mecanico.model.WcVehiculo;
import com.taller.mecanico.repository.WcRepuestoRepository;
import com.taller.mecanico.seguridad.WcSesion;
import com.taller.mecanico.service.WcOrdenServicioService.WcNuevaOrden;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class WcOrdenServicioIntegracionTest {
    private static final String WC_CLAVE_DE_PRUEBA = "taller123";

    @DynamicPropertySource
    static void wcBaseTemporal(DynamicPropertyRegistry wcRegistro) throws Exception {
        File wcDb = Files.createTempFile("ordenes-test", ".db").toFile();
        wcDb.deleteOnExit();
        wcRegistro.add("spring.datasource.url", () -> "jdbc:sqlite:" + wcDb.getAbsolutePath());
        wcRegistro.add("app.seeder.enabled", () -> "true");
        wcRegistro.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired WcOrdenServicioService wcOrdenService;
    @Autowired WcUsuarioService wcUsuarioService;
    @Autowired WcVehiculoService wcVehiculoService;
    @Autowired WcServicioService wcServicioService;
    @Autowired WcRepuestoRepository wcRepuestoRepository;
    @Autowired WcSesion wcSesion;

    @AfterEach
    void wcCerrarSesion() {
        wcSesion.wcCerrar();
    }

    @Test
    void wcCompletaElFlujoDeUnaOrdenYDescuentaElInventario() {
        WcUsuario wcMecanico = wcUsuarioService.wcIniciarSesion("jquispe", WC_CLAVE_DE_PRUEBA);
        wcUsuarioService.wcIniciarSesion("recepcion", WC_CLAVE_DE_PRUEBA);
        WcOrdenServicio wcOrden = wcRegistrarOrden(wcMecanico);
        WcRepuesto wcRepuesto = wcRepuestoConStock();
        int wcStockInicial = wcRepuesto.getWcStock();

        wcUsuarioService.wcIniciarSesion("jquispe", WC_CLAVE_DE_PRUEBA);
        wcOrdenService.wcCambiarEstado(wcOrden.getWcId(), WcEstadoOrden.WC_EN_DIAGNOSTICO);
        wcOrdenService.wcGuardarDiagnostico(wcOrden.getWcId(), "Pastillas gastadas");
        wcOrdenService.wcAgregarServicio(wcOrden.getWcId(), wcPrimerServicio().getWcId());
        wcOrdenService.wcAgregarRepuesto(wcOrden.getWcId(), wcRepuesto.getWcId(), 2);
        wcOrdenService.wcCambiarEstado(wcOrden.getWcId(), WcEstadoOrden.WC_EN_REPARACION);
        WcOrdenServicio wcLista = wcOrdenService.wcCambiarEstado(wcOrden.getWcId(), WcEstadoOrden.WC_LISTO);

        assertThat(wcStockDe(wcRepuesto)).isEqualTo(wcStockInicial - 2);
        assertThat(wcLista.getWcBaseImponible().add(wcLista.getWcIgv())).isEqualByComparingTo(wcLista.getWcTotal());
        assertThat(wcLista.getWcTotal()).isEqualByComparingTo(wcLista.getWcTotalServicios().add(wcLista.getWcTotalRepuestos()));
        assertThatThrownBy(() -> wcOrdenService.wcEntregar(wcOrden.getWcId(), WcMetodoPago.WC_EFECTIVO))
                .isInstanceOf(WcNegocioException.class);

        wcUsuarioService.wcIniciarSesion("recepcion", WC_CLAVE_DE_PRUEBA);
        assertThatThrownBy(() -> wcOrdenService.wcEntregar(wcOrden.getWcId(), null))
                .isInstanceOf(WcNegocioException.class)
                .hasMessageContaining("metodo de pago");
        WcOrdenServicio wcEntregada = wcOrdenService.wcEntregar(wcOrden.getWcId(), WcMetodoPago.WC_YAPE);
        assertThat(wcEntregada.getWcFechaEntrega()).isNotNull();
        assertThat(wcEntregada.getWcMetodoPago()).isEqualTo(WcMetodoPago.WC_YAPE);
        assertThat(wcVehiculoService.wcHistorial(wcOrden.getWcVehiculo().getWcId())).extracting(WcOrdenServicio::getWcCodigo)
                .contains(wcOrden.getWcCodigo());
    }

    @Test
    void wcAlCancelarDevuelveLosRepuestosAlInventario() {
        wcUsuarioService.wcIniciarSesion("recepcion", WC_CLAVE_DE_PRUEBA);
        WcOrdenServicio wcOrden = wcRegistrarOrden(null);
        WcRepuesto wcRepuesto = wcRepuestoConStock();
        int wcStockInicial = wcRepuesto.getWcStock();

        wcOrdenService.wcAgregarRepuesto(wcOrden.getWcId(), wcRepuesto.getWcId(), 3);
        assertThat(wcStockDe(wcRepuesto)).isEqualTo(wcStockInicial - 3);

        wcOrdenService.wcCambiarEstado(wcOrden.getWcId(), WcEstadoOrden.WC_CANCELADO);
        assertThat(wcStockDe(wcRepuesto)).isEqualTo(wcStockInicial);
    }

    @Test
    void wcUnMecanicoSoloVeSusOrdenes() {
        WcUsuario wcAsignado = wcUsuarioService.wcIniciarSesion("jquispe", WC_CLAVE_DE_PRUEBA);
        wcUsuarioService.wcIniciarSesion("recepcion", WC_CLAVE_DE_PRUEBA);
        WcOrdenServicio wcOrden = wcRegistrarOrden(wcAsignado);

        wcUsuarioService.wcIniciarSesion("pmamani", WC_CLAVE_DE_PRUEBA);
        assertThat(wcOrdenService.wcListar("", null, 0, 50).getContent()).extracting(WcOrdenServicio::getWcId)
                .doesNotContain(wcOrden.getWcId());
        assertThatThrownBy(() -> wcOrdenService.wcObtener(wcOrden.getWcId()))
                .isInstanceOf(WcNegocioException.class);
        assertThatThrownBy(() -> wcOrdenService.wcRegistrar(new WcNuevaOrden(wcOrden.getWcVehiculo().getWcId(), null, 1, "x")))
                .isInstanceOf(WcNegocioException.class);
    }

    @Test
    void wcRechazaCredencialesIncorrectas() {
        assertThatThrownBy(() -> wcUsuarioService.wcIniciarSesion("admin", "incorrecta"))
                .isInstanceOf(WcNegocioException.class)
                .hasMessageContaining("incorrectos");
    }

    private WcOrdenServicio wcRegistrarOrden(WcUsuario wcMecanico) {
        WcVehiculo wcVehiculo = wcVehiculoService.wcListar("", 0, 1).getContent().get(0);
        return wcOrdenService.wcRegistrar(new WcNuevaOrden(wcVehiculo.getWcId(),
                wcMecanico == null ? null : wcMecanico.getWcId(),
                wcVehiculo.getWcKilometraje() + 100, "Ruido al frenar"));
    }

    private WcRepuesto wcRepuestoConStock() {
        return wcRepuestoRepository.findAll().stream()
                .filter(wcR -> wcR.getWcStock() >= 5)
                .findFirst()
                .orElseThrow();
    }

    private WcServicio wcPrimerServicio() {
        return wcServicioService.wcActivos().get(0);
    }

    private int wcStockDe(WcRepuesto wcRepuesto) {
        return wcRepuestoRepository.findById(wcRepuesto.getWcId()).orElseThrow().getWcStock();
    }
}
