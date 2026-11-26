package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcEstadoOrden;
import com.taller.mecanico.model.WcLineaRepuesto;
import com.taller.mecanico.model.WcLineaServicio;
import com.taller.mecanico.model.WcOrdenServicio;
import com.taller.mecanico.model.WcMetodoPago;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcServicio;
import com.taller.mecanico.model.WcTipoMovimiento;
import com.taller.mecanico.model.WcUsuario;
import com.taller.mecanico.model.WcVehiculo;
import com.taller.mecanico.repository.WcOrdenServicioRepository;
import com.taller.mecanico.repository.WcRepuestoRepository;
import com.taller.mecanico.repository.WcServicioRepository;
import com.taller.mecanico.repository.WcUsuarioRepository;
import com.taller.mecanico.repository.WcVehiculoRepository;
import com.taller.mecanico.seguridad.WcSesion;
import com.taller.mecanico.service.WcCalculadoraIgv.WcTotales;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WcOrdenServicioService {
    private static final int WC_LONGITUD_MAXIMA_PROBLEMA = 500;
    private static final int WC_LONGITUD_MAXIMA_DIAGNOSTICO = 1000;

    private final WcOrdenServicioRepository wcRepository;
    private final WcVehiculoRepository wcVehiculoRepository;
    private final WcUsuarioRepository wcUsuarioRepository;
    private final WcServicioRepository wcServicioRepository;
    private final WcRepuestoRepository wcRepuestoRepository;
    private final WcInventarioService wcInventarioService;
    private final WcSesion wcSesion;

    public record WcNuevaOrden(Long wcVehiculoId, Long wcMecanicoId, Integer wcKilometraje, String wcProblemaReportado) {}

    public record WcResumenTaller(long wcEnTaller, long wcListasParaEntrega, long wcIngresadasEnElMes,
                                BigDecimal wcFacturadoEnElMes) {}

    public Page<WcOrdenServicio> wcListar(String wcTexto, WcEstadoOrden wcEstado, int wcPagina, int wcTamanio) {
        Long wcSoloDelMecanico = wcSesion.wcTiene(WcPermiso.WC_VER_TODAS_LAS_ORDENES) ? null : wcSesion.wcUsuarioActual().getWcId();
        return wcRepository.wcBuscar(wcTexto == null ? "" : wcTexto.trim(), wcEstado, wcSoloDelMecanico,
                PageRequest.of(wcPagina, wcTamanio, Sort.by("wcFechaIngreso").descending()));
    }

    public WcOrdenServicio wcObtener(Long wcId) {
        return wcConDetalle(wcBuscarConAcceso(wcId));
    }

    public WcResumenTaller wcResumen() {
        LocalDateTime wcAhora = LocalDateTime.now();
        LocalDateTime wcInicioDeMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return new WcResumenTaller(
                wcRepository.countByWcEstadoIn(WcEstadoOrden.wcEnTaller()),
                wcRepository.countByWcEstado(WcEstadoOrden.WC_LISTO),
                wcRepository.countByWcFechaIngresoBetween(wcInicioDeMes, wcAhora),
                wcADecimal(wcRepository.wcSumarTotal(WcEstadoOrden.WC_ENTREGADO, wcInicioDeMes, wcAhora)));
    }

    @Transactional
    public WcOrdenServicio wcRegistrar(WcNuevaOrden wcNueva) {
        wcSesion.wcExigir(WcPermiso.WC_CREAR_ORDENES);
        wcValidarNuevaOrden(wcNueva);
        WcVehiculo wcVehiculo = wcVehiculoRepository.findById(wcNueva.wcVehiculoId())
                .orElseThrow(() -> new WcNegocioException("El vehiculo ya no existe en la base de datos."));
        wcVehiculo.wcActualizarKilometraje(wcNueva.wcKilometraje());

        WcOrdenServicio wcOrden = wcRepository.save(WcOrdenServicio.builder()
                .wcCodigo(WcOrdenServicio.wcFormatearCodigo(wcRepository.wcUltimoNumero() + 1))
                .wcFechaIngreso(LocalDateTime.now())
                .wcVehiculo(wcVehiculo)
                .wcMecanico(wcNueva.wcMecanicoId() == null ? null : wcBuscarMecanico(wcNueva.wcMecanicoId()))
                .wcKilometrajeIngreso(wcNueva.wcKilometraje())
                .wcProblemaReportado(wcNueva.wcProblemaReportado().trim())
                .wcEstado(WcEstadoOrden.WC_RECIBIDO)
                .build());
        log.info("Orden registrada: {} placa={}", wcOrden.getWcCodigo(), wcVehiculo.getWcPlaca());
        return wcOrden;
    }

    @Transactional
    public WcOrdenServicio wcAsignarMecanico(Long wcOrdenId, Long wcMecanicoId) {
        wcSesion.wcExigir(WcPermiso.WC_CREAR_ORDENES);
        WcOrdenServicio wcOrden = wcBuscarConAcceso(wcOrdenId);
        wcExigirEnTaller(wcOrden);
        wcOrden.setWcMecanico(wcBuscarMecanico(wcMecanicoId));
        return wcConDetalle(wcOrden);
    }

    @Transactional
    public WcOrdenServicio wcGuardarDiagnostico(Long wcOrdenId, String wcDiagnostico) {
        WcOrdenServicio wcOrden = wcBuscarParaTrabajar(wcOrdenId);
        String wcTexto = wcDiagnostico == null ? "" : wcDiagnostico.trim();
        if (wcTexto.length() > WC_LONGITUD_MAXIMA_DIAGNOSTICO) {
            throw new WcNegocioException("El diagnostico no puede superar " + WC_LONGITUD_MAXIMA_DIAGNOSTICO + " caracteres.");
        }
        wcOrden.setWcDiagnostico(wcTexto.isEmpty() ? null : wcTexto);
        return wcConDetalle(wcOrden);
    }

    @Transactional
    public WcOrdenServicio wcCambiarEstado(Long wcOrdenId, WcEstadoOrden wcDestino) {
        if (wcDestino == WcEstadoOrden.WC_ENTREGADO) {
            throw new WcNegocioException("Para entregar la orden registra primero el pago.");
        }
        WcOrdenServicio wcOrden = wcBuscarConAcceso(wcOrdenId);
        wcSesion.wcExigir(wcDestino.getWcPermisoRequerido());
        wcValidarCambioDeEstado(wcOrden, wcDestino);
        if (wcDestino == WcEstadoOrden.WC_CANCELADO) {
            wcDevolverRepuestos(wcOrden);
        }
        wcOrden.setWcEstado(wcDestino);
        log.info("Orden {}: estado {}", wcOrden.getWcCodigo(), wcDestino);
        return wcConDetalle(wcOrden);
    }

    @Transactional
    public WcOrdenServicio wcEntregar(Long wcOrdenId, WcMetodoPago wcMetodoPago) {
        if (wcMetodoPago == null) {
            throw new WcNegocioException("Selecciona el metodo de pago.");
        }
        WcOrdenServicio wcOrden = wcBuscarConAcceso(wcOrdenId);
        wcSesion.wcExigir(WcEstadoOrden.WC_ENTREGADO.getWcPermisoRequerido());
        wcValidarCambioDeEstado(wcOrden, WcEstadoOrden.WC_ENTREGADO);
        wcOrden.setWcMetodoPago(wcMetodoPago);
        wcOrden.setWcFechaEntrega(LocalDateTime.now());
        wcOrden.setWcEstado(WcEstadoOrden.WC_ENTREGADO);
        log.info("Orden {} entregada, pago {}", wcOrden.getWcCodigo(), wcMetodoPago);
        return wcConDetalle(wcOrden);
    }

    @Transactional
    public WcOrdenServicio wcAgregarServicio(Long wcOrdenId, Long wcServicioId) {
        WcOrdenServicio wcOrden = wcBuscarParaTrabajar(wcOrdenId);
        WcServicio wcServicio = wcServicioRepository.findById(wcServicioId)
                .filter(WcServicio::isWcActivo)
                .orElseThrow(() -> new WcNegocioException("El servicio no esta disponible."));
        boolean wcYaRegistrado = wcOrden.getWcServicios().stream()
                .anyMatch(wcL -> wcL.getWcServicio().getWcId().equals(wcServicioId));
        if (wcYaRegistrado) {
            throw new WcNegocioException(wcServicio.getWcNombre() + " ya figura en la orden.");
        }
        wcOrden.wcAgregarServicio(wcServicio);
        return wcGuardarConTotales(wcOrden);
    }

    @Transactional
    public WcOrdenServicio wcQuitarServicio(Long wcOrdenId, Long wcLineaId) {
        WcOrdenServicio wcOrden = wcBuscarParaTrabajar(wcOrdenId);
        wcOrden.getWcServicios().removeIf(wcL -> wcL.getWcId().equals(wcLineaId));
        return wcGuardarConTotales(wcOrden);
    }

    @Transactional
    public WcOrdenServicio wcAgregarRepuesto(Long wcOrdenId, Long wcRepuestoId, int wcCantidad) {
        if (wcCantidad <= 0) {
            throw new WcNegocioException("La cantidad debe ser mayor a cero.");
        }
        WcOrdenServicio wcOrden = wcBuscarParaTrabajar(wcOrdenId);
        WcRepuesto wcRepuesto = wcRepuestoRepository.findById(wcRepuestoId)
                .orElseThrow(() -> new WcNegocioException("El repuesto ya no existe en la base de datos."));
        wcInventarioService.wcSalida(wcRepuesto, wcCantidad, WcTipoMovimiento.WC_CONSUMO_ORDEN, wcOrden.getWcCodigo(),
                LocalDateTime.now(), null, null);
        wcOrden.wcAgregarRepuesto(wcRepuesto, wcCantidad);
        return wcGuardarConTotales(wcOrden);
    }

    @Transactional
    public WcOrdenServicio wcQuitarRepuesto(Long wcOrdenId, Long wcLineaId) {
        WcOrdenServicio wcOrden = wcBuscarParaTrabajar(wcOrdenId);
        WcLineaRepuesto wcLinea = wcOrden.getWcRepuestos().stream()
                .filter(wcL -> wcL.getWcId().equals(wcLineaId))
                .findFirst()
                .orElseThrow(() -> new WcNegocioException("El repuesto ya no figura en la orden."));
        wcDevolverAlInventario(wcOrden, wcLinea);
        wcOrden.getWcRepuestos().remove(wcLinea);
        return wcGuardarConTotales(wcOrden);
    }

    private void wcValidarNuevaOrden(WcNuevaOrden wcNueva) {
        if (wcNueva.wcVehiculoId() == null) {
            throw new WcNegocioException("Selecciona el vehiculo.");
        }
        if (wcNueva.wcKilometraje() == null || wcNueva.wcKilometraje() < 0) {
            throw new WcNegocioException("Ingresa el kilometraje actual del vehiculo.");
        }
        String wcProblema = wcNueva.wcProblemaReportado() == null ? "" : wcNueva.wcProblemaReportado().trim();
        if (wcProblema.isEmpty()) {
            throw new WcNegocioException("Describe el problema que reporta el cliente.");
        }
        if (wcProblema.length() > WC_LONGITUD_MAXIMA_PROBLEMA) {
            throw new WcNegocioException("El problema reportado no puede superar " + WC_LONGITUD_MAXIMA_PROBLEMA + " caracteres.");
        }
    }

    private void wcValidarCambioDeEstado(WcOrdenServicio wcOrden, WcEstadoOrden wcDestino) {
        if (!wcOrden.getWcEstado().wcPuedePasarA(wcDestino)) {
            throw new WcNegocioException("La orden " + wcOrden.getWcCodigo() + " no puede pasar de \""
                    + wcOrden.getWcEstado() + "\" a \"" + wcDestino + "\".");
        }
        if (wcDestino == WcEstadoOrden.WC_EN_REPARACION && wcOrden.getWcMecanico() == null) {
            throw new WcNegocioException("Asigna un mecanico antes de iniciar la reparacion.");
        }
        boolean wcSinTrabajos = wcOrden.getWcServicios().isEmpty() && wcOrden.getWcRepuestos().isEmpty();
        if (wcDestino == WcEstadoOrden.WC_LISTO && wcSinTrabajos) {
            throw new WcNegocioException("Registra al menos un servicio o repuesto antes de marcarla como lista.");
        }
    }

    private void wcDevolverRepuestos(WcOrdenServicio wcOrden) {
        wcOrden.getWcRepuestos().forEach(wcLinea -> wcDevolverAlInventario(wcOrden, wcLinea));
    }

    private void wcDevolverAlInventario(WcOrdenServicio wcOrden, WcLineaRepuesto wcLinea) {
        wcInventarioService.wcEntrada(wcLinea.getWcRepuesto(), wcLinea.getWcCantidad(), null, WcTipoMovimiento.WC_DEVOLUCION_ORDEN,
                wcOrden.getWcCodigo(), LocalDateTime.now(), null, null);
    }

    private WcOrdenServicio wcGuardarConTotales(WcOrdenServicio wcOrden) {
        BigDecimal wcTotalServicios = wcOrden.getWcServicios().stream()
                .map(WcLineaServicio::getWcPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal wcTotalRepuestos = wcOrden.getWcRepuestos().stream()
                .map(WcLineaRepuesto::getWcImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        WcTotales wcTotales = WcCalculadoraIgv.wcCalcular(List.of(wcTotalServicios, wcTotalRepuestos), true, true);

        wcOrden.setWcTotalServicios(wcTotalServicios);
        wcOrden.setWcTotalRepuestos(wcTotalRepuestos);
        wcOrden.setWcBaseImponible(wcTotales.wcBaseImponible());
        wcOrden.setWcIgv(wcTotales.wcIgv());
        wcOrden.setWcTotal(wcTotales.wcTotal());
        return wcConDetalle(wcRepository.saveAndFlush(wcOrden));
    }

    private WcOrdenServicio wcBuscarParaTrabajar(Long wcOrdenId) {
        wcSesion.wcExigir(WcPermiso.WC_TRABAJAR_ORDENES);
        WcOrdenServicio wcOrden = wcBuscarConAcceso(wcOrdenId);
        if (!wcOrden.getWcEstado().wcEsEditable()) {
            throw new WcNegocioException("La orden " + wcOrden.getWcCodigo() + " esta en estado \""
                    + wcOrden.getWcEstado() + "\" y ya no se puede modificar.");
        }
        return wcOrden;
    }

    private WcOrdenServicio wcBuscarConAcceso(Long wcOrdenId) {
        WcOrdenServicio wcOrden = wcRepository.findById(wcOrdenId)
                .orElseThrow(() -> new WcNegocioException("La orden ya no existe en la base de datos."));
        boolean wcEsAjena = !wcSesion.wcTiene(WcPermiso.WC_VER_TODAS_LAS_ORDENES)
                && !wcOrden.wcEstaAsignadaA(wcSesion.wcUsuarioActual());
        if (wcEsAjena) {
            throw new WcNegocioException("La orden " + wcOrden.getWcCodigo() + " esta asignada a otro mecanico.");
        }
        return wcOrden;
    }

    private void wcExigirEnTaller(WcOrdenServicio wcOrden) {
        if (!wcOrden.getWcEstado().wcEstaEnTaller()) {
            throw new WcNegocioException("La orden " + wcOrden.getWcCodigo() + " ya esta cerrada.");
        }
    }

    private WcUsuario wcBuscarMecanico(Long wcMecanicoId) {
        return wcUsuarioRepository.findById(wcMecanicoId)
                .filter(WcUsuario::wcEsMecanico)
                .filter(WcUsuario::isWcActivo)
                .orElseThrow(() -> new WcNegocioException("El mecanico seleccionado no esta disponible."));
    }

    private static BigDecimal wcADecimal(Number wcNumero) {
        return new BigDecimal(wcNumero.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private WcOrdenServicio wcConDetalle(WcOrdenServicio wcOrden) {
        Hibernate.initialize(wcOrden.getWcServicios());
        Hibernate.initialize(wcOrden.getWcRepuestos());
        return wcOrden;
    }
}
