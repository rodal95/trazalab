# TRAZALAB — Proyecto integrador (Actividad Práctica 4)

Versión completa del sistema: incorpora la capa de persistencia sobre MySQL aplicando
los patrones **MVC**, **DAO** y **singleton**.

## Requisitos
- JDK 17 o superior (desarrollado y probado con JDK 21).
- MySQL 8.0 en `localhost:3306`.
- Conector JDBC incluido en `lib/mysql-connector-j-8.4.0.jar`.

## Preparar la base de datos
```bash
mysql -u root -p < sql/01-esquema.sql
mysql -u root -p < sql/02-datos.sql
```

## Configurar la conexión
Si el usuario, la clave o el puerto difieren, editar las constantes de
`src/trazalab/persistencia/ConexionBD.java`:

```java
private static final String URL_BASE = "jdbc:mysql://localhost:3306/trazalab?...";
private static final String USUARIO  = "root";
private static final String CLAVE    = "";
```

## Compilar
```bash
javac -encoding UTF-8 -cp "lib/mysql-connector-j-8.4.0.jar" -d build $(find src -name "*.java")
```

## Ejecutar
```bash
# Linux / macOS
java -cp "build:lib/mysql-connector-j-8.4.0.jar" trazalab.app.MenuPrincipal

# Windows
java -cp "build;lib\mysql-connector-j-8.4.0.jar" trazalab.app.MenuPrincipal
```

Las opciones 1 a 9 operan en memoria (igual que en la AP3). La opción **10** abre el
módulo persistente, que trabaja contra MySQL.

La versión entregada con la AP3 —el prototipo en memoria, sin la capa de persistencia—
queda identificada en el repositorio con la etiqueta `ap3`.

## Estructura
```
src/trazalab/
  dominio/        13 archivos — entidades, enum, interfaz Trazable
  excepciones/     5 archivos — 4 del dominio + PersistenciaException
  servicio/        2 archivos — Laboratorio y AgendaTurnos
  util/            4 archivos — Ordenamiento, Busqueda, Consola, ExportadorArchivos
  persistencia/    5 archivos — ConexionBD, GenericoDAO, PacienteDAO, EstudioDAO, MuestraDAO
  mvc/             3 archivos — Modelo, Vista y Controlador de trazabilidad
  app/             2 archivos — MenuPrincipal y MenuBaseDatos
lib/    conector JDBC
sql/    scripts de la base de datos (los mismos de la AP2)
salidas/  certificados de trazabilidad exportados por el sistema
```

## Recorrido sugerido del módulo persistente (opción 10)
1. **1** Listar muestras en circuito — consulta con `JOIN` sobre tres tablas.
2. **2** Registrar evento sobre `M-SAN-0003`, estado 3 → transacción sobre dos tablas.
   Repetir sobre `M-SUE-0001` con estado 2 → la transición inválida se rechaza.
3. **3** Consultar catálogo — `ArrayList` convertido a arreglo y ordenado con quicksort.
4. **4** Actualizar precios (por ejemplo 15 %) → `UPDATE` masivo.
5. **5** Indicadores calculados con `GROUP BY` en el motor.
6. **7** Exportar el certificado de trazabilidad a `salidas/`.

Si el servidor MySQL está detenido, el módulo informa el error y el resto del sistema
sigue funcionando en memoria.
