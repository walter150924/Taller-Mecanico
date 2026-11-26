package com.taller.mecanico.service;

import com.taller.mecanico.model.WcEstadoVenta;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.model.WcVentaDetalle;
import com.taller.mecanico.repository.WcVentaDetalleRepository;
import com.taller.mecanico.repository.WcVentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WcReporteVentasService {
    private static final int WC_DIAS_MAXIMOS_SERIE_DIARIA = 92;

    private final WcVentaRepository wcVentaRepository;
    private final WcVentaDetalleRepository wcDetalleRepository;

    public record WcPuntoSerie(LocalDate wcFecha, BigDecimal wcTotal, long wcComprobantes) {}

    public record WcRepuestoVendido(String wcNombre, long wcUnidades, BigDecimal wcImporte) {}

    public record WcClienteFrecuente(String wcNombre, String wcDni, long wcCompras, BigDecimal wcImporte) {}

    public record WcResumenVentas(
            LocalDate wcDesde,
            LocalDate wcHasta,
            BigDecimal wcTotal,
            BigDecimal wcBaseImponible,
            BigDecimal wcIgv,
            long wcComprobantes,
            long wcAnuladas,
            BigDecimal wcTicketPromedio,

            Double wcVariacionTotal,
            boolean wcSeriePorMes,
            List<WcPuntoSerie> wcSerie,
            List<WcRepuestoVendido> wcTopRepuestos,
            List<WcClienteFrecuente> wcTopClientes,
            List<WcVenta> wcUltimas) {}

    public record WcDatosReporte(List<WcVenta> wcVentas, List<WcVentaDetalle> wcDetalles, WcResumenVentas wcResumen) {}

    public WcResumenVentas wcResumen(LocalDate wcDesde, LocalDate wcHasta) {
        List<WcVenta> wcVentas = wcVentaRepository.wcListarPeriodo(WcVentaService.wcInicio(wcDesde), WcVentaService.wcFin(wcHasta));
        List<WcVenta> wcEmitidas = wcVentas.stream().filter(wcV -> !wcV.isWcAnulada()).toList();

        BigDecimal wcTotal = wcSumar(wcEmitidas, WcVenta::getWcTotal);
        BigDecimal wcBase = wcSumar(wcEmitidas, WcVenta::getWcBaseImponible);
        BigDecimal wcIgv = wcSumar(wcEmitidas, WcVenta::getWcIgv);
        BigDecimal wcTicket = wcEmitidas.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : wcTotal.divide(BigDecimal.valueOf(wcEmitidas.size()), 2, RoundingMode.HALF_UP);

        long wcDias = ChronoUnit.DAYS.between(wcDesde, wcHasta) + 1;
        BigDecimal wcTotalAnterior = wcVentaRepository.wcListarPeriodo(
                        WcVentaService.wcInicio(wcDesde.minusDays(wcDias)), WcVentaService.wcFin(wcDesde.minusDays(1))).stream()
                .filter(wcV -> !wcV.isWcAnulada())
                .map(WcVenta::getWcTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Double wcVariacion = wcTotalAnterior.signum() == 0
                ? null
                : wcTotal.subtract(wcTotalAnterior).doubleValue() / wcTotalAnterior.doubleValue() * 100;

        boolean wcPorMes = wcDias > WC_DIAS_MAXIMOS_SERIE_DIARIA;

        return new WcResumenVentas(wcDesde, wcHasta, wcTotal, wcBase, wcIgv, wcEmitidas.size(),
                wcVentas.size() - wcEmitidas.size(), wcTicket, wcVariacion, wcPorMes,
                wcSerie(wcEmitidas, wcDesde, wcHasta, wcPorMes),
                wcRepuestos(wcDesde, wcHasta),
                wcClientes(wcEmitidas),
                wcVentaRepository.wcUltimas());
    }

    public WcDatosReporte wcDatosReporte(LocalDate wcDesde, LocalDate wcHasta) {
        return new WcDatosReporte(
                wcVentaRepository.wcListarPeriodo(WcVentaService.wcInicio(wcDesde), WcVentaService.wcFin(wcHasta)),
                wcDetalleRepository.wcListarPeriodo(WcVentaService.wcInicio(wcDesde), WcVentaService.wcFin(wcHasta)),
                wcResumen(wcDesde, wcHasta));
    }

    private List<WcPuntoSerie> wcSerie(List<WcVenta> wcVentas, LocalDate wcDesde, LocalDate wcHasta, boolean wcPorMes) {
        Map<LocalDate, BigDecimal> wcTotales = new TreeMap<>();
        Map<LocalDate, Long> wcConteo = new TreeMap<>();
        for (WcVenta wcV : wcVentas) {
            LocalDate wcClave = wcClave(wcV.getWcFecha().toLocalDate(), wcPorMes);
            wcTotales.merge(wcClave, wcV.getWcTotal(), BigDecimal::add);
            wcConteo.merge(wcClave, 1L, Long::sum);
        }

        List<WcPuntoSerie> wcPuntos = new ArrayList<>();
        LocalDate wcCursor = wcClave(wcDesde, wcPorMes);
        while (!wcCursor.isAfter(wcHasta)) {
            wcPuntos.add(new WcPuntoSerie(wcCursor,
                    wcTotales.getOrDefault(wcCursor, BigDecimal.ZERO),
                    wcConteo.getOrDefault(wcCursor, 0L)));
            wcCursor = wcPorMes ? wcCursor.plusMonths(1) : wcCursor.plusDays(1);
        }
        return wcPuntos;
    }

    private static LocalDate wcClave(LocalDate wcFecha, boolean wcPorMes) {
        return wcPorMes ? YearMonth.from(wcFecha).atDay(1) : wcFecha;
    }

    private List<WcRepuestoVendido> wcRepuestos(LocalDate wcDesde, LocalDate wcHasta) {
        return wcVentaRepository.wcResumenPorRepuesto(WcEstadoVenta.WC_EMITIDA,
                        WcVentaService.wcInicio(wcDesde), WcVentaService.wcFin(wcHasta)).stream()
                .map(wcF -> new WcRepuestoVendido((String) wcF[0], ((Number) wcF[1]).longValue(), WcVentaService.wcDecimal(wcF[2])))
                .toList();
    }

    private List<WcClienteFrecuente> wcClientes(List<WcVenta> wcVentas) {
        Map<Long, WcClienteFrecuente> wcPorCliente = new LinkedHashMap<>();
        for (WcVenta wcV : wcVentas) {
            wcPorCliente.merge(wcV.getWcCliente().getWcId(),
                    new WcClienteFrecuente(wcV.getWcCliente().getWcNombreCompleto(), wcV.getWcCliente().getWcDni(), 1, wcV.getWcTotal()),
                    (wcA, wcB) -> new WcClienteFrecuente(wcA.wcNombre(), wcA.wcDni(), wcA.wcCompras() + 1, wcA.wcImporte().add(wcB.wcImporte())));
        }
        return wcPorCliente.values().stream()
                .sorted(Comparator.comparing(WcClienteFrecuente::wcImporte).reversed())
                .toList();
    }

    private static BigDecimal wcSumar(List<WcVenta> wcVentas, Function<WcVenta, BigDecimal> wcCampo) {
        return wcVentas.stream().map(wcCampo).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }
}
