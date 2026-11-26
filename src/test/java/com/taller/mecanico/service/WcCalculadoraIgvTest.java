package com.taller.mecanico.service;

import com.taller.mecanico.service.WcCalculadoraIgv.WcTotales;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WcCalculadoraIgvTest {
    private static final List<BigDecimal> WC_IMPORTES = List.of(new BigDecimal("100.00"), new BigDecimal("18.00"));

    @Test
    void wcPreciosConIgvExtraeLaBase() {
        WcTotales wcT = WcCalculadoraIgv.wcCalcular(WC_IMPORTES, true, true);
        assertThat(wcT.wcTotal()).isEqualByComparingTo("118.00");
        assertThat(wcT.wcBaseImponible()).isEqualByComparingTo("100.00");
        assertThat(wcT.wcIgv()).isEqualByComparingTo("18.00");
    }

    @Test
    void wcPreciosSinIgvLoAgregaEncima() {
        WcTotales wcT = WcCalculadoraIgv.wcCalcular(WC_IMPORTES, true, false);
        assertThat(wcT.wcBaseImponible()).isEqualByComparingTo("118.00");
        assertThat(wcT.wcIgv()).isEqualByComparingTo("21.24");
        assertThat(wcT.wcTotal()).isEqualByComparingTo("139.24");
    }

    @Test
    void wcSinIgvElTotalEsLaSuma() {
        WcTotales wcT = WcCalculadoraIgv.wcCalcular(WC_IMPORTES, false, true);
        assertThat(wcT.wcBaseImponible()).isEqualByComparingTo("118.00");
        assertThat(wcT.wcIgv()).isEqualByComparingTo("0");
        assertThat(wcT.wcTotal()).isEqualByComparingTo("118.00");
        assertThat(wcT.wcTasa()).isEqualByComparingTo("0");
    }

    @Test
    void wcBaseMasIgvSiempreCuadraConElTotal() {
        for (int wcCentimos = 1; wcCentimos < 5000; wcCentimos += 7) {
            BigDecimal wcImporte = BigDecimal.valueOf(wcCentimos, 2);
            WcTotales wcT = WcCalculadoraIgv.wcCalcular(List.of(wcImporte), true, true);
            assertThat(wcT.wcBaseImponible().add(wcT.wcIgv())).isEqualByComparingTo(wcT.wcTotal());
        }
    }

    @Test
    void wcImporteRedondeaADosDecimales() {
        assertThat(WcCalculadoraIgv.wcImporte(3, new BigDecimal("3.335"))).isEqualByComparingTo("10.01");
    }
}
