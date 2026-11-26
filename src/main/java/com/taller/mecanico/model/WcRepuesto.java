package com.taller.mecanico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "repuesto",
        uniqueConstraints = @UniqueConstraint(name = "uk_repuesto_codigo", columnNames = "codigo_repuesto"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "wcId")
public class WcRepuesto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    @Column(name = "nombre", nullable = false, length = 255)
    private String wcNombre;

    @NotBlank(message = "El codigo de repuesto es obligatorio")
    @Size(max = 255)
    @Column(name = "codigo_repuesto", nullable = false, length = 255)
    private String wcCodigoRepuesto;

    @Size(max = 255)
    @Column(name = "descripcion", length = 255)
    private String wcDescripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio debe tener como maximo 8 enteros y 2 decimales")
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal wcPrecio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private WcCategoria wcCategoria;

    @Builder.Default
    @Column(name = "stock", nullable = false, columnDefinition = "integer default 0")
    private Integer wcStock = 0;

    @Builder.Default
    @Min(value = 0, message = "El stock minimo no puede ser negativo")
    @Max(value = 99999, message = "El stock minimo maximo es 99999")
    @Column(name = "stock_minimo", nullable = false, columnDefinition = "integer default 5")
    private Integer wcStockMinimo = 5;

    @Column(name = "costo_promedio", precision = 12, scale = 4)
    private BigDecimal wcCostoPromedio;

    public boolean isWcSinStock() {
        return wcStock == null || wcStock <= 0;
    }

    public boolean isWcStockBajo() {
        return !isWcSinStock() && wcStock <= wcStockMinimo;
    }
}
