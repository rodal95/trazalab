-- =====================================================================
-- TRAZALAB - Script 2 de 4: insercion de datos de prueba
-- Permite verificar el modelo antes de implementar la aplicacion.
-- Autor: Alday, Rodrigo Matias - Seminario de Practica Profesional (AP2)
-- =====================================================================
USE trazalab;

-- ------------------------------------------------------- obras sociales
INSERT INTO obras_sociales (nombre, plan, cobertura_pct) VALUES
    ('OSDE',     '210',      70.00),
    ('APROSS',   'Basico',   60.00),
    ('PAMI',     'Unico',   100.00),
    ('Particular','-',         0.00);

-- ------------------------------------------------------------ pacientes
INSERT INTO pacientes (dni, apellido, nombre, fecha_nacimiento, sexo, telefono, email, obra_social_id) VALUES
    ('30111222','Gomez',  'Marcela','1983-04-12','F','3534-556677','mgomez@mail.com',   1),
    ('45222333','Suarez', 'Tomas',  '2015-09-02','M','3534-112233','flia.suarez@mail.com',2),
    ('12888999','Rios',   'Alberto','1949-01-25','M','3534-778899',NULL,                 3),
    ('35777888','Paez',   'Julieta','1990-11-30','F','3534-334455','jpaez@mail.com',     1),
    ('27444555','Molina', 'Sergio', '1979-06-18','M','3534-990011',NULL,                 4);

-- ------------------------------------------------------- profesionales
INSERT INTO profesionales (dni, apellido, nombre, matricula, rol) VALUES
    ('28456789','Ferreyra','Lucia',  'BQ-4821','Bioquimico'),
    ('33112233','Ledesma', 'Carla',  'TL-1204','Extraccionista'),
    ('31555666','Ojeda',   'Natalia', NULL,    'Recepcionista'),
    ('26999888','Alday',   'Rodrigo', NULL,    'Administrador');

-- ------------------------------------------------------------- usuarios
-- El hash corresponde a SHA-256 y se almacena en lugar de la contrasenia.
INSERT INTO usuarios (profesional_id, username, password_hash) VALUES
    (1,'lferreyra', SHA2('Lab2026.bq',256)),
    (2,'cledesma',  SHA2('Lab2026.tl',256)),
    (3,'nojeda',    SHA2('Lab2026.rc',256)),
    (4,'admin',     SHA2('Lab2026.ad',256));

-- ------------------------------------------------------------- estudios
INSERT INTO estudios (codigo,nombre,area,tipo_muestra,precio_base,horas_ayuno,tipo,
                      descuento_pct,recargo_pct,derivado,dias_derivacion,unidad,ref_min,ref_max) VALUES
    ('HEM01','Hemograma completo',               'Hematologia',      'Sangre entera',  8500,  0,'RUTINA',        0, 0,0, 0,'10^3/uL',  4.50,  11.00),
    ('GLU01','Glucemia en ayunas',               'Quimica clinica',  'Suero',          4200,  8,'RUTINA',       10, 0,0, 0,'mg/dL',   70.00, 110.00),
    ('COL01','Colesterol total',                 'Quimica clinica',  'Suero',          4600, 12,'RUTINA',       10, 0,0, 0,'mg/dL',  150.00, 200.00),
    ('TRI01','Trigliceridos',                    'Quimica clinica',  'Suero',          4800, 12,'RUTINA',       10, 0,0, 0,'mg/dL',   50.00, 150.00),
    ('ORI01','Orina completa',                   'Uroanalisis',      'Orina',          5200,  0,'RUTINA',        0, 0,0, 0,'-',        0.00,   0.00),
    ('TSH01','TSH ultrasensible',                'Endocrinologia',   'Suero',         11500,  0,'RUTINA',        5, 0,0, 0,'uUI/mL',   0.40,   4.00),
    ('VIT01','Vitamina D 25-OH',                 'Endocrinologia',   'Suero',         21000,  0,'ESPECIALIZADO', 0,15,0, 0,'ng/mL',   30.00, 100.00),
    ('PCR01','PCR cuantitativa',                 'Inmunologia',      'Suero',         18500,  0,'ESPECIALIZADO', 0,20,0, 0,'mg/L',     0.00,   5.00),
    ('GEN01','Panel genetico trombofilia',       'Biologia molecular','Sangre entera', 96000,  0,'ESPECIALIZADO', 0,25,1,10,'-',        0.00,   0.00),
    ('CEL01','Anticuerpos anti-transglutaminasa','Inmunologia',      'Suero',         24000,  4,'ESPECIALIZADO', 0,18,1, 5,'U/mL',     0.00,  10.00);

