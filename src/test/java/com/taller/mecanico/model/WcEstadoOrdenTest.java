package com.taller.mecanico.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WcEstadoOrdenTest {
    @Test
    void wcSigueElFlujoDelTaller() {
        assertThat(WcEstadoOrden.WC_RECIBIDO.wcSiguientes())
                .containsExactly(WcEstadoOrden.WC_EN_DIAGNOSTICO, WcEstadoOrden.WC_EN_REPARACION, WcEstadoOrden.WC_CANCELADO);
        assertThat(WcEstadoOrden.WC_EN_REPARACION.wcSiguientes())
                .containsExactly(WcEstadoOrden.WC_LISTO, WcEstadoOrden.WC_CANCELADO);
        assertThat(WcEstadoOrden.WC_LISTO.wcSiguientes())
                .containsExactly(WcEstadoOrden.WC_ENTREGADO, WcEstadoOrden.WC_CANCELADO);
    }

    @Test
    void wcLasOrdenesCerradasNoCambianDeEstado() {
        assertThat(WcEstadoOrden.WC_ENTREGADO.wcSiguientes()).isEmpty();
        assertThat(WcEstadoOrden.WC_CANCELADO.wcSiguientes()).isEmpty();
        assertThat(WcEstadoOrden.WC_LISTO.wcEsEditable()).isFalse();
        assertThat(WcEstadoOrden.WC_EN_DIAGNOSTICO.wcEsEditable()).isTrue();
    }

    @Test
    void wcElMecanicoNoPuedeEntregarNiCancelar() {
        assertThat(WcEstadoOrden.WC_LISTO.getWcPermisoRequerido().wcPermiteA(WcRol.WC_MECANICO)).isTrue();
        assertThat(WcEstadoOrden.WC_ENTREGADO.getWcPermisoRequerido().wcPermiteA(WcRol.WC_MECANICO)).isFalse();
        assertThat(WcEstadoOrden.WC_CANCELADO.getWcPermisoRequerido().wcPermiteA(WcRol.WC_MECANICO)).isFalse();
    }
}
