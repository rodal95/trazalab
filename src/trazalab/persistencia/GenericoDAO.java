package trazalab.persistencia;

import java.util.List;
import trazalab.excepciones.PersistenciaException;

/**
 * INTERFAZ generica que define el contrato de todo objeto de acceso a datos.
 * Es el punto donde la abstraccion favorece la reutilizacion de metodos: todos
 * los DAO del sistema exponen la misma firma de operaciones basicas.
 *
 * @param <T> tipo de entidad del dominio que administra el DAO.
 * @author Alday, Rodrigo Matias
 */
public interface GenericoDAO<T> {

    /** Inserta una entidad y devuelve el identificador generado. */
    int insertar(T entidad) throws PersistenciaException;

    /** Devuelve todas las entidades de la tabla. */
    List<T> listar() throws PersistenciaException;

    /** Busca una entidad por su clave primaria; devuelve null si no existe. */
    T buscarPorId(int id) throws PersistenciaException;

    /** Actualiza una entidad existente; devuelve la cantidad de filas afectadas. */
    int actualizar(T entidad) throws PersistenciaException;

    /** Elimina una entidad por su clave primaria. */
    int eliminar(int id) throws PersistenciaException;
}
