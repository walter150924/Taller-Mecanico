package com.taller.mecanico.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WcMetodoPago {
    WC_EFECTIVO("Efectivo"),
    WC_TARJETA("Tarjeta"),
    WC_YAPE("Yape"),
    WC_PLIN("Plin"),
    WC_TRANSFERENCIA("Transferencia");

    private final String wcDescripcion;

    @Override
    public String toString() {
        return wcDescripcion;
    }
}
