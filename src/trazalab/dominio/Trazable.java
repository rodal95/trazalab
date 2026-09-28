package trazalab.dominio;

import java.util.List;

/**
 * INTERFAZ que define el contrato de todo elemento cuyo recorrido debe quedar
 * documentado. Abstraccion completa: declara el que, no el como.
 *
 * @author Alday, Rodrigo Matias
 */
public interface Trazable {

    /** Identificador univoco del elemento trazado. */
    String getIdentificacion();

    /** Registra un evento en el historial del elemento. */
    void registrarEvento(EventoTraza evento);

    /** Devuelve el historial completo, del mas antiguo al mas reciente. */
    List<EventoTraza> getHistorial();

    /** Devuelve el ultimo evento registrado o null si no hay historial. */
    default EventoTraza getUltimoEvento() {
        List<EventoTraza> historial = getHistorial();
        return historial.isEmpty() ? null : historial.get(historial.size() - 1);
    }
}
