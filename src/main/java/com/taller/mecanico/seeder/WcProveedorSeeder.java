package com.taller.mecanico.seeder;

import com.taller.mecanico.model.WcProveedor;
import com.taller.mecanico.repository.WcProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WcProveedorSeeder {
    static final List<WcProveedor> WC_PROVEEDORES = List.of(
            wcProveedor("20601111111", "AUTOPARTES DEL SUR S.A.C.", "Av. Industrial 1450, Arequipa", "054221100"),
            wcProveedor("20602222222", "LUBRICANTES ANDINOS E.I.R.L.", "Jr. Los Pinos 230, Juliaca", "051332211"),
            wcProveedor("20603333333", "FRENOS Y EMBRAGUES PERU S.A.C.", "Av. Colonial 2210, Lima", "015443322"),
            wcProveedor("20604444444", "IMPORTADORA REPUESTOS ORIENTE S.R.L.", "Av. Grau 980, Cusco", "084245566"),
            wcProveedor("20605555555", "BATERIAS Y ELECTRICIDAD AUTOMOTRIZ S.A.C.", "Av. Tacna 615, Puno", "051366778"));

    private final WcProveedorRepository wcRepository;

    @Transactional
    public int wcSembrar() {
        Set<String> wcExistentes = wcRepository.findAll().stream().map(WcProveedor::getWcRuc).collect(Collectors.toSet());
        List<WcProveedor> wcNuevos = new ArrayList<>();
        for (WcProveedor wcP : WC_PROVEEDORES) {
            if (!wcExistentes.contains(wcP.getWcRuc())) {
                wcNuevos.add(WcProveedor.builder().wcRuc(wcP.getWcRuc()).wcRazonSocial(wcP.getWcRazonSocial())
                        .wcDireccion(wcP.getWcDireccion()).wcTelefono(wcP.getWcTelefono()).build());
            }
        }
        wcRepository.saveAll(wcNuevos);
        return wcNuevos.size();
    }

    private static WcProveedor wcProveedor(String wcRuc, String wcRazonSocial, String wcDireccion, String wcTelefono) {
        return WcProveedor.builder().wcRuc(wcRuc).wcRazonSocial(wcRazonSocial).wcDireccion(wcDireccion).wcTelefono(wcTelefono).build();
    }
}
