package trazalab.persistencia;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import trazalab.dominio.Paciente;
import trazalab.excepciones.PersistenciaException;

/**
 * DAO de la entidad Paciente. IMPLEMENTA la interfaz generica GenericoDAO.
 * Todas las sentencias se ejecutan con PreparedStatement para evitar la
 * inyeccion de SQL (RNF03) y para que el motor reutilice el plan de ejecucion.
 *
 * @author Alday, Rodrigo Matias
 */
public class PacienteDAO implements GenericoDAO<Paciente> {

    private final Connection conexion;

    public PacienteDAO(Connection conexion) {
        this.conexion = conexion;
    }

    @Override
    public int insertar(Paciente paciente) throws PersistenciaException {
        String sql = "INSERT INTO pacientes (dni, apellido, nombre, fecha_nacimiento, sexo, "
                + "telefono, obra_social_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, paciente.getDni());
            ps.setString(2, paciente.getApellido());
            ps.setString(3, paciente.getNombre());
            ps.setDate(4, Date.valueOf(paciente.getFechaNacimiento()));
            ps.setString(5, "X");
            ps.setString(6, paciente.getTelefono());
            ps.setInt(7, resolverObraSocial(paciente.getObraSocial()));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                return claves.next() ? claves.getInt(1) : 0;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new PersistenciaException("Ya existe un paciente con DNI "
                        + paciente.getDni() + " en la base de datos.", e);
            }
            throw new PersistenciaException("Error al insertar el paciente: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Paciente> listar() throws PersistenciaException {
        String sql = "SELECT p.dni, p.apellido, p.nombre, p.fecha_nacimiento, p.telefono, "
                + "COALESCE(os.nombre,'Particular') AS obra_social "
                + "FROM pacientes p LEFT JOIN obras_sociales os "
                + "ON os.obra_social_id = p.obra_social_id ORDER BY p.apellido, p.nombre";
        List<Paciente> pacientes = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                pacientes.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Error al listar pacientes: " + e.getMessage(), e);
        }
        return pacientes;
    }

    @Override
    public Paciente buscarPorId(int id) throws PersistenciaException {
        String sql = "SELECT p.dni, p.apellido, p.nombre, p.fecha_nacimiento, p.telefono, "
                + "COALESCE(os.nombre,'Particular') AS obra_social "
                + "FROM pacientes p LEFT JOIN obras_sociales os "
                + "ON os.obra_social_id = p.obra_social_id WHERE p.paciente_id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Error al buscar el paciente: " + e.getMessage(), e);
        }
    }

    /** Busqueda por documento, la mas usada en el mostrador de recepcion. */
    public Paciente buscarPorDni(String dni) throws PersistenciaException {
        String sql = "SELECT p.dni, p.apellido, p.nombre, p.fecha_nacimiento, p.telefono, "
                + "COALESCE(os.nombre,'Particular') AS obra_social "
                + "FROM pacientes p LEFT JOIN obras_sociales os "
                + "ON os.obra_social_id = p.obra_social_id WHERE p.dni = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Error al buscar por DNI: " + e.getMessage(), e);
        }
    }

    @Override
    public int actualizar(Paciente paciente) throws PersistenciaException {
        String sql = "UPDATE pacientes SET apellido = ?, nombre = ?, telefono = ? WHERE dni = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, paciente.getApellido());
            ps.setString(2, paciente.getNombre());
            ps.setString(3, paciente.getTelefono());
            ps.setString(4, paciente.getDni());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Error al actualizar el paciente: " + e.getMessage(), e);
        }
    }

    @Override
    public int eliminar(int id) throws PersistenciaException {
        String sql = "DELETE FROM pacientes WHERE paciente_id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                throw new PersistenciaException("No se puede eliminar: el paciente tiene "
                        + "turnos u ordenes asociadas. Se debe dar de baja logica.", e);
            }
            throw new PersistenciaException("Error al eliminar el paciente: " + e.getMessage(), e);
        }
    }

    /** Convierte una fila del ResultSet en un objeto del dominio. */
    private Paciente mapear(ResultSet rs) throws SQLException {
        return new Paciente(
                rs.getString("dni"),
                rs.getString("apellido"),
                rs.getString("nombre"),
                rs.getDate("fecha_nacimiento").toLocalDate(),
                rs.getString("obra_social"),
                rs.getString("telefono"));
    }

    /** Resuelve el id del financiador por nombre; devuelve el de particular si no existe. */
    private int resolverObraSocial(String nombre) throws SQLException {
        String sql = "SELECT obra_social_id FROM obras_sociales WHERE nombre = ? LIMIT 1";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 4; // Particular
    }
}
