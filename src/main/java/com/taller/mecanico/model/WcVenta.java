package com.taller.mecanico.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venta",
        uniqueConstraints = @UniqueConstraint(name = "uk_venta_comprobante", columnNames = "comprobante"),
        indexes = @Index(name = "ix_venta_fecha", columnList = "fecha"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"wcCliente", "wcDetalles"})
@EqualsAndHashCode(of = "wcId")
public class WcVenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    private WcTipoComprobante wcTipoComprobante;

    @NotBlank
    @Column(name = "serie", nullable = false, length = 4)
    private String wcSerie;

    @NotNull
    @Column(name = "numero", nullable = false)
    private Integer wcNumero;

    @NotBlank
    @Column(name = "comprobante", nullable = false, length = 20)
    private String wcComprobante;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private LocalDateTime wcFecha;

    @NotNull(message = "Selecciona un cliente")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private WcCliente wcCliente;

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
    @Column(name = "estado", nullable = false, length = 10)
    private WcEstadoVenta wcEstado;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private WcMetodoPago wcMetodoPago;

    @Size(max = 255)
    @Column(name = "observacion", length = 255)
    private String wcObservacion;

    @Valid
    @NotEmpty(message = "Agrega al menos un repuesto al carrito")
    @Builder.Default
    @OneToMany(mappedBy = "wcVenta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("wcItem ASC")
    private List<WcVentaDetalle> wcDetalles = new ArrayList<>();

    public void wcAgregarDetalle(WcVentaDetalle wcDetalle) {
        wcDetalle.setWcVenta(this);
        wcDetalle.setWcItem(wcDetalles.size() + 1);
        wcDetalles.add(wcDetalle);
    }

    public boolean isWcAnulada() {
        return wcEstado == WcEstadoVenta.WC_ANULADA;
    }

    public static String wcFormatearComprobante(String wcSerie, int wcNumero) {
        return wcSerie + "-" + String.format("%08d", wcNumero);
    }
}
