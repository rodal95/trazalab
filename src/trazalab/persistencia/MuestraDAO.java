package trazalab.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import trazalab.dominio.EstadoMuestra;
import trazalab.excepciones.PersistenciaException;

/**
 * DAO de muestras y de su bitacora de trazabilidad. Concentra la operacion mas
 * critica del sistema: el cambio de estado, que se ejecuta como TRANSACCION
 * porque debe impactar en dos tablas o en ninguna.
 *
 * @author Alday, Rodrigo Matias
 */
public class MuestraDAO {

    /** Formato de fecha y hora de los eventos, con segundos en todos los casos. */
    private static final DateTimeFormatter FMT_EVENTO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Connection conexion;

    public MuestraDAO(Connection conexion) {
        this.conexion = conexion;
    }

    /** Registro plano de una muestra, para presentar en la vista. */
    public static class FilaMuestra {
        public final int id;
        public final String codigoBarra;
        public final String tipoMuestra;
        public final String estado;
        public final String orden;
        public final String paciente;

        public FilaMuestra(int id, String codigoBarra, String tipoMuestra,
                           String estado, String orden, String paciente) {
            this.id = id;
            this.codigoBarra = codigoBarra;
            this.tipoMuestra = tipoMuestra;
            this.estado = estado;
            this.orden = orden;
            this.paciente = paciente;
        }
    }

    /** Devuelve las muestras que todavia estan dentro del circuito. */
    public List<FilaMuestra> listarEnCircuito() throws PersistenciaException {
        String sql = "SELECT m.muestra_id, m.codigo_barra, m.tipo_muestra, m.estado, "
                + "o.numero, CONCAT(p.apellido, ', ', p.nombre) AS paciente "
                + "FROM muestras m "
                + "JOIN ordenes o   ON o.orden_id = m.orden_id "
                + "JOIN pacientes p ON p.paciente_id = o.paciente_id "
                + "WHERE m.estado NOT IN ('INFORMADA','ANULADA') "
                + "ORDER BY m.codigo_barra";
        List<FilaMuestra> muestras = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                muestras.add(new FilaMuestra(rs.getInt("muestra_id"), rs.getString("codigo_barra"),
                        rs.getString("tipo_muestra"), rs.getString("estado"),
                        rs.getString("numero"), rs.getString("paciente")));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Error al listar las muestras: " + e.getMessage(), e);
        }
        return muestras;
    }

    /** Busca una muestra por codigo de barra; devuelve null si no existe. */
    public FilaMuestra buscarPorCodigo(String codigoBarra) throws PersistenciaException {
        String sql = "SELECT m.muestra_id, m.codigo_barra, m.tipo_muestra, m.estado, "
                + "o.numero, CONCAT(p.apellido, ', ', p.nombre) AS paciente "
                + "FROM muestras m "
                + "JOIN ordenes o   ON o.orden_id = m.orden_id "
                + "JOIN pacientes p ON p.paciente_id = o.paciente_id "
                + "WHERE m.codigo_barra = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, codigoBarra);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FilaMuestra(rs.getInt("muestra_id"), rs.getString("codigo_barra"),
                            rs.getString("tipo_muestra"), rs.getString("estado"),
                            rs.getString("numero"), rs.getString("paciente"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Error al buscar la muestra: " + e.getMessage(), e);
        }
    }

    /**
     * Cambia el estado de la muestra y registra el evento de trazabilidad dentro
     * de una unica TRANSACCION. Si falla el segundo INSERT se revierte el UPDATE:
     * nunca puede quedar una muestra con estado nuevo y sin traza que lo respalde.
     */
    public void cambiarEstado(int muestraId, EstadoMuestra estadoAnterior, EstadoMuestra estadoNuevo,
                              int profesionalId, String observacion) throws PersistenciaException {
        String sqlUpdate = "UPDATE muestras SET estado = ?"
                + (estadoNuevo == EstadoMuestra.EXTRAIDA ? ", fecha_extraccion = NOW()" : "")
                + " WHERE muestra_id = ?";
        String sqlTraza = "INSERT INTO trazas_muestra (muestra_id, estado_anterior, estado_nuevo, "
                + "profesional_id, observacion) VALUES (?, ?, ?, ?, ?)";
        boolean autoCommitOriginal = true;
        try {
            autoCommitOriginal = conexion.getAutoCommit();
            conexion.setAutoCommit(false);

            try (PreparedStatement ps = conexion.prepareStatement(sqlUpdate)) {
                ps.setString(1, estadoNuevo.name());
                ps.setInt(2, muestraId);
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("La muestra " + muestraId + " no existe.");
                }
            }
            try (PreparedStatement ps = conexion.prepareStatement(sqlTraza)) {
                ps.setInt(1, muestraId);
                if (estadoAnterior == null) {
                    ps.setNull(2, java.sql.Types.VARCHAR);
                } else {
                    ps.setString(2, estadoAnterior.name());
                }
                ps.setString(3, estadoNuevo.name());
                ps.setInt(4, profesionalId);
                ps.setString(5, observacion);
                ps.executeUpdate();
            }
            conexion.commit();
        } catch (SQLException e) {
            try {
                conexion.rollback();
            } catch (SQLException ex) {
                throw new PersistenciaException("Error al revertir la transaccion: " + ex.getMessage(), ex);
            }
            throw new PersistenciaException("No se pudo registrar el cambio de estado, "
                    + "la operacion fue revertida: " + e.getMessage(), e);
        } finally {
            try {
                conexion.setAutoCommit(autoCommitOriginal);
            } catch (SQLException e) {
                System.err.println("Aviso: no se pudo restaurar el modo de confirmacion. " + e.getMessage());
            }
        }
    }

    /** Devuelve el historial de trazabilidad de una muestra ya formateado. */
    public List<String> obtenerHistorial(String codigoBarra) throws PersistenciaException {
        String sql = "SELECT tr.fecha_hora, COALESCE(tr.estado_anterior,'(alta)') AS desde, "
                + "tr.estado_nuevo AS hacia, CONCAT(pr.apellido, ', ', pr.nombre) AS responsable, "
                + "pr.rol, tr.observacion "
                + "FROM trazas_muestra tr "
                + "JOIN muestras m       ON m.muestra_id = tr.muestra_id "
                + "LEFT JOIN profesionales pr ON pr.profesional_id = tr.profesional_id "
                + "WHERE m.codigo_barra = ? ORDER BY tr.fecha_hora";
        List<String> historial = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, codigoBarra);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    historial.add(String.format("%s  %-10s -> %-10s  %-20s %-15s %s",
                            rs.getTimestamp("fecha_hora").toLocalDateTime().format(FMT_EVENTO),
                            rs.getString("desde"), rs.getString("hacia"),
                            rs.getString("responsable") == null ? "(sin dato)" : rs.getString("responsable"),
                            rs.getString("rol") == null ? "" : rs.getString("rol"),
                            rs.getString("observacion") == null ? "" : rs.getString("observacion")));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Error al obtener el historial: " + e.getMessage(), e);
        }
        return historial;
    }

    /** Indicador de gestion: cantidad de muestras por estado. */
    public List<String> contarPorEstado() throws PersistenciaException {
        String sql = "SELECT estado, COUNT(*) AS cantidad FROM muestras GROUP BY estado ORDER BY 2 DESC";
        List<String> filas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(String.format("  %-12s %3d muestra(s)", rs.getString("estado"), rs.getInt("cantidad")));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Error al calcular el indicador: " + e.getMessage(), e);
        }
        return filas;
    }
}
