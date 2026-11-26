package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcRepuesto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WcRepuestoRepository extends JpaRepository<WcRepuesto, Long>, JpaSpecificationExecutor<WcRepuesto> {
    Optional<WcRepuesto> findByWcCodigoRepuesto(String wcCodigoRepuesto);

    boolean existsByWcCodigoRepuesto(String wcCodigoRepuesto);

    @Query("""
            SELECT wcP FROM WcRepuesto wcP
            LEFT JOIN FETCH wcP.wcCategoria
            """)
    Page<WcRepuesto> wcFindAllConCategoria(Pageable wcPageable);

    @Query("""
            SELECT wcP FROM WcRepuesto wcP
            LEFT JOIN FETCH wcP.wcCategoria
            WHERE LOWER(wcP.wcNombre)          LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcP.wcCodigoRepuesto)  LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcP.wcDescripcion)     LIKE LOWER(CONCAT('%', :wcTexto, '%'))
            """)
    Page<WcRepuesto> wcBuscar(@Param("wcTexto") String wcTexto, Pageable wcPageable);

    @Query(value = """
            SELECT wcP FROM WcRepuesto wcP
            LEFT JOIN FETCH wcP.wcCategoria
            WHERE (LOWER(wcP.wcNombre)         LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcP.wcCodigoRepuesto) LIKE LOWER(CONCAT('%', :wcTexto, '%')))
              AND (:wcEstado = 'WC_TODOS'
                OR (:wcEstado = 'WC_SIN_STOCK'  AND wcP.wcStock <= 0)
                OR (:wcEstado = 'WC_BAJO'       AND wcP.wcStock > 0 AND wcP.wcStock <= wcP.wcStockMinimo)
                OR (:wcEstado = 'WC_DISPONIBLE' AND wcP.wcStock > wcP.wcStockMinimo))
            """,
            countQuery = """
            SELECT COUNT(wcP) FROM WcRepuesto wcP
            WHERE (LOWER(wcP.wcNombre)         LIKE LOWER(CONCAT('%', :wcTexto, '%'))
                OR LOWER(wcP.wcCodigoRepuesto) LIKE LOWER(CONCAT('%', :wcTexto, '%')))
              AND (:wcEstado = 'WC_TODOS'
                OR (:wcEstado = 'WC_SIN_STOCK'  AND wcP.wcStock <= 0)
                OR (:wcEstado = 'WC_BAJO'       AND wcP.wcStock > 0 AND wcP.wcStock <= wcP.wcStockMinimo)
                OR (:wcEstado = 'WC_DISPONIBLE' AND wcP.wcStock > wcP.wcStockMinimo))
            """)
    Page<WcRepuesto> wcInventario(@Param("wcTexto") String wcTexto, @Param("wcEstado") String wcEstado, Pageable wcPageable);

    @Query("SELECT wcP FROM WcRepuesto wcP LEFT JOIN FETCH wcP.wcCategoria ORDER BY wcP.wcCodigoRepuesto")
    List<WcRepuesto> wcListarConCategoria();

    @Query("""
            SELECT COALESCE(wcC.wcNombre, 'Sin categoria'), COUNT(wcP), AVG(wcP.wcPrecio)
            FROM WcRepuesto wcP
            LEFT JOIN wcP.wcCategoria wcC
            GROUP BY wcC.wcId, wcC.wcNombre
            ORDER BY COUNT(wcP) DESC
            """)
    List<Object[]> wcResumenPorCategoria();

    @Query("SELECT wcP.wcPrecio FROM WcRepuesto wcP")
    List<BigDecimal> wcListarPrecios();

    @Query("SELECT wcP FROM WcRepuesto wcP LEFT JOIN FETCH wcP.wcCategoria ORDER BY wcP.wcPrecio DESC LIMIT 5")
    List<WcRepuesto> wcTop5MasCaros();
}
