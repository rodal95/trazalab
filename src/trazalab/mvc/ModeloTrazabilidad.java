package trazalab.mvc;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import trazalab.dominio.EstadoMuestra;
import trazalab.dominio.Estudio;
import trazalab.excepciones.PersistenciaException;
import trazalab.persistencia.EstudioDAO;
import trazalab.persistencia.MuestraDAO;
import trazalab.persistencia.PacienteDAO;

/**
 * MODELO del patron MVC. Es el nucleo funcional que gestiona los datos
 * manipulados por la aplicacion: no dibuja nada por pantalla ni lee del teclado.
 * Accede a la base de datos delegando en los DAO.
 *
 * @author Alday, Rodrigo Matias
 */
public class ModeloTrazabilidad {

    private final MuestraDAO muestraDAO;
    private final EstudioDAO estudioDAO;
    private final PacienteDAO pacienteDAO;

    public ModeloTrazabilidad(Connection conexion) {
        this.muestraDAO = new MuestraDAO(conexion);
        this.estudioDAO = new EstudioDAO(conexion);
        this.pacienteDAO = new PacienteDAO(conexion);
    }

    public List<MuestraDAO.FilaMuestra> obtenerMuestrasEnCircuito() throws PersistenciaException {
        return muestraDAO.listarEnCircuito();
    }

    public MuestraDAO.FilaMuestra buscarMuestra(String codigoBarra) throws PersistenciaException {
        return muestraDAO.buscarPorCodigo(codigoBarra);
    }

    /**
     * Regla de negocio aplicada antes de persistir: solo se aceptan las
     * transiciones validas del circuito, definidas en el enum del dominio.
     */
    public void registrarCambioDeEstado(MuestraDAO.FilaMuestra muestra, EstadoMuestra destino,
                                        int profesionalId, String observacion)
            throws PersistenciaException {
        EstadoMuestra actual = EstadoMuestra.valueOf(muestra.estado);
        if (!actual.puedeTransicionarA(destino)) {
            throw new PersistenciaException("La muestra " + muestra.codigoBarra + " esta en estado "
                    + actual + " y no puede pasar a " + destino + ".");
        }
        muestraDAO.cambiarEstado(muestra.id, actual, destino, profesionalId, observacion);
    }

    public List<String> obtenerHistorial(String codigoBarra) throws PersistenciaException {
        return muestraDAO.obtenerHistorial(codigoBarra);
    }

    public List<String> obtenerIndicadorPorEstado() throws PersistenciaException {
        return muestraDAO.contarPorEstado();
    }

    /**
     * Devuelve el catalogo en las dos estructuras a la vez: el ARREGLO para
     * aplicar los algoritmos y la LISTA para recorrerlo en la vista.
     */
    public Estudio[] obtenerCatalogoComoArreglo() throws PersistenciaException {
        return estudioDAO.listarComoArreglo();
    }

    public List<Estudio> obtenerCatalogo() throws PersistenciaException {
        return estudioDAO.listar();
    }

    public Estudio buscarEstudioPorCodigo(String codigo) throws PersistenciaException {
        return estudioDAO.buscarPorCodigo(codigo);
    }

    public int actualizarPreciosDeLista(double porcentaje) throws PersistenciaException {
        return estudioDAO.actualizarPrecios(porcentaje);
    }

    public List<String> obtenerPacientesFormateados() throws PersistenciaException {
        List<String> filas = new ArrayList<>();
        pacienteDAO.listar().forEach(p -> filas.add("  " + p));
        return filas;
    }

    public PacienteDAO getPacienteDAO() {
        return pacienteDAO;
    }
}
