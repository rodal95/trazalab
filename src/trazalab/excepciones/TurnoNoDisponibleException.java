package trazalab.excepciones;

/**
 * Excepcion propia VERIFICADA que se lanza cuando no es posible otorgar un turno
 * en el horario solicitado (fuera de agenda o cupo completo).
 *
 * @author Alday, Rodrigo Matias
 */
public class TurnoNoDisponibleException extends Exception {

    /** Identificador de version para la serializacion de la excepcion. */
    private static final long serialVersionUID = 1L;

    public TurnoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
