package trazalab.excepciones;

/**
 * Excepcion NO verificada que envuelve una PersistenciaException cuando la
 * falla ocurre dentro de una regla de negocio cuya firma no la declara (version
 * web, con persistencia completa). El menu la captura en cada opcion y muestra
 * el mensaje sin cortar la sesion.
 *
 * @author Alday, Rodrigo Matias
 */
public class FallaDePersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FallaDePersistenciaException(PersistenciaException causa) {
        super(causa.getMessage(), causa);
    }
}
