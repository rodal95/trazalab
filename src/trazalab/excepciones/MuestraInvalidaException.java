package trazalab.excepciones;

/**
 * Excepcion propia VERIFICADA que se lanza ante una transicion de estado no
 * permitida en el circuito de la muestra.
 *
 * @author Alday, Rodrigo Matias
 */
public class MuestraInvalidaException extends Exception {

    /** Identificador de version para la serializacion de la excepcion. */
    private static final long serialVersionUID = 1L;

    public MuestraInvalidaException(String mensaje) {
        super(mensaje);
    }
}
