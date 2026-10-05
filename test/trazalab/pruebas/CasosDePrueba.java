package trazalab.pruebas;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import trazalab.dominio.EstadoMuestra;
import trazalab.dominio.EventoTraza;
import trazalab.dominio.Muestra;
import trazalab.dominio.Orden;
import trazalab.dominio.Paciente;
import trazalab.dominio.Profesional;
import trazalab.dominio.Turno;
import trazalab.excepciones.AyunoInsuficienteException;
import trazalab.excepciones.MuestraInvalidaException;
import trazalab.excepciones.TurnoNoDisponibleException;
import trazalab.servicio.AgendaTurnos;
import trazalab.servicio.BitacoraTrazabilidad;
import trazalab.servicio.Laboratorio;
import trazalab.util.Consola;

/**
 * Casos de prueba de componente e integracion del plan de pruebas (apartado 5
 * del informe): CPC01, CPF02, CPC03, CPF04 y CPI05.
 *
 * Cada caso imprime una tabla con la entrada, el resultado esperado, el obtenido
 * y si la prueba pasa. El resultado esperado sale de la regla del requerimiento,
 * no del codigo: por eso una prueba puede fallar. Termina con codigo 1 si alguna
 * falla.
 *
 * @author Alday, Rodrigo Matias
 */
public class CasosDePrueba {

