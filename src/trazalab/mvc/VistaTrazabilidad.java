package trazalab.mvc;

import java.util.List;
import trazalab.dominio.EstadoMuestra;
import trazalab.dominio.Estudio;
import trazalab.persistencia.MuestraDAO;
import trazalab.util.Consola;

/**
 * VISTA del patron MVC. Unico componente que presenta informacion al usuario y
 * solicita datos. No conoce la base de datos ni las reglas de negocio: si
 * manana la interfaz pasa a ser grafica, solo se reemplaza esta clase.
 *
 * @author Alday, Rodrigo Matias
 */
public class VistaTrazabilidad {

    public void mostrarTitulo(String texto) {
        Consola.titulo(texto);
    }

    public void mostrarMuestras(List<MuestraDAO.FilaMuestra> muestras) {
        if (muestras.isEmpty()) {
            System.out.println("  No hay muestras dentro del circuito.");
            return;
        }
        System.out.printf("  %-12s %-14s %-12s %-8s %s%n",
                "CODIGO", "TIPO", "ESTADO", "ORDEN", "PACIENTE");
        Consola.separador();
        for (MuestraDAO.FilaMuestra m : muestras) {
            System.out.printf("  %-12s %-14s %-12s %-8s %s%n",
                    m.codigoBarra, m.tipoMuestra, m.estado, m.orden, m.paciente);
        }
    }

    public void mostrarHistorial(String codigoBarra, List<String> historial) {
        System.out.println("\n  Trazabilidad de la muestra " + codigoBarra + ":");
        Consola.separador();
        if (historial.isEmpty()) {
            System.out.println("  Sin eventos registrados.");
        } else {
            historial.forEach(linea -> System.out.println("  " + linea));
        }
    }

    public void mostrarCatalogo(Estudio[] catalogo) {
        System.out.printf("  %-8s %-34s %-18s %12s%n", "CODIGO", "PRACTICA", "AREA", "PRECIO");
        Consola.separador();
        for (Estudio estudio : catalogo) {
            System.out.printf("  %-8s %-34s %-18s %12.2f%n",
                    estudio.getCodigo(), estudio.getNombre(), estudio.getArea(), estudio.calcularPrecio());
        }
    }

    public void mostrarLineas(List<String> lineas) {
        lineas.forEach(System.out::println);
    }

    public void mostrarMensaje(String mensaje) {
        System.out.println("  " + mensaje);
    }

    public void mostrarExito(String mensaje) {
        System.out.println("  [OK] " + mensaje);
    }

    public void mostrarError(String mensaje) {
        System.err.println("  [X] " + mensaje);
    }

    public String solicitarCodigoMuestra() {
        return Consola.leerTexto("  Codigo de barra de la muestra: ");
    }

    public String solicitarObservacion() {
        return Consola.leerTexto("  Observacion del evento: ");
    }

    /** Presenta los estados posibles y devuelve el elegido por el operador. */
    public EstadoMuestra solicitarEstadoDestino() {
        System.out.println("\n  Estados del circuito:");
        EstadoMuestra[] estados = EstadoMuestra.values();
        for (int i = 0; i < estados.length; i++) {
            System.out.println("    " + (i + 1) + ". " + estados[i] + " - " + estados[i].getDescripcion());
        }
        int opcion = Consola.leerEntero("  Nuevo estado: ", 1, estados.length);
        return estados[opcion - 1];
    }

    public double solicitarPorcentaje() {
        return Consola.leerDecimal("  Porcentaje de aumento a aplicar: ");
    }

    public boolean confirmar(String mensaje) {
        return Consola.confirmar("  " + mensaje);
    }
}
