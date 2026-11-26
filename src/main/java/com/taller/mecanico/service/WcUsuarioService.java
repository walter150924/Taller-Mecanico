package com.taller.mecanico.service;

import com.taller.mecanico.exception.WcNegocioException;
import com.taller.mecanico.model.WcPermiso;
import com.taller.mecanico.model.WcRol;
import com.taller.mecanico.model.WcUsuario;
import com.taller.mecanico.repository.WcOrdenServicioRepository;
import com.taller.mecanico.repository.WcUsuarioRepository;
import com.taller.mecanico.seguridad.WcCifradorClave;
import com.taller.mecanico.seguridad.WcSesion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Transactional(readOnly = true)
public class WcUsuarioService {
    public static final int WC_LONGITUD_MINIMA_CLAVE = 6;
    private static final String WC_CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos.";

    private final WcUsuarioRepository wcRepository;
    private final WcOrdenServicioRepository wcOrdenRepository;
    private final WcCifradorClave wcCifrador;
    private final WcSesion wcSesion;

    public Page<WcUsuario> wcListar(String wcTexto, int wcPagina, int wcTamanio) {
        return wcRepository.wcBuscar(wcTexto == null ? "" : wcTexto.trim(),
                PageRequest.of(wcPagina, wcTamanio, Sort.by("wcRol", "wcNombreCompleto")));
    }

    public List<WcUsuario> wcMecanicosActivos() {
        return wcRepository.findByWcRolAndWcActivoTrueOrderByWcNombreCompleto(WcRol.WC_MECANICO);
    }

    public WcUsuario wcIniciarSesion(String wcNombreUsuario, String wcClave) {
        WcUsuario wcUsuario = wcRepository.findByWcNombreUsuario(wcNombreUsuario.trim().toLowerCase())
                .filter(WcUsuario::isWcActivo)
                .filter(wcU -> wcCifrador.wcCoincide(wcClave, wcU.getWcClaveCifrada()))
                .orElseThrow(() -> new WcNegocioException(WC_CREDENCIALES_INVALIDAS));
        wcSesion.wcIniciar(wcUsuario);
        log.info("Inicio de sesion: {} ({})", wcUsuario.getWcNombreUsuario(), wcUsuario.getWcRol());
        return wcUsuario;
    }

    @Transactional
    public WcUsuario wcGuardar(@Valid WcUsuario wcUsuario, String wcClaveNueva) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_USUARIOS);
        wcValidarNombreUnico(wcUsuario);
        wcValidarQueNoSeDesactiveASiMismo(wcUsuario);
        wcValidarQueQuedeUnAdministrador(wcUsuario);
        wcAsignarClave(wcUsuario, wcClaveNueva);
        return wcRepository.save(wcUsuario);
    }

    @Transactional
    public WcUsuario wcRegistrarSiNoExiste(String wcNombreUsuario, String wcNombreCompleto, WcRol wcRol, String wcClave) {
        return wcRepository.findByWcNombreUsuario(wcNombreUsuario).orElseGet(() -> wcRepository.save(WcUsuario.builder()
                .wcNombreUsuario(wcNombreUsuario)
                .wcNombreCompleto(wcNombreCompleto)
                .wcRol(wcRol)
                .wcClaveCifrada(wcCifrador.wcCifrar(wcClave))
                .build()));
    }

    @Transactional
    public void wcEliminar(Long wcId) {
        wcSesion.wcExigir(WcPermiso.WC_GESTIONAR_USUARIOS);
        WcUsuario wcUsuario = wcRepository.findById(wcId)
                .orElseThrow(() -> new WcNegocioException("El usuario ya no existe en la base de datos."));
        if (wcUsuario.getWcId().equals(wcSesion.wcUsuarioActual().getWcId())) {
            throw new WcNegocioException("No puedes eliminar tu propio usuario.");
        }
        if (wcOrdenRepository.existsByWcMecanicoWcId(wcId)) {
            throw new WcNegocioException("No se puede eliminar: tiene ordenes de servicio asignadas. Desactivalo en su lugar.");
        }
        if (wcEsUltimoAdministradorActivo(wcUsuario)) {
            throw new WcNegocioException("Debe quedar al menos un administrador activo.");
        }
        wcRepository.delete(wcUsuario);
    }

    public boolean wcHayAdministradorActivo() {
        return wcRepository.countByWcRolAndWcActivoTrue(WcRol.WC_ADMINISTRADOR) > 0;
    }

    private void wcValidarNombreUnico(WcUsuario wcUsuario) {
        wcRepository.findByWcNombreUsuario(wcUsuario.getWcNombreUsuario())
                .filter(wcExistente -> !wcExistente.getWcId().equals(wcUsuario.getWcId()))
                .ifPresent(wcExistente -> {
                    throw new WcNegocioException("El usuario " + wcUsuario.getWcNombreUsuario() + " ya esta registrado.");
                });
    }

    private void wcValidarQueNoSeDesactiveASiMismo(WcUsuario wcUsuario) {
        boolean wcEsElUsuarioActual = wcUsuario.getWcId() != null
                && wcUsuario.getWcId().equals(wcSesion.wcUsuarioActual().getWcId());
        if (wcEsElUsuarioActual && (!wcUsuario.isWcActivo() || wcUsuario.getWcRol() != WcRol.WC_ADMINISTRADOR)) {
            throw new WcNegocioException("No puedes desactivar ni quitar el rol de administrador a tu propio usuario.");
        }
    }

    private void wcValidarQueQuedeUnAdministrador(WcUsuario wcUsuario) {
        if (wcUsuario.getWcId() == null) {
            return;
        }
        boolean wcDejaDeSerAdministrador = wcUsuario.getWcRol() != WcRol.WC_ADMINISTRADOR || !wcUsuario.isWcActivo();
        WcUsuario wcAnterior = wcRepository.findById(wcUsuario.getWcId()).orElseThrow();
        if (wcDejaDeSerAdministrador && wcEsUltimoAdministradorActivo(wcAnterior)) {
            throw new WcNegocioException("Debe quedar al menos un administrador activo.");
        }
    }

    private boolean wcEsUltimoAdministradorActivo(WcUsuario wcUsuario) {
        return wcUsuario.getWcRol() == WcRol.WC_ADMINISTRADOR && wcUsuario.isWcActivo()
                && wcRepository.countByWcRolAndWcActivoTrue(WcRol.WC_ADMINISTRADOR) <= 1;
    }

    private void wcAsignarClave(WcUsuario wcUsuario, String wcClaveNueva) {
        boolean wcSinClaveNueva = wcClaveNueva == null || wcClaveNueva.isBlank();
        if (wcSinClaveNueva && wcUsuario.getWcId() == null) {
            throw new WcNegocioException("La contraseña es obligatoria para un usuario nuevo.");
        }
        if (wcSinClaveNueva) {
            wcUsuario.setWcClaveCifrada(wcRepository.findById(wcUsuario.getWcId()).orElseThrow().getWcClaveCifrada());
            return;
        }
        if (wcClaveNueva.length() < WC_LONGITUD_MINIMA_CLAVE) {
            throw new WcNegocioException("La contraseña debe tener al menos " + WC_LONGITUD_MINIMA_CLAVE + " caracteres.");
        }
        wcUsuario.setWcClaveCifrada(wcCifrador.wcCifrar(wcClaveNueva));
    }
}
