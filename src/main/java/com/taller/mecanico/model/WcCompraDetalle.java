package com.taller.mecanico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "compra_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"wcCompra", "wcRepuesto"})
@EqualsAndHashCode(of = "wcId")
public class WcCompraDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private WcCompra wcCompra;

    @Column(name = "item", nullable = false)
    private Integer wcItem;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repuesto_id", nullable = false)
    private WcRepuesto wcRepuesto;

    @NotBlank
    @Column(name = "codigo_repuesto", nullable = false, length = 255)
    private String wcCodigoRepuesto;

    @NotBlank
    @Column(name = "nombre_repuesto", nullable = false, length = 255)
    private String wcNombreRepuesto;

    @NotNull
    @Min(1)
    @Column(name = "cantidad", nullable = false)
    private Integer wcCantidad;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 4)
    private BigDecimal wcCostoUnitario;

    @NotNull
    @Column(name = "importe", nullable = false, precision = 12, scale = 2)
    private BigDecimal wcImporte;
}
