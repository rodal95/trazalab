package trazalab.excepciones;

/**
 * Excepcion propia VERIFICADA que impide extraer una muestra cuando el paciente
 * no cumple con las horas de ayuno exigidas por los estudios solicitados.
 *
 * @author Alday, Rodrigo Matias
 */
public class AyunoInsuficienteException extends Exception {

    /** Identificador de version para la serializacion de la excepcion. */
    private static final long serialVersionUID = 1L;

    private final int horasRequeridas;
    private final int horasDeclaradas;

    public AyunoInsuficienteException(String mensaje, int horasRequeridas, int horasDeclaradas) {
        super(mensaje);
        this.horasRequeridas = horasRequeridas;
        this.horasDeclaradas = horasDeclaradas;
    }

    public int getHorasRequeridas() {
        return horasRequeridas;
    }

    public int getHorasDeclaradas() {
        return horasDeclaradas;
    }
}
