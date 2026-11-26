package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcMovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WcMovimientoInventarioRepository extends JpaRepository<WcMovimientoInventario, Long> {
    @Query("""
            SELECT wcM FROM WcMovimientoInventario wcM
            WHERE wcM.wcRepuesto.wcId = :wcRepuestoId
            ORDER BY wcM.wcFecha, wcM.wcId
            """)
    List<WcMovimientoInventario> wcKardex(@Param("wcRepuestoId") Long wcRepuestoId);

    boolean existsByWcVentaId(Long wcVentaId);

    boolean existsByWcRepuestoWcId(Long wcRepuestoId);
}
