package com.taller.mecanico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra",
        uniqueConstraints = @UniqueConstraint(name = "uk_compra_codigo", columnNames = "codigo"),
        indexes = @Index(name = "ix_compra_fecha", columnList = "fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"wcProveedor", "wcDetalles"})
@EqualsAndHashCode(of = "wcId")
public class WcCompra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @NotBlank
    @Column(name = "codigo", nullable = false, length = 20)
    private String wcCodigo;

    @NotNull
    @Column(name = "numero", nullable = false)
    private Integer wcNumero;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private LocalDateTime wcFecha;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private WcProveedor wcProveedor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private WcTipoDocumentoCompra wcTipoDocumento;

    @NotBlank
    @Size(max = 20)
    @Column(name = "numero_documento", nullable = false, length = 20)
    private String wcNumeroDocumento;

    @NotNull
    @Column(name = "fecha_documento", nullable = false)
    private LocalDate wcFechaDocumento;

    @Column(name = "aplica_igv", nullable = false)
    private boolean wcAplicaIgv;

    @Column(name = "precios_incluyen_igv", nullable = false)
    private boolean wcPreciosIncluyenIgv;

    @NotNull
    @Column(name = "tasa_igv", nullable = false, precision = 5, scale = 4)
    private BigDecimal wcTasaIgv;

    @NotNull
    @Column(name = "base_imponible", nullable = false, precision = 12, scale = 2)
    private BigDecimal wcBaseImponible;

    @NotNull
    @Column(name = "igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal wcIgv;

    @NotNull
    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal wcTotal;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private WcEstadoCompra wcEstado;

    @Size(max = 255)
    @Column(name = "observacion", length = 255)
    private String wcObservacion;

    @Builder.Default
    @OneToMany(mappedBy = "wcCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("wcItem ASC")
    private List<WcCompraDetalle> wcDetalles = new ArrayList<>();

    public void wcAgregarDetalle(WcCompraDetalle wcDetalle) {
        wcDetalle.setWcCompra(this);
        wcDetalle.setWcItem(wcDetalles.size() + 1);
        wcDetalles.add(wcDetalle);
    }

    public boolean isWcAnulada() {
        return wcEstado == WcEstadoCompra.WC_ANULADA;
    }

    public static String wcFormatearCodigo(int wcNumero) {
        return "CP-" + String.format("%08d", wcNumero);
    }
}
