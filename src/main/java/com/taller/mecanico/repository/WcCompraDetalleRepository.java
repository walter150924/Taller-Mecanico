package com.taller.mecanico.repository;

import com.taller.mecanico.model.WcCompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WcCompraDetalleRepository extends JpaRepository<WcCompraDetalle, Long> {
    boolean existsByWcRepuestoWcId(Long wcRepuestoId);
}
