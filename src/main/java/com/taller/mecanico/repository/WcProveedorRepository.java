package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcProveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WcProveedorRepository extends JpaRepository<WcProveedor, Long>, JpaSpecificationExecutor<WcProveedor> {
    Optional<WcProveedor> findByWcRuc(String wcRuc);

    @Query("""
            SELECT wcP FROM WcProveedor wcP
            WHERE wcP.wcRuc LIKE CONCAT('%', :wcTexto, '%')
               OR LOWER(wcP.wcRazonSocial) LIKE LOWER(CONCAT('%', :wcTexto, '%'))
            """)
    Page<WcProveedor> wcBuscar(@Param("wcTexto") String wcTexto, Pageable wcPageable);
}
