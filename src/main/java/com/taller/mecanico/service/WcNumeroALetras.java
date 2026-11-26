package com.taller.mecanico.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class WcNumeroALetras {
    private static final String[] WC_UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE",
            "OCHO", "NUEVE", "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
            "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO",
            "VEINTICINCO", "VEINTISEIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] WC_DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA",
            "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] WC_CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS",
            "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private WcNumeroALetras() {
    }

    public static String wcImporte(BigDecimal wcValor) {
        BigDecimal wcRedondeado = wcValor.setScale(2, RoundingMode.HALF_UP);
        long wcEntero = wcRedondeado.longValue();
        int wcCentimos = wcRedondeado.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        String wcLetras = wcEntero == 0 ? "CERO" : wcConvertir(wcEntero);
        return wcLetras + " CON " + String.format("%02d", wcCentimos) + "/100 SOLES";
    }

    static String wcConvertir(long wcN) {
        if (wcN >= 1_000_000) {
            long wcMillones = wcN / 1_000_000;
            String wcPrefijo = wcMillones == 1 ? "UN MILLON" : wcApocopar(wcConvertir(wcMillones)) + " MILLONES";
            long wcResto = wcN % 1_000_000;
            return wcResto == 0 ? wcPrefijo : wcPrefijo + " " + wcConvertir(wcResto);
        }
        if (wcN >= 1000) {
            long wcMiles = wcN / 1000;
            String wcPrefijo = wcMiles == 1 ? "MIL" : wcApocopar(wcConvertir(wcMiles)) + " MIL";
            long wcResto = wcN % 1000;
            return wcResto == 0 ? wcPrefijo : wcPrefijo + " " + wcConvertir(wcResto);
        }
        return wcMenorQueMil((int) wcN);
    }

    private static String wcMenorQueMil(int wcN) {
        if (wcN == 100) {
            return "CIEN";
        }
        int wcC = wcN / 100;
        int wcResto = wcN % 100;
        String wcCentena = WC_CENTENAS[wcC];
        String wcDecena;
        if (wcResto < 30) {
            wcDecena = WC_UNIDADES[wcResto];
        } else {
            wcDecena = WC_DECENAS[wcResto / 10] + (wcResto % 10 == 0 ? "" : " Y " + WC_UNIDADES[wcResto % 10]);
        }
        return (wcCentena + " " + wcDecena).trim();
    }

    private static String wcApocopar(String wcTexto) {
        return wcTexto.endsWith("UNO") ? wcTexto.substring(0, wcTexto.length() - 1) : wcTexto;
    }
}
