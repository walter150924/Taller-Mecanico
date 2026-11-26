package com.taller.mecanico.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@RequiredArgsConstructor
public enum WcPeriodo {
    WC_HOY("Hoy"),
    WC_ULTIMOS_7("Ultimos 7 dias"),
    WC_ESTE_MES("Este mes"),
    WC_ULTIMOS_30("Ultimos 30 dias"),
    WC_ULTIMOS_90("Ultimos 90 dias"),
    WC_ESTE_ANIO("Este año");

    private final String wcEtiqueta;

    public LocalDate wcDesde(LocalDate wcHoy) {
        return switch (this) {
            case WC_HOY -> wcHoy;
            case WC_ULTIMOS_7 -> wcHoy.minusDays(6);
            case WC_ESTE_MES -> wcHoy.withDayOfMonth(1);
            case WC_ULTIMOS_30 -> wcHoy.minusDays(29);
            case WC_ULTIMOS_90 -> wcHoy.minusDays(89);
            case WC_ESTE_ANIO -> wcHoy.withDayOfYear(1);
        };
    }

    public long wcDias(LocalDate wcHoy) {
        return ChronoUnit.DAYS.between(wcDesde(wcHoy), wcHoy) + 1;
    }

    @Override
    public String toString() {
        return wcEtiqueta;
    }
}
