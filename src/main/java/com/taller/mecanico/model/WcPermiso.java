package com.taller.mecanico.model;

import java.util.EnumSet;
import java.util.Set;

import static com.taller.mecanico.model.WcRol.WC_ADMINISTRADOR;
import static com.taller.mecanico.model.WcRol.WC_MECANICO;
import static com.taller.mecanico.model.WcRol.WC_RECEPCIONISTA;

public enum WcPermiso {
    WC_VER_DASHBOARD(WC_ADMINISTRADOR),
    WC_GESTIONAR_VENTAS(WC_ADMINISTRADOR, WC_RECEPCIONISTA),
    WC_GESTIONAR_COMPRAS(WC_ADMINISTRADOR),
    WC_VER_INVENTARIO(WC_ADMINISTRADOR, WC_RECEPCIONISTA),
    WC_GESTIONAR_CLIENTES(WC_ADMINISTRADOR, WC_RECEPCIONISTA),
    WC_GESTIONAR_VEHICULOS(WC_ADMINISTRADOR, WC_RECEPCIONISTA),
    WC_GESTIONAR_CATALOGO(WC_ADMINISTRADOR),
    WC_GESTIONAR_USUARIOS(WC_ADMINISTRADOR),
    WC_CREAR_ORDENES(WC_ADMINISTRADOR, WC_RECEPCIONISTA),
    WC_TRABAJAR_ORDENES(WC_ADMINISTRADOR, WC_RECEPCIONISTA, WC_MECANICO),
    WC_ENTREGAR_ORDENES(WC_ADMINISTRADOR, WC_RECEPCIONISTA),
    WC_CANCELAR_ORDENES(WC_ADMINISTRADOR, WC_RECEPCIONISTA),
    WC_VER_TODAS_LAS_ORDENES(WC_ADMINISTRADOR, WC_RECEPCIONISTA);

    private final Set<WcRol> wcRolesPermitidos;

    WcPermiso(WcRol wcPrimero, WcRol... wcOtros) {
        this.wcRolesPermitidos = EnumSet.of(wcPrimero, wcOtros);
    }

    public boolean wcPermiteA(WcRol wcRol) {
        return wcRolesPermitidos.contains(wcRol);
    }
}
