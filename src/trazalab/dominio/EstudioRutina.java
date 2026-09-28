package trazalab.dominio;

/**
 * Estudio de rutina procesado integramente en el laboratorio.
 * POLIMORFISMO: implementa a su manera los metodos abstractos de Estudio.
 *
 * @author Alday, Rodrigo Matias
 */
public class EstudioRutina extends Estudio {

    /** Descuento aplicado cuando el estudio integra un perfil o panel. */
    private final double descuentoPerfil;

    public EstudioRutina(String codigo, String nombre, String area, String tipoMuestra,
                         double precioBase, int horasAyuno, double descuentoPerfil) {
        super(codigo, nombre, area, tipoMuestra, precioBase, horasAyuno);
        this.descuentoPerfil = descuentoPerfil;
    }

    @Override
    public double calcularPrecio() {
        return precioBase - (precioBase * descuentoPerfil / 100.0);
    }

    @Override
    public int calcularHorasDemora() {
        return 24;
    }

    public double getDescuentoPerfil() {
        return descuentoPerfil;
    }
}
