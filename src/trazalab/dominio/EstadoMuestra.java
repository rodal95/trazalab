package trazalab.dominio;

/**
 * Estados posibles del circuito de una muestra. El enum garantiza que una
 * muestra solo pueda encontrarse en un estado valido del proceso.
 *
 * @author Alday, Rodrigo Matias
 */
public enum EstadoMuestra {

    GENERADA("Rotulo generado, pendiente de extraccion"),
    EXTRAIDA("Extraida al paciente"),
    EN_PROCESO("En proceso analitico"),
    ANALIZADA("Analizada, pendiente de validacion"),
    INFORMADA("Informada y liberada"),
    ANULADA("Anulada / requiere reextraccion");

    private final String descripcion;

    EstadoMuestra(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Define las transiciones validas del circuito preanalitico y analitico.
     * Desde cualquier estado no final se puede anular.
     */
    public boolean puedeTransicionarA(EstadoMuestra destino) {
        if (this == INFORMADA || this == ANULADA) {
            return false;
        }
        if (destino == ANULADA) {
            return true;
        }
        return destino.ordinal() == this.ordinal() + 1;
    }
}
