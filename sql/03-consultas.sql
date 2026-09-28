-- =====================================================================
-- TRAZALAB - Script 3 de 4: consultas de verificacion
-- Autor: Alday, Rodrigo Matias - Seminario de Practica Profesional (AP2)
-- =====================================================================
USE trazalab;

-- Consulta 1 --------------------------------------------------------
-- Agenda del dia con paciente y cobertura. Uso de JOIN explicito e
-- INNER/LEFT JOIN para no perder pacientes sin obra social.
SELECT  t.fecha_hora                          AS 'Fecha y hora',
        t.box                                 AS 'Box',
        CONCAT(p.apellido,', ',p.nombre)      AS 'Paciente',
        TIMESTAMPDIFF(YEAR,p.fecha_nacimiento,CURDATE()) AS 'Edad',
        COALESCE(os.nombre,'Sin cobertura')   AS 'Obra social',
        t.estado                              AS 'Estado'
FROM        turnos t
INNER JOIN  pacientes p       ON p.paciente_id     = t.paciente_id
LEFT  JOIN  obras_sociales os ON os.obra_social_id = p.obra_social_id
WHERE       DATE(t.fecha_hora) = CURDATE()
ORDER BY    t.fecha_hora, t.box;

-- Consulta 2 --------------------------------------------------------
-- Trazabilidad completa de una muestra: el corazon del sistema.
SELECT  m.codigo_barra                              AS 'Muestra',
        tr.fecha_hora                               AS 'Fecha y hora',
        COALESCE(tr.estado_anterior,'(alta)')       AS 'Desde',
        tr.estado_nuevo                             AS 'Hacia',
        CONCAT(pr.apellido,', ',pr.nombre)          AS 'Responsable',
        pr.rol                                      AS 'Rol',
        tr.observacion                              AS 'Observacion'
FROM        trazas_muestra tr
INNER JOIN  muestras       m  ON m.muestra_id     = tr.muestra_id
LEFT  JOIN  profesionales  pr ON pr.profesional_id = tr.profesional_id
WHERE       m.codigo_barra = 'M-SUE-0001'
ORDER BY    tr.fecha_hora;

-- Consulta 3 --------------------------------------------------------
-- Muestras demoradas: mas de 120 minutos en circuito sin informar.
-- Utiliza la vista creada en el script de esquema.
SELECT  codigo_barra AS 'Muestra',
        paciente     AS 'Paciente',
        estado       AS 'Estado',
        minutos_en_proceso AS 'Minutos en proceso'
FROM    v_muestras_en_circuito
WHERE   minutos_en_proceso > 120
ORDER BY minutos_en_proceso DESC;

-- Consulta 4 --------------------------------------------------------
-- Facturacion por obra social en el periodo (GROUP BY + HAVING).
SELECT  COALESCE(os.nombre,'Particular') AS 'Financiador',
        COUNT(DISTINCT o.orden_id)       AS 'Ordenes',
        SUM(d.precio)                    AS 'Facturado',
        ROUND(AVG(d.precio),2)           AS 'Ticket promedio'
FROM        ordenes o
INNER JOIN  pacientes      p  ON p.paciente_id     = o.paciente_id
LEFT  JOIN  obras_sociales os ON os.obra_social_id = p.obra_social_id
INNER JOIN  orden_detalle  d  ON d.orden_id        = o.orden_id
WHERE       o.fecha BETWEEN DATE_FORMAT(CURDATE(),'%Y-%m-01') AND LAST_DAY(CURDATE())
GROUP BY    COALESCE(os.nombre,'Particular')
HAVING      SUM(d.precio) > 0
ORDER BY    SUM(d.precio) DESC;

