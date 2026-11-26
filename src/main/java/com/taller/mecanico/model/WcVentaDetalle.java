package com.taller.mecanico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "venta_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"wcVenta", "wcRepuesto"})
@EqualsAndHashCode(of = "wcId")
public class WcVentaDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private WcVenta wcVenta;

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
    @Min(value = 1, message = "La cantidad minima es 1")
    @Max(value = 9999, message = "La cantidad maxima es 9999")
    @Column(name = "cantidad", nullable = false)
    private Integer wcCantidad;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a cero")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal wcPrecioUnitario;

    @NotNull
    @Column(name = "importe", nullable = false, precision = 12, scale = 2)
    private BigDecimal wcImporte;
}
