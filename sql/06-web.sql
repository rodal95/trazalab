-- =====================================================================
-- TRAZALAB - Version web de demostracion
-- Se ejecuta despues de 01-esquema.sql y 02-datos.sql:
--   mysql -u root -p < sql/06-web.sql
--
-- 1. Agrega la hora de llegada al turno: la sala de espera es una COLA y
--    se atiende por orden de llegada, no por horario del turno.
-- 2. Crea el usuario generico de la demostracion.
-- 3. Completa los datos ficticios para que cada opcion del menu tenga
--    algo que mostrar. Las fechas se anclan al dia de la carga.
--
-- El numero de cada orden, turno y muestra sale de su clave primaria
-- (O-0004 es la orden 4, M-SAN-0004 la muestra 4): por eso las filas se
-- insertan en orden.
-- =====================================================================
USE trazalab;
SET time_zone = '-03:00';

-- ------------------------------------------------ 1. hora de llegada
ALTER TABLE turnos ADD COLUMN hora_llegada DATETIME NULL AFTER estado;
UPDATE turnos SET hora_llegada = TIMESTAMP(CURDATE(),'07:52:00') WHERE turno_id = 3;

-- ------------------------------------------------ 2. usuario generico
-- Usuario: demo   Contrasenia: demo2026   (rol Bioquimico: puede validar)
INSERT INTO profesionales (dni, apellido, nombre, matricula, rol) VALUES
    ('20111222','Demo','Usuario','BQ-0000','Bioquimico');
INSERT INTO usuarios (profesional_id, username, password_hash) VALUES
    (5,'demo', SHA2('demo2026',256));

-- ------------------------------------------------ 3. datos ficticios
INSERT INTO pacientes (dni, apellido, nombre, fecha_nacimiento, sexo, telefono, email, obra_social_id) VALUES
    ('38123456','Bustos',  'Ana',      '1994-03-08','F','3534-201010','abustos@mail.com', 2),   -- 6
    ('29876543','Luna',    'Pedro',    '1981-12-19','M','3534-202020',NULL,               1),   -- 7
    ('50111333','Herrera', 'Valentina','2012-07-05','F','3534-203030','flia.herrera@mail.com',2),-- 8
    ('16555444','Castro',  'Elena',    '1958-02-27','F','3534-204040',NULL,               3),   -- 9
    ('41999000','Vera',    'Lucas',    '1999-10-14','M','3534-205050','lvera@mail.com',   4),   -- 10
    ('33444777','Godoy',   'Mariana',  '1988-05-21','F','3534-206060','mgodoy@mail.com',  1),   -- 11
    ('24333111','Sosa',    'Ricardo',  '1975-09-03','M','3534-207070',NULL,               3);   -- 12

-- Turnos (ids 6 a 15)
INSERT INTO turnos (paciente_id, fecha_hora, box, estado, hora_llegada) VALUES
    (5,  TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'07:30:00'), 1, 'Atendido', TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'07:24:00')), -- 6
    (6,  TIMESTAMP(CURDATE(),'07:30:00'), 3, 'Atendido', TIMESTAMP(CURDATE(),'07:21:00')),   -- 7
    (7,  TIMESTAMP(CURDATE(),'08:00:00'), 3, 'Atendido', TIMESTAMP(CURDATE(),'07:49:00')),   -- 8
    (8,  TIMESTAMP(CURDATE(),'08:30:00'), 1, 'Presente', TIMESTAMP(CURDATE(),'08:21:00')),   -- 9  en la sala de espera
    (9,  TIMESTAMP(CURDATE(),'08:30:00'), 2, 'Presente', TIMESTAMP(CURDATE(),'08:24:00')),   -- 10 en la sala de espera
    -- Manana: la franja de las 9 tiene los cuatro boxes ocupados (cupo completo).
    (10, TIMESTAMP(CURDATE() + INTERVAL 1 DAY,'09:00:00'), 1, 'Otorgado', NULL),             -- 11
    (11, TIMESTAMP(CURDATE() + INTERVAL 1 DAY,'09:00:00'), 2, 'Otorgado', NULL),             -- 12
    (12, TIMESTAMP(CURDATE() + INTERVAL 1 DAY,'09:00:00'), 3, 'Otorgado', NULL),             -- 13
    (6,  TIMESTAMP(CURDATE() + INTERVAL 1 DAY,'09:00:00'), 4, 'Otorgado', NULL),             -- 14
    (7,  TIMESTAMP(CURDATE() + INTERVAL 1 DAY,'10:00:00'), 1, 'Otorgado', NULL);             -- 15

