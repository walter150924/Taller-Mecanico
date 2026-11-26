package com.taller.mecanico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "cliente",
        uniqueConstraints = @UniqueConstraint(name = "uk_cliente_dni", columnNames = "dni"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "wcId")
public class WcCliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
    @Column(name = "nombre", nullable = false, length = 60)
    private String wcNombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 60)
    @Column(name = "apellido", nullable = false, length = 60)
    private String wcApellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 digitos")
    @Column(name = "dni", nullable = false, length = 8)
    private String wcDni;

    @Email(message = "El correo no tiene un formato valido")
    @Size(max = 120)
    @Column(name = "email", length = 120)
    private String wcEmail;

    @Pattern(regexp = "|\\d{9}", message = "El telefono debe tener 9 digitos")
    @Column(name = "telefono", length = 9)
    private String wcTelefono;

    public String getWcNombreCompleto() {
        return wcNombre + " " + wcApellido;
    }
}
