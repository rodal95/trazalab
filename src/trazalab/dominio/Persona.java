package trazalab.dominio;

/**
 * Clase abstracta que generaliza a toda persona registrada en el sistema.
 * Aplica ABSTRACCION: define el estado y el comportamiento comun de pacientes
 * y profesionales, pero no tiene sentido instanciarla por si misma.
 *
 * @author Alday, Rodrigo Matias
 */
public abstract class Persona {

    // ENCAPSULAMIENTO: los atributos son privados y solo se accede por metodos.
    private final String dni;
    private String apellido;
    private String nombre;

    /**
     * Constructor con parametros. Inicializa el estado comun de toda persona.
     */
    public Persona(String dni, String apellido, String nombre) {
        this.dni = dni;
        this.apellido = apellido;
        this.nombre = nombre;
    }

    /** Metodo abstracto: cada subclase define como se identifica ante el sistema. */
    public abstract String obtenerRol();

    public String getDni() {
        return dni;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** Devuelve el nombre completo en formato "Apellido, Nombre". */
    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }

    @Override
    public String toString() {
        return getNombreCompleto() + " (DNI " + dni + ") - " + obtenerRol();
    }
}
