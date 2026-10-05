-- =====================================================================
-- TRAZALAB - Usuarios de MySQL
-- Ejecutar una vez en el servidor, como administrador, despues de
-- 01-esquema.sql:   mysql -u root -p < sql/05-usuarios.sql
--
-- La aplicacion no se conecta como root sino como trazalab_app, que
-- solo puede hacer lo que el sistema necesita en la base trazalab. El
-- respaldo diario usa trazalab_respaldo, de solo lectura.
--
-- Las claves no estan escritas aca: MySQL las genera (RANDOM PASSWORD) y
-- las muestra una sola vez. La de trazalab_app va a
-- config/trazalab.properties de cada puesto; la de trazalab_respaldo, al
-- archivo de opciones del respaldo (servidor/respaldo.cnf.ejemplo).
-- =====================================================================

-- Solo desde la red del laboratorio (RNF07).
CREATE USER 'trazalab_app'@'192.168.10.%'
    IDENTIFIED BY RANDOM PASSWORD
    REQUIRE SSL;

-- Lectura de todas las tablas y de la vista.
GRANT SELECT ON trazalab.* TO 'trazalab_app'@'192.168.10.%';

-- Escritura acotada a lo que registra el sistema. No hay DELETE en
-- ninguna tabla: lo que se corrige se anula o se desactiva.
GRANT INSERT, UPDATE ON trazalab.pacientes      TO 'trazalab_app'@'192.168.10.%';
GRANT INSERT, UPDATE ON trazalab.turnos         TO 'trazalab_app'@'192.168.10.%';
GRANT INSERT, UPDATE ON trazalab.ordenes        TO 'trazalab_app'@'192.168.10.%';
GRANT INSERT, UPDATE ON trazalab.orden_detalle  TO 'trazalab_app'@'192.168.10.%';
GRANT INSERT, UPDATE ON trazalab.muestras       TO 'trazalab_app'@'192.168.10.%';
GRANT INSERT, UPDATE ON trazalab.resultados     TO 'trazalab_app'@'192.168.10.%';
GRANT INSERT, UPDATE ON trazalab.estudios       TO 'trazalab_app'@'192.168.10.%';
GRANT UPDATE (ultimo_acceso) ON trazalab.usuarios TO 'trazalab_app'@'192.168.10.%';

-- La bitacora solo admite agregar eventos (RNF04). Los disparadores de
-- 01-esquema.sql rechazan UPDATE y DELETE; ademas, el usuario de la
-- aplicacion no tiene esos permisos sobre la tabla.
GRANT INSERT ON trazalab.trazas_muestra TO 'trazalab_app'@'192.168.10.%';

-- Respaldo: solo desde el propio servidor.
CREATE USER 'trazalab_respaldo'@'localhost'
    IDENTIFIED BY RANDOM PASSWORD;
GRANT SELECT, SHOW VIEW, TRIGGER, LOCK TABLES ON trazalab.* TO 'trazalab_respaldo'@'localhost';
