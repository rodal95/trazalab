package trazalab.servicio;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import trazalab.dominio.EstadoMuestra;
import trazalab.dominio.Estudio;
import trazalab.dominio.EstudioEspecializado;
import trazalab.dominio.EstudioRutina;
import trazalab.dominio.Muestra;
import trazalab.dominio.Orden;
import trazalab.dominio.Paciente;
import trazalab.dominio.Profesional;
import trazalab.dominio.Resultado;
import trazalab.excepciones.AyunoInsuficienteException;
import trazalab.excepciones.EntidadNoEncontradaException;
import trazalab.excepciones.MuestraInvalidaException;
import trazalab.util.Busqueda;
import trazalab.util.Ordenamiento;

/**
 * Fachada de negocio del laboratorio. Concentra las reglas del circuito
 * preanalitico y analitico y administra las colecciones del prototipo.
 *
 * @author Alday, Rodrigo Matias
 */
public class Laboratorio {

    private final String nombre;

    /** ARREGLO de tamanio fijo: el catalogo de practicas es estable en el tiempo. */
    private final Estudio[] catalogo;
    /** LISTAS dinamicas: crecen con la operacion diaria. */
    private final List<Paciente> pacientes;
    private final List<Orden> ordenes;
    private final List<Muestra> muestras;
    private final List<Resultado> resultados;
    /** ESTRUCTURA PILA (LIFO): historial de acciones, la mas reciente en el tope. */
    private final Deque<String> pilaDeAcciones;

    private final AgendaTurnos agenda;
    private int contadorOrdenes;
    private int contadorMuestras;

    public Laboratorio(String nombre) {
        this.nombre = nombre;
        this.pacientes = new ArrayList<>();
        this.ordenes = new ArrayList<>();
        this.muestras = new ArrayList<>();
        this.resultados = new ArrayList<>();
        this.pilaDeAcciones = new ArrayDeque<>();
        this.agenda = new AgendaTurnos();
        this.catalogo = construirCatalogo();
        // El catalogo se ordena por codigo para habilitar la busqueda binaria.
        Ordenamiento.insercionPorCodigo(catalogo);
    }

    /** Carga inicial del catalogo de practicas con datos de prueba. */
    private Estudio[] construirCatalogo() {
        Estudio[] practicas = new Estudio[10];
        practicas[0] = new EstudioRutina("HEM01", "Hemograma completo", "Hematologia", "Sangre entera", 8500, 0, 0);
        practicas[1] = new EstudioRutina("GLU01", "Glucemia en ayunas", "Quimica clinica", "Suero", 4200, 8, 10);
        practicas[2] = new EstudioRutina("COL01", "Colesterol total", "Quimica clinica", "Suero", 4600, 12, 10);
        practicas[3] = new EstudioRutina("TRI01", "Trigliceridos", "Quimica clinica", "Suero", 4800, 12, 10);
        practicas[4] = new EstudioRutina("ORI01", "Orina completa", "Uroanalisis", "Orina", 5200, 0, 0);
        practicas[5] = new EstudioRutina("TSH01", "TSH ultrasensible", "Endocrinologia", "Suero", 11500, 0, 5);
        practicas[6] = new EstudioEspecializado("VIT01", "Vitamina D 25-OH", "Endocrinologia", "Suero", 21000, 0, 15, false, 0);
        practicas[7] = new EstudioEspecializado("PCR01", "PCR cuantitativa", "Inmunologia", "Suero", 18500, 0, 20, false, 0);
        practicas[8] = new EstudioEspecializado("GEN01", "Panel genetico trombofilia", "Biologia molecular", "Sangre entera", 96000, 0, 25, true, 10);
        practicas[9] = new EstudioEspecializado("CEL01", "Anticuerpos anti-transglutaminasa", "Inmunologia", "Suero", 24000, 4, 18, true, 5);
        return practicas;
    }

    // ---------------------------------------------------------------- pacientes

    public void registrarPaciente(Paciente paciente) {
        pacientes.add(paciente);
        pilaDeAcciones.push("Alta de paciente " + paciente.getDni());
    }

    public Paciente buscarPaciente(String dni) throws EntidadNoEncontradaException {
        return Busqueda.linealPorDni(pacientes, dni);
    }

