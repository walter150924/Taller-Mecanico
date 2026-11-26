package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.seguridad.WcSesion;
import com.taller.mecanico.repository.WcVehiculoRepository;
import com.taller.mecanico.model.WcCliente;
import com.taller.mecanico.repository.WcClienteRepository;
import com.taller.mecanico.repository.WcVentaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WcClienteService {
    private static final int WC_TAMANIO_PAGINA = 15;
    private static final int WC_LIMITE_SUGERENCIAS = 8;

    private final WcClienteRepository wcRepository;
    private final WcVentaRepository wcVentaRepository;
    private final WcVehiculoRepository wcVehiculoRepository;
    private final WcSesion wcSesion;

    public Page<WcCliente> wcListar(String wcFiltro, int wcPagina) {
        return wcListar(wcFiltro, wcPagina, WC_TAMANIO_PAGINA);
    }

    public Page<WcCliente> wcListar(String wcFiltro, int wcPagina, int wcTamanio) {
        Pageable wcPageable = PageRequest.of(wcPagina, wcTamanio, Sort.by("wcId").ascending());
        if (wcFiltro == null || wcFiltro.isBlank()) {
            return wcRepository.findAll(wcPageable);
        }
        return wcRepository.wcBuscar(wcFiltro.trim(), wcPageable);
    }

    public List<WcCliente> wcAutocompletar(String wcTexto) {
        if (wcTexto == null || wcTexto.isBlank()) {
            return List.of();
        }
        String wcT = wcTexto.trim();
        Specification<WcCliente> wcFiltro = wcT.matches("\\d+")
                ? (wcRaiz, wcQ, wcCb) -> wcCb.like(wcRaiz.get("wcDni"), wcT + "%")
                : wcPorPalabras(wcT);
        return wcRepository.findAll(wcFiltro,
                PageRequest.of(0, WC_LIMITE_SUGERENCIAS, Sort.by("wcApellido", "wcNombre"))).getContent();
    }

    public Optional<WcCliente> wcBuscarPorDni(String wcDni) {
        return wcDni == null ? Optional.empty() : wcRepository.findByWcDni(wcDni.trim());
    }

    private static Specification<WcCliente> wcPorPalabras(String wcTexto) {
        Specification<WcCliente> wcSpec = Specification.where(null);
        for (String wcPalabra : wcTexto.toLowerCase().split("\\s+")) {
            String wcPatron = "%" + wcPalabra + "%";
            wcSpec = wcSpec.and((wcRaiz, wcQ, wcCb) -> wcCb.or(
                    wcCb.like(wcCb.lower(wcRaiz.get("wcNombre")), wcPatron),
                    wcCb.like(wcCb.lower(wcRaiz.get("wcApellido")), wcPatron)));
        }
        return wcSpec;
    }

    @Transactional
    public WcCliente wcGuardar(@Valid WcCliente wcCliente) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_CLIENTES);
        wcRepository.findByWcDni(wcCliente.getWcDni()).ifPresent(wcExistente -> {
            boolean wcEsOtroCliente = wcCliente.getWcId() == null
                    || !wcExistente.getWcId().equals(wcCliente.getWcId());
            if (wcEsOtroCliente) {
                throw new WcNegocioException(
                        "Ya existe un cliente registrado con el DNI " + wcCliente.getWcDni());
            }
        });

        WcCliente wcGuardado = wcRepository.save(wcCliente);
        log.info("Cliente guardado: id={} dni={}", wcGuardado.getWcId(), wcGuardado.getWcDni());
        return wcGuardado;
    }

    @Transactional
    public void wcEliminar(Long wcId) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_CLIENTES);
        if (!wcRepository.existsById(wcId)) {
            throw new WcNegocioException("El cliente ya no existe en la base de datos.");
        }
        if (wcVentaRepository.existsByWcClienteWcId(wcId)) {
            throw new WcNegocioException("No se puede eliminar: el cliente tiene ventas registradas.");
        }
        if (wcVehiculoRepository.existsByWcClienteWcId(wcId)) {
            throw new WcNegocioException("No se puede eliminar: el cliente tiene vehiculos registrados.");
        }
        wcRepository.deleteById(wcId);
    }

    public long wcContar() {
        return wcRepository.count();
    }
}
