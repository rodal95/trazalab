package trazalab.dominio;

import java.time.LocalDateTime;

/**
 * Muestra biologica extraida a un paciente. IMPLEMENTA la interfaz Trazable:
 * su recorrido queda documentado en la bitacora de trazabilidad.
 *
 * Es una entidad: guarda sus datos y los expone. No cambia su propio estado ni
 * escribe su historial; eso lo hace la clase de control (Laboratorio), que
 * valida cada transicion y la registra en la bitacora.
 *
 * @author Alday, Rodrigo Matias
 */
public class Muestra implements Trazable {

    private final String codigoBarra;
    private final String tipoMuestra;
    private final Orden orden;
    private EstadoMuestra estado;
    private LocalDateTime fechaExtraccion;

    /** Toda muestra nace GENERADA: el rotulo existe antes que la extraccion. */
    public Muestra(String codigoBarra, String tipoMuestra, Orden orden) {
        this.codigoBarra = codigoBarra;
        this.tipoMuestra = tipoMuestra;
        this.orden = orden;
        this.estado = EstadoMuestra.GENERADA;
    }

    @Override
    public String getIdentificacion() {
        return codigoBarra;
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

    public void setEstado(EstadoMuestra estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaExtraccion() {
        return fechaExtraccion;
    }

    public void setFechaExtraccion(LocalDateTime fechaExtraccion) {
        this.fechaExtraccion = fechaExtraccion;
    }

    @Override
    public String toString() {
        return String.format("%-12s %-10s orden %-8s %-12s (%s)",
                codigoBarra, tipoMuestra, orden.getNumero(), estado, estado.getDescripcion());
    }
}