    // ----------------------------------------------------------------- catalogo

    public Estudio buscarEstudio(String codigo) throws EntidadNoEncontradaException {
        return Busqueda.binariaPorCodigo(catalogo, codigo);
    }

    /** Devuelve una copia del catalogo ordenada por precio de mayor a menor. */
    public Estudio[] catalogoPorPrecioDescendente() {
        Estudio[] copia = new Estudio[catalogo.length];
        System.arraycopy(catalogo, 0, copia, 0, catalogo.length);
        Ordenamiento.quickSortPorPrecioDesc(copia, 0, copia.length - 1);
        return copia;
    }

    // ------------------------------------------------------------------ ordenes

    public Orden crearOrden(Paciente paciente, String medico) {
        contadorOrdenes++;
        Orden orden = new Orden(String.format("O-%04d", contadorOrdenes), paciente, medico);
        ordenes.add(orden);
        pilaDeAcciones.push("Alta de orden " + orden.getNumero());
        return orden;
    }

    public Orden buscarOrden(String numero) throws EntidadNoEncontradaException {
        for (Orden orden : ordenes) {
            if (orden.getNumero().equalsIgnoreCase(numero)) {
                return orden;
            }
        }
        throw new EntidadNoEncontradaException("No existe la orden " + numero + ".");
    }

    // ------------------------------------------------------------------ muestras

    /**
     * Genera una muestra por cada tipo requerido por la orden que todavia no tenga
     * una muestra vigente, validando el ayuno declarado por el paciente. Una
     * muestra anulada deja de ser vigente, lo que permite la reextraccion.
     *
     * @throws MuestraInvalidaException si la orden no tiene practicas o ya tiene sus muestras.
     * @throws AyunoInsuficienteException si el ayuno declarado no alcanza.
     */
    public List<Muestra> generarMuestras(Orden orden, int horasAyunoDeclaradas, String responsable)
            throws MuestraInvalidaException, AyunoInsuficienteException {
        if (orden.getEstudios().isEmpty()) {
            throw new MuestraInvalidaException("La orden " + orden.getNumero()
                    + " no tiene practicas: no hay muestras que extraer.");
        }
        List<String> tiposPendientes = new ArrayList<>();
        for (String tipo : orden.obtenerTiposDeMuestra()) {
            if (buscarMuestraVigente(orden, tipo) == null) {
                tiposPendientes.add(tipo);
            }
        }
        if (tiposPendientes.isEmpty()) {
            throw new MuestraInvalidaException("La orden " + orden.getNumero()
                    + " ya tiene sus muestras. Para repetir una extraccion, primero se anula la muestra.");
        }
        int requeridas = orden.calcularAyunoRequerido();
        if (horasAyunoDeclaradas < requeridas) {
            throw new AyunoInsuficienteException(
                    "La orden " + orden.getNumero() + " exige " + requeridas
                            + " h de ayuno y el paciente declaro " + horasAyunoDeclaradas + " h.",
                    requeridas, horasAyunoDeclaradas);
        }
        List<Muestra> generadas = new ArrayList<>();
        for (String tipo : tiposPendientes) {
            contadorMuestras++;
            String codigo = String.format("M-%s-%04d", tipo.substring(0, 3).toUpperCase(), contadorMuestras);
            Muestra muestra = new Muestra(codigo, tipo, orden, responsable);
            muestras.add(muestra);
            generadas.add(muestra);
        }
        orden.setEstado("Con muestras");
        pilaDeAcciones.push("Generacion de " + generadas.size() + " muestra(s) para " + orden.getNumero());
        return generadas;
    }

    public Muestra buscarMuestra(String codigoBarra) throws EntidadNoEncontradaException {
        return Busqueda.linealPorCodigoBarra(muestras, codigoBarra);
    }

    /** Devuelve las muestras generadas para una orden, en el orden en que se generaron. */
    public List<Muestra> muestrasDeOrden(Orden orden) {
        List<Muestra> propias = new ArrayList<>();
        for (Muestra muestra : muestras) {
            if (muestra.getOrden().equals(orden)) {
                propias.add(muestra);
            }
        }
        return propias;
    }

