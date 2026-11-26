# Taller Mecanico - Sistema de gestion

Aplicacion de escritorio (Java 17, Spring Boot, JavaFX, SQLite) para administrar un taller mecanico.

## Ejecutar

```bash
./mvnw javafx:run
```

La base de datos `taller.db` se crea sola en la carpeta del proyecto con datos de prueba.

## Usuarios de prueba

| Rol           | Usuario     | Contraseña |
|---------------|-------------|------------|
| Administrador | `admin`     | `admin123` |
| Recepcionista | `recepcion` | `taller123` |
| Mecanico      | `jquispe`   | `taller123` |
| Mecanico      | `pmamani`   | `taller123` |

## Que puede hacer cada rol

| Modulo                         | Administrador | Recepcionista | Mecanico |
|--------------------------------|:---:|:---:|:---:|
| Dashboard                      | ✅ | ❌ | ❌ |
| Ordenes de servicio            | Todas | Todas | Solo las suyas |
| Crear ordenes y asignar mecanico | ✅ | ✅ | ❌ |
| Diagnostico, servicios y repuestos | ✅ | ✅ | ✅ |
| Entregar o cancelar ordenes    | ✅ | ✅ | ❌ |
| Clientes y vehiculos           | ✅ | ✅ | ❌ |
| Venta de mostrador y ventas    | ✅ | ✅ | ❌ |
| Inventario                     | ✅ | Ver | ❌ |
| Compras y proveedores          | ✅ | ❌ | ❌ |
| Catalogo (servicios, repuestos, categorias) | ✅ | ❌ | ❌ |
| Usuarios                       | ✅ | ❌ | ❌ |

## Flujo de una orden de servicio

Recibido → En diagnostico → En reparacion → Listo para entrega → Entregado

Una orden abierta se puede cancelar; al cancelarla, los repuestos usados vuelven al inventario.

## Pruebas

```bash
./mvnw test
```
