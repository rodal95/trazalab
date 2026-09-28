package trazalab.dominio;

/**
 * Clase ABSTRACTA que representa una practica bioquimica del catalogo.
 * Define el contrato comun y deja el calculo del precio final a cada subclase.
 *
 * @author Alday, Rodrigo Matias
 */
public abstract class Estudio implements Comparable<Estudio> {

    // ENCAPSULAMIENTO con visibilidad protected: accesible desde las subclases.
    protected final String codigo;
    protected final String nombre;
    protected final String area;
    protected final String tipoMuestra;
    protected final double precioBase;
    protected final int horasAyuno;

    public Estudio(String codigo, String nombre, String area, String tipoMuestra,
                   double precioBase, int horasAyuno) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.area = area;
        this.tipoMuestra = tipoMuestra;
        this.precioBase = precioBase;
        this.horasAyuno = horasAyuno;
    }

    /**
     * METODO ABSTRACTO: cada tipo de estudio calcula su precio de forma distinta.
     * Es el punto donde se apoya el polimorfismo del sistema.
     */
    public abstract double calcularPrecio();

    /** METODO ABSTRACTO: cada tipo de estudio informa su demora de proceso. */
    public abstract int calcularHorasDemora();

    /** Metodo concreto heredado por ambas subclases. */
    public boolean requiereAyuno() {
        return horasAyuno > 0;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getArea() {
        return area;
    }

    public String getTipoMuestra() {
        return tipoMuestra;
    }

    public double getPrecioBase() {
        return precioBase;
    }

    public int getHorasAyuno() {
        return horasAyuno;
    }

    /** Orden natural del catalogo: por codigo, para permitir la busqueda binaria. */
    @Override
    public int compareTo(Estudio otro) {
        return this.codigo.compareTo(otro.codigo);
    }

    @Override
    public String toString() {
        return String.format("%-6s %-32s %-14s $%9.2f  %s",
                codigo, nombre, area, calcularPrecio(),
                requiereAyuno() ? horasAyuno + " h ayuno" : "sin ayuno");
    }
}
