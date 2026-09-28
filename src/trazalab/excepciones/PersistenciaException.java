package trazalab.excepciones;

/**
 * Excepcion propia que envuelve a las SQLException de la capa de datos.
 * De este modo la capa de negocio y la vista nunca dependen de java.sql:
 * el detalle tecnico queda encapsulado dentro de la capa de persistencia.
 *
 * @author Alday, Rodrigo Matias
 */
public class PersistenciaException extends Exception {

    /** Identificador de version para la serializacion de la excepcion. */
    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    /** Conserva la excepcion original como causa, para no perder el diagnostico. */
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
