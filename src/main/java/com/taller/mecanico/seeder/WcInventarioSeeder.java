package com.taller.mecanico.seeder;

import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcTipoMovimiento;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.model.WcVentaDetalle;
import com.taller.mecanico.repository.WcCompraRepository;
import com.taller.mecanico.repository.WcMovimientoInventarioRepository;
import com.taller.mecanico.repository.WcRepuestoRepository;
import com.taller.mecanico.repository.WcVentaRepository;
import com.taller.mecanico.service.WcInventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class WcInventarioSeeder {
    private static final long WC_SEMILLA = 7L;
    private static final int WC_REPUESTOS_POR_COMPRA = 20;
    private static final int WC_DIAS_ANTES_DE_LA_PRIMERA_VENTA = 5;
    private static final int WC_DIAS_HISTORIA = 125;

    private final WcComprasSimuladas wcCompras;
    private final WcCompraRepository wcCompraRepository;
    private final WcRepuestoRepository wcRepuestoRepository;
    private final WcVentaRepository wcVentaRepository;
    private final WcMovimientoInventarioRepository wcMovimientoRepository;
    private final WcInventarioService wcInventarioService;

    @Transactional
    public int wcSembrar() {
        if (wcCompraRepository.count() > 0) {
            return 0;
        }
        List<WcRepuesto> wcRepuestos = wcRepuestoRepository.findAll(Sort.by("wcId"));
        if (wcRepuestos.isEmpty()) {
            return 0;
        }

        List<WcVenta> wcPrevias = wcVentaRepository.findAll().stream()
                .filter(wcV -> !wcMovimientoRepository.existsByWcVentaId(wcV.getWcId()))
                .sorted(Comparator.comparing(WcVenta::getWcFecha).thenComparing(WcVenta::getWcId))
                .toList();
        Map<Long, Integer> wcVendidas = new HashMap<>();
        for (WcVenta wcV : wcPrevias) {
            if (!wcV.isWcAnulada()) {
                for (WcVentaDetalle wcD : wcV.getWcDetalles()) {
                    wcVendidas.merge(wcD.getWcRepuesto().getWcId(), wcD.getWcCantidad(), Integer::sum);
                }
            }
        }

        LocalDateTime wcApertura = wcPrevias.isEmpty()
                ? LocalDateTime.now().minusDays(WC_DIAS_HISTORIA).withHour(9).withMinute(0)
                : wcPrevias.get(0).getWcFecha().minusDays(WC_DIAS_ANTES_DE_LA_PRIMERA_VENTA).withHour(9).withMinute(0);

        Random wcAzar = new Random(WC_SEMILLA);
        int wcRegistradas = 0;
        for (int wcI = 0; wcI < wcRepuestos.size(); wcI += WC_REPUESTOS_POR_COMPRA) {
            Map<WcRepuesto, Integer> wcLote = new LinkedHashMap<>();
            for (WcRepuesto wcP : wcRepuestos.subList(wcI, Math.min(wcRepuestos.size(), wcI + WC_REPUESTOS_POR_COMPRA))) {
                wcLote.put(wcP, WcComprasSimuladas.wcCantidadReposicion(wcP, wcAzar) + wcVendidas.getOrDefault(wcP.getWcId(), 0));
            }
            wcCompras.wcComprar(wcLote, wcApertura.plusMinutes(15L * wcRegistradas), wcAzar);
            wcRegistradas++;
        }

        for (WcVenta wcV : wcPrevias) {
            if (wcV.isWcAnulada()) {
                continue;
            }
            for (WcVentaDetalle wcD : new ArrayList<>(wcV.getWcDetalles())) {
                wcInventarioService.wcSalida(wcD.getWcRepuesto(), wcD.getWcCantidad(), WcTipoMovimiento.WC_VENTA,
                        wcV.getWcComprobante(), wcV.getWcFecha(), wcV.getWcId(), null);
            }
        }
        return wcRegistradas;
    }
}