-- Ordenes (ids 4 a 7)
INSERT INTO ordenes (numero, paciente_id, turno_id, medico_solicitante, fecha, estado, total) VALUES
    ('O-0004', 4, 4, 'Dr. Quiroga, Martin', CURDATE(),                     'Abierta',    16420.00), -- lista para extraer (12 h de ayuno)
    ('O-0005', 5, 6, 'Dra. Perez, Silvia',  CURDATE() - INTERVAL 1 DAY,    'Informada',   8500.00), -- circuito completo
    ('O-0006', 6, 7, 'Dra. Ibarra, Paula',  CURDATE(),                     'En proceso', 35075.00), -- resultados por cargar
    ('O-0007', 7, 8, 'Dr. Quiroga, Martin', CURDATE(),                     'Con muestras', 5200.00);

-- Detalle (ids 6 a 12), con el precio vigente de cada practica
INSERT INTO orden_detalle (orden_id, estudio_id, precio) VALUES
    (4, 2,  3780.00),   -- O-0004 Glucemia
    (4, 3,  4140.00),   -- O-0004 Colesterol
    (4, 1,  8500.00),   -- O-0004 Hemograma
    (5, 1,  8500.00),   -- O-0005 Hemograma
    (6, 6, 10925.00),   -- O-0006 TSH
    (6, 7, 24150.00),   -- O-0006 Vitamina D
    (7, 5,  5200.00);   -- O-0007 Orina completa

-- Muestras (ids 4 a 6)
INSERT INTO muestras (codigo_barra, orden_id, tipo_muestra, estado, fecha_generacion, fecha_extraccion) VALUES
    ('M-SAN-0004', 5, 'Sangre entera', 'INFORMADA',  TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'07:33:00'), TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'07:35:00')),
    ('M-SUE-0005', 6, 'Suero',         'EN_PROCESO', TIMESTAMP(CURDATE(),'07:31:00'), TIMESTAMP(CURDATE(),'07:34:00')),
    ('M-ORI-0006', 7, 'Orina',         'EXTRAIDA',   TIMESTAMP(CURDATE(),'08:02:00'), TIMESTAMP(CURDATE(),'08:06:00'));

INSERT INTO trazas_muestra (muestra_id, estado_anterior, estado_nuevo, fecha_hora, profesional_id, observacion) VALUES
    (4, NULL,         'GENERADA',   TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'07:33:00'), 2, 'Rotulo generado en box 1'),
    (4, 'GENERADA',   'EXTRAIDA',   TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'07:35:00'), 2, 'Extraccion en box 1'),
    (4, 'EXTRAIDA',   'EN_PROCESO', TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'08:05:00'), 1, 'Ingresa al contador hematologico'),
    (4, 'EN_PROCESO', 'ANALIZADA',  TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'09:10:00'), 1, 'Analisis finalizado'),
    (4, 'ANALIZADA',  'INFORMADA',  TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'11:20:00'), 1, 'Resultados validados, informe liberado'),
    (5, NULL,         'GENERADA',   TIMESTAMP(CURDATE(),'07:31:00'), 2, 'Rotulo generado en box 3'),
    (5, 'GENERADA',   'EXTRAIDA',   TIMESTAMP(CURDATE(),'07:34:00'), 2, 'Extraccion en box 3'),
    (5, 'EXTRAIDA',   'EN_PROCESO', TIMESTAMP(CURDATE(),'08:15:00'), 1, 'Ingresa a endocrinologia'),
    (6, NULL,         'GENERADA',   TIMESTAMP(CURDATE(),'08:02:00'), 2, 'Rotulo generado en box 3'),
    (6, 'GENERADA',   'EXTRAIDA',   TIMESTAMP(CURDATE(),'08:06:00'), 2, 'Muestra entregada por el paciente');

INSERT INTO resultados (detalle_id, muestra_id, valor, unidad, ref_min, ref_max, fecha_carga, cargado_por,
                        validado, fecha_validacion, validado_por) VALUES
    (9, 4, 7.200, '10^3/uL', 4.50, 11.00, TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'09:08:00'), 1,
     1, TIMESTAMP(CURDATE() - INTERVAL 1 DAY,'11:20:00'), 1);

SELECT 'Datos de la version web cargados. Usuario: demo / Contrasenia: demo2026' AS resultado;
