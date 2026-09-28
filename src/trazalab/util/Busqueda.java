package trazalab.util;

import java.util.List;
import trazalab.dominio.Estudio;
import trazalab.dominio.Muestra;
import trazalab.dominio.Paciente;
import trazalab.excepciones.EntidadNoEncontradaException;

/**
 * Algoritmos de BUSQUEDA implementados de forma explicita.
 *
 * @author Alday, Rodrigo Matias
 */
public final class Busqueda {

    private Busqueda() {
    }

    /**
     * Busqueda BINARIA sobre un arreglo de estudios previamente ordenado por
     * codigo. Complejidad O(log n).
     *
     * @throws EntidadNoEncontradaException si el codigo no existe en el catalogo.
     */
    public static Estudio binariaPorCodigo(Estudio[] estudios, String codigo)
            throws EntidadNoEncontradaException {
        int inicio = 0;
        int fin = estudios.length - 1;
        while (inicio <= fin) {
            int medio = (inicio + fin) / 2;
            int comparacion = estudios[medio].getCodigo().compareToIgnoreCase(codigo);
            if (comparacion == 0) {
                return estudios[medio];
            } else if (comparacion < 0) {
                inicio = medio + 1;
            } else {
                fin = medio - 1;
            }
        }
        throw new EntidadNoEncontradaException("No existe un estudio con codigo " + codigo + ".");
    }

    /**
     * Busqueda LINEAL de un paciente por documento. Complejidad O(n).
     *
     * @throws EntidadNoEncontradaException si el paciente no esta registrado.
     */
    public static Paciente linealPorDni(List<Paciente> pacientes, String dni)
            throws EntidadNoEncontradaException {
        for (Paciente paciente : pacientes) {
            if (paciente.getDni().equals(dni)) {
                return paciente;
            }
        }
        throw new EntidadNoEncontradaException("No hay un paciente registrado con DNI " + dni + ".");
    }

    /** Busqueda lineal de una muestra por su codigo de barra. */
    public static Muestra linealPorCodigoBarra(List<Muestra> muestras, String codigoBarra)
            throws EntidadNoEncontradaException {
        for (Muestra muestra : muestras) {
            if (muestra.getCodigoBarra().equalsIgnoreCase(codigoBarra)) {
                return muestra;
            }
        }
        throw new EntidadNoEncontradaException("No existe la muestra " + codigoBarra + ".");
    }
}
