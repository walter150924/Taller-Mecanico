package com.taller.mecanico.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum WcEstadoOrden {
    WC_RECIBIDO("Recibido", "Recibir", WcPermiso.WC_CREAR_ORDENES),
    WC_EN_DIAGNOSTICO("En diagnostico", "Iniciar diagnostico", WcPermiso.WC_TRABAJAR_ORDENES),
    WC_EN_REPARACION("En reparacion", "Iniciar reparacion", WcPermiso.WC_TRABAJAR_ORDENES),
    WC_LISTO("Listo para entrega", "Marcar como listo", WcPermiso.WC_TRABAJAR_ORDENES),
    WC_ENTREGADO("Entregado", "Entregar al cliente", WcPermiso.WC_ENTREGAR_ORDENES),
    WC_CANCELADO("Cancelado", "Cancelar orden", WcPermiso.WC_CANCELAR_ORDENES);

    private final String wcDescripcion;
    private final String wcAccion;
    private final WcPermiso wcPermisoRequerido;

    public boolean wcEsEditable() {
        return this == WC_RECIBIDO || this == WC_EN_DIAGNOSTICO || this == WC_EN_REPARACION;
    }

    public boolean wcEstaEnTaller() {
        return this != WC_ENTREGADO && this != WC_CANCELADO;
    }

    public boolean wcPuedePasarA(WcEstadoOrden wcDestino) {
        return switch (wcDestino) {
            case WC_EN_DIAGNOSTICO -> this == WC_RECIBIDO;
            case WC_EN_REPARACION -> this == WC_RECIBIDO || this == WC_EN_DIAGNOSTICO;
            case WC_LISTO -> this == WC_EN_REPARACION;
            case WC_ENTREGADO -> this == WC_LISTO;
            case WC_CANCELADO -> wcEstaEnTaller();
            case WC_RECIBIDO -> false;
        };
    }

    public List<WcEstadoOrden> wcSiguientes() {
        return Arrays.stream(values()).filter(this::wcPuedePasarA).toList();
    }

    public static List<WcEstadoOrden> wcEnTaller() {
        return Arrays.stream(values()).filter(WcEstadoOrden::wcEstaEnTaller).toList();
    }

    @Override
    public String toString() {
        return wcDescripcion;
    }
}
