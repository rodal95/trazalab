package trazalab.persistencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import trazalab.dominio.Estudio;
import trazalab.dominio.EstudioEspecializado;
import trazalab.dominio.EstudioRutina;
import trazalab.excepciones.PersistenciaException;

/**
 * DAO del catalogo de practicas. Ejemplo de POLIMORFISMO en la capa de datos:
 * a partir de la columna "tipo" se instancia la subclase concreta que
 * corresponde, y el resto del sistema trabaja siempre con el tipo Estudio.
 *
 * @author Alday, Rodrigo Matias
 */
public class EstudioDAO implements GenericoDAO<Estudio> {

    private final Connection conexion;

    public EstudioDAO(Connection conexion) {
        this.conexion = conexion;
    }

    @Override
    public int insertar(Estudio estudio) throws PersistenciaException {
        String sql = "INSERT INTO estudios (codigo, nombre, area, tipo_muestra, precio_base, "
                + "horas_ayuno, tipo, descuento_pct, recargo_pct, derivado, dias_derivacion) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, estudio.getCodigo());
            ps.setString(2, estudio.getNombre());
            ps.setString(3, estudio.getArea());
            ps.setString(4, estudio.getTipoMuestra());
            ps.setDouble(5, estudio.getPrecioBase());
            ps.setInt(6, estudio.getHorasAyuno());
            boolean especializado = estudio instanceof EstudioEspecializado;
            ps.setString(7, especializado ? "ESPECIALIZADO" : "RUTINA");
            ps.setDouble(8, especializado ? 0 : ((EstudioRutina) estudio).getDescuentoPerfil());
            ps.setDouble(9, especializado ? ((EstudioEspecializado) estudio).getRecargoComplejidad() : 0);
            ps.setBoolean(10, especializado && ((EstudioEspecializado) estudio).esDerivado());
            ps.setInt(11, especializado ? ((EstudioEspecializado) estudio).getDiasDerivacion() : 0);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Error al insertar el estudio: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Estudio> listar() throws PersistenciaException {
        String sql = "SELECT * FROM estudios WHERE activo = 1 ORDER BY codigo";
        List<Estudio> estudios = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                estudios.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Error al listar el catalogo: " + e.getMessage(), e);
        }
        return estudios;
    }

    /**
     * Devuelve el catalogo como ARREGLO de tamanio exacto. La aplicacion usa la
     * lista para construirlo (tamanio desconocido) y el arreglo para aplicar los
     * algoritmos de ordenacion y busqueda binaria: uso complementario de ambas
     * estructuras, tal como exige la consigna.
     */
    public Estudio[] listarComoArreglo() throws PersistenciaException {
        List<Estudio> lista = listar();
        Estudio[] arreglo = new Estudio[lista.size()];
        return lista.toArray(arreglo);
    }

    @Override
    public Estudio buscarPorId(int id) throws PersistenciaException {
        return buscar("SELECT * FROM estudios WHERE estudio_id = ?", String.valueOf(id), true);
    }

    /** Busca una practica por su codigo de catalogo. */
    public Estudio buscarPorCodigo(String codigo) throws PersistenciaException {
        return buscar("SELECT * FROM estudios WHERE codigo = ?", codigo, false);
    }

    private Estudio buscar(String sql, String valor, boolean esEntero) throws PersistenciaException {
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            if (esEntero) {
                ps.setInt(1, Integer.parseInt(valor));
            } else {
                ps.setString(1, valor);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Error al buscar el estudio: " + e.getMessage(), e);
        }
    }

    @Override
    public int actualizar(Estudio estudio) throws PersistenciaException {
        String sql = "UPDATE estudios SET precio_base = ?, horas_ayuno = ? WHERE codigo = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setDouble(1, estudio.getPrecioBase());
            ps.setInt(2, estudio.getHorasAyuno());
            ps.setString(3, estudio.getCodigo());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Error al actualizar el estudio: " + e.getMessage(), e);
        }
    }

    /** Actualiza el precio de lista aplicando un porcentaje de aumento. */
    public int actualizarPrecios(double porcentaje) throws PersistenciaException {
        String sql = "UPDATE estudios SET precio_base = ROUND(precio_base * (1 + ? / 100), 2) WHERE activo = 1";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setDouble(1, porcentaje);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Error al actualizar precios: " + e.getMessage(), e);
        }
    }

    @Override
    public int eliminar(int id) throws PersistenciaException {
        String sql = "UPDATE estudios SET activo = 0 WHERE estudio_id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Error al dar de baja el estudio: " + e.getMessage(), e);
        }
    }

    /** Instancia la subclase concreta segun el discriminador "tipo". */
    private Estudio mapear(ResultSet rs) throws SQLException {
        String tipo = rs.getString("tipo");
        if ("ESPECIALIZADO".equals(tipo)) {
            return new EstudioEspecializado(
                    rs.getString("codigo"), rs.getString("nombre"), rs.getString("area"),
                    rs.getString("tipo_muestra"), rs.getDouble("precio_base"),
                    rs.getInt("horas_ayuno"), rs.getDouble("recargo_pct"),
                    rs.getBoolean("derivado"), rs.getInt("dias_derivacion"));
        }
        return new EstudioRutina(
                rs.getString("codigo"), rs.getString("nombre"), rs.getString("area"),
                rs.getString("tipo_muestra"), rs.getDouble("precio_base"),
                rs.getInt("horas_ayuno"), rs.getDouble("descuento_pct"));
    }
}
