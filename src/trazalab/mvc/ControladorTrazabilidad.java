package trazalab.mvc;

import java.sql.Connection;
import java.util.List;
import trazalab.dominio.EstadoMuestra;
import trazalab.dominio.Estudio;
import trazalab.excepciones.PersistenciaException;
import trazalab.persistencia.MuestraDAO;
import trazalab.util.ExportadorArchivos;
import trazalab.util.Ordenamiento;

/**
 * CONTROLADOR del patron MVC. Recibe los eventos que provienen del usuario y
 * los traduce en consultas al modelo o en pedidos a la vista. Es la unica clase
 * que conoce a las otras dos.
 *
 * @author Alday, Rodrigo Matias
 */
public class ControladorTrazabilidad {

    private final ModeloTrazabilidad modelo;
    private final VistaTrazabilidad vista;
    private final int profesionalId;

    public ControladorTrazabilidad(Connection conexion, int profesionalId) {
        this.modelo = new ModeloTrazabilidad(conexion);
        this.vista = new VistaTrazabilidad();
        this.profesionalId = profesionalId;
    }

    /** Caso de uso: consultar el estado de las muestras en circuito. */
    public void listarMuestrasEnCircuito() {
        vista.mostrarTitulo("Muestras en circuito (consulta a MySQL)");
        try {
            vista.mostrarMuestras(modelo.obtenerMuestrasEnCircuito());
        } catch (PersistenciaException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    /** Caso de uso: registrar un cambio de estado con su traza. */
    public void registrarCambioDeEstado() {
        vista.mostrarTitulo("Registrar evento de trazabilidad");
        try {
            List<MuestraDAO.FilaMuestra> muestras = modelo.obtenerMuestrasEnCircuito();
            vista.mostrarMuestras(muestras);
            if (muestras.isEmpty()) {
                return;
            }
            String codigo = vista.solicitarCodigoMuestra();
            MuestraDAO.FilaMuestra muestra = modelo.buscarMuestra(codigo);
            if (muestra == null) {
                vista.mostrarError("No existe una muestra con codigo " + codigo + ".");
                return;
            }
            vista.mostrarHistorial(muestra.codigoBarra, modelo.obtenerHistorial(muestra.codigoBarra));
            EstadoMuestra destino = vista.solicitarEstadoDestino();
            String observacion = vista.solicitarObservacion();
            modelo.registrarCambioDeEstado(muestra, destino, profesionalId, observacion);
            vista.mostrarExito("Evento registrado en la base de datos. Muestra "
                    + muestra.codigoBarra + " en estado " + destino + ".");
            vista.mostrarHistorial(muestra.codigoBarra, modelo.obtenerHistorial(muestra.codigoBarra));
        } catch (PersistenciaException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    /** Caso de uso: consultar el catalogo persistido y ordenarlo. */
    public void consultarCatalogo() {
        vista.mostrarTitulo("Catalogo de practicas (consulta a MySQL)");
        try {
            // La consulta devuelve un ArrayList; se lo convierte a ARREGLO para
            // aplicar el algoritmo de ordenacion propio.
            Estudio[] catalogo = modelo.obtenerCatalogoComoArreglo();
            if (catalogo.length == 0) {
                vista.mostrarMensaje("El catalogo esta vacio. Ejecute el script 02-datos.sql.");
                return;
            }
            Ordenamiento.quickSortPorPrecioDesc(catalogo, 0, catalogo.length - 1);
            vista.mostrarMensaje("Catalogo ordenado por precio descendente (quicksort sobre arreglo):");
            vista.mostrarCatalogo(catalogo);
            vista.mostrarMensaje("");
            vista.mostrarMensaje("Total de practicas activas: " + catalogo.length);
        } catch (PersistenciaException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    /** Caso de uso: actualizar registros en la base (UPDATE masivo). */
    public void actualizarPrecios() {
        vista.mostrarTitulo("Actualizacion de precios de lista");
        try {
            double porcentaje = vista.solicitarPorcentaje();
            if (!vista.confirmar("Confirma aplicar un " + porcentaje + " % a todo el catalogo?")) {
                vista.mostrarMensaje("Operacion cancelada.");
                return;
            }
            int filas = modelo.actualizarPreciosDeLista(porcentaje);
            vista.mostrarExito(filas + " practica(s) actualizada(s) en la base de datos.");
            vista.mostrarCatalogo(modelo.obtenerCatalogoComoArreglo());
        } catch (PersistenciaException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    /** Caso de uso: indicadores de gestion calculados por el motor. */
    public void mostrarIndicadores() {
        vista.mostrarTitulo("Indicadores de gestion (GROUP BY en MySQL)");
        try {
            vista.mostrarLineas(modelo.obtenerIndicadorPorEstado());
        } catch (PersistenciaException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    /** Caso de uso: exportar la trazabilidad de una muestra a un archivo. */
    public void exportarTrazabilidad() {
        vista.mostrarTitulo("Exportar trazabilidad a archivo");
        try {
            String codigo = vista.solicitarCodigoMuestra();
            MuestraDAO.FilaMuestra muestra = modelo.buscarMuestra(codigo);
            if (muestra == null) {
                vista.mostrarError("No existe una muestra con codigo " + codigo + ".");
                return;
            }
            List<String> historial = modelo.obtenerHistorial(codigo);
            String ruta = ExportadorArchivos.exportarTrazabilidad(muestra.codigoBarra,
                    muestra.paciente, muestra.orden, historial);
            vista.mostrarExito("Archivo generado: " + ruta);
        } catch (PersistenciaException e) {
            vista.mostrarError(e.getMessage());
        } catch (java.io.IOException e) {
            vista.mostrarError("No se pudo escribir el archivo: " + e.getMessage());
        }
    }

    public void listarPacientes() {
        vista.mostrarTitulo("Pacientes registrados en la base de datos");
        try {
            List<String> filas = modelo.obtenerPacientesFormateados();
            if (filas.isEmpty()) {
                vista.mostrarMensaje("No hay pacientes cargados.");
            } else {
                vista.mostrarLineas(filas);
                vista.mostrarMensaje("");
                vista.mostrarMensaje("Total: " + filas.size() + " paciente(s).");
            }
        } catch (PersistenciaException e) {
            vista.mostrarError(e.getMessage());
        }
    }
}
