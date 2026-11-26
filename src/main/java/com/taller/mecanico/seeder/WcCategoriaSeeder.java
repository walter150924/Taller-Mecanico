package com.taller.mecanico.seeder;

import com.taller.mecanico.model.WcCategoria;
import com.taller.mecanico.repository.WcCategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WcCategoriaSeeder {
    static final List<String> WC_NOMBRES = List.of(
            "Aceites y lubricantes", "Filtros", "Frenos", "Suspension", "Direccion",
            "Encendido", "Bujias", "Baterias", "Sistema electrico", "Iluminacion",
            "Refrigeracion", "Transmision", "Embrague", "Escape", "Correas y cadenas",
            "Neumaticos", "Plumillas", "Liquidos y aditivos", "Rodamientos", "Empaquetaduras");

    private static final List<String> WC_PLANTILLAS_DESCRIPCION = List.of(
            "Repuestos de %s",
            "Linea de %s para autos y camionetas",
            "%s de marcas originales y alternativas",
            "Stock de %s para el taller");

    private final WcCategoriaRepository wcRepository;

    @Transactional
    public int wcSembrar() {
        Set<String> wcExistentes = wcRepository.findAll().stream()
                .map(WcCategoria::getWcCodigoCategoria)
                .collect(Collectors.toSet());

        List<WcCategoria> wcNuevas = new ArrayList<>();
        for (int wcI = 0; wcI < WC_NOMBRES.size(); wcI++) {
            String wcCodigo = String.format("CAT-%03d", wcI + 1);
            if (wcExistentes.contains(wcCodigo)) {
                continue;
            }
            String wcNombre = WC_NOMBRES.get(wcI);

            String wcPlantilla = WC_PLANTILLAS_DESCRIPCION.get(wcI % WC_PLANTILLAS_DESCRIPCION.size());
            String wcDescripcion = wcI % 7 == 6
                    ? null
                    : String.format(wcPlantilla, wcPlantilla.startsWith("%s") ? wcNombre : wcNombre.toLowerCase());
            wcNuevas.add(WcCategoria.builder()
                    .wcCodigoCategoria(wcCodigo)
                    .wcNombre(wcNombre)
                    .wcDescripcion(wcDescripcion)
                    .build());
        }
        wcRepository.saveAll(wcNuevas);
        return wcNuevas.size();
    }
}
