package com.taller.mecanico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "orden_servicio",
        uniqueConstraints = @UniqueConstraint(name = "uk_orden_codigo", columnNames = "codigo"),
        indexes = @Index(name = "ix_orden_estado", columnList = "estado"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"wcVehiculo", "wcMecanico", "wcServicios", "wcRepuestos"})
@EqualsAndHashCode(of = "wcId")
public class WcOrdenServicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @Column(name = "codigo", nullable = false, length = 12)
    private String wcCodigo;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime wcFechaIngreso;

    @Column(name = "fecha_entrega")
    private LocalDateTime wcFechaEntrega;

    @NotNull(message = "Selecciona el vehiculo")
    @ManyToOne(optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private WcVehiculo wcVehiculo;

    @ManyToOne
    @JoinColumn(name = "mecanico_id")
    private WcUsuario wcMecanico;

    @NotNull(message = "Ingresa el kilometraje")
    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    @Column(name = "kilometraje_ingreso", nullable = false)
    private Integer wcKilometrajeIngreso;

    @NotBlank(message = "Describe el problema que reporta el cliente")
    @Size(max = 500, message = "Maximo 500 caracteres")
    @Column(name = "problema_reportado", nullable = false, length = 500)
    private String wcProblemaReportado;

    @Size(max = 1000, message = "Maximo 1000 caracteres")
    @Column(name = "diagnostico", length = 1000)
    private String wcDiagnostico;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private WcEstadoOrden wcEstado;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private WcMetodoPago wcMetodoPago;

    @Builder.Default
    @Column(name = "total_servicios", nullable = false, precision = 12, scale = 2)
    private BigDecimal wcTotalServicios = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_repuestos", nullable = false, precision = 12, scale = 2)
    private BigDecimal wcTotalRepuestos = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "base_imponible", nullable = false, precision = 12, scale = 2)
    private BigDecimal wcBaseImponible = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal wcIgv = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal wcTotal = BigDecimal.ZERO;

    @Builder.Default
    @OneToMany(mappedBy = "wcOrden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("wcId ASC")
    private List<WcLineaServicio> wcServicios = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "wcOrden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("wcId ASC")
    private List<WcLineaRepuesto> wcRepuestos = new ArrayList<>();

    public WcCliente getWcCliente() {
        return wcVehiculo.getWcCliente();
    }

    public String getWcNombreMecanico() {
        return wcMecanico == null ? "Sin asignar" : wcMecanico.getWcNombreCompleto();
    }

    public boolean wcEstaAsignadaA(WcUsuario wcUsuario) {
        return wcMecanico != null && wcMecanico.getWcId().equals(wcUsuario.getWcId());
    }

    public void wcAgregarServicio(WcServicio wcServicio) {
        wcServicios.add(WcLineaServicio.builder().wcOrden(this).wcServicio(wcServicio).wcPrecio(wcServicio.getWcPrecio()).build());
    }

    public Optional<WcLineaRepuesto> wcLineaDe(WcRepuesto wcRepuesto) {
        return wcRepuestos.stream().filter(wcL -> wcL.getWcRepuesto().getWcId().equals(wcRepuesto.getWcId())).findFirst();
    }

    public void wcAgregarRepuesto(WcRepuesto wcRepuesto, int wcCantidad) {
        wcLineaDe(wcRepuesto).ifPresentOrElse(
                wcLinea -> wcLinea.setWcCantidad(wcLinea.getWcCantidad() + wcCantidad),
                () -> wcRepuestos.add(WcLineaRepuesto.builder().wcOrden(this).wcRepuesto(wcRepuesto)
                        .wcCantidad(wcCantidad).wcPrecioUnitario(wcRepuesto.getWcPrecio()).build()));
    }

    public static String wcFormatearCodigo(long wcNumero) {
        return String.format("OS-%06d", wcNumero);
    }
}
