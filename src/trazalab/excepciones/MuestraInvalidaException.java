package trazalab.excepciones;

/**
 * Excepcion propia VERIFICADA que se lanza ante una operacion no permitida sobre
 * una muestra: una transicion de estado fuera del circuito, una extraccion
 * repetida, un resultado cargado dos veces o una validacion prematura.
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
