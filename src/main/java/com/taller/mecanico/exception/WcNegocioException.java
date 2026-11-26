package com.taller.mecanico.exception;

public class WcNegocioException extends RuntimeException {
    public WcNegocioException(String wcMensaje) {
        super(wcMensaje);
    }
}
