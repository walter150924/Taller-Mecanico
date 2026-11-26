package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcCliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WcClienteRepository extends JpaRepository<WcCliente, Long>, JpaSpecificationExecutor<WcCliente> {
    Optional<WcCliente> findByWcDni(String wcDni);

    boolean existsByWcDni(String wcDni);

    @Query("""
            SELECT wcP FROM WcCliente wcP
            WHERE LOWER(wcP.wcNombre)   LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcP.wcApellido) LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR wcP.wcDni             LIKE CONCAT('%', :wcTexto, '%')
            """)
    Page<WcCliente> wcBuscar(@Param("wcTexto") String wcTexto, Pageable wcPageable);

    @Query("SELECT COUNT(wcP) FROM WcCliente wcP WHERE wcP.wcEmail IS NOT NULL AND wcP.wcEmail <> ''")
    long wcContarConEmail();

    @Query("SELECT COUNT(wcP) FROM WcCliente wcP WHERE wcP.wcTelefono IS NOT NULL AND wcP.wcTelefono <> ''")
    long wcContarConTelefono();

    @Query("""
            SELECT COUNT(wcP) FROM WcCliente wcP
            WHERE wcP.wcEmail IS NOT NULL AND wcP.wcEmail <> ''
              AND wcP.wcTelefono IS NOT NULL AND wcP.wcTelefono <> ''
            """)
    long wcContarContactoCompleto();
}