    private static int pasan;
    private static int fallan;
    private static final PrintStream SALIDA = System.out;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.forLanguageTag("es-AR"));
        // CPF02 prueba la lectura de Consola, que crea su Scanner al cargarse la clase:
        // la entrada simulada tiene que estar puesta antes de usarla por primera vez.
        System.setIn(new ByteArrayInputStream("-1\n49\n12\n".getBytes(StandardCharsets.UTF_8)));

        titulo("TRAZALAB - CASOS DE PRUEBA DE COMPONENTE E INTEGRACION");
        ejecutar("CPC01", CasosDePrueba::cpc01);
        ejecutar("CPF02", CasosDePrueba::cpf02);
        ejecutar("CPC03", CasosDePrueba::cpc03);
        ejecutar("CPF04", CasosDePrueba::cpf04);
        ejecutar("CPI05", CasosDePrueba::cpi05);

        System.out.println("\n" + "=".repeat(100));
        System.out.printf("  RESUMEN: %d comprobaciones, %d pasan, %d fallan%n", pasan + fallan, pasan, fallan);
        System.out.println("=".repeat(100));
        System.exit(fallan == 0 ? 0 : 1);
    }

    /** Un caso que se interrumpe con una excepcion inesperada cuenta como fallido. */
    private interface Caso {
        void correr() throws Exception;
    }

    private static void ejecutar(String codigo, Caso caso) {
        try {
            caso.correr();
        } catch (Exception e) {
            System.setOut(SALIDA);
            fallan++;
            System.out.println("  " + codigo + " interrumpido: " + e.getClass().getSimpleName() + " - " + e.getMessage()
                    + "  FALLA");
        }
    }

    // ------------------------------------------------------------------ CPC01

    /** Cobertura de sentencias (caja blanca) sobre Orden.calcularAyunoRequerido(). */
    private static void cpc01() throws Exception {
        encabezado("CPC01", "Componente - cobertura de sentencias (caja blanca)",
                "Orden.calcularAyunoRequerido(): ayuno maximo exigido por las practicas de la orden (RF12)");
        Laboratorio lab = new Laboratorio("Prueba");
        Paciente paciente = paciente("30111222");
        Object[][] filas = {
            {"C1", "orden sin practicas", new String[] {}, 0,
                "no entra al ciclo: return con el valor inicial"},
            {"C2", "GLU01 (8 h)", new String[] {"GLU01"}, 8,
                "entra al ciclo y la condicion es verdadera: asigna el maximo"},
            {"C3", "HEM01 (0 h), ORI01 (0 h)", new String[] {"HEM01", "ORI01"}, 0,
                "entra al ciclo y la condicion es siempre falsa"},
            {"C4", "GLU01 (8 h), COL01 (12 h), TSH01 (0 h)", new String[] {"GLU01", "COL01", "TSH01"}, 12,
                "el maximo no es el primero: la condicion es verdadera y falsa"},
        };
        cabeceraTabla();
        for (Object[] f : filas) {
            Orden orden = new Orden("O-PRUEBA", paciente, "Dr. Prueba");
            for (String codigo : (String[]) f[2]) {
                orden.agregarEstudio(lab.buscarEstudio(codigo));
            }
            fila((String) f[0], (String) f[1], f[3] + " h", orden.calcularAyunoRequerido() + " h");
        }
        nota("Las cuatro entradas ejecutan todas las sentencias del metodo: C1 la salida sin iterar, "
                + "C2 la asignacion y C3-C4 la condicion en sus dos valores.");
    }

    // ------------------------------------------------------------------ CPF02

    /** Particion de equivalencia y valores de frontera sobre la validacion del ayuno. */
    private static void cpf02() throws Exception {
        encabezado("CPF02", "Componente - particion de equivalencia y analisis de frontera (caja negra)",
                "Laboratorio.generarMuestras(): la extraccion se habilita si el ayuno declarado >= el exigido (RF12, RF16)");
        nota("Orden de prueba: GLU01 + COL01 + ORI01 (ayuno exigido 12 h, muestras de suero y orina).");
        cabeceraTabla();
        int[] valores = {0, 1, 11, 12, 13, 47, 48};
        for (int v : valores) {
            Laboratorio lab = new Laboratorio("Prueba");
            Orden orden = ordenGluColOri(lab);
            String clase = v < 12 ? "CEV1" : "CEV2";
            String esperado = v < 12 ? "rechaza: faltan " + (12 - v) + " h" : "genera 2 muestras (suero, orina)";
            String obtenido;
            try {
                List<Muestra> generadas = lab.generarMuestras(orden, v, "Ledesma, Carla");
                obtenido = "genera " + generadas.size() + " muestras ("
                        + generadas.get(0).getTipoMuestra().toLowerCase() + ", "
                        + generadas.get(1).getTipoMuestra().toLowerCase() + ")";
            } catch (AyunoInsuficienteException e) {
                obtenido = "rechaza: faltan " + (e.getHorasRequeridas() - e.getHorasDeclaradas()) + " h";
            }
            fila(clase + " / " + v, "ayuno declarado " + v + " h", esperado, obtenido);
        }
        // CEI1 y CEI2: el rango 0..48 lo valida la lectura de consola antes de llegar al dominio.
        ByteArrayOutputStream capturado = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturado, true, StandardCharsets.UTF_8));
        int leido = Consola.leerEntero("", 0, 48);
        System.setOut(SALIDA);
        String mensajes = capturado.toString(StandardCharsets.UTF_8);
        int rechazos = mensajes.split("El valor debe estar entre 0 y 48", -1).length - 1;
        fila("CEI1 / -1", "ayuno declarado -1 h", "rechaza el dato y lo vuelve a pedir",
                rechazos >= 1 ? "rechaza el dato y lo vuelve a pedir" : "lo acepta");
        fila("CEI2 / 49", "ayuno declarado 49 h", "rechaza el dato y lo vuelve a pedir",
                rechazos == 2 ? "rechaza el dato y lo vuelve a pedir" : "lo acepta");
        fila("CEV2 / 12", "dato valido tras los rechazos", "acepta 12", "acepta " + leido);
        nota("Mensaje emitido por la consola ante -1 y 49: \"" + mensajes.trim().split("\\R")[0].trim() + "\"");
    }

    // ------------------------------------------------------------------ CPC03

    /** Secuencia del circuito segun RF18, escrita aparte del codigo bajo prueba. */
    private static final List<EstadoMuestra> SECUENCIA = Arrays.asList(EstadoMuestra.GENERADA,
            EstadoMuestra.EXTRAIDA, EstadoMuestra.EN_PROCESO, EstadoMuestra.ANALIZADA, EstadoMuestra.INFORMADA);

    /** RF18: desde un estado no final, solo el estado siguiente o la anulacion. */
    private static boolean permitidaPorRf18(EstadoMuestra origen, EstadoMuestra destino) {
        if (origen == EstadoMuestra.INFORMADA || origen == EstadoMuestra.ANULADA) {
            return false;
        }
        if (destino == EstadoMuestra.ANULADA) {
            return true;
        }
        int i = SECUENCIA.indexOf(origen);
        return i + 1 < SECUENCIA.size() && SECUENCIA.get(i + 1) == destino;
    }

    /** Cobertura de decisiones (caja blanca) sobre EstadoMuestra.puedeTransicionarA(). */
    private static void cpc03() throws Exception {
        encabezado("CPC03", "Componente - cobertura de decisiones (caja blanca)",
                "EstadoMuestra.puedeTransicionarA(): transiciones validas del circuito de la muestra (RF18)");
        nota("Decisiones del metodo: D1 origen final (INFORMADA o ANULADA) | D2 destino ANULADA | "
                + "D3 destino es el estado siguiente.");
        cabeceraTabla();
        Object[][] casos = {
            {"D1 V", EstadoMuestra.INFORMADA, EstadoMuestra.ANULADA},
            {"D1 V", EstadoMuestra.ANULADA, EstadoMuestra.GENERADA},
            {"D1 F, D2 V", EstadoMuestra.EN_PROCESO, EstadoMuestra.ANULADA},
            {"D2 F, D3 V", EstadoMuestra.GENERADA, EstadoMuestra.EXTRAIDA},
            {"D3 F (retroceso)", EstadoMuestra.ANALIZADA, EstadoMuestra.EXTRAIDA},
            {"D3 F (salto)", EstadoMuestra.EXTRAIDA, EstadoMuestra.ANALIZADA},
        };
        for (Object[] c : casos) {
            EstadoMuestra o = (EstadoMuestra) c[1];
            EstadoMuestra d = (EstadoMuestra) c[2];
            fila((String) c[0], o + " -> " + d, permitidaPorRf18(o, d) ? "permitida" : "rechazada",
                    o.puedeTransicionarA(d) ? "permitida" : "rechazada");
        }
        // Verificacion exhaustiva: las 36 combinaciones contra la regla de RF18.
        System.out.println("\n  Matriz completa (P = permitida, - = rechazada). Fila: origen. Columna: destino.");
        EstadoMuestra[] todos = EstadoMuestra.values();
        StringBuilder cab = new StringBuilder(String.format("  %-12s", ""));
        for (EstadoMuestra d : todos) {
            cab.append(String.format("%-12s", d));
        }
        System.out.println(cab);
        int coinciden = 0;
        for (EstadoMuestra o : todos) {
            StringBuilder linea = new StringBuilder(String.format("  %-12s", o));
            for (EstadoMuestra d : todos) {
                boolean real = o.puedeTransicionarA(d);
                linea.append(String.format("%-12s", real ? "P" : "-"));
                if (real == permitidaPorRf18(o, d)) {
                    coinciden++;
                }
            }
            System.out.println(linea);
        }
        fila("Matriz", "36 combinaciones origen-destino", "36 coinciden con RF18", coinciden + " coinciden con RF18");
    }

    // ------------------------------------------------------------------ CPF04

    /** Particion de equivalencia sobre la franja horaria y el cupo de turnos. */
    private static void cpf04() throws Exception {
        encabezado("CPF04", "Componente - particion de equivalencia y valores de frontera (caja negra)",
                "AgendaTurnos.otorgarTurno(): extraccion de 7:00 a 11:00, cupo de 3 turnos por franja (RF05, RF06, RF07)");
        nota("Clases: CEV1 7 <= hora < 11 | CEI1 hora < 7 | CEI2 hora >= 11 | CEV2 franja con cupo | "
                + "CEI3 franja completa (3 turnos).");
        cabeceraTabla();
        LocalDate dia = LocalDate.of(2026, 10, 19);
        Object[][] horas = {
            {"CEI1", 6, 59, "rechaza: fuera de horario"},
            {"CEV1", 7, 0, "otorga, box 1"},
            {"CEV1", 10, 59, "otorga, box 1"},
            {"CEI2", 11, 0, "rechaza: fuera de horario"},
        };
        for (Object[] h : horas) {
            AgendaTurnos agenda = new AgendaTurnos();
            LocalDateTime fh = dia.atTime((int) h[1], (int) h[2]);
            fila((String) h[0], String.format("turno a las %02d:%02d", (int) h[1], (int) h[2]), (String) h[3],
                    intentarTurno(agenda, paciente("30111222"), fh));
        }
        AgendaTurnos agenda = new AgendaTurnos();
        LocalDateTime ocho = dia.atTime(8, 0);
        for (int k = 1; k <= 4; k++) {
            String esperado = k <= 3 ? "otorga, box " + k : "rechaza: franja completa";
            fila(k <= 3 ? "CEV2" : "CEI3", "turno " + k + " de las 08:00", esperado,
                    intentarTurno(agenda, paciente("3011122" + k), ocho));
        }
        fila("CEV2", "08:00 del dia siguiente", "otorga, box 1",
                intentarTurno(agenda, paciente("30111229"), ocho.plusDays(1)));
    }

    private static String intentarTurno(AgendaTurnos agenda, Paciente paciente, LocalDateTime fh) {
        try {
            Turno turno = agenda.otorgarTurno(paciente, fh);
            return "otorga, box " + turno.getBox();
        } catch (TurnoNoDisponibleException e) {
            return e.getMessage().contains("cupo") ? "rechaza: franja completa" : "rechaza: fuera de horario";
        }
    }

    // ------------------------------------------------------------------ CPI05

    /** Integracion incremental ascendente: generacion de muestras + registro de trazas. */
    private static void cpi05() throws Exception {
        encabezado("CPI05", "Integracion - incremental ascendente",
                "Generacion de muestras y registro de trazas combinados: BitacoraTrazabilidad -> Laboratorio (RF15, RF17, RF18, RF19)");
        cabeceraTabla();

        // Paso 1: la bitacora sola, con una muestra.
        Laboratorio labBase = new Laboratorio("Prueba");
        Orden ordenBase = ordenGluColOri(labBase);
        Muestra suelta = new Muestra("M-TEST-0001", "Suero", ordenBase);
        BitacoraTrazabilidad bitacora = new BitacoraTrazabilidad();
        bitacora.registrar(suelta, new EventoTraza(null, EstadoMuestra.GENERADA, "Ledesma", "alta"));
        fila("1 Bitacora", "registra el alta de una muestra", "historial con 1 evento",
                "historial con " + bitacora.historialDe(suelta).size() + " evento");
        String salto;
        try {
            bitacora.registrar(suelta, new EventoTraza(EstadoMuestra.ANALIZADA, EstadoMuestra.INFORMADA, "x", "salto"));
            salto = "lo acepta";
        } catch (IllegalArgumentException e) {
            salto = "rechaza el evento";
        }
        fila("1 Bitacora", "evento que no continua la cadena", "rechaza el evento", salto);

        // Paso 2: se integra Laboratorio (generacion de muestras con alta en la bitacora).
        Laboratorio lab = new Laboratorio("Prueba");
        Orden orden = lab.crearOrden(paciente("30111222"), "Dra. Perez");
        orden.agregarEstudio(lab.buscarEstudio("GLU01"));
        orden.agregarEstudio(lab.buscarEstudio("ORI01"));
        List<Muestra> muestras = lab.generarMuestras(orden, 12, "Ledesma, Carla");
        Muestra suero = muestras.get(0);
        Muestra orina = muestras.get(1);
        boolean altas = true;
        for (Muestra m : muestras) {
            List<EventoTraza> h = lab.historialDe(m);
            altas &= m.getEstado() == EstadoMuestra.GENERADA && h.size() == 1
                    && "Ledesma, Carla".equals(h.get(0).getResponsable());
        }
        fila("2 + Laboratorio", "generarMuestras(GLU01 + ORI01, 12 h)", "2 muestras GENERADA con su alta",
                muestras.size() + " muestras " + (altas ? "GENERADA con su alta" : "con datos inconsistentes"));

        // Paso 3: transicion valida.
        lab.avanzarMuestra(suero, EstadoMuestra.EXTRAIDA, "Ledesma, Carla", "extraccion");
        EventoTraza ultimo = lab.getBitacora().ultimoEventoDe(suero);
        fila("3 Transicion", "suero GENERADA -> EXTRAIDA", "EXTRAIDA, con fecha y 2 eventos",
                suero.getEstado() + (suero.getFechaExtraccion() != null ? ", con fecha" : ", sin fecha")
                        + " y " + lab.historialDe(suero).size() + " eventos");
        fila("3 Transicion", "ultimo evento registrado", "GENERADA -> EXTRAIDA",
                ultimo.getEstadoAnterior() + " -> " + ultimo.getEstadoNuevo());

        // Paso 4: circuito completo hasta INFORMADA.
        lab.avanzarMuestra(suero, EstadoMuestra.EN_PROCESO, "Ferreyra, Lucia", "");
        lab.cargarResultado(suero, lab.buscarEstudio("GLU01"), 178, "mg/dL", 70, 110);
        lab.avanzarMuestra(suero, EstadoMuestra.ANALIZADA, "Ferreyra, Lucia", "");
        int validados = lab.validarResultadosDeOrden(orden,
                new Profesional("28456789", "Ferreyra", "Lucia", "BQ-4821", "Bioquimico"));
        fila("4 Circuito", "validar la orden con el suero ANALIZADA", "1 validado, INFORMADA, 5 eventos",
                validados + " validado, " + suero.getEstado() + ", " + lab.historialDe(suero).size() + " eventos");
        fila("4 Circuito", "estado de la orden (orina pendiente)", "Informada en parte", orden.getEstado());
        fila("4 Circuito", "TAT del suero", "calculado (>= 0 min)",
                lab.calcularTatMinutos(suero) >= 0 ? "calculado (>= 0 min)" : "no calculado");

        // Paso 5: una transicion invalida no deja rastro.
        String invalida;
        try {
            lab.avanzarMuestra(suero, EstadoMuestra.ANULADA, "x", "");
            invalida = "la acepta";
        } catch (MuestraInvalidaException e) {
            invalida = "la rechaza";
        }
        fila("5 Invalida", "suero INFORMADA -> ANULADA", "la rechaza y quedan 5 eventos",
                invalida + " y quedan " + lab.historialDe(suero).size() + " eventos");

        // Paso 6: anulacion y reextraccion de la orina.
        lab.avanzarMuestra(orina, EstadoMuestra.ANULADA, "Ledesma, Carla", "muestra hemolizada");
        List<Muestra> nuevas = lab.generarMuestras(orden, 12, "Ledesma, Carla");
        fila("6 Reextraccion", "anular la orina y volver a generar", "1 muestra nueva de orina",
                nuevas.size() + " muestra nueva de " + nuevas.get(0).getTipoMuestra().toLowerCase());
        fila("6 Reextraccion", "historial de la orina anulada", "2 eventos (alta y anulacion)",
                lab.historialDe(orina).size() + " eventos (alta y anulacion)");
    }

    // --------------------------------------------------------------- auxiliares

    private static Orden ordenGluColOri(Laboratorio lab) throws Exception {
        Orden orden = lab.crearOrden(paciente("30111222"), "Dra. Perez, Silvia");
        for (String codigo : new String[] {"GLU01", "COL01", "ORI01"}) {
            orden.agregarEstudio(lab.buscarEstudio(codigo));
        }
        return orden;
    }

    private static Paciente paciente(String dni) {
        return new Paciente(dni, "Gomez", "Marcela", LocalDate.of(1983, 4, 12), "OSDE 210", "3534-556677");
    }

    private static void titulo(String texto) {
        System.out.println("=".repeat(100));
        System.out.println("  " + texto);
        System.out.println("=".repeat(100));
    }

    private static void encabezado(String codigo, String tipo, String objeto) {
        System.out.println("\n" + "-".repeat(100));
        System.out.println("  " + codigo + " | " + tipo);
        System.out.println("  " + objeto);
        System.out.println("-".repeat(100));
    }

    private static void cabeceraTabla() {
        System.out.printf("  %-17s %-40s %-34s %-34s %s%n", "Caso", "Entrada", "Esperado", "Obtenido", "Resultado");
    }

    private static void fila(String caso, String entrada, String esperado, String obtenido) {
        boolean ok = esperado.equals(obtenido);
        if (ok) {
            pasan++;
        } else {
            fallan++;
        }
        System.out.printf("  %-17s %-40s %-34s %-34s %s%n", caso, entrada, esperado, obtenido, ok ? "PASA" : "FALLA");
    }

    private static void nota(String texto) {
        System.out.println("  " + texto);
    }
}
