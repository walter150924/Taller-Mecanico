package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.seguridad.WcSesion;
import com.taller.mecanico.repository.WcOrdenServicioRepository;
import com.taller.mecanico.model.WcCategoria;
import com.taller.mecanico.model.WcRepuesto;
import com.taller.mecanico.repository.WcCategoriaRepository;
import com.taller.mecanico.repository.WcCompraDetalleRepository;
import com.taller.mecanico.repository.WcRepuestoRepository;
import com.taller.mecanico.repository.WcVentaDetalleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WcRepuestoService {
    private static final int WC_TAMANIO_PAGINA = 15;
    private static final int WC_LIMITE_SUGERENCIAS = 10;

    private final WcRepuestoRepository wcRepository;
    private final WcCategoriaRepository wcCategoriaRepository;
    private final WcVentaDetalleRepository wcVentaDetalleRepository;
    private final WcCompraDetalleRepository wcCompraDetalleRepository;
    private final WcOrdenServicioRepository wcOrdenRepository;
    private final WcSesion wcSesion;

    public Page<WcRepuesto> wcListar(String wcFiltro, int wcPagina) {
        return wcListar(wcFiltro, wcPagina, WC_TAMANIO_PAGINA);
    }

    public Page<WcRepuesto> wcListar(String wcFiltro, int wcPagina, int wcTamanio) {
        Pageable wcPageable = PageRequest.of(wcPagina, wcTamanio, Sort.by("wcId").ascending());
        if (wcFiltro == null || wcFiltro.isBlank()) {
            return wcRepository.wcFindAllConCategoria(wcPageable);
        }
        return wcRepository.wcBuscar(wcFiltro.trim(), wcPageable);
    }

    public List<WcRepuesto> wcAutocompletar(String wcTexto) {
        if (wcTexto == null || wcTexto.isBlank()) {
            return List.of();
        }
        Specification<WcRepuesto> wcSpec = Specification.where(null);
        for (String wcPalabra : wcTexto.trim().toLowerCase().split("\\s+")) {
            String wcPatron = "%" + wcPalabra + "%";
            wcSpec = wcSpec.and((wcRaiz, wcQ, wcCb) -> wcCb.or(
                    wcCb.like(wcCb.lower(wcRaiz.get("wcCodigoRepuesto")), wcPatron),
                    wcCb.like(wcCb.lower(wcRaiz.get("wcNombre")), wcPatron)));
        }
        return wcRepository.findAll(wcSpec,
                PageRequest.of(0, WC_LIMITE_SUGERENCIAS, Sort.by("wcNombre"))).getContent();
    }

    public List<WcCategoria> wcListarCategorias() {
        return wcCategoriaRepository.findAll(Sort.by("wcNombre").ascending());
    }

    @Transactional
    public WcRepuesto wcGuardar(@Valid WcRepuesto wcRepuesto) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_CATALOGO);
        wcRepository.findByWcCodigoRepuesto(wcRepuesto.getWcCodigoRepuesto()).ifPresent(wcExistente -> {
            boolean wcEsOtroRepuesto = wcRepuesto.getWcId() == null
                    || !wcExistente.getWcId().equals(wcRepuesto.getWcId());
            if (wcEsOtroRepuesto) {
                throw new WcNegocioException(
                        "Ya existe un repuesto registrado con el codigo " + wcRepuesto.getWcCodigoRepuesto());
            }
        });

        if (wcRepuesto.getWcId() != null) {
            wcRepository.findById(wcRepuesto.getWcId()).ifPresent(wcActual -> {
                wcRepuesto.setWcStock(wcActual.getWcStock());
                wcRepuesto.setWcCostoPromedio(wcActual.getWcCostoPromedio());
            });
        } else {
            wcRepuesto.setWcStock(0);
            wcRepuesto.setWcCostoPromedio(null);
        }
        if (wcRepuesto.getWcStockMinimo() == null) {
            wcRepuesto.setWcStockMinimo(5);
        }
        WcRepuesto wcGuardado = wcRepository.save(wcRepuesto);
        log.info("Repuesto guardado: id={} codigo={}", wcGuardado.getWcId(), wcGuardado.getWcCodigoRepuesto());
        return wcGuardado;
    }

    @Transactional
    public void wcEliminar(Long wcId) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_CATALOGO);
        if (!wcRepository.existsById(wcId)) {
            throw new WcNegocioException("El repuesto ya no existe en la base de datos.");
        }
        if (wcVentaDetalleRepository.existsByWcRepuestoWcId(wcId)) {
            throw new WcNegocioException("No se puede eliminar: el repuesto figura en ventas registradas.");
        }
        if (wcCompraDetalleRepository.existsByWcRepuestoWcId(wcId)) {
            throw new WcNegocioException("No se puede eliminar: el repuesto figura en compras registradas.");
        }
        if (wcOrdenRepository.wcUsaRepuesto(wcId)) {
            throw new WcNegocioException("No se puede eliminar: el repuesto figura en ordenes de servicio.");
        }
        wcRepository.deleteById(wcId);
    }

    public long wcContar() {
        return wcRepository.count();
    }
}