    /** Devuelve la muestra no anulada de un tipo para la orden, o null si no la hay. */
    private Muestra buscarMuestraVigente(Orden orden, String tipoMuestra) {
        for (Muestra muestra : muestrasDeOrden(orden)) {
            if (muestra.getTipoMuestra().equals(tipoMuestra) && muestra.getEstado() != EstadoMuestra.ANULADA) {
                return muestra;
            }
        }
        return null;
    }

    public void avanzarMuestra(Muestra muestra, EstadoMuestra destino, String responsable, String observacion)
            throws MuestraInvalidaException {
        muestra.cambiarEstado(destino, responsable, observacion);
        pilaDeAcciones.push("Muestra " + muestra.getCodigoBarra() + " -> " + destino);
    }

    // ---------------------------------------------------------------- resultados

    /**
     * Registra el resultado de un estudio sobre una muestra en proceso analitico.
     *
     * @throws MuestraInvalidaException si la muestra no esta en proceso o analizada, si el
     *         estudio no corresponde a la muestra o si el resultado ya fue cargado.
     */
    public Resultado cargarResultado(Muestra muestra, Estudio estudio, double valor, String unidad,
                                     double refMin, double refMax) throws MuestraInvalidaException {
        if (muestra.getEstado() != EstadoMuestra.EN_PROCESO && muestra.getEstado() != EstadoMuestra.ANALIZADA) {
            throw new MuestraInvalidaException("La muestra " + muestra.getCodigoBarra() + " esta en estado "
                    + muestra.getEstado() + ": los resultados se cargan en EN_PROCESO o ANALIZADA.");
        }
        if (!muestra.getOrden().getEstudios().contains(estudio)
                || !estudio.getTipoMuestra().equals(muestra.getTipoMuestra())) {
            throw new MuestraInvalidaException("El estudio " + estudio.getCodigo()
                    + " no se procesa sobre la muestra " + muestra.getCodigoBarra() + ".");
        }
        if (tieneResultado(muestra, estudio)) {
            throw new MuestraInvalidaException("El resultado de " + estudio.getCodigo() + " en la muestra "
                    + muestra.getCodigoBarra() + " ya fue cargado.");
        }
        Resultado resultado = new Resultado(muestra, estudio, valor, unidad, refMin, refMax);
        resultados.add(resultado);
        pilaDeAcciones.push("Carga de resultado " + estudio.getCodigo() + " en " + muestra.getCodigoBarra());
        return resultado;
    }

    /** Indica si ya se cargo el resultado de un estudio sobre una muestra. */
    public boolean tieneResultado(Muestra muestra, Estudio estudio) {
        for (Resultado resultado : resultados) {
            if (resultado.getMuestra().equals(muestra) && resultado.getEstudio().equals(estudio)) {
                return true;
            }
        }
        return false;
    }

    /** Devuelve los resultados de una orden, ordenados por codigo de estudio. */
    public List<Resultado> resultadosDeOrden(Orden orden) {
        List<Resultado> propios = new ArrayList<>();
        for (Resultado resultado : resultados) {
            if (resultado.getMuestra().getOrden().equals(orden)) {
                propios.add(resultado);
            }
        }
        Ordenamiento.burbujaResultados(propios);
        return propios;
    }

