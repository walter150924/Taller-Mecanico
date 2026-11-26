package com.taller.mecanico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_nombre", columnNames = "nombre_usuario"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "wcClaveCifrada")
@EqualsAndHashCode(of = "wcId")
public class WcUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @NotBlank(message = "El usuario es obligatorio")
    @Pattern(regexp = "[a-z0-9._]{4,30}",
            message = "Usa de 4 a 30 caracteres: minusculas, numeros, punto o guion bajo")
    @Column(name = "nombre_usuario", nullable = false, length = 30)
    private String wcNombreUsuario;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String wcNombreCompleto;

    @Column(name = "clave_cifrada", nullable = false, length = 200)
    private String wcClaveCifrada;

    @NotNull(message = "Selecciona un rol")
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private WcRol wcRol;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean wcActivo = true;

    public boolean wcEsMecanico() {
        return wcRol == WcRol.WC_MECANICO;
    }

    public boolean wcTiene(WcPermiso wcPermiso) {
        return wcActivo && wcPermiso.wcPermiteA(wcRol);
    }
}
