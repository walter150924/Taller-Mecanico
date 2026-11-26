package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcEstadoVenta;
import com.taller.mecanico.model.WcVenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WcVentaRepository extends JpaRepository<WcVenta, Long> {
    @Query("SELECT MAX(wcV.wcNumero) FROM WcVenta wcV WHERE wcV.wcSerie = :wcSerie")
    Integer wcUltimoNumero(@Param("wcSerie") String wcSerie);

    boolean existsByWcClienteWcId(Long wcClienteId);

    @Query("SELECT MIN(wcV.wcFecha) FROM WcVenta wcV")
    LocalDateTime wcPrimeraFecha();

    @Query(value = """
            SELECT wcV FROM WcVenta wcV
            JOIN FETCH wcV.wcCliente wcC
            WHERE wcV.wcFecha BETWEEN :wcDesde AND :wcHasta
              AND (LOWER(wcV.wcComprobante)                          LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(CONCAT(wcC.wcNombre, ' ', wcC.wcApellido))      LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR wcC.wcDni                                         LIKE CONCAT('%', :wcTexto, '%'))
            """,
            countQuery = """
            SELECT COUNT(wcV) FROM WcVenta wcV
            JOIN wcV.wcCliente wcC
            WHERE wcV.wcFecha BETWEEN :wcDesde AND :wcHasta
              AND (LOWER(wcV.wcComprobante)                          LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(CONCAT(wcC.wcNombre, ' ', wcC.wcApellido))      LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR wcC.wcDni                                         LIKE CONCAT('%', :wcTexto, '%'))
            """)
    Page<WcVenta> wcBuscar(@Param("wcTexto") String wcTexto,
                       @Param("wcDesde") LocalDateTime wcDesde,
                       @Param("wcHasta") LocalDateTime wcHasta,
                       Pageable wcPageable);

    @Query("""
            SELECT COUNT(wcV), COALESCE(SUM(wcV.wcBaseImponible), 0), COALESCE(SUM(wcV.wcIgv), 0), COALESCE(SUM(wcV.wcTotal), 0)
            FROM WcVenta wcV
            JOIN wcV.wcCliente wcC
            WHERE wcV.wcEstado = :wcEstado
              AND wcV.wcFecha BETWEEN :wcDesde AND :wcHasta
              AND (LOWER(wcV.wcComprobante)                          LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(CONCAT(wcC.wcNombre, ' ', wcC.wcApellido))      LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR wcC.wcDni                                         LIKE CONCAT('%', :wcTexto, '%'))
            """)
    List<Object[]> wcTotalizar(@Param("wcTexto") String wcTexto,
                             @Param("wcEstado") WcEstadoVenta wcEstado,
                             @Param("wcDesde") LocalDateTime wcDesde,
                             @Param("wcHasta") LocalDateTime wcHasta);

    @Query("""
            SELECT DISTINCT wcV FROM WcVenta wcV
            JOIN FETCH wcV.wcCliente
            LEFT JOIN FETCH wcV.wcDetalles
            WHERE wcV.wcId = :wcId
            """)
    Optional<WcVenta> wcBuscarConDetalle(@Param("wcId") Long wcId);

    @Query("""
            SELECT wcV FROM WcVenta wcV
            JOIN FETCH wcV.wcCliente
            WHERE wcV.wcFecha BETWEEN :wcDesde AND :wcHasta
            ORDER BY wcV.wcFecha
            """)
    List<WcVenta> wcListarPeriodo(@Param("wcDesde") LocalDateTime wcDesde, @Param("wcHasta") LocalDateTime wcHasta);

    @Query("""
            SELECT wcV FROM WcVenta wcV
            JOIN FETCH wcV.wcCliente
            ORDER BY wcV.wcFecha DESC, wcV.wcId DESC
            LIMIT 8
            """)
    List<WcVenta> wcUltimas();

    @Query("""
            SELECT wcD.wcNombreRepuesto, SUM(wcD.wcCantidad), SUM(wcD.wcImporte)
            FROM WcVentaDetalle wcD
            JOIN wcD.wcVenta wcV
            WHERE wcV.wcEstado = :wcEstado AND wcV.wcFecha BETWEEN :wcDesde AND :wcHasta
            GROUP BY wcD.wcRepuesto.wcId, wcD.wcNombreRepuesto
            ORDER BY SUM(wcD.wcImporte) DESC
            """)
    List<Object[]> wcResumenPorRepuesto(@Param("wcEstado") WcEstadoVenta wcEstado,
                                      @Param("wcDesde") LocalDateTime wcDesde,
                                      @Param("wcHasta") LocalDateTime wcHasta);
}