-- --------------------------------------------------------------- turnos
INSERT INTO turnos (paciente_id, fecha_hora, box, estado) VALUES
    (1, TIMESTAMP(CURDATE(),'07:30:00'), 1, 'Atendido'),
    (2, TIMESTAMP(CURDATE(),'07:30:00'), 2, 'Atendido'),
    (3, TIMESTAMP(CURDATE(),'08:00:00'), 1, 'Presente'),
    (4, TIMESTAMP(CURDATE(),'08:00:00'), 2, 'Otorgado'),
    (5, TIMESTAMP(CURDATE(),'09:00:00'), 1, 'Otorgado');

-- -------------------------------------------------------------- ordenes
INSERT INTO ordenes (numero, paciente_id, turno_id, medico_solicitante, fecha, estado, total) VALUES
    ('O-0001',1,1,'Dra. Perez, Silvia',  CURDATE(),'En proceso',  13120.00),
    ('O-0002',2,2,'Dr. Quiroga, Martin', CURDATE(),'Con muestras', 8500.00),
    ('O-0003',3,3,'Dra. Perez, Silvia',  CURDATE(),'Abierta',     10925.00);

-- -------------------------------------------------------- orden_detalle
INSERT INTO orden_detalle (orden_id, estudio_id, precio) VALUES
    (1,2, 3780.00),   -- O-0001 Glucemia (con 10 % de descuento de perfil)
    (1,3, 4140.00),   -- O-0001 Colesterol
    (1,5, 5200.00),   -- O-0001 Orina completa
    (2,1, 8500.00),   -- O-0002 Hemograma
    (3,6,10925.00);   -- O-0003 TSH

-- ------------------------------------------------------------- muestras
-- Las fechas se anclan al dia de ejecucion y respetan el horario de los
-- turnos (07:30) y de atencion del laboratorio.
INSERT INTO muestras (codigo_barra, orden_id, tipo_muestra, estado, fecha_extraccion) VALUES
    ('M-SUE-0001',1,'Suero',        'ANALIZADA', TIMESTAMP(CURDATE(),'07:36:00')),
    ('M-ORI-0002',1,'Orina',        'EN_PROCESO',TIMESTAMP(CURDATE(),'07:41:00')),
    ('M-SAN-0003',2,'Sangre entera','EXTRAIDA',  TIMESTAMP(CURDATE(),'07:46:00'));

-- -------------------------------------------------------- trazabilidad
INSERT INTO trazas_muestra (muestra_id, estado_anterior, estado_nuevo, fecha_hora, profesional_id, observacion) VALUES
    (1,NULL,        'GENERADA',  TIMESTAMP(CURDATE(),'07:32:00'),2,'Rotulo generado en box 1'),
    (1,'GENERADA',  'EXTRAIDA',  TIMESTAMP(CURDATE(),'07:36:00'),2,'Extraccion en box 1'),
    (1,'EXTRAIDA',  'EN_PROCESO',TIMESTAMP(CURDATE(),'08:11:00'),1,'Ingresa a autoanalizador'),
    (1,'EN_PROCESO','ANALIZADA', TIMESTAMP(CURDATE(),'09:06:00'),1,'Analisis finalizado'),
    (2,NULL,        'GENERADA',  TIMESTAMP(CURDATE(),'07:32:00'),2,'Rotulo generado en box 1'),
    (2,'GENERADA',  'EXTRAIDA',  TIMESTAMP(CURDATE(),'07:41:00'),2,'Muestra entregada por el paciente'),
    (2,'EXTRAIDA',  'EN_PROCESO',TIMESTAMP(CURDATE(),'08:21:00'),1,'Sedimento urinario en proceso'),
    (3,NULL,        'GENERADA',  TIMESTAMP(CURDATE(),'07:43:00'),2,'Rotulo generado en box 2'),
    (3,'GENERADA',  'EXTRAIDA',  TIMESTAMP(CURDATE(),'07:46:00'),2,'Extraccion pediatrica en box 2');

-- ----------------------------------------------------------- resultados
INSERT INTO resultados (detalle_id, muestra_id, valor, unidad, ref_min, ref_max, cargado_por, validado) VALUES
    (1,1,178.000,'mg/dL', 70.00,110.00,1,0),
    (2,1,245.000,'mg/dL',150.00,200.00,1,0);

SELECT 'Datos de prueba insertados correctamente.' AS resultado;
