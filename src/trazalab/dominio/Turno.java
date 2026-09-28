package trazalab.dominio;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Turno de extraccion otorgado a un paciente.
 *
 * @author Alday, Rodrigo Matias
 */
public class Turno {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String numero;
    private final Paciente paciente;
    private final LocalDateTime fechaHora;
    private final int box;
    private String estado;

    public Turno(String numero, Paciente paciente, LocalDateTime fechaHora, int box) {
        this.numero = numero;
        this.paciente = paciente;
        this.fechaHora = fechaHora;
        this.box = box;
        this.estado = "Otorgado";
    }

    public void confirmarPresente() {
        this.estado = "Presente";
    }

    public void marcarAtendido() {
        this.estado = "Atendido";
    }

    public String getNumero() {
        return numero;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public int getBox() {
        return box;
    }

    public String getEstado() {
        return estado;
    }

    @Override
    public String toString() {
        return String.format("%-8s %s  box %d  %-24s %s",
                numero, fechaHora.format(FMT), box, paciente.getNombreCompleto(), estado);
    }
}