    /**
     * Valida los resultados de una orden si el profesional esta habilitado. Solo se
     * validan las muestras ANALIZADA que tienen cargados todos sus resultados; cada
     * una pasa a INFORMADA. Las demas quedan pendientes para un informe posterior.
     *
     * @return cantidad de resultados validados en esta operacion (0 si no habia pendientes).
     * @throws MuestraInvalidaException si hay resultados pendientes pero ninguna muestra
     *         de la orden esta en condiciones de ser informada.
     */
    public int validarResultadosDeOrden(Orden orden, Profesional bioquimico) throws MuestraInvalidaException {
        if (!bioquimico.puedeValidarResultados()) {
            return 0;
        }
        boolean hayPendientes = false;
        for (Resultado resultado : resultadosDeOrden(orden)) {
            if (!resultado.isValidado()) {
                hayPendientes = true;
            }
        }
        if (!hayPendientes) {
            return 0;
        }
        int validados = 0;
        String primerMotivo = null;
        for (Muestra muestra : muestrasDeOrden(orden)) {
            if (muestra.getEstado() == EstadoMuestra.INFORMADA || muestra.getEstado() == EstadoMuestra.ANULADA) {
                continue;
            }
            String motivo = motivoParaNoInformar(muestra);
            if (motivo != null) {
                if (primerMotivo == null) {
                    primerMotivo = motivo;
                }
                continue;
            }
            for (Resultado resultado : resultados) {
                if (resultado.getMuestra().equals(muestra) && !resultado.isValidado()) {
                    resultado.validar(bioquimico);
                    validados++;
                }
            }
            muestra.cambiarEstado(EstadoMuestra.INFORMADA, bioquimico.getNombreCompleto(),
                    "Resultados validados, informe liberado");
        }
        if (validados == 0) {
            throw new MuestraInvalidaException("No se puede validar la orden " + orden.getNumero() + ": "
                    + (primerMotivo != null ? primerMotivo : "sus muestras ya estan informadas o anuladas") + ".");
        }
        boolean completa = true;
        for (Muestra muestra : muestrasDeOrden(orden)) {
            if (muestra.getEstado() != EstadoMuestra.INFORMADA && muestra.getEstado() != EstadoMuestra.ANULADA) {
                completa = false;
            }
        }
        orden.setEstado(completa ? "Informada" : "Informada en parte");
        pilaDeAcciones.push("Validacion de " + validados + " resultado(s) de " + orden.getNumero());
        return validados;
    }

    /**
     * Indica por que una muestra todavia no puede informarse: no esta ANALIZADA o le
     * falta algun resultado. Devuelve null si esta en condiciones de informarse.
     */
    private String motivoParaNoInformar(Muestra muestra) {
        if (muestra.getEstado() != EstadoMuestra.ANALIZADA) {
            return "la muestra " + muestra.getCodigoBarra() + " esta en " + muestra.getEstado()
                    + " y debe estar ANALIZADA";
        }
        for (Estudio estudio : muestra.getOrden().getEstudios()) {
            if (estudio.getTipoMuestra().equals(muestra.getTipoMuestra()) && !tieneResultado(muestra, estudio)) {
                return "falta el resultado de " + estudio.getCodigo() + " en la muestra " + muestra.getCodigoBarra();
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- indicadores

    /** Tiempo de respuesta promedio, en minutos, de las muestras ya informadas. */
    public double calcularTatPromedio() {
        long suma = 0;
        int contadas = 0;
        for (Muestra muestra : muestras) {
            long tat = muestra.calcularTatMinutos();
            if (muestra.getEstado() == EstadoMuestra.INFORMADA && tat >= 0) {
                suma += tat;
                contadas++;
            }
        }
        return contadas == 0 ? 0 : (double) suma / contadas;
    }

    /** Cuenta las muestras que se encuentran en un estado determinado. */
    public int contarMuestrasEn(EstadoMuestra estado) {
        int contador = 0;
        for (Muestra muestra : muestras) {
            if (muestra.getEstado() == estado) {
                contador++;
            }
        }
        return contador;
    }

    /** Devuelve la accion en el tope de la pila, la mas reciente, sin quitarla. */
    public String consultarUltimaAccion() {
        return pilaDeAcciones.peek();
    }

    /**
     * Devuelve hasta 'cantidad' acciones recorriendo la pila desde el tope (LIFO):
     * la primera de la lista es la mas reciente.
     */
    public List<String> consultarUltimasAcciones(int cantidad) {
        List<String> recientes = new ArrayList<>();
        for (String accion : pilaDeAcciones) {
            if (recientes.size() == cantidad) {
                break;
            }
            recientes.add(accion);
        }
        return recientes;
    }

    public LocalDateTime ahora() {
        return LocalDateTime.now();
    }

    // -------------------------------------------------------------------- getters

    public String getNombre() {
        return nombre;
    }

    public Estudio[] getCatalogo() {
        return catalogo;
    }

    public List<Paciente> getPacientes() {
        return pacientes;
    }

    public List<Orden> getOrdenes() {
        return ordenes;
    }

    public List<Muestra> getMuestras() {
        return muestras;
    }

    public List<Resultado> getResultados() {
        return resultados;
    }

    public AgendaTurnos getAgenda() {
        return agenda;
    }
}
