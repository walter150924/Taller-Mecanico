package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCliente;
import com.taller.mecanico.model.WcEstadoVenta;
import com.taller.mecanico.model.WcMetodoPago;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcTipoComprobante;
import com.taller.mecanico.model.WcTipoMovimiento;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.model.WcVentaDetalle;
import com.taller.mecanico.repository.WcClienteRepository;
import com.taller.mecanico.repository.WcRepuestoRepository;
import com.taller.mecanico.repository.WcVentaRepository;
import com.taller.mecanico.service.WcCalculadoraIgv.WcTotales;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WcVentaService {
    private static final LocalDate WC_FECHA_MINIMA = LocalDate.of(2000, 1, 1);
    private static final LocalDate WC_FECHA_MAXIMA = LocalDate.of(2100, 12, 31);
    private static final LocalTime WC_FIN_DEL_DIA = LocalTime.of(23, 59, 59, 999_000_000);

    private final WcVentaRepository wcRepository;
    private final WcClienteRepository wcClienteRepository;
    private final WcRepuestoRepository wcRepuestoRepository;
    private final WcInventarioService wcInventarioService;

    public record WcLineaCarrito(Long wcRepuestoId, int wcCantidad, BigDecimal wcPrecioUnitario) {}

    public record WcNuevaVenta(WcTipoComprobante wcTipo,
                             Long wcClienteId,
                             boolean wcAplicaIgv,
                             boolean wcPreciosIncluyenIgv,
                             String wcObservacion,
                             List<WcLineaCarrito> wcLineas,
                             WcMetodoPago wcMetodoPago) {}

    public record WcTotalizado(long wcComprobantes, BigDecimal wcBaseImponible, BigDecimal wcIgv, BigDecimal wcTotal) {}

    public Page<WcVenta> wcListar(String wcTexto, LocalDate wcDesde, LocalDate wcHasta, int wcPagina, int wcTamanio) {
        return wcRepository.wcBuscar(
                wcTexto == null ? "" : wcTexto.trim(),
                wcInicio(wcDesde), wcFin(wcHasta),
                PageRequest.of(wcPagina, wcTamanio, Sort.by("wcFecha").descending().and(Sort.by("wcId").descending())));
    }

    public WcTotalizado wcTotalizar(String wcTexto, LocalDate wcDesde, LocalDate wcHasta) {
        Object[] wcFila = wcRepository.wcTotalizar(wcTexto == null ? "" : wcTexto.trim(), WcEstadoVenta.WC_EMITIDA,
                wcInicio(wcDesde), wcFin(wcHasta)).get(0);
        return new WcTotalizado(((Number) wcFila[0]).longValue(), wcDecimal(wcFila[1]), wcDecimal(wcFila[2]), wcDecimal(wcFila[3]));
    }

    public WcVenta wcObtener(Long wcId) {
        return wcRepository.wcBuscarConDetalle(wcId)
                .orElseThrow(() -> new WcNegocioException("La venta ya no existe en la base de datos."));
    }

    public LocalDate wcPrimeraFecha() {
        LocalDateTime wcPrimera = wcRepository.wcPrimeraFecha();
        return wcPrimera == null ? LocalDate.now() : wcPrimera.toLocalDate();
    }

    public String wcSiguienteComprobante(WcTipoComprobante wcTipo) {
        return WcVenta.wcFormatearComprobante(wcTipo.getWcSerie(), wcSiguienteNumero(wcTipo.getWcSerie()));
    }

    @Transactional
    public WcVenta wcRegistrar(WcNuevaVenta wcSolicitud) {
        return wcRegistrar(wcSolicitud, LocalDateTime.now());
    }

    @Transactional
    public WcVenta wcRegistrar(WcNuevaVenta wcSolicitud, LocalDateTime wcFecha) {
        wcValidar(wcSolicitud);
        WcCliente wcCliente = wcClienteRepository.findById(wcSolicitud.wcClienteId())
                .orElseThrow(() -> new WcNegocioException("El cliente seleccionado ya no existe."));
        Map<Long, WcRepuesto> wcRepuestos = wcCargarRepuestos(wcSolicitud.wcLineas());

        WcVenta wcVenta = wcCrearVenta(wcSolicitud, wcCliente, wcFecha);
        wcAgregarDetalles(wcVenta, wcSolicitud.wcLineas(), wcRepuestos);
        wcAplicarTotales(wcVenta);

        WcVenta wcGuardada = wcRepository.save(wcVenta);
        wcDescontarStock(wcGuardada, wcFecha);
        log.info("Venta registrada: {} total={}", wcGuardada.getWcComprobante(), wcGuardada.getWcTotal());
        return wcGuardada;
    }

    private Map<Long, WcRepuesto> wcCargarRepuestos(List<WcLineaCarrito> wcLineas) {
        Map<Long, WcRepuesto> wcRepuestos = wcRepuestoRepository.findAllById(
                        wcLineas.stream().map(WcLineaCarrito::wcRepuestoId).toList()).stream()
                .collect(Collectors.toMap(WcRepuesto::getWcId, Function.identity()));
        if (wcLineas.stream().anyMatch(wcL -> !wcRepuestos.containsKey(wcL.wcRepuestoId()))) {
            throw new WcNegocioException("Un repuesto del carrito ya no existe en el catalogo.");
        }
        wcInventarioService.wcValidarDisponible(wcLineas.stream()
                .collect(Collectors.groupingBy(wcL -> wcRepuestos.get(wcL.wcRepuestoId()),
                        Collectors.summingInt(WcLineaCarrito::wcCantidad))));
        return wcRepuestos;
    }

    private WcVenta wcCrearVenta(WcNuevaVenta wcSolicitud, WcCliente wcCliente, LocalDateTime wcFecha) {
        WcTipoComprobante wcTipo = wcSolicitud.wcTipo();
        int wcNumero = wcSiguienteNumero(wcTipo.getWcSerie());
        return WcVenta.builder()
                .wcTipoComprobante(wcTipo)
                .wcSerie(wcTipo.getWcSerie())
                .wcNumero(wcNumero)
                .wcComprobante(WcVenta.wcFormatearComprobante(wcTipo.getWcSerie(), wcNumero))
                .wcFecha(wcFecha)
                .wcCliente(wcCliente)
                .wcAplicaIgv(wcSolicitud.wcAplicaIgv())
                .wcPreciosIncluyenIgv(wcSolicitud.wcPreciosIncluyenIgv())
                .wcEstado(WcEstadoVenta.WC_EMITIDA)
                .wcMetodoPago(wcSolicitud.wcMetodoPago())
                .wcObservacion(wcTextoONull(wcSolicitud.wcObservacion()))
                .build();
    }

    private void wcAgregarDetalles(WcVenta wcVenta, List<WcLineaCarrito> wcLineas, Map<Long, WcRepuesto> wcRepuestos) {
        for (WcLineaCarrito wcLinea : wcLineas) {
            WcRepuesto wcRepuesto = wcRepuestos.get(wcLinea.wcRepuestoId());
            wcVenta.wcAgregarDetalle(WcVentaDetalle.builder()
                    .wcRepuesto(wcRepuesto)
                    .wcCodigoRepuesto(wcRepuesto.getWcCodigoRepuesto())
                    .wcNombreRepuesto(wcRepuesto.getWcNombre())
                    .wcCantidad(wcLinea.wcCantidad())
                    .wcPrecioUnitario(wcLinea.wcPrecioUnitario())
                    .wcImporte(WcCalculadoraIgv.wcImporte(wcLinea.wcCantidad(), wcLinea.wcPrecioUnitario()))
                    .build());
        }
    }

    private void wcAplicarTotales(WcVenta wcVenta) {
        WcTotales wcTotales = WcCalculadoraIgv.wcCalcular(
                wcVenta.getWcDetalles().stream().map(WcVentaDetalle::getWcImporte).toList(),
                wcVenta.isWcAplicaIgv(), wcVenta.isWcPreciosIncluyenIgv());
        wcVenta.setWcBaseImponible(wcTotales.wcBaseImponible());
        wcVenta.setWcIgv(wcTotales.wcIgv());
        wcVenta.setWcTotal(wcTotales.wcTotal());
        wcVenta.setWcTasaIgv(wcTotales.wcTasa());
    }

    private void wcDescontarStock(WcVenta wcVenta, LocalDateTime wcFecha) {
        for (WcVentaDetalle wcD : wcVenta.getWcDetalles()) {
            wcInventarioService.wcSalida(wcD.getWcRepuesto(), wcD.getWcCantidad(), WcTipoMovimiento.WC_VENTA,
                    wcVenta.getWcComprobante(), wcFecha, wcVenta.getWcId(), null);
        }
    }

    @Transactional
    public void wcAnular(Long wcId) {
        wcAnular(wcId, LocalDateTime.now());
    }

    @Transactional
    public void wcAnular(Long wcId, LocalDateTime wcAhora) {
        WcVenta wcVenta = wcObtener(wcId);
        if (wcVenta.isWcAnulada()) {
            throw new WcNegocioException("El comprobante " + wcVenta.getWcComprobante() + " ya estaba anulado.");
        }
        for (WcVentaDetalle wcD : wcVenta.getWcDetalles()) {
            wcInventarioService.wcEntrada(wcD.getWcRepuesto(), wcD.getWcCantidad(), null, WcTipoMovimiento.WC_ANULACION_VENTA,
                    wcVenta.getWcComprobante() + " (anulada)", wcAhora, wcVenta.getWcId(), null);
        }
        wcVenta.setWcEstado(WcEstadoVenta.WC_ANULADA);
        log.info("Venta anulada: {}", wcVenta.getWcComprobante());
    }

    private void wcValidar(WcNuevaVenta wcS) {
        if (wcS.wcTipo() == null) {
            throw new WcNegocioException("Selecciona el tipo de comprobante.");
        }
        if (wcS.wcClienteId() == null) {
            throw new WcNegocioException("Selecciona un cliente para la venta.");
        }
        if (wcS.wcMetodoPago() == null) {
            throw new WcNegocioException("Selecciona el metodo de pago.");
        }
        if (wcS.wcLineas() == null || wcS.wcLineas().isEmpty()) {
            throw new WcNegocioException("Agrega al menos un repuesto al carrito.");
        }
        for (WcLineaCarrito wcL : wcS.wcLineas()) {
            if (wcL.wcCantidad() < 1) {
                throw new WcNegocioException("Todas las cantidades deben ser mayores a cero.");
            }
            if (wcL.wcPrecioUnitario() == null || wcL.wcPrecioUnitario().signum() <= 0) {
                throw new WcNegocioException("Todos los precios deben ser mayores a cero.");
            }
        }
    }

    private int wcSiguienteNumero(String wcSerie) {
        Integer wcUltimo = wcRepository.wcUltimoNumero(wcSerie);
        return wcUltimo == null ? 1 : wcUltimo + 1;
    }

    static LocalDateTime wcInicio(LocalDate wcFecha) {
        return (wcFecha == null ? WC_FECHA_MINIMA : wcFecha).atStartOfDay();
    }

    static LocalDateTime wcFin(LocalDate wcFecha) {
        return (wcFecha == null ? WC_FECHA_MAXIMA : wcFecha).atTime(WC_FIN_DEL_DIA);
    }

    static BigDecimal wcDecimal(Object wcValor) {
        if (wcValor instanceof BigDecimal wcB) {
            return wcB.setScale(2, RoundingMode.HALF_UP);
        }
        return wcValor == null
                ? BigDecimal.ZERO.setScale(2)
                : new BigDecimal(wcValor.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private static String wcTextoONull(String wcTexto) {
        return wcTexto == null || wcTexto.isBlank() ? null : wcTexto.trim();
    }
}
