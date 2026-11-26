package com.taller.mecanico.seeder;

import com.taller.mecanico.model.WcCliente;
import com.taller.mecanico.model.WcMetodoPago;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcTipoComprobante;
import com.taller.mecanico.model.WcVenta;
import com.taller.mecanico.repository.WcClienteRepository;
import com.taller.mecanico.repository.WcRepuestoRepository;
import com.taller.mecanico.repository.WcVentaRepository;
import com.taller.mecanico.service.WcVentaService;
import com.taller.mecanico.service.WcVentaService.WcLineaCarrito;
import com.taller.mecanico.service.WcVentaService.WcNuevaVenta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WcVentaSeeder {
    private static final int WC_CANTIDAD = 120;
    private static final int WC_DIAS_HISTORIA = 120;
    private static final long WC_SEMILLA = 2026L;
    private static final BigDecimal WC_PRECIO_ECONOMICO = new BigDecimal("100");

    private final WcVentaService wcVentaService;
    private final WcComprasSimuladas wcCompras;
    private final WcVentaRepository wcVentaRepository;
    private final WcClienteRepository wcClienteRepository;
    private final WcRepuestoRepository wcRepuestoRepository;

    @Transactional
    public int wcSembrar() {
        if (wcVentaRepository.count() > 0) {
            return 0;
        }
        List<WcCliente> wcClientes = wcClienteRepository.findAll();
        List<WcRepuesto> wcRepuestos = wcRepuestoRepository.findAll();
        if (wcClientes.isEmpty() || wcRepuestos.isEmpty()) {
            return 0;
        }

        List<WcRepuesto> wcEconomicos = wcRepuestos.stream()
                .filter(wcP -> wcP.getWcPrecio().compareTo(WC_PRECIO_ECONOMICO) < 0)
                .toList();

        Random wcAzar = new Random(WC_SEMILLA);
        List<LocalDateTime> wcFechas = wcGenerarFechas(wcAzar);

        int wcRegistradas = 0;
        LocalDateTime wcAnterior = wcFechas.get(0).minusDays(1);
        for (LocalDateTime wcFecha : wcFechas) {
            boolean wcEsBoleta = wcAzar.nextDouble() < 0.85;
            boolean wcAplicaIgv = wcEsBoleta || wcAzar.nextDouble() < 0.5;
            boolean wcIncluyeIgv = !wcAplicaIgv || wcAzar.nextDouble() < 0.9;

            List<WcLineaCarrito> wcLineas = wcArmarCarrito(wcAzar, wcEconomicos, wcRepuestos);

            wcReponerSiFalta(wcLineas, wcRepuestos, wcAnterior, wcFecha, wcAzar);
            wcAnterior = wcFecha;

            WcVenta wcVenta = wcVentaService.wcRegistrar(new WcNuevaVenta(
                    wcEsBoleta ? WcTipoComprobante.WC_BOLETA : WcTipoComprobante.WC_NOTA_VENTA,
                    wcClientes.get(wcAzar.nextInt(wcClientes.size())).getWcId(),
                    wcAplicaIgv, wcIncluyeIgv,
                    wcAzar.nextDouble() < 0.1 ? "Venta en mostrador" : null,
                    wcLineas, wcMetodoAleatorio(wcAzar)), wcFecha);
            wcRegistradas++;

            if (wcAzar.nextDouble() < 0.04) {
                wcVentaService.wcAnular(wcVenta.getWcId(), wcFecha.plusSeconds(1));
            }
        }
        return wcRegistradas;
    }

    private List<LocalDateTime> wcGenerarFechas(Random wcAzar) {
        LocalDate wcHoy = LocalDate.now();
        List<LocalDateTime> wcFechas = new ArrayList<>();
        while (wcFechas.size() < WC_CANTIDAD) {
            LocalDate wcDia = wcHoy.minusDays(wcAzar.nextInt(WC_DIAS_HISTORIA));
            boolean wcFinDeSemana = wcDia.getDayOfWeek() == DayOfWeek.SATURDAY || wcDia.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (!wcFinDeSemana && wcAzar.nextDouble() < 0.35) {
                continue;
            }
            LocalDateTime wcMomento = wcDia.atTime(8 + wcAzar.nextInt(13), wcAzar.nextInt(60), wcAzar.nextInt(60));
            if (wcMomento.isAfter(LocalDateTime.now())) {
                wcMomento = LocalDateTime.now().minusMinutes(1 + wcAzar.nextInt(90));
            }
            wcFechas.add(wcMomento);
        }
        wcFechas.sort(Comparator.naturalOrder());
        return wcFechas;
    }

    private List<WcLineaCarrito> wcArmarCarrito(Random wcAzar, List<WcRepuesto> wcEconomicos, List<WcRepuesto> wcRepuestos) {
        List<WcLineaCarrito> wcLineas = new ArrayList<>();
        Set<Long> wcUsados = new HashSet<>();
        int wcItems = 1 + wcAzar.nextInt(5);
        while (wcLineas.size() < wcItems) {
            List<WcRepuesto> wcFuente = wcAzar.nextDouble() < 0.85 && !wcEconomicos.isEmpty() ? wcEconomicos : wcRepuestos;
            WcRepuesto wcP = wcFuente.get(wcAzar.nextInt(wcFuente.size()));
            if (!wcUsados.add(wcP.getWcId())) {
                continue;
            }
            boolean wcCaro = wcP.getWcPrecio().compareTo(WC_PRECIO_ECONOMICO) >= 0;
            int wcCantidad = wcCaro ? 1 : 1 + wcAzar.nextInt(wcAzar.nextDouble() < 0.7 ? 3 : 8);
            wcLineas.add(new WcLineaCarrito(wcP.getWcId(), wcCantidad, wcP.getWcPrecio()));
        }
        return wcLineas;
    }

    private void wcReponerSiFalta(List<WcLineaCarrito> wcLineas, List<WcRepuesto> wcRepuestos,
                                LocalDateTime wcAnterior, LocalDateTime wcFecha, Random wcAzar) {
        Map<WcRepuesto, Integer> wcFaltantes = new LinkedHashMap<>();
        for (WcLineaCarrito wcL : wcLineas) {
            WcRepuesto wcP = wcRepuestos.stream().filter(wcX -> wcX.getWcId().equals(wcL.wcRepuestoId())).findFirst().orElseThrow();
            if (wcP.getWcStock() < wcL.wcCantidad()) {
                wcFaltantes.put(wcP, WcComprasSimuladas.wcCantidadReposicion(wcP, wcAzar) + wcL.wcCantidad());
            }
        }
        if (!wcFaltantes.isEmpty()) {
            LocalDateTime wcMomento = wcFecha.minusHours(2);
            wcCompras.wcComprar(wcFaltantes, wcMomento.isAfter(wcAnterior) ? wcMomento : wcAnterior.plusMinutes(1), wcAzar);
        }
    }

    private static WcMetodoPago wcMetodoAleatorio(Random wcAzar) {
        return WcMetodoPago.values()[wcAzar.nextInt(WcMetodoPago.values().length)];
    }
}
