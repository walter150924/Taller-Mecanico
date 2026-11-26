package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcRol;
import com.taller.mecanico.model.WcUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WcUsuarioRepository extends JpaRepository<WcUsuario, Long> {
    Optional<WcUsuario> findByWcNombreUsuario(String wcNombreUsuario);

    List<WcUsuario> findByWcRolAndWcActivoTrueOrderByWcNombreCompleto(WcRol wcRol);

    long countByWcRolAndWcActivoTrue(WcRol wcRol);

    @Query("""
            SELECT wcU FROM WcUsuario wcU
            WHERE LOWER(wcU.wcNombreUsuario)  LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcU.wcNombreCompleto) LIKE LOWER(CONCAT('%', :wcTexto, '%'))
            """)
    Page<WcUsuario> wcBuscar(@Param("wcTexto") String wcTexto, Pageable wcPageable);
}
