package trazalab.dominio;

/**
 * INTERFAZ que identifica a todo elemento cuyo recorrido se documenta en la
 * bitacora de trazabilidad. Abstraccion completa: declara el que, no el como.
 *
 * Solo exige que el elemento sepa identificarse. Registrar y consultar sus
 * eventos es responsabilidad de la bitacora (una clase de servicio), no del
 * elemento: las entidades no se gestionan a si mismas.
 *
 * @author Alday, Rodrigo Matias
 */
public interface Trazable {

    /** Identificador univoco del elemento trazado. */
    String getIdentificacion();
}
