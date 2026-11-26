package com.taller.mecanico.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WcNumeroALetrasTest {
    @Test
    void wcConvierteImportes() {
        assertThat(WcNumeroALetras.wcImporte(new BigDecimal("0.50"))).isEqualTo("CERO CON 50/100 SOLES");
        assertThat(WcNumeroALetras.wcImporte(new BigDecimal("1"))).isEqualTo("UNO CON 00/100 SOLES");
        assertThat(WcNumeroALetras.wcImporte(new BigDecimal("100"))).isEqualTo("CIEN CON 00/100 SOLES");
        assertThat(WcNumeroALetras.wcImporte(new BigDecimal("118.45"))).isEqualTo("CIENTO DIECIOCHO CON 45/100 SOLES");
        assertThat(WcNumeroALetras.wcImporte(new BigDecimal("1250.5"))).isEqualTo("MIL DOSCIENTOS CINCUENTA CON 50/100 SOLES");
        assertThat(WcNumeroALetras.wcImporte(new BigDecimal("21000"))).isEqualTo("VEINTIUN MIL CON 00/100 SOLES");
        assertThat(WcNumeroALetras.wcImporte(new BigDecimal("31999.99")))
                .isEqualTo("TREINTA Y UN MIL NOVECIENTOS NOVENTA Y NUEVE CON 99/100 SOLES");
        assertThat(WcNumeroALetras.wcImporte(new BigDecimal("1000000"))).isEqualTo("UN MILLON CON 00/100 SOLES");
        assertThat(WcNumeroALetras.wcImporte(new BigDecimal("2500100"))).isEqualTo("DOS MILLONES QUINIENTOS MIL CIEN CON 00/100 SOLES");
    }
}
