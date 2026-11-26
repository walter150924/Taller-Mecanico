package com.taller.mecanico.model;

import com.taller.mecanico.service.WcCalculadoraIgv;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orden_linea_repuesto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "wcOrden")
@EqualsAndHashCode(of = "wcId")
public class WcLineaRepuesto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private WcOrdenServicio wcOrden;

    @ManyToOne(optional = false)
    @JoinColumn(name = "repuesto_id", nullable = false)
    private WcRepuesto wcRepuesto;

    @Column(name = "cantidad", nullable = false)
    private Integer wcCantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal wcPrecioUnitario;

    public BigDecimal getWcImporte() {
        return WcCalculadoraIgv.wcImporte(wcCantidad, wcPrecioUnitario);
    }
}
