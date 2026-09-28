package trazalab.util;

import java.util.List;
import trazalab.dominio.Estudio;
import trazalab.dominio.Resultado;

/**
 * Algoritmos de ORDENACION implementados de forma explicita, sin delegar en
 * Collections.sort(), para evidenciar el trabajo con estructuras y arreglos.
 *
 * @author Alday, Rodrigo Matias
 */
public final class Ordenamiento {

    /** Clase de utilidad: no se instancia. */
    private Ordenamiento() {
    }

    /**
     * Ordenamiento por insercion sobre un ARREGLO de estudios, por codigo.
     * Se usa para dejar el catalogo listo para la busqueda binaria.
     * Complejidad O(n^2), adecuada para el tamanio del catalogo del prototipo.
     */
    public static void insercionPorCodigo(Estudio[] estudios) {
        for (int i = 1; i < estudios.length; i++) {
            Estudio actual = estudios[i];
            int j = i - 1;
            while (j >= 0 && estudios[j].getCodigo().compareTo(actual.getCodigo()) > 0) {
                estudios[j + 1] = estudios[j];
                j--;
            }
            estudios[j + 1] = actual;
        }
    }

    /**
     * Quicksort sobre un ARREGLO de estudios, ordenando por precio final
     * de mayor a menor. Complejidad promedio O(n log n).
     */
    public static void quickSortPorPrecioDesc(Estudio[] estudios, int inicio, int fin) {
        if (inicio < fin) {
            int posicionPivote = particionar(estudios, inicio, fin);
            quickSortPorPrecioDesc(estudios, inicio, posicionPivote - 1);
            quickSortPorPrecioDesc(estudios, posicionPivote + 1, fin);
        }
    }

    private static int particionar(Estudio[] estudios, int inicio, int fin) {
        double pivote = estudios[fin].calcularPrecio();
        int i = inicio - 1;
        for (int j = inicio; j < fin; j++) {
            if (estudios[j].calcularPrecio() > pivote) {
                i++;
                intercambiar(estudios, i, j);
            }
        }
        intercambiar(estudios, i + 1, fin);
        return i + 1;
    }

    private static void intercambiar(Estudio[] estudios, int a, int b) {
        Estudio temporal = estudios[a];
        estudios[a] = estudios[b];
        estudios[b] = temporal;
    }

    /**
     * Ordenamiento burbuja optimizado sobre una LISTA de resultados, usando el
     * orden natural definido en Resultado.compareTo() (por codigo de estudio).
     */
    public static void burbujaResultados(List<Resultado> resultados) {
        boolean huboIntercambio = true;
        int n = resultados.size();
        while (huboIntercambio) {
            huboIntercambio = false;
            for (int i = 0; i < n - 1; i++) {
                if (resultados.get(i).compareTo(resultados.get(i + 1)) > 0) {
                    Resultado temporal = resultados.get(i);
                    resultados.set(i, resultados.get(i + 1));
                    resultados.set(i + 1, temporal);
                    huboIntercambio = true;
                }
            }
            n--;
        }
    }
}
