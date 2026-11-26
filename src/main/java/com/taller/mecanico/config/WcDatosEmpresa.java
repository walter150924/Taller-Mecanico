package com.taller.mecanico.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.empresa")
@Getter
@Setter
public class WcDatosEmpresa {
    private String wcRazonSocial = "MI EMPRESA S.A.C.";
    private String wcNombreComercial = "Mi Empresa";
    private String wcRuc = "20000000001";
    private String wcDireccion = "Av. Principal 123, Lima";
    private String wcTelefono = "";
    private String wcEmail = "";
}
