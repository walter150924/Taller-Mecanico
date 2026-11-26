package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCompra;
import com.taller.mecanico.model.WcCompraDetalle;
import com.taller.mecanico.model.WcEstadoCompra;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcProveedor;
import com.taller.mecanico.model.WcTipoDocumentoCompra;
import com.taller.mecanico.model.WcTipoMovimiento;
import com.taller.mecanico.repository.WcCompraRepository;
import com.taller.mecanico.repository.WcRepuestoRepository;
import com.taller.mecanico.repository.WcProveedorRepository;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WcCompraService {
    private static final String WC_PATRON_RUC = "(10|15|17|20)\\d{9}";

    private final WcCompraRepository wcRepository;
    private final WcProveedorRepository wcProveedorRepository;
    private final WcRepuestoRepository wcRepuestoRepository;
    private final WcInventarioService wcInventarioService;

    public record WcLineaCompra(Long wcRepuestoId, int wcCantidad, BigDecimal wcCostoUnitario) {}

    public record WcNuevaCompra(String wcRucProveedor,
                              String wcRazonSocialProveedor,
                              WcTipoDocumentoCompra wcTipoDocumento,
                              String wcNumeroDocumento,
                              LocalDate wcFechaDocumento,
                              boolean wcAplicaIgv,
                              boolean wcPreciosIncluyenIgv,
                              String wcObservacion,
                              List<WcLineaCompra> wcLineas) {}

    public record WcTotalizado(long wcCompras, BigDecimal wcBaseImponible, BigDecimal wcIgv, BigDecimal wcTotal) {}

    public Page<WcCompra> wcListar(String wcTexto, LocalDate wcDesde, LocalDate wcHasta, int wcPagina, int wcTamanio) {
        return wcRepository.wcBuscar(wcTexto == null ? "" : wcTexto.trim(),
                WcVentaService.wcInicio(wcDesde), WcVentaService.wcFin(wcHasta),
                PageRequest.of(wcPagina, wcTamanio, Sort.by("wcFecha").descending().and(Sort.by("wcId").descending())));
    }

    public WcTotalizado wcTotalizar(String wcTexto, LocalDate wcDesde, LocalDate wcHasta) {
        Object[] wcFila = wcRepository.wcTotalizar(wcTexto == null ? "" : wcTexto.trim(), WcEstadoCompra.WC_REGISTRADA,
                WcVentaService.wcInicio(wcDesde), WcVentaService.wcFin(wcHasta)).get(0);
        return new WcTotalizado(((Number) wcFila[0]).longValue(), WcVentaService.wcDecimal(wcFila[1]),
                WcVentaService.wcDecimal(wcFila[2]), WcVentaService.wcDecimal(wcFila[3]));
    }

    public WcCompra wcObtener(Long wcId) {
        return wcRepository.wcBuscarConDetalle(wcId)
                .orElseThrow(() -> new WcNegocioException("La compra ya no existe en la base de datos."));
    }

    public String wcSiguienteCodigo() {
        return WcCompra.wcFormatearCodigo(wcSiguienteNumero());
    }

    @Transactional
    public WcCompra wcRegistrar(WcNuevaCompra wcSolicitud) {
        return wcRegistrar(wcSolicitud, LocalDateTime.now());
    }

    @Transactional
    public WcCompra wcRegistrar(WcNuevaCompra wcS, LocalDateTime wcFecha) {
        wcValidar(wcS, wcFecha);
        WcProveedor wcProveedor = wcProveedor(wcS.wcRucProveedor().trim(), wcS.wcRazonSocialProveedor().trim());
        String wcNumeroDocumento = wcS.wcNumeroDocumento().trim().toUpperCase();
        wcValidarDocumentoUnico(wcProveedor, wcS.wcTipoDocumento(), wcNumeroDocumento);

        WcCompra wcCompra = wcCrearCompra(wcS, wcProveedor, wcNumeroDocumento, wcFecha);
        wcAgregarDetalles(wcCompra, wcS.wcLineas());
        wcAplicarTotales(wcCompra);

        WcCompra wcGuardada = wcRepository.save(wcCompra);
        wcIngresarStock(wcGuardada, wcFecha);
        log.info("Compra registrada: {} total={}", wcGuardada.getWcCodigo(), wcGuardada.getWcTotal());
        return wcGuardada;
    }

    private void wcValidarDocumentoUnico(WcProveedor wcProveedor, WcTipoDocumentoCompra wcTipo, String wcNumeroDocumento) {
        if (wcProveedor.getWcId() != null && wcRepository.existsByWcProveedorWcIdAndWcTipoDocumentoAndWcNumeroDocumentoAndWcEstado(
                wcProveedor.getWcId(), wcTipo, wcNumeroDocumento, WcEstadoCompra.WC_REGISTRADA)) {
            throw new WcNegocioException("Ya se registro la " + wcTipo.getWcDescripcion().toLowerCase()
                    + " " + wcNumeroDocumento + " de " + wcProveedor.getWcRazonSocial() + ".");
        }
    }

    private WcCompra wcCrearCompra(WcNuevaCompra wcS, WcProveedor wcProveedor, String wcNumeroDocumento, LocalDateTime wcFecha) {
        int wcNumero = wcSiguienteNumero();
        return WcCompra.builder()
                .wcCodigo(WcCompra.wcFormatearCodigo(wcNumero))
                .wcNumero(wcNumero)
                .wcFecha(wcFecha)
                .wcProveedor(wcProveedor)
                .wcTipoDocumento(wcS.wcTipoDocumento())
                .wcNumeroDocumento(wcNumeroDocumento)
                .wcFechaDocumento(wcS.wcFechaDocumento())
                .wcAplicaIgv(wcS.wcAplicaIgv())
                .wcPreciosIncluyenIgv(wcS.wcPreciosIncluyenIgv())
                .wcEstado(WcEstadoCompra.WC_REGISTRADA)
                .wcObservacion(wcS.wcObservacion() == null || wcS.wcObservacion().isBlank() ? null : wcS.wcObservacion().trim())
                .build();
    }

    private void wcAgregarDetalles(WcCompra wcCompra, List<WcLineaCompra> wcLineas) {
        Map<Long, WcRepuesto> wcRepuestos = wcRepuestoRepository.findAllById(
                        wcLineas.stream().map(WcLineaCompra::wcRepuestoId).toList()).stream()
                .collect(Collectors.toMap(WcRepuesto::getWcId, Function.identity()));
        for (WcLineaCompra wcLinea : wcLineas) {
            WcRepuesto wcRepuesto = wcRepuestos.get(wcLinea.wcRepuestoId());
            if (wcRepuesto == null) {
                throw new WcNegocioException("Un repuesto de la compra ya no existe en el catalogo.");
            }
            wcCompra.wcAgregarDetalle(WcCompraDetalle.builder()
                    .wcRepuesto(wcRepuesto)
                    .wcCodigoRepuesto(wcRepuesto.getWcCodigoRepuesto())
                    .wcNombreRepuesto(wcRepuesto.getWcNombre())
                    .wcCantidad(wcLinea.wcCantidad())
                    .wcCostoUnitario(wcLinea.wcCostoUnitario().setScale(4, RoundingMode.HALF_UP))
                    .wcImporte(WcCalculadoraIgv.wcImporte(wcLinea.wcCantidad(), wcLinea.wcCostoUnitario()))
                    .build());
        }
    }

    private void wcAplicarTotales(WcCompra wcCompra) {
        WcTotales wcT = WcCalculadoraIgv.wcCalcular(wcCompra.getWcDetalles().stream().map(WcCompraDetalle::getWcImporte).toList(),
                wcCompra.isWcAplicaIgv(), wcCompra.isWcPreciosIncluyenIgv());
        wcCompra.setWcBaseImponible(wcT.wcBaseImponible());
        wcCompra.setWcIgv(wcT.wcIgv());
        wcCompra.setWcTotal(wcT.wcTotal());
        wcCompra.setWcTasaIgv(wcT.wcTasa());
    }

    private void wcIngresarStock(WcCompra wcCompra, LocalDateTime wcFecha) {
        for (WcCompraDetalle wcD : wcCompra.getWcDetalles()) {
            wcInventarioService.wcEntrada(wcD.getWcRepuesto(), wcD.getWcCantidad(), wcCostoSinIgv(wcCompra, wcD.getWcCostoUnitario()),
                    WcTipoMovimiento.WC_COMPRA, wcCompra.getWcCodigo() + " · " + wcCompra.getWcNumeroDocumento(),
                    wcFecha, null, wcCompra.getWcId());
        }
    }

    @Transactional
    public void wcAnular(Long wcId) {
        WcCompra wcCompra = wcObtener(wcId);
        if (wcCompra.isWcAnulada()) {
            throw new WcNegocioException("La compra " + wcCompra.getWcCodigo() + " ya estaba anulada.");
        }
        Map<WcRepuesto, Integer> wcRetiro = wcCompra.getWcDetalles().stream()
                .collect(Collectors.groupingBy(WcCompraDetalle::getWcRepuesto,
                        Collectors.summingInt(WcCompraDetalle::getWcCantidad)));
        try {
            wcInventarioService.wcValidarDisponible(wcRetiro);
        } catch (WcNegocioException wcE) {
            throw new WcNegocioException("No se puede anular: parte de la mercaderia ya se vendio. " + wcE.getMessage());
        }
        LocalDateTime wcAhora = LocalDateTime.now();
        for (WcCompraDetalle wcD : wcCompra.getWcDetalles()) {
            wcInventarioService.wcSalida(wcD.getWcRepuesto(), wcD.getWcCantidad(), WcTipoMovimiento.WC_ANULACION_COMPRA,
                    wcCompra.getWcCodigo() + " (anulada)", wcAhora, null, wcCompra.getWcId());
        }
        wcCompra.setWcEstado(WcEstadoCompra.WC_ANULADA);
        log.info("Compra anulada: {}", wcCompra.getWcCodigo());
    }

    static BigDecimal wcCostoSinIgv(WcCompra wcCompra, BigDecimal wcCostoDocumento) {
        if (wcCompra.isWcAplicaIgv() && wcCompra.isWcPreciosIncluyenIgv()) {
            return wcCostoDocumento.divide(BigDecimal.ONE.add(WcCalculadoraIgv.WC_TASA_IGV), 4, RoundingMode.HALF_UP);
        }
        return wcCostoDocumento;
    }

    private WcProveedor wcProveedor(String wcRuc, String wcRazonSocial) {
        return wcProveedorRepository.findByWcRuc(wcRuc)
                .orElseGet(() -> wcProveedorRepository.save(WcProveedor.builder()
                        .wcRuc(wcRuc)
                        .wcRazonSocial(wcRazonSocial.toUpperCase())
                        .build()));
    }

    private void wcValidar(WcNuevaCompra wcS, LocalDateTime wcFecha) {
        if (wcS.wcRucProveedor() == null || !wcS.wcRucProveedor().trim().matches(WC_PATRON_RUC)) {
            throw new WcNegocioException("Ingresa un RUC de proveedor valido (11 digitos).");
        }
        if (wcS.wcRazonSocialProveedor() == null || wcS.wcRazonSocialProveedor().isBlank()) {
            throw new WcNegocioException("Ingresa la razon social del proveedor.");
        }
        if (wcS.wcTipoDocumento() == null) {
            throw new WcNegocioException("Selecciona el tipo de documento del proveedor.");
        }
        if (wcS.wcNumeroDocumento() == null || wcS.wcNumeroDocumento().isBlank()) {
            throw new WcNegocioException("Ingresa la serie y numero del documento del proveedor.");
        }
        if (wcS.wcFechaDocumento() == null || wcS.wcFechaDocumento().isAfter(wcFecha.toLocalDate())) {
            throw new WcNegocioException("La fecha del documento no puede ser posterior a hoy.");
        }
        if (wcS.wcLineas() == null || wcS.wcLineas().isEmpty()) {
            throw new WcNegocioException("Agrega al menos un repuesto a la compra.");
        }
        for (WcLineaCompra wcL : wcS.wcLineas()) {
            if (wcL.wcCantidad() < 1) {
                throw new WcNegocioException("Todas las cantidades deben ser mayores a cero.");
            }
            if (wcL.wcCostoUnitario() == null || wcL.wcCostoUnitario().signum() <= 0) {
                throw new WcNegocioException("Todos los costos deben ser mayores a cero.");
            }
        }
    }

    private int wcSiguienteNumero() {
        Integer wcUltimo = wcRepository.wcUltimoNumero();
        return wcUltimo == null ? 1 : wcUltimo + 1;
    }
}
