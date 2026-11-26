package com.taller.mecanico.ui;

import javafx.util.StringConverter;

import java.util.function.Function;

public final class WcConvertidores {
    private WcConvertidores() {
    }

    public static <T> StringConverter<T> wcMostrando(Function<T, String> wcTexto, String wcTextoSinValor) {
        return new StringConverter<>() {
            @Override
            public String toString(T wcValor) {
                return wcValor == null ? wcTextoSinValor : wcTexto.apply(wcValor);
            }

            @Override
            public T fromString(String wcTexto) {
                throw new UnsupportedOperationException("Seleccion de solo lectura");
            }
        };
    }
}
