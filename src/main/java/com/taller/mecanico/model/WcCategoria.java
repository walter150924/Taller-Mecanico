package com.taller.mecanico.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "categoria",
        uniqueConstraints = @UniqueConstraint(name = "uk_categoria_codigo", columnNames = "codigo_categoria"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "wcId")
public class WcCategoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long wcId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    @Column(name = "nombre", nullable = false, length = 255)
    private String wcNombre;

    @NotBlank(message = "El codigo de categoria es obligatorio")
    @Size(max = 255)
    @Column(name = "codigo_categoria", nullable = false, length = 255)
    private String wcCodigoCategoria;

    @Size(max = 255)
    @Column(name = "descripcion", length = 255)
    private String wcDescripcion;
}
