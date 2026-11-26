package com.taller.mecanico.service;

import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.repository.WcCategoriaRepository;
import com.taller.mecanico.repository.WcClienteRepository;
import com.taller.mecanico.repository.WcRepuestoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WcDashboardService {
    private static final int[] WC_LIMITES_PRECIO = {10, 50, 100, 500, 1000};

    private final WcClienteRepository wcClienteRepository;
    private final WcCategoriaRepository wcCategoriaRepository;
    private final WcRepuestoRepository wcRepuestoRepository;

    public record WcSerieCategoria(String wcNombre, long wcRepuestos, double wcPrecioPromedio) {}

    public record WcRangoPrecio(String wcEtiqueta, long wcCantidad) {}

    public record WcResumen(
            long wcClientes,
            long wcCategorias,
            long wcRepuestos,
            BigDecimal wcValorCatalogo,
            BigDecimal wcPrecioPromedio,
            long wcClientesConEmail,
            long wcClientesConTelefono,
            long wcClientesContactoCompleto,
            List<WcSerieCategoria> wcPorCategoria,
            List<WcRangoPrecio> wcRangosPrecio,
            List<WcRepuesto> wcMasCaros) {}

    public WcResumen wcObtenerResumen() {
        List<BigDecimal> wcPrecios = wcRepuestoRepository.wcListarPrecios();
        BigDecimal wcTotal = wcPrecios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal wcPromedio = wcPrecios.isEmpty()
                ? BigDecimal.ZERO
                : wcTotal.divide(BigDecimal.valueOf(wcPrecios.size()), 2, RoundingMode.HALF_UP);

        List<WcSerieCategoria> wcPorCategoria = wcRepuestoRepository.wcResumenPorCategoria().stream()
                .map(wcFila -> new WcSerieCategoria(
                        (String) wcFila[0],
                        ((Number) wcFila[1]).longValue(),
                        wcFila[2] == null ? 0 : ((Number) wcFila[2]).doubleValue()))
                .toList();

        return new WcResumen(
                wcClienteRepository.count(),
                wcCategoriaRepository.count(),
                wcPrecios.size(),
                wcTotal,
                wcPromedio,
                wcClienteRepository.wcContarConEmail(),
                wcClienteRepository.wcContarConTelefono(),
                wcClienteRepository.wcContarContactoCompleto(),
                wcPorCategoria,
                wcAgruparPorRango(wcPrecios),
                wcRepuestoRepository.wcTop5MasCaros());
    }

    private List<WcRangoPrecio> wcAgruparPorRango(List<BigDecimal> wcPrecios) {
        long[] wcConteo = new long[WC_LIMITES_PRECIO.length + 1];
        for (BigDecimal wcPrecio : wcPrecios) {
            int wcI = 0;
            while (wcI < WC_LIMITES_PRECIO.length && wcPrecio.doubleValue() >= WC_LIMITES_PRECIO[wcI]) {
                wcI++;
            }
            wcConteo[wcI]++;
        }

        List<WcRangoPrecio> wcRangos = new ArrayList<>();
        int wcDesde = 0;
        for (int wcI = 0; wcI < WC_LIMITES_PRECIO.length; wcI++) {
            wcRangos.add(new WcRangoPrecio(wcDesde + "–" + WC_LIMITES_PRECIO[wcI], wcConteo[wcI]));
            wcDesde = WC_LIMITES_PRECIO[wcI];
        }
        wcRangos.add(new WcRangoPrecio("≥ " + wcDesde, wcConteo[WC_LIMITES_PRECIO.length]));
        return wcRangos;
    }
}
