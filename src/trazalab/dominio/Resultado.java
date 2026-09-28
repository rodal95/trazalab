package trazalab.dominio;

import java.time.LocalDateTime;

/**
 * Resultado cargado para un estudio de una muestra determinada.
 *
 * @author Alday, Rodrigo Matias
 */
public class Resultado implements Comparable<Resultado> {

    private final Muestra muestra;
    private final Estudio estudio;
    private final double valor;
    private final String unidad;
    private final double referenciaMin;
    private final double referenciaMax;
    private final LocalDateTime fechaCarga;
    private boolean validado;
    private String validadoPor;

    public Resultado(Muestra muestra, Estudio estudio, double valor, String unidad,
                     double referenciaMin, double referenciaMax) {
        this.muestra = muestra;
        this.estudio = estudio;
        this.valor = valor;
        this.unidad = unidad;
        this.referenciaMin = referenciaMin;
        this.referenciaMax = referenciaMax;
        this.fechaCarga = LocalDateTime.now();
        this.validado = false;
    }

    /** Un resultado esta fuera de rango si cae por debajo o por encima de la referencia. */
    public boolean estaFueraDeRango() {
        return valor < referenciaMin || valor > referenciaMax;
    }

    /** Marca critica: duplica el limite superior o cae a menos de la mitad del inferior. */
    public boolean esCritico() {
        return valor > referenciaMax * 2 || valor < referenciaMin / 2;
    }

    public void validar(Profesional bioquimico) {
        this.validado = true;
        this.validadoPor = bioquimico.getNombreCompleto() + " (Mat. " + bioquimico.getMatricula() + ")";
    }

    /** Orden natural: por codigo de estudio, para ordenar el informe. */
    @Override
    public int compareTo(Resultado otro) {
        return this.estudio.getCodigo().compareTo(otro.estudio.getCodigo());
    }

    public Muestra getMuestra() {
        return muestra;
    }

    public Estudio getEstudio() {
        return estudio;
    }

    public double getValor() {
        return valor;
    }

    public String getUnidad() {
        return unidad;
    }

    public double getReferenciaMin() {
        return referenciaMin;
    }

    public double getReferenciaMax() {
        return referenciaMax;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public boolean isValidado() {
        return validado;
    }

    public String getValidadoPor() {
        return validadoPor;
    }

    @Override
    public String toString() {
        String marca = esCritico() ? "  ** CRITICO **" : (estaFueraDeRango() ? "  * fuera de rango" : "");
        return String.format("%-6s %-30s %9.2f %-8s [%.2f - %.2f] %s%s",
                estudio.getCodigo(), estudio.getNombre(), valor, unidad,
                referenciaMin, referenciaMax, validado ? "validado" : "pendiente", marca);
    }
}
