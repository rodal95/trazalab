package trazalab.dominio;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import trazalab.excepciones.MuestraInvalidaException;

/**
 * Muestra biologica extraida a un paciente. IMPLEMENTA la interfaz Trazable:
 * cada cambio de estado queda documentado en su historial.
 *
 * @author Alday, Rodrigo Matias
 */
public class Muestra implements Trazable {

    private final String codigoBarra;
    private final String tipoMuestra;
    private final Orden orden;
    private EstadoMuestra estado;
    private LocalDateTime fechaExtraccion;
    private final List<EventoTraza> historial;

    public Muestra(String codigoBarra, String tipoMuestra, Orden orden, String responsable) {
        this.codigoBarra = codigoBarra;
        this.tipoMuestra = tipoMuestra;
        this.orden = orden;
        this.estado = EstadoMuestra.GENERADA;
        this.historial = new ArrayList<>();
        // Se agrega directamente a la coleccion y no mediante registrarEvento(), que es
        // sobrescribible: invocar un metodo redefinible desde el constructor dejaria
        // expuesto un objeto todavia no construido por completo.
        this.historial.add(new EventoTraza(null, EstadoMuestra.GENERADA, responsable,
                "Rotulo generado para la orden " + orden.getNumero()));
    }

    /**
     * Cambia el estado de la muestra validando la transicion.
     *
     * @throws MuestraInvalidaException si la transicion no es valida en el circuito.
     */
    public void cambiarEstado(EstadoMuestra destino, String responsable, String observacion)
            throws MuestraInvalidaException {
        if (!estado.puedeTransicionarA(destino)) {
            throw new MuestraInvalidaException("La muestra " + codigoBarra
                    + " esta en estado " + estado + " y no puede pasar a " + destino + ".");
        }
        EventoTraza evento = new EventoTraza(estado, destino, responsable, observacion);
        this.estado = destino;
        if (destino == EstadoMuestra.EXTRAIDA) {
            this.fechaExtraccion = LocalDateTime.now();
        }
        registrarEvento(evento);
    }

    /**
     * Tiempo de respuesta (TAT) en minutos entre la extraccion y el evento actual.
     * Devuelve -1 si la muestra todavia no fue extraida.
     */
    public long calcularTatMinutos() {
        if (fechaExtraccion == null) {
            return -1;
        }
        EventoTraza ultimo = getUltimoEvento();
        return Duration.between(fechaExtraccion, ultimo.getFechaHora()).toMinutes();
    }

    @Override
    public String getIdentificacion() {
        return codigoBarra;
    }

    @Override
    public void registrarEvento(EventoTraza evento) {
        historial.add(evento);
    }

    /** Devuelve una vista no modificable: protege el historial de alteraciones. */
    @Override
    public List<EventoTraza> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public String getTipoMuestra() {
        return tipoMuestra;
    }

    public Orden getOrden() {
        return orden;
    }

    public EstadoMuestra getEstado() {
        return estado;
    }

    public LocalDateTime getFechaExtraccion() {
        return fechaExtraccion;
    }

    @Override
    public String toString() {
        return String.format("%-12s %-10s orden %-8s %-12s (%s)",
                codigoBarra, tipoMuestra, orden.getNumero(), estado, estado.getDescripcion());
    }
}
