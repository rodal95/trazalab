# TRAZALAB — Proyecto integrador (Actividad Práctica 4)

Versión completa del sistema: incorpora la capa de persistencia sobre MySQL aplicando
los patrones **MVC**, **DAO** y **singleton**.

## Requisitos
- Desarrollo: JDK 17 o superior (se desarrolla con el JDK 21).
- Puestos: JRE 17 o superior.
- Servidor: MySQL 8.0.
- Conector JDBC incluido en `lib/mysql-connector-j-8.4.0.jar`.

## Desarrollo en un solo equipo
```bash
mysql -u root -p < sql/01-esquema.sql
mysql -u root -p < sql/02-datos.sql
javac -encoding UTF-8 -cp "lib/mysql-connector-j-8.4.0.jar" -d build $(find src -name "*.java")

# Linux / macOS
java -cp "build:lib/mysql-connector-j-8.4.0.jar" trazalab.app.MenuPrincipal
# Windows
java -cp "build;lib\mysql-connector-j-8.4.0.jar" trazalab.app.MenuPrincipal
```
Sin archivo de configuración, la aplicación se conecta a MySQL en `localhost:3306`
como `root` sin clave. Esa configuración es solo para desarrollo.

## Instalación en el laboratorio

**Servidor (192.168.10.10, Linux).**
1. Crear la base y los usuarios de MySQL:
   ```bash
   mysql -u root -p < sql/01-esquema.sql
   mysql -u root -p < sql/02-datos.sql      # solo para la base de prueba
   mysql -u root -p < sql/05-usuarios.sql
   ```
   `05-usuarios.sql` crea dos usuarios:
   - `trazalab_app`, que solo se conecta desde la red 192.168.10.0/24 y con TLS.
   - `trazalab_respaldo`, de solo lectura.

   MySQL genera las claves y las muestra una sola vez.
2. Programar el respaldo diario:
   ```bash
   install -D -m 700 servidor/respaldo.sh /opt/trazalab/respaldo.sh
   install -D -m 600 servidor/respaldo.cnf.ejemplo /etc/trazalab/respaldo.cnf   # completar la clave
   echo "0 22 * * * root /opt/trazalab/respaldo.sh" > /etc/cron.d/trazalab
   ```
   Cada copia queda en `/var/backups/trazalab/` y se conservan 30 días. Para
   restaurar: `gunzip -c trazalab-AAAAMMDD-HHMM.sql.gz | mysql -u root -p`.

**Construcción (en el equipo de desarrollo).**
```bash
sh construir.sh
```
Genera `dist/trazalab/`, la carpeta que se copia a cada puesto:
- `trazalab.jar`, compilado para Java 17.
- El conector JDBC.
- El ejemplo de configuración.
- Los scripts SQL.

**Puestos (Windows 10, JRE 17).**
1. Copiar `dist/trazalab/` a `C:\TRAZALAB\`.
2. Copiar `config\trazalab.properties.ejemplo` como `config\trazalab.properties`
   y completar `db.clave` con la clave de `trazalab_app`.
3. Ejecutar desde esa carpeta:
   ```bat
   cd C:\TRAZALAB
   java -jar trazalab.jar
   ```

Con `-Dtrazalab.config=ruta` se usa otro archivo de configuración.
`config/trazalab.properties` lleva la clave y no se sube al repositorio.

Las opciones 1 a 9 operan en memoria (igual que en la AP3). La opción **10** abre el
módulo persistente, que trabaja contra MySQL.

Las etiquetas `ap3` y `ap4` identifican el código de cada entrega. El AP3 es
incremental: incluye las definiciones de implementación del sistema completo
(construcción, configuración, usuarios de MySQL y respaldo). Por eso `ap3` apunta al
mismo código que `ap4`. El prototipo en memoria que desarrolla el AP3 corresponde a
las opciones 1 a 9 del menú.

## Pruebas
```bash
sh test/ejecutar-pruebas.sh              # salidas en pantalla
sh test/ejecutar-pruebas.sh evidencias   # salidas guardadas en esa carpeta
```
Compila el prototipo junto con `test/` y ejecuta:
- `trazalab.pruebas.CasosDePrueba`: casos de componente e integración CPC01, CPF02,
  CPC03, CPF04 y CPI05. Cada fila muestra entrada, resultado esperado, obtenido y si
  pasa; el programa termina con error si alguna falla.
- `trazalab.pruebas.EjecutorSistema` con cada guion de `test/sistema/` (CPF02, CPS06,
  CPS07): ejecuta el menú completo con las entradas del guion, como si las tipeara
  un usuario, y deja la sesión en pantalla.

## Estructura
```
src/trazalab/
  dominio/        13 archivos — entidades, enum, interfaz Trazable
  excepciones/     5 archivos — 4 del dominio + PersistenciaException
  servicio/        3 archivos — Laboratorio (control), AgendaTurnos y BitacoraTrazabilidad
  util/            4 archivos — Ordenamiento, Busqueda, Consola, ExportadorArchivos
  persistencia/    5 archivos — ConexionBD, GenericoDAO, PacienteDAO, EstudioDAO, MuestraDAO
  mvc/             3 archivos — Modelo, Vista y Controlador de trazabilidad
  app/             2 archivos — MenuPrincipal y MenuBaseDatos
test/
  trazalab/pruebas/  CasosDePrueba (componente e integración) y EjecutorSistema
  sistema/           guiones de las pruebas de sistema (cpf02, cps06, cps07)
  ejecutar-pruebas.sh
lib/      conector JDBC
sql/      scripts de la base de datos (01 a 04, los de la AP2; 05, usuarios de MySQL)
config/   ejemplo de configuración de la conexión
servidor/ respaldo diario de la base
construir.sh  arma dist/trazalab/ con trazalab.jar
salidas/  certificados de trazabilidad exportados por el sistema
```

## Recorrido sugerido del módulo persistente (opción 10)
1. **1** Listar muestras en circuito — consulta con `JOIN` sobre tres tablas.
2. **2** Registrar evento sobre `M-SAN-0003`, estado 3 → transacción sobre dos tablas.
   Repetir sobre `M-SUE-0001` con estado 2 → la transición inválida se rechaza.
   Con estado 5 también se rechaza: la muestra pasa a INFORMADA recién cuando un
   bioquímico valida sus resultados, no con un evento manual.
3. **3** Consultar catálogo — `ArrayList` convertido a arreglo y ordenado con quicksort.
4. **4** Actualizar precios (por ejemplo 15 %) → `UPDATE` masivo.
5. **5** Indicadores calculados con `GROUP BY` en el motor.
6. **7** Exportar el certificado de trazabilidad a `salidas/`.

Si el servidor MySQL está detenido, el módulo informa el error y el resto del sistema
sigue funcionando en memoria.
