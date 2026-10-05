package trazalab.servicio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import trazalab.dominio.EventoTraza;
import trazalab.dominio.Trazable;

/**
 * Bitacora inalterable de trazabilidad (RNF04). Es la clase de servicio que
 * registra y consulta los eventos de cada elemento Trazable: los elementos no
 * escriben su propio historial.
 *
 * Solo permite agregar eventos al final de la cadena. No ofrece operaciones para
 * modificarlos ni borrarlos, igual que la tabla trazas_muestra, protegida en la
 * base de datos por disparadores que rechazan UPDATE y DELETE.
 *
 * @author Alday, Rodrigo Matias
 */
public class BitacoraTrazabilidad {

    /** Eventos de cada elemento, por identificacion, del mas antiguo al mas reciente. */
    private final Map<String, List<EventoTraza>> eventos = new HashMap<>();

    /**
     * Agrega un evento al final del historial del elemento. La cadena tiene que
     * ser continua: el estado anterior del evento es el estado nuevo del ultimo
     * evento registrado (o null en el alta del elemento).
     *
     * @throws IllegalArgumentException si el evento no continua la cadena.
     */
    public void registrar(Trazable elemento, EventoTraza evento) {
        EventoTraza ultimo = ultimoEventoDe(elemento);
        boolean continuaLaCadena = ultimo == null
                ? evento.getEstadoAnterior() == null
                : evento.getEstadoAnterior() == ultimo.getEstadoNuevo();
        if (!continuaLaCadena) {
            throw new IllegalArgumentException("El evento no continua la cadena de "
                    + elemento.getIdentificacion() + ": la bitacora no admite saltos ni alteraciones.");
        }
        eventos.computeIfAbsent(elemento.getIdentificacion(), clave -> new ArrayList<>()).add(evento);
    }

    /** Devuelve el historial del elemento como vista no modificable. */
    public List<EventoTraza> historialDe(Trazable elemento) {
        List<EventoTraza> propios = eventos.get(elemento.getIdentificacion());
        return propios == null ? Collections.emptyList() : Collections.unmodifiableList(propios);
    }

    /** Devuelve el ultimo evento registrado del elemento, o null si no tiene historial. */
    public EventoTraza ultimoEventoDe(Trazable elemento) {
        List<EventoTraza> propios = eventos.get(elemento.getIdentificacion());
        return propios == null || propios.isEmpty() ? null : propios.get(propios.size() - 1);
    }
}
