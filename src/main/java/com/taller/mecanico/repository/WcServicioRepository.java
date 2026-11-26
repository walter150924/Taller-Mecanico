package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WcServicioRepository extends JpaRepository<WcServicio, Long> {
    Optional<WcServicio> findByWcNombreIgnoreCase(String wcNombre);

    List<WcServicio> findByWcActivoTrueOrderByWcNombre();

    @Query("""
            SELECT wcS FROM WcServicio wcS
            WHERE LOWER(wcS.wcNombre) LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(COALESCE(wcS.wcDescripcion, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<WcServicio> wcBuscar(@Param("wcTexto") String wcTexto, Pageable wcPageable);
}
