package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcVentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WcVentaDetalleRepository extends JpaRepository<WcVentaDetalle, Long> {
    boolean existsByWcRepuestoWcId(Long wcRepuestoId);

    @Query("""
            SELECT wcD FROM WcVentaDetalle wcD
            JOIN FETCH wcD.wcVenta wcV
            JOIN FETCH wcV.wcCliente
            WHERE wcV.wcFecha BETWEEN :wcDesde AND :wcHasta
            ORDER BY wcV.wcFecha, wcV.wcId, wcD.wcItem
            """)
    List<WcVentaDetalle> wcListarPeriodo(@Param("wcDesde") LocalDateTime wcDesde, @Param("wcHasta") LocalDateTime wcHasta);
}
