package com.taller.mecanico;

import com.taller.mecanico.ui.WcExportador;
import com.taller.mecanico.ui.WcVentana;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class WcJavaFxApplication extends Application {
    private ConfigurableApplicationContext wcContext;

    @Override
    public void init() {
        this.wcContext = new SpringApplicationBuilder(WcTallerMecanicoApplication.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    @Override
    public void start(Stage wcStage) {
        wcContext.getBean(WcExportador.class).setWcHostServices(getHostServices());
        wcContext.getBean(WcVentana.class).wcIniciar(wcStage);
    }

    @Override
    public void stop() {
        wcContext.close();
        Platform.exit();
    }
}
