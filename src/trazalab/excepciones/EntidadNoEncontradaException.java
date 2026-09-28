package trazalab.excepciones;

/**
 * Excepcion propia VERIFICADA que se lanza cuando una busqueda no devuelve
 * resultados: paciente, estudio, orden o muestra inexistente.
 *
 * @author Alday, Rodrigo Matias
 */
public class EntidadNoEncontradaException extends Exception {

    /** Identificador de version para la serializacion de la excepcion. */
    private static final long serialVersionUID = 1L;

    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
