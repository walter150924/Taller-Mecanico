package com.taller.mecanico.seeder;

import com.taller.mecanico.model.WcCliente;
import com.taller.mecanico.model.WcRol;
import com.taller.mecanico.model.WcServicio;
import com.taller.mecanico.model.WcVehiculo;
import com.taller.mecanico.repository.WcClienteRepository;
import com.taller.mecanico.repository.WcVehiculoRepository;
import com.taller.mecanico.service.WcServicioService;
import com.taller.mecanico.service.WcUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class WcTallerSeeder {
    private static final int WC_VEHICULOS_DE_PRUEBA = 30;
    private static final long WC_SEMILLA = 2026L;
    private static final String WC_CLAVE_DE_PRUEBA = "taller123";
    private static final String WC_LETRAS_PLACA = "ABCDEFGHJKLMNPRSTUVWXYZ";

    private record WcModeloVehiculo(String wcMarca, String wcModelo) {}

    private static final List<WcModeloVehiculo> WC_MODELOS = List.of(
            new WcModeloVehiculo("Toyota", "Yaris"), new WcModeloVehiculo("Toyota", "Corolla"),
            new WcModeloVehiculo("Toyota", "Hilux"), new WcModeloVehiculo("Hyundai", "Accent"),
            new WcModeloVehiculo("Hyundai", "Tucson"), new WcModeloVehiculo("Kia", "Rio"),
            new WcModeloVehiculo("Kia", "Sportage"), new WcModeloVehiculo("Nissan", "Sentra"),
            new WcModeloVehiculo("Nissan", "Frontier"), new WcModeloVehiculo("Chevrolet", "Sail"),
            new WcModeloVehiculo("Suzuki", "Swift"), new WcModeloVehiculo("Mitsubishi", "L200"));

    private static final List<String> WC_COLORES = List.of("Blanco", "Plata", "Negro", "Rojo", "Azul", "Gris");

    private record WcServicioBase(String wcNombre, String wcDescripcion, String wcPrecio) {
        WcServicio wcCrear() {
            return WcServicio.builder().wcNombre(wcNombre).wcDescripcion(wcDescripcion).wcPrecio(new BigDecimal(wcPrecio)).build();
        }
    }

    private static final List<WcServicioBase> WC_SERVICIOS = List.of(
            new WcServicioBase("Cambio de aceite y filtro", "Incluye revision de niveles", "40.00"),
            new WcServicioBase("Mantenimiento preventivo 10 000 km", "Revision general segun manual", "180.00"),
            new WcServicioBase("Cambio de pastillas de freno", "Eje delantero o posterior", "60.00"),
            new WcServicioBase("Rectificado de discos", "Por par de discos", "80.00"),
            new WcServicioBase("Alineamiento y balanceo", "Cuatro ruedas", "70.00"),
            new WcServicioBase("Afinamiento electronico", "Limpieza de inyectores y cambio de bujias", "150.00"),
            new WcServicioBase("Diagnostico con escaner", "Lectura y borrado de codigos de falla", "50.00"),
            new WcServicioBase("Cambio de embrague", "Mano de obra de desmontaje y montaje", "250.00"),
            new WcServicioBase("Cambio de faja de distribucion", "Incluye tensor si corresponde", "200.00"),
            new WcServicioBase("Mantenimiento del sistema de refrigeracion", "Purga y cambio de refrigerante", "90.00"),
            new WcServicioBase("Revision del sistema electrico", "Bateria, alternador y arranque", "60.00"),
            new WcServicioBase("Cambio de amortiguadores", "Por par", "120.00"));

    private final WcUsuarioService wcUsuarioService;
    private final WcServicioService wcServicioService;
    private final WcClienteRepository wcClienteRepository;
    private final WcVehiculoRepository wcVehiculoRepository;

    @Transactional
    public int wcSembrar() {
        wcUsuarioService.wcRegistrarSiNoExiste("recepcion", "Rosa Flores Diaz", WcRol.WC_RECEPCIONISTA, WC_CLAVE_DE_PRUEBA);
        wcUsuarioService.wcRegistrarSiNoExiste("jquispe", "Juan Quispe Mamani", WcRol.WC_MECANICO, WC_CLAVE_DE_PRUEBA);
        wcUsuarioService.wcRegistrarSiNoExiste("pmamani", "Pedro Mamani Condori", WcRol.WC_MECANICO, WC_CLAVE_DE_PRUEBA);
        WC_SERVICIOS.forEach(wcBase -> wcServicioService.wcRegistrarSiNoExiste(wcBase.wcCrear()));
        return wcSembrarVehiculos();
    }

    private int wcSembrarVehiculos() {
        if (wcVehiculoRepository.count() > 0) {
            return 0;
        }
        List<WcCliente> wcClientes = wcClienteRepository.findAll(Sort.by("wcId"));
        Random wcAzar = new Random(WC_SEMILLA);
        int wcCantidad = Math.min(WC_VEHICULOS_DE_PRUEBA, wcClientes.size());
        for (int wcI = 0; wcI < wcCantidad; wcI++) {
            WcModeloVehiculo wcModelo = WC_MODELOS.get(wcAzar.nextInt(WC_MODELOS.size()));
            wcVehiculoRepository.save(WcVehiculo.builder()
                    .wcPlaca(wcPlaca(wcI, wcAzar))
                    .wcMarca(wcModelo.wcMarca())
                    .wcModelo(wcModelo.wcModelo())
                    .wcAnio(2008 + wcAzar.nextInt(17))
                    .wcColor(WC_COLORES.get(wcAzar.nextInt(WC_COLORES.size())))
                    .wcKilometraje(5_000 + wcAzar.nextInt(195_000))
                    .wcCliente(wcClientes.get(wcI))
                    .build());
        }
        return wcCantidad;
    }

    private static String wcPlaca(int wcIndice, Random wcAzar) {
        char wcPrimera = WC_LETRAS_PLACA.charAt(wcAzar.nextInt(WC_LETRAS_PLACA.length()));
        char wcSegunda = WC_LETRAS_PLACA.charAt(wcAzar.nextInt(WC_LETRAS_PLACA.length()));
        return String.format("%c%c%d-%03d", wcPrimera, wcSegunda, wcIndice % 10, 100 + wcIndice * 29 % 900);
    }
}
