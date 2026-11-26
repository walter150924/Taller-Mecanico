package com.taller.mecanico.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "orden_linea_servicio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "wcOrden")
@EqualsAndHashCode(of = "wcId")
public class WcLineaServicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private WcOrdenServicio wcOrden;

    @ManyToOne(optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private WcServicio wcServicio;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal wcPrecio;
}
