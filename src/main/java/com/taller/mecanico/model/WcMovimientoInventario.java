package com.taller.mecanico.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_inventario",
        indexes = {
                @Index(name = "ix_movimiento_repuesto", columnList = "repuesto_id, fecha"),
                @Index(name = "ix_movimiento_venta", columnList = "venta_id"),
                @Index(name = "ix_movimiento_compra", columnList = "compra_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "wcRepuesto")
@EqualsAndHashCode(of = "wcId")
public class WcMovimientoInventario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime wcFecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repuesto_id", nullable = false)
    private WcRepuesto wcRepuesto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private WcTipoMovimiento wcTipo;

    @Column(name = "cantidad", nullable = false)
    private Integer wcCantidad;

    @Column(name = "stock_resultante", nullable = false)
    private Integer wcStockResultante;

    @Column(name = "costo_unitario", precision = 12, scale = 4)
    private BigDecimal wcCostoUnitario;

    @Column(name = "documento", nullable = false, length = 40)
    private String wcDocumento;

    @Column(name = "venta_id")
    private Long wcVentaId;

    @Column(name = "compra_id")
    private Long wcCompraId;

    public int getWcEntrada() {
        return wcTipo.isWcEntrada() ? wcCantidad : 0;
    }

    public int getWcSalida() {
        return wcTipo.isWcEntrada() ? 0 : wcCantidad;
    }
}
