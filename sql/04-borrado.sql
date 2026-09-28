-- =====================================================================
-- TRAZALAB - Script 4 de 4: borrado de registros y verificacion
-- Se respeta el orden inverso a las dependencias de clave foranea.
-- Autor: Alday, Rodrigo Matias - Seminario de Practica Profesional (AP2)
-- =====================================================================
USE trazalab;

-- Borrado selectivo: se anula una muestra en lugar de eliminarla, para
-- no perder la trazabilidad exigida por la normativa del laboratorio.
UPDATE muestras SET estado = 'ANULADA' WHERE codigo_barra = 'M-ORI-0002';
INSERT INTO trazas_muestra (muestra_id, estado_anterior, estado_nuevo, profesional_id, observacion)
SELECT muestra_id, 'EN_PROCESO', 'ANULADA', 1, 'Muestra insuficiente, se solicita reextraccion'
FROM   muestras WHERE codigo_barra = 'M-ORI-0002';

SELECT codigo_barra, estado FROM muestras WHERE codigo_barra = 'M-ORI-0002';

-- Borrado en cascada: al eliminar una orden se eliminan su detalle, sus
-- muestras, las trazas de esas muestras y sus resultados (ON DELETE CASCADE).
DELETE FROM ordenes WHERE numero = 'O-0003';

SELECT 'ordenes'        AS tabla, COUNT(*) AS filas FROM ordenes
UNION ALL SELECT 'orden_detalle',  COUNT(*) FROM orden_detalle
UNION ALL SELECT 'muestras',       COUNT(*) FROM muestras
UNION ALL SELECT 'trazas_muestra', COUNT(*) FROM trazas_muestra
UNION ALL SELECT 'resultados',     COUNT(*) FROM resultados;

-- Limpieza total del juego de datos de prueba.
-- Se desactiva temporalmente la verificacion de claves foraneas.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE resultados;
TRUNCATE TABLE trazas_muestra;
TRUNCATE TABLE muestras;
TRUNCATE TABLE orden_detalle;
TRUNCATE TABLE ordenes;
TRUNCATE TABLE turnos;
TRUNCATE TABLE usuarios;
TRUNCATE TABLE profesionales;
TRUNCATE TABLE estudios;
TRUNCATE TABLE pacientes;
TRUNCATE TABLE obras_sociales;
SET FOREIGN_KEY_CHECKS = 1;

-- Verificacion final: todas las tablas deben quedar en cero.
SELECT 'obras_sociales' AS tabla, COUNT(*) AS filas FROM obras_sociales
UNION ALL SELECT 'pacientes',     COUNT(*) FROM pacientes
UNION ALL SELECT 'profesionales', COUNT(*) FROM profesionales
UNION ALL SELECT 'usuarios',      COUNT(*) FROM usuarios
UNION ALL SELECT 'estudios',      COUNT(*) FROM estudios
UNION ALL SELECT 'turnos',        COUNT(*) FROM turnos
UNION ALL SELECT 'ordenes',       COUNT(*) FROM ordenes
UNION ALL SELECT 'orden_detalle', COUNT(*) FROM orden_detalle
UNION ALL SELECT 'muestras',      COUNT(*) FROM muestras
UNION ALL SELECT 'trazas_muestra',COUNT(*) FROM trazas_muestra
UNION ALL SELECT 'resultados',    COUNT(*) FROM resultados;
