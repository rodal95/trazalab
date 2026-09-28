package trazalab.dominio;

import java.time.LocalDate;
import java.time.Period;

/**
 * Paciente del laboratorio. HERENCIA: extiende Persona y agrega los atributos
 * propios del dominio clinico.
 *
 * @author Alday, Rodrigo Matias
 */
public class Paciente extends Persona {

    private final LocalDate fechaNacimiento;
    private String obraSocial;
    private String telefono;

    public Paciente(String dni, String apellido, String nombre,
                    LocalDate fechaNacimiento, String obraSocial, String telefono) {
        // La primera linea invoca al constructor de la superclase.
        super(dni, apellido, nombre);
        this.fechaNacimiento = fechaNacimiento;
        this.obraSocial = obraSocial;
        this.telefono = telefono;
    }

    /** POLIMORFISMO: sobrescribe el metodo abstracto de Persona. */
    @Override
    public String obtenerRol() {
        return "Paciente";
    }

    /** Calcula la edad en anios cumplidos a la fecha actual. */
    public int getEdad() {
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    /** Un paciente es pediatrico si tiene menos de 15 anios. */
    public boolean esPediatrico() {
        return getEdad() < 15;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getObraSocial() {
        return obraSocial;
    }

    public void setObraSocial(String obraSocial) {
        this.obraSocial = obraSocial;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    @Override
    public String toString() {
        return String.format("%-28s DNI %-10s %3d anios  O.S.: %s",
                getNombreCompleto(), getDni(), getEdad(), obraSocial);
    }
}
