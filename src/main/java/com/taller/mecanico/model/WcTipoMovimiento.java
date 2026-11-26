package com.taller.mecanico.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WcTipoMovimiento {
    WC_COMPRA("Compra", true),
    WC_ANULACION_VENTA("Anulacion de venta", true),
    WC_VENTA("Venta", false),
    WC_ANULACION_COMPRA("Anulacion de compra", false),
    WC_CONSUMO_ORDEN("Consumo en orden de servicio", false),
    WC_DEVOLUCION_ORDEN("Devolucion de orden de servicio", true);

    private final String wcDescripcion;
    private final boolean wcEntrada;

    @Override
    public String toString() {
        return wcDescripcion;
    }
}
