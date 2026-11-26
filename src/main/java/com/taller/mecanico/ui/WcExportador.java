package com.taller.mecanico.ui;

import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@RequiredArgsConstructor
@Slf4j
public class WcExportador {
    @FunctionalInterface
    public interface WcGenerador {
        void wcGenerar(File wcDestino) throws Exception;
    }

    public enum WcFormato {
        WC_PDF("Documento PDF", "*.pdf", ".pdf"),
        WC_EXCEL("Libro de Excel", "*.xlsx", ".xlsx");

        final String wcDescripcion;
        final String wcPatron;
        final String wcExtension;

        WcFormato(String wcDescripcion, String wcPatron, String wcExtension) {
            this.wcDescripcion = wcDescripcion;
            this.wcPatron = wcPatron;
            this.wcExtension = wcExtension;
        }
    }

    private final WcNotificador wcNotificador;
    private HostServices wcHostServices;
    private File wcUltimaCarpeta = new File(System.getProperty("user.home"));

    public void setWcHostServices(HostServices wcHostServices) {
        this.wcHostServices = wcHostServices;
    }

    public void wcGuardar(Window wcVentana, String wcTitulo, String wcNombreSugerido, WcFormato wcFormato, WcGenerador wcGenerador) {
        FileChooser wcSelector = new FileChooser();
        wcSelector.setTitle(wcTitulo);
        wcSelector.setInitialFileName(wcNombreSugerido + wcFormato.wcExtension);
        wcSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter(wcFormato.wcDescripcion, wcFormato.wcPatron));
        if (wcUltimaCarpeta.isDirectory()) {
            wcSelector.setInitialDirectory(wcUltimaCarpeta);
        }
        File wcElegido = wcSelector.showSaveDialog(wcVentana);
        if (wcElegido == null) {
            return;
        }
        File wcDestino = wcElegido.getName().toLowerCase().endsWith(wcFormato.wcExtension)
                ? wcElegido
                : new File(wcElegido.getParentFile(), wcElegido.getName() + wcFormato.wcExtension);
        wcUltimaCarpeta = wcDestino.getParentFile();

        wcEjecutar(wcDestino, wcGenerador, () -> {
            wcNotificador.wcExito("Archivo guardado: " + wcDestino.getName());
            wcAbrir(wcDestino);
        });
    }

    public void wcAbrirTemporal(String wcNombre, WcFormato wcFormato, WcGenerador wcGenerador) {
        File wcDestino;
        try {
            File wcCarpeta = Files.createTempDirectory("comprobantes").toFile();
            wcCarpeta.deleteOnExit();
            wcDestino = new File(wcCarpeta, wcNombre + wcFormato.wcExtension);
            wcDestino.deleteOnExit();
        } catch (IOException wcE) {
            wcNotificador.wcError("No se pudo crear el archivo temporal: " + wcE.getMessage());
            return;
        }
        wcEjecutar(wcDestino, wcGenerador, () -> {
            wcNotificador.wcInfo("Abriendo " + wcDestino.getName() + "…");
            wcAbrir(wcDestino);
        });
    }

    private void wcEjecutar(File wcDestino, WcGenerador wcGenerador, Runnable wcAlTerminar) {
        Task<Void> wcTarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                wcGenerador.wcGenerar(wcDestino);
                return null;
            }
        };
        wcTarea.setOnSucceeded(wcE -> wcAlTerminar.run());
        wcTarea.setOnFailed(wcE -> {
            Throwable wcCausa = wcTarea.getException();
            log.error("No se pudo generar {}", wcDestino, wcCausa);
            wcNotificador.wcError("No se pudo generar el archivo: " + wcCausa.getMessage());
        });
        Thread wcHilo = new Thread(wcTarea, "exportador");
        wcHilo.setDaemon(true);
        wcHilo.start();
    }

    private void wcAbrir(File wcArchivo) {
        if (wcHostServices != null) {
            wcHostServices.showDocument(wcArchivo.toURI().toString());
        }
    }
}
