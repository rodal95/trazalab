package trazalab.excepciones;

/**
 * Excepcion NO verificada de la version web: el usuario escribio "volver" en un dato
 * para cancelar la operacion. La captura el menu desde el que se entro, que se vuelve
 * a mostrar; nada de la operacion cancelada se registra.
 *
 * @author Alday, Rodrigo Matias
 */
public class VolverAlMenuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VolverAlMenuException() {
        super("Operacion cancelada por el usuario.");
    }
}
