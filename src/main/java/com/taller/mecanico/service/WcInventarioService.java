package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcMovimientoInventario;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcTipoMovimiento;
import com.taller.mecanico.repository.WcMovimientoInventarioRepository;
import com.taller.mecanico.repository.WcRepuestoRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WcInventarioService {
    private final WcRepuestoRepository wcRepuestoRepository;
    private final WcMovimientoInventarioRepository wcMovimientoRepository;

    @Getter
    @RequiredArgsConstructor
    public enum WcFiltroStock {
        WC_TODOS("Todos los repuestos"),
        WC_DISPONIBLE("Stock suficiente"),
        WC_BAJO("Stock bajo"),
        WC_SIN_STOCK("Sin stock");

        private final String wcEtiqueta;

        @Override
        public String toString() {
            return wcEtiqueta;
        }
    }

    public record WcResumenInventario(long wcRepuestos, long wcUnidades, BigDecimal wcValorCosto,
                                    BigDecimal wcValorVenta, long wcStockBajo, long wcSinStock) {}

    public Page<WcRepuesto> wcListar(String wcTexto, WcFiltroStock wcFiltro, int wcPagina, int wcTamanio) {
        return wcRepuestoRepository.wcInventario(wcTexto == null ? "" : wcTexto.trim(),
                (wcFiltro == null ? WcFiltroStock.WC_TODOS : wcFiltro).name(),
                PageRequest.of(wcPagina, wcTamanio, Sort.by("wcCodigoRepuesto")));
    }

    public List<WcRepuesto> wcListarTodo() {
        return wcRepuestoRepository.wcListarConCategoria();
    }

    public WcResumenInventario wcResumen() {
        List<WcRepuesto> wcRepuestos = wcRepuestoRepository.findAll();
        long wcUnidades = 0;
        long wcBajo = 0;
        long wcSin = 0;
        BigDecimal wcCosto = BigDecimal.ZERO;
        BigDecimal wcVenta = BigDecimal.ZERO;
        for (WcRepuesto wcP : wcRepuestos) {
            int wcStock = Math.max(0, wcP.getWcStock());
            wcUnidades += wcStock;
            if (wcP.isWcSinStock()) {
                wcSin++;
            } else if (wcP.isWcStockBajo()) {
                wcBajo++;
            }
            wcCosto = wcCosto.add(wcValorizado(wcP));
            wcVenta = wcVenta.add(wcP.getWcPrecio().multiply(BigDecimal.valueOf(wcStock)));
        }
        return new WcResumenInventario(wcRepuestos.size(), wcUnidades,
                wcCosto.setScale(2, RoundingMode.HALF_UP), wcVenta.setScale(2, RoundingMode.HALF_UP), wcBajo, wcSin);
    }

    public List<WcMovimientoInventario> wcKardex(Long wcRepuestoId) {
        return wcMovimientoRepository.wcKardex(wcRepuestoId);
    }

    public static BigDecimal wcValorizado(WcRepuesto wcP) {
        if (wcP.getWcCostoPromedio() == null || wcP.getWcStock() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return wcP.getWcCostoPromedio().multiply(BigDecimal.valueOf(wcP.getWcStock())).setScale(2, RoundingMode.HALF_UP);
    }

    public void wcValidarDisponible(Map<WcRepuesto, Integer> wcPedido) {
        String wcFaltantes = wcPedido.entrySet().stream()
                .filter(wcE -> wcE.getValue() > wcE.getKey().getWcStock())
                .map(wcE -> wcE.getKey().getWcNombre() + " (disponible " + Math.max(0, wcE.getKey().getWcStock())
                        + ", solicitado " + wcE.getValue() + ")")
                .collect(Collectors.joining("; "));
        if (!wcFaltantes.isEmpty()) {
            throw new WcNegocioException("Stock insuficiente: " + wcFaltantes + ".");
        }
    }

    @Transactional
    public WcMovimientoInventario wcEntrada(WcRepuesto wcRepuesto, int wcCantidad, BigDecimal wcCostoUnitario,
                                        WcTipoMovimiento wcTipo, String wcDocumento, LocalDateTime wcFecha,
                                        Long wcVentaId, Long wcCompraId) {
        if (!wcTipo.isWcEntrada()) {
            throw new IllegalArgumentException(wcTipo + " no es un movimiento de entrada");
        }
        int wcAnterior = Math.max(0, wcRepuesto.getWcStock());
        if (wcTipo == WcTipoMovimiento.WC_COMPRA && wcCostoUnitario != null) {
            wcRepuesto.setWcCostoPromedio(wcCostoPromedio(wcRepuesto.getWcCostoPromedio(), wcAnterior, wcCostoUnitario, wcCantidad));
        }
        wcRepuesto.setWcStock(wcRepuesto.getWcStock() + wcCantidad);
        return wcRegistrar(wcRepuesto, wcTipo, wcCantidad, wcCostoUnitario, wcDocumento, wcFecha, wcVentaId, wcCompraId);
    }

    @Transactional
    public WcMovimientoInventario wcSalida(WcRepuesto wcRepuesto, int wcCantidad, WcTipoMovimiento wcTipo, String wcDocumento,
                                       LocalDateTime wcFecha, Long wcVentaId, Long wcCompraId) {
        if (wcTipo.isWcEntrada()) {
            throw new IllegalArgumentException(wcTipo + " no es un movimiento de salida");
        }
        if (wcCantidad > wcRepuesto.getWcStock()) {
            throw new WcNegocioException("Stock insuficiente de " + wcRepuesto.getWcNombre() + ": disponible "
                    + Math.max(0, wcRepuesto.getWcStock()) + ", se requieren " + wcCantidad + ".");
        }
        wcRepuesto.setWcStock(wcRepuesto.getWcStock() - wcCantidad);
        return wcRegistrar(wcRepuesto, wcTipo, wcCantidad, null, wcDocumento, wcFecha, wcVentaId, wcCompraId);
    }

    private WcMovimientoInventario wcRegistrar(WcRepuesto wcRepuesto, WcTipoMovimiento wcTipo, int wcCantidad, BigDecimal wcCosto,
                                           String wcDocumento, LocalDateTime wcFecha, Long wcVentaId, Long wcCompraId) {
        return wcMovimientoRepository.save(WcMovimientoInventario.builder()
                .wcRepuesto(wcRepuesto)
                .wcTipo(wcTipo)
                .wcCantidad(wcCantidad)
                .wcStockResultante(wcRepuesto.getWcStock())
                .wcCostoUnitario(wcCosto)
                .wcDocumento(wcDocumento)
                .wcFecha(wcFecha)
                .wcVentaId(wcVentaId)
                .wcCompraId(wcCompraId)
                .build());
    }

    static BigDecimal wcCostoPromedio(BigDecimal wcActual, int wcStock, BigDecimal wcNuevo, int wcCantidad) {
        if (wcActual == null || wcStock <= 0) {
            return wcNuevo.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal wcTotal = wcActual.multiply(BigDecimal.valueOf(wcStock))
                .add(wcNuevo.multiply(BigDecimal.valueOf(wcCantidad)));
        return wcTotal.divide(BigDecimal.valueOf((long) wcStock + wcCantidad), 4, RoundingMode.HALF_UP);
    }
}
