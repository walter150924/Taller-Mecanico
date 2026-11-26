package com.taller.mecanico.seeder;

import com.taller.mecanico.model.WcCliente;
import com.taller.mecanico.repository.WcClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WcClienteSeeder {
    private static final int WC_CANTIDAD = 40;
    private static final long WC_SEMILLA = 2026L;

    private static final List<String> WC_NOMBRES = List.of(
            "Maria", "Jose", "Lucia", "Carlos", "Ana", "Luis", "Rosa", "Pedro", "Elena", "Jorge",
            "Carmen", "Miguel", "Sofia", "Diego", "Valeria", "Juan", "Camila", "Andres", "Gabriela", "Ricardo",
            "Daniela", "Fernando", "Patricia", "Alejandro", "Milagros", "Renzo", "Claudia", "Hugo", "Fiorella", "Sebastian");

    private static final List<String> WC_APELLIDOS = List.of(
            "Quispe", "Flores", "Garcia", "Rojas", "Mamani", "Torres", "Huaman", "Diaz", "Vargas", "Castillo",
            "Ramirez", "Chavez", "Mendoza", "Gutierrez", "Salazar", "Ramos", "Espinoza", "Ccori", "Paredes", "Silva",
            "Cruz", "Sanchez", "Condori", "Herrera", "Medina", "Aguilar", "Rivera", "Navarro", "Cardenas", "Leon");

    private static final List<String> WC_DOMINIOS = List.of(
            "gmail.com", "hotmail.com", "outlook.com", "yahoo.com", "correo.pe");

    private final WcClienteRepository wcRepository;

    @Transactional
    public int wcSembrar() {
        Set<String> wcDnisExistentes = wcRepository.findAll().stream()
                .map(WcCliente::getWcDni)
                .collect(Collectors.toSet());

        Random wcAzar = new Random(WC_SEMILLA);
        Set<String> wcNombresUsados = new HashSet<>();
        Set<String> wcDnisUsados = new HashSet<>();
        Set<String> wcCorreosUsados = new HashSet<>();
        List<WcCliente> wcNuevas = new ArrayList<>();

        int wcGeneradas = 0;
        while (wcGeneradas < WC_CANTIDAD) {
            String wcNombre = wcElegir(wcAzar, WC_NOMBRES);
            String wcApellido1 = wcElegir(wcAzar, WC_APELLIDOS);
            String wcApellido2 = wcElegir(wcAzar, WC_APELLIDOS);
            if (wcApellido1.equals(wcApellido2) || !wcNombresUsados.add(wcNombre + wcApellido1 + wcApellido2)) {
                continue;
            }

            String wcDni;
            do {
                wcDni = String.valueOf(10_000_000 + wcAzar.nextInt(80_000_000));
            } while (!wcDnisUsados.add(wcDni));

            String wcCorreo;
            do {
                wcCorreo = (wcNombre + "." + wcApellido1 + wcAzar.nextInt(100)).toLowerCase()
                        + "@" + wcElegir(wcAzar, WC_DOMINIOS);
            } while (!wcCorreosUsados.add(wcCorreo));

            boolean wcConCorreo = wcAzar.nextDouble() < 0.85;
            boolean wcConTelefono = wcAzar.nextDouble() < 0.70;
            String wcTelefono = "9" + String.format("%08d", wcAzar.nextInt(100_000_000));

            wcGeneradas++;
            if (wcDnisExistentes.contains(wcDni)) {
                continue;
            }
            wcNuevas.add(WcCliente.builder()
                    .wcNombre(wcNombre)
                    .wcApellido(wcApellido1 + " " + wcApellido2)
                    .wcDni(wcDni)
                    .wcEmail(wcConCorreo ? wcCorreo : null)
                    .wcTelefono(wcConTelefono ? wcTelefono : null)
                    .build());
        }
        wcRepository.saveAll(wcNuevas);
        return wcNuevas.size();
    }

    private static String wcElegir(Random wcAzar, List<String> wcOpciones) {
        return wcOpciones.get(wcAzar.nextInt(wcOpciones.size()));
    }
}
