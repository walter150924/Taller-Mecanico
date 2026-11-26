package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.model.WcServicio;
import com.taller.mecanico.repository.WcOrdenServicioRepository;
import com.taller.mecanico.repository.WcServicioRepository;
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
public class WcServicioService {
    private final WcServicioRepository wcRepository;
    private final WcOrdenServicioRepository wcOrdenRepository;
    private final WcSesion wcSesion;

    public Page<WcServicio> wcListar(String wcTexto, int wcPagina, int wcTamanio) {
        return wcRepository.wcBuscar(wcTexto == null ? "" : wcTexto.trim(),
                PageRequest.of(wcPagina, wcTamanio, Sort.by("wcNombre")));
    }

    public List<WcServicio> wcActivos() {
        return wcRepository.findByWcActivoTrueOrderByWcNombre();
    }

    @Transactional
    public WcServicio wcGuardar(@Valid WcServicio wcServicio) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_CATALOGO);
        wcRepository.findByWcNombreIgnoreCase(wcServicio.getWcNombre())
                .filter(wcExistente -> !wcExistente.getWcId().equals(wcServicio.getWcId()))
                .ifPresent(wcExistente -> {
                    throw new WcNegocioException("Ya existe el servicio " + wcExistente.getWcNombre());
                });
        return wcRepository.save(wcServicio);
    }

    @Transactional
    public WcServicio wcRegistrarSiNoExiste(WcServicio wcServicio) {
        return wcRepository.findByWcNombreIgnoreCase(wcServicio.getWcNombre()).orElseGet(() -> wcRepository.save(wcServicio));
    }

    @Transactional
    public void wcEliminar(Long wcId) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_CATALOGO);
        if (!wcRepository.existsById(wcId)) {
            throw new WcNegocioException("El servicio ya no existe en la base de datos.");
        }
        if (wcOrdenRepository.wcUsaServicio(wcId)) {
            throw new WcNegocioException("No se puede eliminar: figura en ordenes de servicio. Desactivalo en su lugar.");
        }
        wcRepository.deleteById(wcId);
    }
}
