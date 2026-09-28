package trazalab.servicio;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import trazalab.dominio.Paciente;
import trazalab.dominio.Turno;
import trazalab.excepciones.TurnoNoDisponibleException;

/**
 * Agenda de turnos del laboratorio. Utiliza una COLA (FIFO) para la sala de
 * espera: el primer paciente que se presenta es el primero en ser atendido.
 *
 * @author Alday, Rodrigo Matias
 */
public class AgendaTurnos {

    /** Cupo maximo de turnos por franja horaria (boxes de extraccion). */
    private static final int CUPO_POR_FRANJA = 3;
    private static final int HORA_APERTURA = 7;
    private static final int HORA_CIERRE = 11;

    private final List<Turno> turnosOtorgados;
    /** ESTRUCTURA COLA: sala de espera, atencion por orden de llegada. */
    private final Queue<Turno> salaDeEspera;
    private int contadorTurnos;

    public AgendaTurnos() {
        this.turnosOtorgados = new ArrayList<>();
        this.salaDeEspera = new ArrayDeque<>();
        this.contadorTurnos = 0;
    }

    /**
     * Otorga un turno validando franja horaria y cupo disponible.
     *
     * @throws TurnoNoDisponibleException si esta fuera del horario o sin cupo.
     */
    public Turno otorgarTurno(Paciente paciente, LocalDateTime fechaHora)
            throws TurnoNoDisponibleException {
        int hora = fechaHora.getHour();
        if (hora < HORA_APERTURA || hora >= HORA_CIERRE) {
            throw new TurnoNoDisponibleException("El laboratorio extrae de "
                    + HORA_APERTURA + ":00 a " + HORA_CIERRE + ":00. Horario solicitado: " + hora + ":00.");
        }
        int ocupados = contarTurnosEnFranja(fechaHora);
        if (ocupados >= CUPO_POR_FRANJA) {
            throw new TurnoNoDisponibleException("La franja de las " + hora
                    + ":00 ya tiene " + ocupados + " turnos y el cupo es de " + CUPO_POR_FRANJA + ".");
        }
        contadorTurnos++;
        int box = ocupados + 1;
        Turno turno = new Turno(String.format("T-%04d", contadorTurnos), paciente, fechaHora, box);
        turnosOtorgados.add(turno);
        return turno;
    }

    /** Cuenta los turnos ya otorgados en la misma fecha y hora. */
    private int contarTurnosEnFranja(LocalDateTime fechaHora) {
        int contador = 0;
        for (Turno turno : turnosOtorgados) {
            if (turno.getFechaHora().getHour() == fechaHora.getHour()
                    && turno.getFechaHora().toLocalDate().equals(fechaHora.toLocalDate())) {
                contador++;
            }
        }
        return contador;
    }

    /** El paciente se presenta: ingresa al final de la cola de espera. */
    public void registrarPresente(Turno turno) {
        turno.confirmarPresente();
        salaDeEspera.offer(turno);
    }

    /** Llama al siguiente paciente: sale el primero de la cola (FIFO). */
    public Turno llamarSiguiente() throws TurnoNoDisponibleException {
        Turno turno = salaDeEspera.poll();
        if (turno == null) {
            throw new TurnoNoDisponibleException("No hay pacientes en la sala de espera.");
        }
        turno.marcarAtendido();
        return turno;
    }

    public int getPacientesEsperando() {
        return salaDeEspera.size();
    }

    public Queue<Turno> getSalaDeEspera() {
        return salaDeEspera;
    }

    public List<Turno> getTurnosOtorgados() {
        return turnosOtorgados;
    }
}
