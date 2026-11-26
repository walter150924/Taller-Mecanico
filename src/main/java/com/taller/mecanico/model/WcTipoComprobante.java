package com.taller.mecanico.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WcTipoComprobante {
    WC_BOLETA("B001", "Boleta de venta electrónica"),
    WC_NOTA_VENTA("NV01", "Nota de venta");

    private final String wcSerie;
    private final String wcDescripcion;

    @Override
    public String toString() {
        return wcDescripcion;
    }
}
