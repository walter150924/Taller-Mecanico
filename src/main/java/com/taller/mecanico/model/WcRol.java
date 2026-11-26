package com.taller.mecanico.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WcRol {
    WC_ADMINISTRADOR("Administrador"),
    WC_RECEPCIONISTA("Recepcionista"),
    WC_MECANICO("Mecanico");

    private final String wcDescripcion;

    @Override
    public String toString() {
        return wcDescripcion;
    }
}
