package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcCategoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WcCategoriaRepository extends JpaRepository<WcCategoria, Long> {
    Optional<WcCategoria> findByWcCodigoCategoria(String wcCodigoCategoria);

    boolean existsByWcCodigoCategoria(String wcCodigoCategoria);

    @Query("""
            SELECT wcC FROM WcCategoria wcC
            WHERE LOWER(wcC.wcNombre)           LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcC.wcCodigoCategoria)  LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcC.wcDescripcion)      LIKE LOWER(CONCAT('%', :wcTexto, '%'))
            """)
    Page<WcCategoria> wcBuscar(@Param("wcTexto") String wcTexto, Pageable wcPageable);
}
