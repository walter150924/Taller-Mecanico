package com.taller.mecanico.seeder;

import com.taller.mecanico.model.WcCompra;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.model.WcProveedor;
import com.taller.mecanico.model.WcTipoDocumentoCompra;
import com.taller.mecanico.repository.WcProveedorRepository;
import com.taller.mecanico.service.WcCompraService;
import com.taller.mecanico.service.WcCompraService.WcLineaCompra;
import com.taller.mecanico.service.WcCompraService.WcNuevaCompra;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@RequiredArgsConstructor
class WcComprasSimuladas {
    static final BigDecimal WC_PRECIO_ECONOMICO = new BigDecimal("100");

    private final WcCompraService wcCompraService;
    private final WcProveedorRepository wcProveedorRepository;
    private final Map<String, Integer> wcCorrelativos = new HashMap<>();

    static int wcCantidadReposicion(WcRepuesto wcP, Random wcAzar) {
        return wcP.getWcPrecio().compareTo(WC_PRECIO_ECONOMICO) < 0 ? 24 + wcAzar.nextInt(37) : 3 + wcAzar.nextInt(6);
    }

    WcCompra wcComprar(Map<WcRepuesto, Integer> wcCantidades, LocalDateTime wcFecha, Random wcAzar) {
        List<WcProveedor> wcProveedores = wcProveedorRepository.findAll();
        WcProveedor wcProveedor = wcProveedores.get(wcAzar.nextInt(wcProveedores.size()));
        int wcCorrelativo = wcCorrelativos.merge(wcProveedor.getWcRuc(), 100 + wcAzar.nextInt(400), (wcA, wcB) -> wcA + 1 + wcAzar.nextInt(20));

        List<WcLineaCompra> wcLineas = wcCantidades.entrySet().stream()
                .map(wcE -> new WcLineaCompra(wcE.getKey().getWcId(), wcE.getValue(), wcCosto(wcE.getKey(), wcAzar)))
                .toList();
        return wcCompraService.wcRegistrar(new WcNuevaCompra(
                wcProveedor.getWcRuc(), wcProveedor.getWcRazonSocial(),
                WcTipoDocumentoCompra.WC_FACTURA,
                "F" + String.format("%03d", 1 + wcProveedor.getWcRuc().charAt(10) % 3) + "-" + String.format("%06d", wcCorrelativo),
                wcFecha.toLocalDate(), true, false, null, wcLineas), wcFecha);
    }

    private static BigDecimal wcCosto(WcRepuesto wcP, Random wcAzar) {
        double wcMargen = 0.60 + wcAzar.nextDouble() * 0.15;
        return wcP.getWcPrecio().divide(new BigDecimal("1.18"), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(wcMargen)).setScale(2, RoundingMode.HALF_UP)
                .max(new BigDecimal("0.10"));
    }
}
