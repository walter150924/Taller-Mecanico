package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcCompra;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.model.WcProveedor;
import com.taller.mecanico.repository.WcCompraRepository;
import com.taller.mecanico.repository.WcProveedorRepository;
import com.taller.mecanico.seguridad.WcSesion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WcProveedorService {
    private static final int WC_LIMITE_SUGERENCIAS = 8;

    private final WcProveedorRepository wcRepository;
    private final WcCompraRepository wcCompraRepository;
    private final WcSesion wcSesion;

    public Page<WcProveedor> wcListar(String wcTexto, int wcPagina, int wcTamanio) {
        return wcRepository.wcBuscar(wcTexto == null ? "" : wcTexto.trim(),
                PageRequest.of(wcPagina, wcTamanio, Sort.by("wcRazonSocial")));
    }

    public List<WcCompra> wcCompras(Long wcProveedorId) {
        return wcCompraRepository.findByWcProveedorWcIdOrderByWcFechaDesc(wcProveedorId);
    }

    @Transactional
    public WcProveedor wcGuardar(@Valid WcProveedor wcProveedor) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_COMPRAS);
        wcRepository.findByWcRuc(wcProveedor.getWcRuc())
                .filter(wcExistente -> !wcExistente.getWcId().equals(wcProveedor.getWcId()))
                .ifPresent(wcExistente -> {
                    throw new WcNegocioException("El RUC " + wcProveedor.getWcRuc() + " ya pertenece a "
                            + wcExistente.getWcRazonSocial() + ".");
                });
        return wcRepository.save(wcProveedor);
    }

    @Transactional
    public void wcEliminar(Long wcId) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_COMPRAS);
        if (!wcRepository.existsById(wcId)) {
            throw new WcNegocioException("El proveedor ya no existe en la base de datos.");
        }
        if (wcCompraRepository.existsByWcProveedorWcId(wcId)) {
            throw new WcNegocioException("No se puede eliminar: el proveedor tiene compras registradas.");
        }
        wcRepository.deleteById(wcId);
    }

    public List<WcProveedor> wcAutocompletar(String wcTexto) {
        if (wcTexto == null || wcTexto.isBlank()) {
            return List.of();
        }
        String wcT = wcTexto.trim();
        Specification<WcProveedor> wcSpec;
        if (wcT.matches("\\d+")) {
            wcSpec = (wcRaiz, wcQ, wcCb) -> wcCb.like(wcRaiz.get("wcRuc"), wcT + "%");
        } else {
            wcSpec = Specification.where(null);
            for (String wcPalabra : wcT.toLowerCase().split("\\s+")) {
                String wcPatron = "%" + wcPalabra + "%";
                wcSpec = wcSpec.and((wcRaiz, wcQ, wcCb) -> wcCb.like(wcCb.lower(wcRaiz.get("wcRazonSocial")), wcPatron));
            }
        }
        return wcRepository.findAll(wcSpec, PageRequest.of(0, WC_LIMITE_SUGERENCIAS, Sort.by("wcRazonSocial"))).getContent();
    }

    public Optional<WcProveedor> wcBuscarPorRuc(String wcRuc) {
        return wcRuc == null ? Optional.empty() : wcRepository.findByWcRuc(wcRuc.trim());
    }
}
