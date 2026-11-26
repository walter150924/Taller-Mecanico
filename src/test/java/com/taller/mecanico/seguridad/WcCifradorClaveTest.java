package com.taller.mecanico.seguridad;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WcCifradorClaveTest {
    private final WcCifradorClave wcCifrador = new WcCifradorClave();

    @Test
    void wcReconoceLaClaveCorrecta() {
        String wcCifrada = wcCifrador.wcCifrar("secreta123");

        assertThat(wcCifrada).doesNotContain("secreta123");
        assertThat(wcCifrador.wcCoincide("secreta123", wcCifrada)).isTrue();
        assertThat(wcCifrador.wcCoincide("otra-clave", wcCifrada)).isFalse();
    }

    @Test
    void wcUsaUnaSalDistintaEnCadaCifrado() {
        assertThat(wcCifrador.wcCifrar("misma")).isNotEqualTo(wcCifrador.wcCifrar("misma"));
    }
}
