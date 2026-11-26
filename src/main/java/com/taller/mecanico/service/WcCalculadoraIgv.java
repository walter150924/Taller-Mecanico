package com.taller.mecanico.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

public final class WcCalculadoraIgv {
    public static final BigDecimal WC_TASA_IGV = new BigDecimal("0.18");

    private WcCalculadoraIgv() {
    }

    public record WcTotales(BigDecimal wcBaseImponible, BigDecimal wcIgv, BigDecimal wcTotal, BigDecimal wcTasa) {
        public static final WcTotales WC_CERO = new WcTotales(wcCero(), wcCero(), wcCero(), WC_TASA_IGV);
    }

    public static BigDecimal wcImporte(int wcCantidad, BigDecimal wcPrecioUnitario) {
        return wcPrecioUnitario.multiply(BigDecimal.valueOf(wcCantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    public static WcTotales wcCalcular(Collection<BigDecimal> wcImportes, boolean wcAplicaIgv, boolean wcPreciosIncluyenIgv) {
        BigDecimal wcSuma = wcImportes.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (!wcAplicaIgv) {
            return new WcTotales(wcSuma, wcCero(), wcSuma, BigDecimal.ZERO);
        }
        if (wcPreciosIncluyenIgv) {
            BigDecimal wcBase = wcSuma.divide(BigDecimal.ONE.add(WC_TASA_IGV), 2, RoundingMode.HALF_UP);
            return new WcTotales(wcBase, wcSuma.subtract(wcBase), wcSuma, WC_TASA_IGV);
        }
        BigDecimal wcIgv = wcSuma.multiply(WC_TASA_IGV).setScale(2, RoundingMode.HALF_UP);
        return new WcTotales(wcSuma, wcIgv, wcSuma.add(wcIgv), WC_TASA_IGV);
    }

    private static BigDecimal wcCero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