-- Consulta 5 --------------------------------------------------------
-- Resultados fuera del rango de referencia pendientes de validacion.
SELECT  o.numero                         AS 'Orden',
        CONCAT(p.apellido,', ',p.nombre) AS 'Paciente',
        e.nombre                         AS 'Practica',
        r.valor                          AS 'Valor',
        r.unidad                         AS 'Unidad',
        CONCAT(r.ref_min,' - ',r.ref_max) AS 'Referencia',
        CASE WHEN r.valor > r.ref_max THEN 'ALTO'
             WHEN r.valor < r.ref_min THEN 'BAJO'
             ELSE 'NORMAL' END           AS 'Marca'
FROM        resultados    r
INNER JOIN  orden_detalle d ON d.detalle_id = r.detalle_id
INNER JOIN  ordenes       o ON o.orden_id   = d.orden_id
INNER JOIN  pacientes     p ON p.paciente_id = o.paciente_id
INNER JOIN  estudios      e ON e.estudio_id  = d.estudio_id
WHERE       r.validado = 0
  AND      (r.valor < r.ref_min OR r.valor > r.ref_max)
ORDER BY    o.numero, e.codigo;

-- Consulta 6 --------------------------------------------------------
-- Tiempo de respuesta (TAT) promedio en minutos por tipo de muestra,
-- entre la extraccion y el ultimo evento registrado.
SELECT  m.tipo_muestra                                        AS 'Tipo de muestra',
        COUNT(*)                                              AS 'Muestras',
        ROUND(AVG(TIMESTAMPDIFF(MINUTE, m.fecha_extraccion, u.ultimo)),1) AS 'TAT promedio (min)'
FROM        muestras m
INNER JOIN (SELECT muestra_id, MAX(fecha_hora) AS ultimo
            FROM   trazas_muestra
            GROUP BY muestra_id) u ON u.muestra_id = m.muestra_id
WHERE       m.fecha_extraccion IS NOT NULL
GROUP BY    m.tipo_muestra
ORDER BY    3 DESC;

-- Consulta 7 --------------------------------------------------------
-- Actualizacion: validacion de todos los resultados de una orden,
-- seguida de la verificacion posterior.
UPDATE      resultados    r
INNER JOIN  orden_detalle d ON d.detalle_id = r.detalle_id
INNER JOIN  ordenes       o ON o.orden_id   = d.orden_id
SET         r.validado        = 1,
            r.fecha_validacion = NOW(),
            r.validado_por     = 1
WHERE       o.numero = 'O-0001' AND r.validado = 0;

SELECT ROW_COUNT() AS 'Resultados validados';

SELECT  o.numero AS 'Orden', COUNT(*) AS 'Resultados', SUM(r.validado) AS 'Validados'
FROM        resultados    r
INNER JOIN  orden_detalle d ON d.detalle_id = r.detalle_id
INNER JOIN  ordenes       o ON o.orden_id   = d.orden_id
GROUP BY    o.numero;

-- =====================================================================
-- Consultas complementarias (no incluidas en el informe)
-- =====================================================================

-- Consulta 8 --------------------------------------------------------
-- Detalle completo de una orden: paciente, practicas, precios y total.
SELECT  o.numero                          AS 'Orden',
        CONCAT(p.apellido,', ',p.nombre)  AS 'Paciente',
        e.codigo                          AS 'Codigo',
        e.nombre                          AS 'Practica',
        e.horas_ayuno                     AS 'Ayuno (h)',
        d.precio                          AS 'Precio'
FROM        ordenes o
INNER JOIN  pacientes     p ON p.paciente_id = o.paciente_id
INNER JOIN  orden_detalle d ON d.orden_id    = o.orden_id
INNER JOIN  estudios      e ON e.estudio_id  = d.estudio_id
WHERE       o.numero = 'O-0001'
ORDER BY    e.codigo;

-- Consulta 9 --------------------------------------------------------
-- Indicador de gestion: cantidad de muestras por estado (GROUP BY).
SELECT  estado          AS 'Estado',
        COUNT(*)        AS 'Cantidad',
        ROUND(COUNT(*) * 100 / (SELECT COUNT(*) FROM muestras),2) AS 'Porcentaje'
FROM    muestras
GROUP BY estado
ORDER BY COUNT(*) DESC;
