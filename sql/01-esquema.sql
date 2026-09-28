-- =====================================================================
-- TRAZALAB - Sistema de gestion de turnos y trazabilidad de muestras
-- Laboratorio Bioquimico San Rafael
-- Script 1 de 4: creacion de la base de datos y de las tablas
-- Motor: MySQL 8.0 / InnoDB / utf8mb4
-- Autor: Alday, Rodrigo Matias - Seminario de Practica Profesional (AP2)
-- =====================================================================

DROP DATABASE IF EXISTS trazalab;
CREATE DATABASE trazalab
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_spanish_ci;
USE trazalab;

-- ---------------------------------------------------------------------
-- Tabla obras_sociales
-- Entidad independiente. Se separa de pacientes para evitar la anomalia
-- de actualizacion que produciria repetir el nombre y la cobertura del
-- financiador en cada paciente (2FN y 3FN).
-- ---------------------------------------------------------------------
CREATE TABLE obras_sociales (
    obra_social_id  INT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(80)   NOT NULL,
    plan            VARCHAR(40)   NOT NULL,
    cobertura_pct   DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    activa          TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT uq_obra_plan UNIQUE (nombre, plan),
    CONSTRAINT ck_cobertura CHECK (cobertura_pct BETWEEN 0 AND 100)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla pacientes
-- El DNI es clave candidata (UNIQUE); se usa un id sustituto como clave
-- primaria para independizar las relaciones de un dato de negocio.
-- ---------------------------------------------------------------------
CREATE TABLE pacientes (
    paciente_id      INT AUTO_INCREMENT PRIMARY KEY,
    dni              VARCHAR(10)  NOT NULL,
    apellido         VARCHAR(60)  NOT NULL,
    nombre           VARCHAR(60)  NOT NULL,
    fecha_nacimiento DATE         NOT NULL,
    sexo             ENUM('F','M','X') NOT NULL,
    telefono         VARCHAR(30),
    email            VARCHAR(100),
    obra_social_id   INT,
    fecha_alta       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_paciente_dni UNIQUE (dni),
    CONSTRAINT fk_paciente_obra FOREIGN KEY (obra_social_id)
        REFERENCES obras_sociales (obra_social_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla profesionales
-- Recepcionistas, extraccionistas y bioquimicos que operan el sistema.
-- ---------------------------------------------------------------------
CREATE TABLE profesionales (
    profesional_id  INT AUTO_INCREMENT PRIMARY KEY,
    dni             VARCHAR(10) NOT NULL,
    apellido        VARCHAR(60) NOT NULL,
    nombre          VARCHAR(60) NOT NULL,
    matricula       VARCHAR(20),
    rol             ENUM('Recepcionista','Extraccionista','Bioquimico','Administrador') NOT NULL,
    activo          TINYINT(1)  NOT NULL DEFAULT 1,
    CONSTRAINT uq_profesional_dni UNIQUE (dni)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla usuarios
-- Relacion 1:1 con profesionales. Separa la identidad laboral de las
-- credenciales de acceso (RNF03: contrasenias almacenadas con hash).
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    usuario_id      INT AUTO_INCREMENT PRIMARY KEY,
    profesional_id  INT          NOT NULL,
    username        VARCHAR(40)  NOT NULL,
    password_hash   CHAR(64)     NOT NULL,
    ultimo_acceso   DATETIME,
    CONSTRAINT uq_username UNIQUE (username),
    CONSTRAINT uq_usuario_profesional UNIQUE (profesional_id),
    CONSTRAINT fk_usuario_profesional FOREIGN KEY (profesional_id)
        REFERENCES profesionales (profesional_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla estudios
-- Catalogo de practicas. El campo tipo distingue la subclase concreta
-- del modelo de objetos (estrategia de tabla unica por jerarquia).
-- ---------------------------------------------------------------------
CREATE TABLE estudios (
    estudio_id      INT AUTO_INCREMENT PRIMARY KEY,
    codigo          VARCHAR(10)   NOT NULL,
    nombre          VARCHAR(120)  NOT NULL,
    area            VARCHAR(60)   NOT NULL,
    tipo_muestra    VARCHAR(40)   NOT NULL,
    precio_base     DECIMAL(10,2) NOT NULL,
    horas_ayuno     INT           NOT NULL DEFAULT 0,
    tipo            ENUM('RUTINA','ESPECIALIZADO') NOT NULL,
    descuento_pct   DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    recargo_pct     DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    derivado        TINYINT(1)    NOT NULL DEFAULT 0,
    dias_derivacion INT           NOT NULL DEFAULT 0,
    unidad          VARCHAR(20),
    ref_min         DECIMAL(10,2),
    ref_max         DECIMAL(10,2),
    activo          TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT uq_estudio_codigo UNIQUE (codigo),
    CONSTRAINT ck_precio CHECK (precio_base >= 0),
    CONSTRAINT ck_ayuno  CHECK (horas_ayuno BETWEEN 0 AND 48)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla turnos
-- ---------------------------------------------------------------------
CREATE TABLE turnos (
    turno_id       INT AUTO_INCREMENT PRIMARY KEY,
    paciente_id    INT       NOT NULL,
    fecha_hora     DATETIME  NOT NULL,
    box            INT       NOT NULL,
    estado         ENUM('Otorgado','Presente','Atendido','Ausente','Cancelado')
                   NOT NULL DEFAULT 'Otorgado',
    observaciones  VARCHAR(200),
    CONSTRAINT uq_turno_franja UNIQUE (fecha_hora, box),
    CONSTRAINT fk_turno_paciente FOREIGN KEY (paciente_id)
        REFERENCES pacientes (paciente_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla ordenes
-- ---------------------------------------------------------------------
CREATE TABLE ordenes (
    orden_id           INT AUTO_INCREMENT PRIMARY KEY,
    numero             VARCHAR(12) NOT NULL,
    paciente_id        INT         NOT NULL,
    turno_id           INT,
    medico_solicitante VARCHAR(80) NOT NULL,
    fecha              DATE        NOT NULL,
    estado             ENUM('Abierta','Con muestras','En proceso','Informada','Anulada')
                       NOT NULL DEFAULT 'Abierta',
    total              DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT uq_orden_numero UNIQUE (numero),
    CONSTRAINT fk_orden_paciente FOREIGN KEY (paciente_id)
        REFERENCES pacientes (paciente_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_orden_turno FOREIGN KEY (turno_id)
        REFERENCES turnos (turno_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla orden_detalle
-- Resuelve la relacion N:M entre ordenes y estudios. Guarda el precio
-- historico: es un atributo propio de la relacion, no del estudio (3FN).
-- ---------------------------------------------------------------------
CREATE TABLE orden_detalle (
    detalle_id  INT AUTO_INCREMENT PRIMARY KEY,
    orden_id    INT           NOT NULL,
    estudio_id  INT           NOT NULL,
    precio      DECIMAL(10,2) NOT NULL,
    CONSTRAINT uq_orden_estudio UNIQUE (orden_id, estudio_id),
    CONSTRAINT fk_detalle_orden FOREIGN KEY (orden_id)
        REFERENCES ordenes (orden_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_detalle_estudio FOREIGN KEY (estudio_id)
        REFERENCES estudios (estudio_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla muestras
-- ---------------------------------------------------------------------
CREATE TABLE muestras (
    muestra_id       INT AUTO_INCREMENT PRIMARY KEY,
    codigo_barra     VARCHAR(20) NOT NULL,
    orden_id         INT         NOT NULL,
    tipo_muestra     VARCHAR(40) NOT NULL,
    estado           ENUM('GENERADA','EXTRAIDA','EN_PROCESO','ANALIZADA','INFORMADA','ANULADA')
                     NOT NULL DEFAULT 'GENERADA',
    fecha_generacion DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_extraccion DATETIME,
    CONSTRAINT uq_muestra_codigo UNIQUE (codigo_barra),
    CONSTRAINT fk_muestra_orden FOREIGN KEY (orden_id)
        REFERENCES ordenes (orden_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla trazas_muestra
-- Bitacora inmutable del circuito. Cada fila es un EventoTraza del
-- modelo de objetos. Es la tabla que sostiene el objetivo del sistema.
-- ---------------------------------------------------------------------
CREATE TABLE trazas_muestra (
    traza_id        INT AUTO_INCREMENT PRIMARY KEY,
    muestra_id      INT      NOT NULL,
    estado_anterior ENUM('GENERADA','EXTRAIDA','EN_PROCESO','ANALIZADA','INFORMADA','ANULADA'),
    estado_nuevo    ENUM('GENERADA','EXTRAIDA','EN_PROCESO','ANALIZADA','INFORMADA','ANULADA') NOT NULL,
    fecha_hora      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    profesional_id  INT,
    observacion     VARCHAR(200),
    CONSTRAINT fk_traza_muestra FOREIGN KEY (muestra_id)
        REFERENCES muestras (muestra_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_traza_profesional FOREIGN KEY (profesional_id)
        REFERENCES profesionales (profesional_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tabla resultados
-- ---------------------------------------------------------------------
CREATE TABLE resultados (
    resultado_id      INT AUTO_INCREMENT PRIMARY KEY,
    detalle_id        INT           NOT NULL,
    muestra_id        INT           NOT NULL,
    valor             DECIMAL(12,3) NOT NULL,
    unidad            VARCHAR(20)   NOT NULL,
    ref_min           DECIMAL(10,2) NOT NULL,
    ref_max           DECIMAL(10,2) NOT NULL,
    fecha_carga       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cargado_por       INT,
    validado          TINYINT(1)    NOT NULL DEFAULT 0,
    fecha_validacion  DATETIME,
    validado_por      INT,
    CONSTRAINT uq_resultado_detalle UNIQUE (detalle_id),
    CONSTRAINT fk_resultado_detalle FOREIGN KEY (detalle_id)
        REFERENCES orden_detalle (detalle_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_resultado_muestra FOREIGN KEY (muestra_id)
        REFERENCES muestras (muestra_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_resultado_carga FOREIGN KEY (cargado_por)
        REFERENCES profesionales (profesional_id) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_resultado_valida FOREIGN KEY (validado_por)
        REFERENCES profesionales (profesional_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Indices de apoyo a las consultas mas frecuentes (RNF05: rendimiento)
-- ---------------------------------------------------------------------
CREATE INDEX idx_pacientes_apellido ON pacientes (apellido, nombre);
CREATE INDEX idx_turnos_fecha       ON turnos (fecha_hora);
CREATE INDEX idx_ordenes_paciente   ON ordenes (paciente_id, fecha);
CREATE INDEX idx_muestras_estado    ON muestras (estado);
CREATE INDEX idx_trazas_muestra     ON trazas_muestra (muestra_id, fecha_hora);

-- ---------------------------------------------------------------------
-- Vista de apoyo: estado actual de cada muestra con su paciente
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_muestras_en_circuito AS
SELECT  m.codigo_barra,
        m.tipo_muestra,
        m.estado,
        o.numero              AS orden,
        CONCAT(p.apellido, ', ', p.nombre) AS paciente,
        m.fecha_extraccion,
        TIMESTAMPDIFF(MINUTE, m.fecha_extraccion, NOW()) AS minutos_en_proceso
FROM    muestras  m
JOIN    ordenes   o ON o.orden_id    = m.orden_id
JOIN    pacientes p ON p.paciente_id = o.paciente_id
WHERE   m.estado NOT IN ('INFORMADA','ANULADA');

SHOW TABLES;
