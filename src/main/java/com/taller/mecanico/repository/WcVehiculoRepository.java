package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcVehiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WcVehiculoRepository extends JpaRepository<WcVehiculo, Long> {
    Optional<WcVehiculo> findByWcPlaca(String wcPlaca);

    boolean existsByWcClienteWcId(Long wcClienteId);

    @Query(value = """
            SELECT wcV FROM WcVehiculo wcV JOIN FETCH wcV.wcCliente wcC
            WHERE LOWER(wcV.wcPlaca)    LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcV.wcMarca)    LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcV.wcModelo)   LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcC.wcNombre)   LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcC.wcApellido) LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR wcC.wcDni             LIKE CONCAT('%', :wcTexto, '%')
            """,
            countQuery = """
            SELECT COUNT(wcV) FROM WcVehiculo wcV JOIN wcV.wcCliente wcC
            WHERE LOWER(wcV.wcPlaca)    LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcV.wcMarca)    LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcV.wcModelo)   LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcC.wcNombre)   LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR LOWER(wcC.wcApellido) LIKE LOWER(CONCAT('%', :wcTexto, '%'))
               OR wcC.wcDni             LIKE CONCAT('%', :wcTexto, '%')
            """)
    Page<WcVehiculo> wcBuscar(@Param("wcTexto") String wcTexto, Pageable wcPageable);
}
