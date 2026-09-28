package trazalab.dominio;

/**
 * Profesional que opera el sistema: recepcionista, extraccionista o bioquimico.
 * HERENCIA: extiende Persona.
 *
 * @author Alday, Rodrigo Matias
 */
public class Profesional extends Persona {

    private final String matricula;
    private final String rol;

    public Profesional(String dni, String apellido, String nombre, String matricula, String rol) {
        super(dni, apellido, nombre);
        this.matricula = matricula;
        this.rol = rol;
    }

    @Override
    public String obtenerRol() {
        return rol;
    }

    public String getMatricula() {
        return matricula;
    }

    /** Solo el bioquimico esta habilitado a validar y liberar un informe. */
    public boolean puedeValidarResultados() {
        return "Bioquimico".equalsIgnoreCase(rol);
    }

    @Override
    public String toString() {
        return String.format("%-28s Mat. %-8s %s", getNombreCompleto(), matricula, rol);
    }
}
