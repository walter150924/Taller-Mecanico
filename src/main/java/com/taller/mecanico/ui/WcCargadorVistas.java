package com.taller.mecanico.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

@Component
@RequiredArgsConstructor
public class WcCargadorVistas {
    private final ApplicationContext wcContext;

    public record WcVista(Parent wcNodo, Object wcControlador) {}

    public WcVista wcCargar(String wcNombre) {
        FXMLLoader wcLoader = new FXMLLoader(getClass().getResource("/view/" + wcNombre + ".fxml"));
        wcLoader.setControllerFactory(wcContext::getBean);
        wcLoader.setClassLoader(getClass().getClassLoader());
        try {
            Parent wcNodo = wcLoader.load();
            return new WcVista(wcNodo, wcLoader.getController());
        } catch (IOException wcE) {
            throw new UncheckedIOException("No se pudo cargar la vista " + wcNombre, wcE);
        }
    }
}
