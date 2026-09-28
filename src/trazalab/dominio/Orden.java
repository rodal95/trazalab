package trazalab.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de estudios solicitada para un paciente. Agrupa las practicas pedidas
 * y conoce el ayuno maximo exigido por el conjunto.
 *
 * @author Alday, Rodrigo Matias
 */
public class Orden {

    private final String numero;
    private final Paciente paciente;
    private final String medicoSolicitante;
    private final LocalDate fecha;
    /** COLECCION DINAMICA: la cantidad de estudios por orden no se conoce de antemano. */
    private final List<Estudio> estudios;
    private String estado;

    public Orden(String numero, Paciente paciente, String medicoSolicitante) {
        this.numero = numero;
        this.paciente = paciente;
        this.medicoSolicitante = medicoSolicitante;
        this.fecha = LocalDate.now();
        this.estudios = new ArrayList<>();
        this.estado = "Abierta";
    }

    public void agregarEstudio(Estudio estudio) {
        estudios.add(estudio);
    }

    /** Suma polimorfica: cada estudio resuelve su propio precio. */
    public double calcularTotal() {
        double total = 0;
        for (Estudio estudio : estudios) {
            total += estudio.calcularPrecio();
        }
        return total;
    }

    /** Devuelve el ayuno maximo requerido por los estudios de la orden. */
    public int calcularAyunoRequerido() {
        int maximo = 0;
        for (Estudio estudio : estudios) {
            if (estudio.getHorasAyuno() > maximo) {
                maximo = estudio.getHorasAyuno();
            }
        }
        return maximo;
    }

    /** Devuelve los tipos de muestra distintos que se deben extraer. */
    public List<String> obtenerTiposDeMuestra() {
        List<String> tipos = new ArrayList<>();
        for (Estudio estudio : estudios) {
            if (!tipos.contains(estudio.getTipoMuestra())) {
                tipos.add(estudio.getTipoMuestra());
            }
        }
        return tipos;
    }

    /** Demora estimada del informe: la mayor demora entre los estudios pedidos. */
    public int calcularDemoraEstimadaHoras() {
        int maximo = 0;
        for (Estudio estudio : estudios) {
            if (estudio.calcularHorasDemora() > maximo) {
                maximo = estudio.calcularHorasDemora();
            }
        }
        return maximo;
    }

    public String getNumero() {
        return numero;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public String getMedicoSolicitante() {
        return medicoSolicitante;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public List<Estudio> getEstudios() {
        return estudios;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return String.format("%-8s %-26s %2d estudios  ayuno %2d h  total $%.2f  %s",
                numero, paciente.getNombreCompleto(), estudios.size(),
                calcularAyunoRequerido(), calcularTotal(), estado);
    }
}
