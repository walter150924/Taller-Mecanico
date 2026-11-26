package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.seguridad.WcSesion;
import com.taller.mecanico.model.WcCategoria;
import com.taller.mecanico.repository.WcCategoriaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WcCategoriaService {
    private static final int WC_TAMANIO_PAGINA = 15;

    private final WcCategoriaRepository wcRepository;
    private final WcSesion wcSesion;

    public Page<WcCategoria> wcListar(String wcFiltro, int wcPagina) {
        return wcListar(wcFiltro, wcPagina, WC_TAMANIO_PAGINA);
    }

    public Page<WcCategoria> wcListar(String wcFiltro, int wcPagina, int wcTamanio) {
        Pageable wcPageable = PageRequest.of(wcPagina, wcTamanio, Sort.by("wcId").ascending());
        if (wcFiltro == null || wcFiltro.isBlank()) {
            return wcRepository.findAll(wcPageable);
        }
        return wcRepository.wcBuscar(wcFiltro.trim(), wcPageable);
    }

    @Transactional
    public WcCategoria wcGuardar(@Valid WcCategoria wcCategoria) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_CATALOGO);
        wcRepository.findByWcCodigoCategoria(wcCategoria.getWcCodigoCategoria()).ifPresent(wcExistente -> {
            boolean wcEsOtraCategoria = wcCategoria.getWcId() == null
                    || !wcExistente.getWcId().equals(wcCategoria.getWcId());
            if (wcEsOtraCategoria) {
                throw new WcNegocioException(
                        "Ya existe una categoria registrada con el codigo " + wcCategoria.getWcCodigoCategoria());
            }
        });

        WcCategoria wcGuardada = wcRepository.save(wcCategoria);
        log.info("Categoria guardada: id={} codigo={}", wcGuardada.getWcId(), wcGuardada.getWcCodigoCategoria());
        return wcGuardada;
    }

    @Transactional
    public void wcEliminar(Long wcId) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_CATALOGO);
        if (!wcRepository.existsById(wcId)) {
            throw new WcNegocioException("La categoria ya no existe en la base de datos.");
        }
        wcRepository.deleteById(wcId);
    }

    public long wcContar() {
        return wcRepository.count();
    }
}
