package com.taller.mecanico.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WcTipoDocumentoCompra {
    WC_FACTURA("Factura"),
    WC_BOLETA("Boleta de venta"),
    WC_GUIA_REMISION("Guia de remision"),
    WC_NOTA_VENTA("Nota de venta");

    private final String wcDescripcion;

    @Override
    public String toString() {
        return wcDescripcion;
    }
}
