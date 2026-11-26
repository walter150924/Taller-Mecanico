package com.taller.mecanico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "vehiculo",
        uniqueConstraints = @UniqueConstraint(name = "uk_vehiculo_placa", columnNames = "placa"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "wcCliente")
@EqualsAndHashCode(of = "wcId")
public class WcVehiculo {
    public static final int WC_ANIO_MINIMO = 1950;
    public static final int WC_ANIO_MAXIMO = 2100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @NotBlank(message = "La placa es obligatoria")
    @Pattern(regexp = "[A-Z0-9]{3}-[0-9]{3}", message = "La placa debe tener el formato ABC-123")
    @Column(name = "placa", nullable = false, length = 7)
    private String wcPlaca;

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 40)
    @Column(name = "marca", nullable = false, length = 40)
    private String wcMarca;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 40)
    @Column(name = "modelo", nullable = false, length = 40)
    private String wcModelo;

    @NotNull(message = "El año es obligatorio")
    @Min(value = WC_ANIO_MINIMO, message = "El año no puede ser menor a 1950")
    @Max(value = WC_ANIO_MAXIMO, message = "El año no es valido")
    @Column(name = "anio", nullable = false)
    private Integer wcAnio;

    @Size(max = 30)
    @Column(name = "color", length = 30)
    private String wcColor;

    @NotNull(message = "El kilometraje es obligatorio")
    @Min(value = 0, message = "El kilometraje no puede ser negativo")
    @Column(name = "kilometraje", nullable = false)
    private Integer wcKilometraje;

    @NotNull(message = "Selecciona el propietario")
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private WcCliente wcCliente;

    public String getWcDescripcion() {
        return wcMarca + " " + wcModelo + " " + wcAnio;
    }

    public void wcActualizarKilometraje(int wcKilometrajeLeido) {
        if (wcKilometrajeLeido > wcKilometraje) {
            wcKilometraje = wcKilometrajeLeido;
        }
    }
}
