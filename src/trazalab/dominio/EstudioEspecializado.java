package trazalab.dominio;

/**
 * Estudio de alta complejidad, habitualmente derivado a un laboratorio externo.
 * POLIMORFISMO: redefine el calculo de precio y de demora.
 *
 * @author Alday, Rodrigo Matias
 */
public class EstudioEspecializado extends Estudio {

    private final double recargoComplejidad;
    private final boolean derivado;
    private final int diasDerivacion;

    public EstudioEspecializado(String codigo, String nombre, String area, String tipoMuestra,
                                double precioBase, int horasAyuno, double recargoComplejidad,
                                boolean derivado, int diasDerivacion) {
        super(codigo, nombre, area, tipoMuestra, precioBase, horasAyuno);
        this.recargoComplejidad = recargoComplejidad;
        this.derivado = derivado;
        this.diasDerivacion = diasDerivacion;
    }

    @Override
    public double calcularPrecio() {
        double precio = precioBase + (precioBase * recargoComplejidad / 100.0);
        if (derivado) {
            precio += 2500.0; // costo fijo de logistica de derivacion
        }
        return precio;
    }

    @Override
    public int calcularHorasDemora() {
        return derivado ? diasDerivacion * 24 : 72;
    }

    public boolean esDerivado() {
        return derivado;
    }

    public double getRecargoComplejidad() {
        return recargoComplejidad;
    }

    public int getDiasDerivacion() {
        return diasDerivacion;
    }
}
