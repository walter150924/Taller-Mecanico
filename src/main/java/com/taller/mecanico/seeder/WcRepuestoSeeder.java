package com.taller.mecanico.seeder;

import com.taller.mecanico.model.WcCategoria;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.repository.WcCategoriaRepository;
import com.taller.mecanico.repository.WcRepuestoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WcRepuestoSeeder {
    private record WcPlantilla(String wcNombre, String wcCategoria, double wcPrecio,
                             String wcPresentacionA, String wcPresentacionB, double wcFactorB) {}

    private static final List<WcPlantilla> WC_PLANTILLAS = List.of(
            new WcPlantilla("Aceite de motor 20W-50", "Aceites y lubricantes", 28.00, "1 galon", "balde 5 galones", 4.6),
            new WcPlantilla("Aceite sintetico 5W-30", "Aceites y lubricantes", 45.00, "1 litro", "1 galon", 3.5),
            new WcPlantilla("Filtro de aceite", "Filtros", 18.00, "Toyota", "Hyundai", 1.1),
            new WcPlantilla("Filtro de aire", "Filtros", 35.00, "Toyota", "Kia", 1.1),
            new WcPlantilla("Filtro de combustible", "Filtros", 30.00, "gasolina", "diesel", 1.6),
            new WcPlantilla("Pastillas de freno delanteras", "Frenos", 85.00, "sedan", "camioneta", 1.5),
            new WcPlantilla("Zapatas de freno", "Frenos", 70.00, "sedan", "camioneta", 1.4),
            new WcPlantilla("Disco de freno", "Frenos", 140.00, "sedan", "camioneta", 1.5),
            new WcPlantilla("Liquido de frenos DOT 4", "Liquidos y aditivos", 22.00, "500 ml", "1 litro", 1.8),
            new WcPlantilla("Amortiguador delantero", "Suspension", 180.00, "sedan", "camioneta", 1.6),
            new WcPlantilla("Terminal de direccion", "Direccion", 55.00, "izquierdo", "derecho", 1.0),
            new WcPlantilla("Bujia de iridio", "Bujias", 38.00, "unidad", "juego x4", 3.8),
            new WcPlantilla("Bateria 12V", "Baterias", 320.00, "13 placas", "15 placas", 1.3),
            new WcPlantilla("Foco halogeno H4", "Iluminacion", 25.00, "unidad", "par", 1.9),
            new WcPlantilla("Refrigerante", "Refrigeracion", 30.00, "1 galon", "balde 5 galones", 4.5),
            new WcPlantilla("Bomba de agua", "Refrigeracion", 210.00, "1.5 L", "2.0 L", 1.2),
            new WcPlantilla("Kit de embrague", "Embrague", 450.00, "sedan", "camioneta", 1.5),
            new WcPlantilla("Faja de distribucion", "Correas y cadenas", 120.00, "kit simple", "kit con tensor", 1.8),
            new WcPlantilla("Plumilla limpiaparabrisas", "Plumillas", 20.00, "18 pulgadas", "22 pulgadas", 1.2),
            new WcPlantilla("Rodaje de rueda", "Rodamientos", 75.00, "delantero", "posterior", 1.1));

    private final WcRepuestoRepository wcRepository;
    private final WcCategoriaRepository wcCategoriaRepository;

    @Transactional
    public int wcSembrar() {
        Set<String> wcExistentes = wcRepository.findAll().stream()
                .map(WcRepuesto::getWcCodigoRepuesto)
                .collect(Collectors.toSet());
        Map<String, WcCategoria> wcCategorias = wcCategoriaRepository.findAll().stream()
                .collect(Collectors.toMap(WcCategoria::getWcNombre, Function.identity(), (wcA, wcB) -> wcA));

        List<WcRepuesto> wcNuevos = new ArrayList<>();
        int wcNumero = 0;
        for (WcPlantilla wcP : WC_PLANTILLAS) {
            for (boolean wcEsB : new boolean[]{false, true}) {
                wcNumero++;
                String wcCodigo = String.format("REP-%03d", wcNumero);
                if (wcExistentes.contains(wcCodigo)) {
                    continue;
                }
                String wcPresentacion = wcEsB ? wcP.wcPresentacionB() : wcP.wcPresentacionA();
                BigDecimal wcPrecio = BigDecimal.valueOf(wcEsB ? wcP.wcPrecio() * wcP.wcFactorB() : wcP.wcPrecio())
                        .setScale(2, RoundingMode.HALF_UP);

                WcCategoria wcCategoria = wcCategorias.get(wcP.wcCategoria());

                wcNuevos.add(WcRepuesto.builder()
                        .wcCodigoRepuesto(wcCodigo)
                        .wcNombre(wcP.wcNombre() + " " + wcPresentacion)
                        .wcDescripcion(wcNumero % 6 == 0 ? null : wcP.wcNombre() + ", presentacion " + wcPresentacion)
                        .wcPrecio(wcPrecio)
                        .wcCategoria(wcCategoria)
                        .build());
            }
        }
        wcRepository.saveAll(wcNuevos);
        return wcNuevos.size();
    }
}
