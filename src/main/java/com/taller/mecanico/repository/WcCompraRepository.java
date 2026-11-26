package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcCompra;
import com.taller.mecanico.model.WcEstadoCompra;
import com.taller.mecanico.model.WcTipoDocumentoCompra;
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
public interface WcCompraRepository extends JpaRepository<WcCompra, Long> {
    @Query("SELECT MAX(wcC.wcNumero) FROM WcCompra wcC")
    Integer wcUltimoNumero();

    boolean existsByWcProveedorWcId(Long wcProveedorId);

    List<WcCompra> findByWcProveedorWcIdOrderByWcFechaDesc(Long wcProveedorId);

    boolean existsByWcProveedorWcIdAndWcTipoDocumentoAndWcNumeroDocumentoAndWcEstado(
            Long wcProveedorId, WcTipoDocumentoCompra wcTipo, String wcNumeroDocumento,
            WcEstadoCompra wcEstado);

    @Query(value = """
            SELECT wcC FROM WcCompra wcC
            JOIN FETCH wcC.wcProveedor wcP
            WHERE wcC.wcFecha BETWEEN :wcDesde AND :wcHasta
              AND (LOWER(wcC.wcCodigo)           LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcC.wcNumeroDocumento)  LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcP.wcRazonSocial)      LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR wcP.wcRuc                     LIKE CONCAT('%', :wcTexto, '%'))
            """,
            countQuery = """
            SELECT COUNT(wcC) FROM WcCompra wcC
            JOIN wcC.wcProveedor wcP
            WHERE wcC.wcFecha BETWEEN :wcDesde AND :wcHasta
              AND (LOWER(wcC.wcCodigo)           LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcC.wcNumeroDocumento)  LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcP.wcRazonSocial)      LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR wcP.wcRuc                     LIKE CONCAT('%', :wcTexto, '%'))
            """)
    Page<WcCompra> wcBuscar(@Param("wcTexto") String wcTexto,
                        @Param("wcDesde") LocalDateTime wcDesde,
                        @Param("wcHasta") LocalDateTime wcHasta,
                        Pageable wcPageable);

    @Query("""
            SELECT COUNT(wcC), COALESCE(SUM(wcC.wcBaseImponible), 0), COALESCE(SUM(wcC.wcIgv), 0), COALESCE(SUM(wcC.wcTotal), 0)
            FROM WcCompra wcC
            JOIN wcC.wcProveedor wcP
            WHERE wcC.wcEstado = :wcEstado
              AND wcC.wcFecha BETWEEN :wcDesde AND :wcHasta
              AND (LOWER(wcC.wcCodigo)           LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcC.wcNumeroDocumento)  LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcP.wcRazonSocial)      LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR wcP.wcRuc                     LIKE CONCAT('%', :wcTexto, '%'))
            """)
    List<Object[]> wcTotalizar(@Param("wcTexto") String wcTexto,
                             @Param("wcEstado") WcEstadoCompra wcEstado,
                             @Param("wcDesde") LocalDateTime wcDesde,
                             @Param("wcHasta") LocalDateTime wcHasta);

    @Query("""
            SELECT DISTINCT wcC FROM WcCompra wcC
            JOIN FETCH wcC.wcProveedor
            LEFT JOIN FETCH wcC.wcDetalles
            WHERE wcC.wcId = :wcId
            """)
    Optional<WcCompra> wcBuscarConDetalle(@Param("wcId") Long wcId);
}
