package com.taller.mecanico.seeder;

import com.taller.mecanico.model.WcRol;
import com.taller.mecanico.service.WcUsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class WcUsuarioInicialSeeder implements ApplicationRunner {
    static final String WC_USUARIO_ADMINISTRADOR = "admin";
    static final String WC_CLAVE_ADMINISTRADOR = "admin123";

    private final WcUsuarioService wcUsuarioService;

    @Override
    public void run(ApplicationArguments wcArgs) {
        if (!wcUsuarioService.wcHayAdministradorActivo()) {
            wcUsuarioService.wcRegistrarSiNoExiste(WC_USUARIO_ADMINISTRADOR, "Administrador del taller",
                    WcRol.WC_ADMINISTRADOR, WC_CLAVE_ADMINISTRADOR);
            log.info("Usuario inicial creado: {} / {}", WC_USUARIO_ADMINISTRADOR, WC_CLAVE_ADMINISTRADOR);
        }
    }
}
