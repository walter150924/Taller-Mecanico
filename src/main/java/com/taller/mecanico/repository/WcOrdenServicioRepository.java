package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcEstadoOrden;
import com.taller.mecanico.model.WcOrdenServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface WcOrdenServicioRepository extends JpaRepository<WcOrdenServicio, Long> {
    boolean existsByWcVehiculoWcId(Long wcVehiculoId);

    boolean existsByWcMecanicoWcId(Long wcMecanicoId);

    long countByWcEstadoIn(Collection<WcEstadoOrden> wcEstados);

    long countByWcEstado(WcEstadoOrden wcEstado);

    long countByWcFechaIngresoBetween(LocalDateTime wcDesde, LocalDateTime wcHasta);

    List<WcOrdenServicio> findByWcVehiculoWcIdOrderByWcFechaIngresoDesc(Long wcVehiculoId);

    @Query("SELECT COALESCE(MAX(o.wcId), 0) FROM WcOrdenServicio o")
    long wcUltimoNumero();

    @Query(value = """
            SELECT o FROM WcOrdenServicio o
            JOIN FETCH o.wcVehiculo wcV JOIN FETCH wcV.wcCliente wcC LEFT JOIN FETCH o.wcMecanico wcM
            WHERE (:wcEstado IS NULL OR o.wcEstado = :wcEstado)
              AND (:wcMecanicoId IS NULL OR wcM.wcId = :wcMecanicoId)
              AND (LOWER(o.wcCodigo)   LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcV.wcPlaca)    LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcC.wcNombre)   LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcC.wcApellido) LIKE LOWER(CONCAT('%', :wcTexto, '%')))
            """,
            countQuery = """
            SELECT COUNT(o) FROM WcOrdenServicio o
            JOIN o.wcVehiculo wcV JOIN wcV.wcCliente wcC LEFT JOIN o.wcMecanico wcM
            WHERE (:wcEstado IS NULL OR o.wcEstado = :wcEstado)
              AND (:wcMecanicoId IS NULL OR wcM.wcId = :wcMecanicoId)
              AND (LOWER(o.wcCodigo)   LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcV.wcPlaca)    LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcC.wcNombre)   LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcC.wcApellido) LIKE LOWER(CONCAT('%', :wcTexto, '%')))
            """)
    Page<WcOrdenServicio> wcBuscar(@Param("wcTexto") String wcTexto,
                               @Param("wcEstado") WcEstadoOrden wcEstado,
                               @Param("wcMecanicoId") Long wcMecanicoId,
                               Pageable wcPageable);

    @Query("""
            SELECT COALESCE(SUM(o.wcTotal), 0) FROM WcOrdenServicio o
            WHERE o.wcEstado = :wcEstado AND o.wcFechaEntrega BETWEEN :wcDesde AND :wcHasta
            """)
    Number wcSumarTotal(@Param("wcEstado") WcEstadoOrden wcEstado,
                          @Param("wcDesde") LocalDateTime wcDesde,
                          @Param("wcHasta") LocalDateTime wcHasta);

    @Query("SELECT CASE WHEN COUNT(wcL) > 0 THEN true ELSE false END FROM WcLineaServicio wcL WHERE wcL.wcServicio.wcId = :wcServicioId")
    boolean wcUsaServicio(@Param("wcServicioId") Long wcServicioId);

    @Query("SELECT CASE WHEN COUNT(wcL) > 0 THEN true ELSE false END FROM WcLineaRepuesto wcL WHERE wcL.wcRepuesto.wcId = :wcRepuestoId")
    boolean wcUsaRepuesto(@Param("wcRepuestoId") Long wcRepuestoId);
}
