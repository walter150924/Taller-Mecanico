package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcOrdenServicio;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.model.WcVehiculo;
import com.taller.mecanico.repository.WcOrdenServicioRepository;
import com.taller.mecanico.repository.WcVehiculoRepository;
import com.taller.mecanico.seguridad.WcSesion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WcVehiculoService {
    private static final int WC_LIMITE_SUGERENCIAS = 8;

    private final WcVehiculoRepository wcRepository;
    private final WcOrdenServicioRepository wcOrdenRepository;
    private final WcSesion wcSesion;

    public Page<WcVehiculo> wcListar(String wcTexto, int wcPagina, int wcTamanio) {
        return wcRepository.wcBuscar(wcTexto == null ? "" : wcTexto.trim(),
                PageRequest.of(wcPagina, wcTamanio, Sort.by("wcPlaca")));
    }

    public List<WcVehiculo> wcAutocompletar(String wcTexto) {
        if (wcTexto == null || wcTexto.isBlank()) {
            return List.of();
        }
        return wcListar(wcTexto, 0, WC_LIMITE_SUGERENCIAS).getContent();
    }

    public List<WcOrdenServicio> wcHistorial(Long wcVehiculoId) {
        return wcOrdenRepository.findByWcVehiculoWcIdOrderByWcFechaIngresoDesc(wcVehiculoId);
    }

    @Transactional
    public WcVehiculo wcGuardar(@Valid WcVehiculo wcVehiculo) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_VEHICULOS);
        wcRepository.findByWcPlaca(wcVehiculo.getWcPlaca())
                .filter(wcExistente -> !wcExistente.getWcId().equals(wcVehiculo.getWcId()))
                .ifPresent(wcExistente -> {
                    throw new WcNegocioException("Ya existe un vehiculo con la placa " + wcVehiculo.getWcPlaca());
                });
        return wcRepository.save(wcVehiculo);
    }

    @Transactional
    public void wcEliminar(Long wcId) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_VEHICULOS);
        if (!wcRepository.existsById(wcId)) {
            throw new WcNegocioException("El vehiculo ya no existe en la base de datos.");
        }
        if (wcOrdenRepository.existsByWcVehiculoWcId(wcId)) {
            throw new WcNegocioException("No se puede eliminar: el vehiculo tiene ordenes de servicio registradas.");
        }
        wcRepository.deleteById(wcId);
    }
}
