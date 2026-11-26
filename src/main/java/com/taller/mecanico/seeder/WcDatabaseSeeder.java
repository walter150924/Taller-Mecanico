package com.taller.mecanico.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true")
public class WcDatabaseSeeder implements ApplicationRunner {
    private final WcCategoriaSeeder wcCategoriaSeeder;
    private final WcRepuestoSeeder wcRepuestoSeeder;
    private final WcClienteSeeder wcClienteSeeder;
    private final WcProveedorSeeder wcProveedorSeeder;
    private final WcInventarioSeeder wcInventarioSeeder;
    private final WcVentaSeeder wcVentaSeeder;
    private final WcTallerSeeder wcTallerSeeder;

    @Override
    public void run(ApplicationArguments wcArgs) {
        int wcCategorias = wcCategoriaSeeder.wcSembrar();
        int wcRepuestos = wcRepuestoSeeder.wcSembrar();
        int wcClientes = wcClienteSeeder.wcSembrar();
        int wcProveedores = wcProveedorSeeder.wcSembrar();
        int wcApertura = wcInventarioSeeder.wcSembrar();
        int wcVentas = wcVentaSeeder.wcSembrar();
        int wcVehiculos = wcTallerSeeder.wcSembrar();
        log.info("Seeder: {} categorias, {} repuestos, {} clientes, {} proveedores, {} compras de apertura, "
                + "{} ventas y {} vehiculos nuevos registrados.", wcCategorias, wcRepuestos, wcClientes, wcProveedores, wcApertura, wcVentas, wcVehiculos);
    }
}
