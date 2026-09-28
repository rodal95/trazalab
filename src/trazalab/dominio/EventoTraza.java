package trazalab.dominio;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro inmutable de un cambio de estado de una muestra.
 * Es la unidad de informacion que da nombre al sistema: la trazabilidad.
 *
 * @author Alday, Rodrigo Matias
 */
public class EventoTraza {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final EstadoMuestra estadoAnterior;
    private final EstadoMuestra estadoNuevo;
    private final LocalDateTime fechaHora;
    private final String responsable;
    private final String observacion;

    public EventoTraza(EstadoMuestra estadoAnterior, EstadoMuestra estadoNuevo,
                       String responsable, String observacion) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.fechaHora = LocalDateTime.now();
        this.responsable = responsable;
        this.observacion = observacion;
    }

    public EstadoMuestra getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoMuestra getEstadoNuevo() {
        return estadoNuevo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getResponsable() {
        return responsable;
    }

    public String getObservacion() {
        return observacion;
    }

    @Override
    public String toString() {
        return String.format("%s  %-10s -> %-10s  por %-18s %s",
                fechaHora.format(FMT),
                estadoAnterior == null ? "(alta)" : estadoAnterior,
                estadoNuevo, responsable,
                observacion == null || observacion.isEmpty() ? "" : "| " + observacion);
    }
}
