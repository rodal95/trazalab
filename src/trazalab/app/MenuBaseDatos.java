package trazalab.app;

import java.sql.Connection;
import trazalab.excepciones.PersistenciaException;
import trazalab.mvc.ControladorTrazabilidad;
import trazalab.persistencia.ConexionBD;
import trazalab.util.Consola;

/**
 * Submenu del modulo persistente. Establece la conexion con MySQL y delega
 * cada opcion en el CONTROLADOR del patron MVC.
 *
 * Trabajo Practico 4 - Seminario de Practica Profesional.
 *
 * @author Alday, Rodrigo Matias
 */
public class MenuBaseDatos {

    /** Identificador del profesional que opera; en produccion vendria del login. */
    private static final int PROFESIONAL_ID = 1;

    public static void ejecutar() {
        Consola.titulo("Modulo de persistencia - MySQL");
        ConexionBD conexionBD = ConexionBD.getInstancia();
        System.out.println("  Conectando a " + conexionBD.getUrl().split("\\?")[0] + " ...");

        Connection conexion;
        try {
            conexion = conexionBD.obtenerConexion();
            System.out.println("  [OK] Conexion establecida. Validez: " + conexionBD.estaDisponible());
        } catch (PersistenciaException e) {
            System.err.println("  [X] " + e.getMessage());
            System.err.println("      Verifique que el servidor MySQL este iniciado y que la base");
            System.err.println("      'trazalab' exista (scripts sql/01-esquema.sql y sql/02-datos.sql).");
            System.out.println("\n  El resto del sistema sigue operativo en memoria.");
            return;
        }

        ControladorTrazabilidad controlador = new ControladorTrazabilidad(conexion, PROFESIONAL_ID);
        boolean continuar = true;
        while (continuar) {
            Consola.titulo("TRAZALAB - Modulo persistente (MVC + JDBC)");
            System.out.println("  1. Listar muestras en circuito");
            System.out.println("  2. Registrar evento de trazabilidad");
            System.out.println("  3. Consultar catalogo de practicas");
            System.out.println("  4. Actualizar precios de lista");
            System.out.println("  5. Indicadores de gestion");
            System.out.println("  6. Listar pacientes");
            System.out.println("  7. Exportar trazabilidad a archivo");
            System.out.println("  0. Volver al menu principal");
            Consola.separador();
            int opcion = Consola.leerEntero("  Elija una opcion: ", 0, 7);
            switch (opcion) {
                case 1 -> controlador.listarMuestrasEnCircuito();
                case 2 -> controlador.registrarCambioDeEstado();
                case 3 -> controlador.consultarCatalogo();
                case 4 -> controlador.actualizarPrecios();
                case 5 -> controlador.mostrarIndicadores();
                case 6 -> controlador.listarPacientes();
                case 7 -> controlador.exportarTrazabilidad();
                case 0 -> continuar = false;
                default -> System.out.println("  [!] Opcion inexistente.");
            }
        }

        try {
            conexionBD.cerrar();
            System.out.println("\n  [OK] Conexion cerrada correctamente.");
        } catch (PersistenciaException e) {
            System.err.println("  [X] " + e.getMessage());
        }
    }
}
