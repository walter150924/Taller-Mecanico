package com.taller.mecanico.seguridad;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.model.WcUsuario;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WcSesion {
    private WcUsuario wcUsuarioActual;

    public void wcIniciar(WcUsuario wcUsuario) {
        this.wcUsuarioActual = wcUsuario;
    }

    public void wcCerrar() {
        this.wcUsuarioActual = null;
    }

    public Optional<WcUsuario> wcUsuario() {
        return Optional.ofNullable(wcUsuarioActual);
    }

    public WcUsuario wcUsuarioActual() {
        return wcUsuario().orElseThrow(() -> new WcNegocioException("No hay una sesion iniciada."));
    }

    public boolean wcTiene(WcPermiso wcPermiso) {
        return wcUsuario().map(wcU -> wcU.wcTiene(wcPermiso)).orElse(false);
    }

    public void wcExigir(WcPermiso wcPermiso) {
        if (!wcTiene(wcPermiso)) {
            throw new WcNegocioException("No tienes permiso para realizar esta accion.");
        }
    }

    public boolean wcEsMecanico() {
        return wcUsuario().map(WcUsuario::wcEsMecanico).orElse(false);
    }
}
