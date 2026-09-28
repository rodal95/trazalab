package trazalab.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import trazalab.dominio.EstadoMuestra;
import trazalab.dominio.Estudio;
import trazalab.dominio.Muestra;
import trazalab.dominio.Orden;
import trazalab.dominio.Paciente;
import trazalab.dominio.Profesional;
import trazalab.dominio.Resultado;
import trazalab.dominio.Turno;
import trazalab.excepciones.AyunoInsuficienteException;
import trazalab.excepciones.EntidadNoEncontradaException;
import trazalab.excepciones.MuestraInvalidaException;
import trazalab.excepciones.TurnoNoDisponibleException;
import trazalab.servicio.Laboratorio;
import trazalab.util.Consola;

/**
 * Clase principal del prototipo TRAZALAB. Presenta el menu de seleccion y
 * coordina las operaciones sobre la fachada de negocio.
 *
 * Trabajo Practico 4 - Proyecto integrador.
 * Seminario de Practica Profesional.
 *
 * @author Alday, Rodrigo Matias
 */
public class MenuPrincipal {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static Laboratorio laboratorio;
    private static Profesional usuarioActual;

    public static void main(String[] args) {
        laboratorio = new Laboratorio("Laboratorio Bioquimico San Rafael");
        usuarioActual = new Profesional("28456789", "Ferreyra", "Lucia", "BQ-4821", "Bioquimico");
        cargarDatosDePrueba();

        boolean ejecutando = true;
        while (ejecutando) {
            mostrarMenu();
            int opcion = Consola.leerEntero("  Elija una opcion: ", 0, 10);
            switch (opcion) {
                case 1 -> registrarPaciente();
                case 2 -> otorgarTurno();
                case 3 -> registrarOrden();
                case 4 -> registrarExtraccion();
                case 5 -> avanzarCircuito();
                case 6 -> cargarResultados();
                case 7 -> emitirInforme();
                case 8 -> consultarCatalogo();
                case 9 -> verIndicadores();
                case 10 -> MenuBaseDatos.ejecutar();
                case 0 -> {
                    ejecutando = false;
                    System.out.println("\n  Sesion finalizada. Hasta luego, " + usuarioActual.getNombre() + ".\n");
                }
                default -> System.out.println("  [!] Opcion inexistente.");
            }
        }
    }

    private static void mostrarMenu() {
        Consola.titulo("TRAZALAB - " + laboratorio.getNombre());
        System.out.println("  Usuario: " + usuarioActual.getNombreCompleto()
                + " (" + usuarioActual.obtenerRol() + ")   "
                + LocalDateTime.now().format(FMT_FECHA));
        Consola.separador();
        System.out.println("  1. Registrar paciente");
        System.out.println("  2. Otorgar turno de extraccion");
        System.out.println("  3. Registrar orden de estudios");
        System.out.println("  4. Registrar extraccion (generar muestras)");
        System.out.println("  5. Avanzar circuito de una muestra");
        System.out.println("  6. Cargar resultados");
        System.out.println("  7. Validar y emitir informe");
        System.out.println("  8. Consultar catalogo de practicas");
        System.out.println("  9. Indicadores de gestion");
        System.out.println(" 10. Modulo persistente MySQL (MVC + JDBC)");
        System.out.println("  0. Salir");
        Consola.separador();
    }

    // ------------------------------------------------------------------ opcion 1

    private static void registrarPaciente() {
        Consola.titulo("Registro de paciente");
        String dni = Consola.leerTexto("  DNI: ");
        try {
            Paciente existente = laboratorio.buscarPaciente(dni);
            System.out.println("  [!] El paciente ya esta registrado: " + existente.getNombreCompleto());
            return;
        } catch (EntidadNoEncontradaException e) {
            // Comportamiento esperado: el paciente es nuevo y se continua con el alta.
            System.out.println("  Paciente nuevo, se procede al alta.");
        }
        String apellido = Consola.leerTexto("  Apellido: ");
        String nombre = Consola.leerTexto("  Nombre: ");
        int anio = Consola.leerEntero("  Anio de nacimiento: ", 1900, LocalDate.now().getYear());
        int mes = Consola.leerEntero("  Mes de nacimiento (1-12): ", 1, 12);
        int dia = Consola.leerEntero("  Dia de nacimiento (1-31): ", 1, 31);
        String obraSocial = Consola.leerTexto("  Obra social: ");
        String telefono = Consola.leerTexto("  Telefono: ");

        Paciente paciente = new Paciente(dni, apellido, nombre,
                LocalDate.of(anio, mes, Math.min(dia, LocalDate.of(anio, mes, 1).lengthOfMonth())),
                obraSocial, telefono);
        laboratorio.registrarPaciente(paciente);
        System.out.println("\n  [OK] Paciente registrado: " + paciente);
        if (paciente.esPediatrico()) {
            System.out.println("  [i] Paciente pediatrico: se requiere consentimiento del adulto responsable.");
        }
    }

    // ------------------------------------------------------------------ opcion 2

    private static void otorgarTurno() {
        Consola.titulo("Otorgamiento de turno");
        try {
            Paciente paciente = laboratorio.buscarPaciente(Consola.leerTexto("  DNI del paciente: "));
            int hora = Consola.leerEntero("  Hora deseada (formato 24 h): ", 0, 23);
            LocalDateTime fechaHora = LocalDate.now().plusDays(1).atTime(hora, 0);
            Turno turno = laboratorio.getAgenda().otorgarTurno(paciente, fechaHora);
            System.out.println("\n  [OK] Turno otorgado: " + turno);
            laboratorio.getAgenda().registrarPresente(turno);
            System.out.println("  [i] Paciente ingresado a la sala de espera. En cola: "
                    + laboratorio.getAgenda().getPacientesEsperando());
        } catch (EntidadNoEncontradaException e) {
            System.out.println("  [X] " + e.getMessage());
        } catch (TurnoNoDisponibleException e) {
            System.out.println("  [X] Turno no disponible: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ opcion 3

    private static void registrarOrden() {
        Consola.titulo("Registro de orden de estudios");
        try {
            Paciente paciente = laboratorio.buscarPaciente(Consola.leerTexto("  DNI del paciente: "));
            String medico = Consola.leerTexto("  Medico solicitante: ");
            Orden orden = laboratorio.crearOrden(paciente, medico);

            boolean seguir = true;
            while (seguir) {
                mostrarCatalogoResumido();
                String codigo = Consola.leerTexto("  Codigo de practica a agregar: ");
                try {
                    Estudio estudio = laboratorio.buscarEstudio(codigo);
                    orden.agregarEstudio(estudio);
                    System.out.println("  [OK] Agregado: " + estudio.getNombre()
                            + " ($" + String.format("%.2f", estudio.calcularPrecio()) + ")");
                } catch (EntidadNoEncontradaException e) {
                    System.out.println("  [X] " + e.getMessage());
                }
                seguir = Consola.confirmar("  Agregar otra practica?");
            }

            if (orden.getEstudios().isEmpty()) {
                System.out.println("  [!] La orden quedo sin practicas.");
                return;
            }
            Consola.separador();
            System.out.println("  Orden " + orden.getNumero() + " registrada.");
            System.out.println("  Total a abonar : $" + String.format("%.2f", orden.calcularTotal()));
            System.out.println("  Ayuno requerido: " + orden.calcularAyunoRequerido() + " horas");
            System.out.println("  Demora estimada: " + orden.calcularDemoraEstimadaHoras() + " horas");
            System.out.println("  Muestras a extraer: " + orden.obtenerTiposDeMuestra());
        } catch (EntidadNoEncontradaException e) {
            System.out.println("  [X] " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ opcion 4

    private static void registrarExtraccion() {
        Consola.titulo("Registro de extraccion");
        try {
            Orden orden = laboratorio.buscarOrden(Consola.leerTexto("  Numero de orden (ej. O-0001): "));
            System.out.println("  " + orden);
            int ayuno = Consola.leerEntero("  Horas de ayuno declaradas por el paciente: ", 0, 48);
            List<Muestra> generadas = laboratorio.generarMuestras(orden, ayuno, usuarioActual.getNombreCompleto());
            System.out.println("\n  [OK] Se generaron " + generadas.size() + " muestra(s):");
            for (Muestra muestra : generadas) {
                laboratorio.avanzarMuestra(muestra, EstadoMuestra.EXTRAIDA,
                        usuarioActual.getNombreCompleto(), "Extraccion en box de recepcion");
                System.out.println("    " + muestra);
            }
        } catch (EntidadNoEncontradaException e) {
            System.out.println("  [X] " + e.getMessage());
        } catch (AyunoInsuficienteException e) {
            System.out.println("  [X] Ayuno insuficiente: " + e.getMessage());
            System.out.println("      Faltan " + (e.getHorasRequeridas() - e.getHorasDeclaradas())
                    + " horas. Se debe reprogramar el turno.");
        } catch (MuestraInvalidaException e) {
            System.out.println("  [X] " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ opcion 5

    private static void avanzarCircuito() {
        Consola.titulo("Circuito de la muestra");
        if (laboratorio.getMuestras().isEmpty()) {
            System.out.println("  [!] Todavia no hay muestras registradas.");
            return;
        }
        for (Muestra muestra : laboratorio.getMuestras()) {
            System.out.println("  " + muestra);
        }
        Consola.separador();
        try {
            Muestra muestra = laboratorio.buscarMuestra(Consola.leerTexto("  Codigo de barra: "));
            System.out.println("\n  Historial de trazabilidad de " + muestra.getIdentificacion() + ":");
            muestra.getHistorial().forEach(evento -> System.out.println("    " + evento));
            Consola.separador();
            System.out.println("  Estados posibles:");
            EstadoMuestra[] estados = EstadoMuestra.values();
            for (int i = 0; i < estados.length; i++) {
                System.out.println("    " + (i + 1) + ". " + estados[i] + " - " + estados[i].getDescripcion());
            }
            int opcion = Consola.leerEntero("  Nuevo estado: ", 1, estados.length);
            String observacion = Consola.leerTexto("  Observacion: ");
            laboratorio.avanzarMuestra(muestra, estados[opcion - 1],
                    usuarioActual.getNombreCompleto(), observacion);
            System.out.println("\n  [OK] " + muestra);
            System.out.println("  TAT acumulado: " + muestra.calcularTatMinutos() + " minutos.");
        } catch (EntidadNoEncontradaException e) {
            System.out.println("  [X] " + e.getMessage());
        } catch (MuestraInvalidaException e) {
            System.out.println("  [X] Transicion invalida: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ opcion 6

    private static void cargarResultados() {
        Consola.titulo("Carga de resultados");
        try {
            Muestra muestra = laboratorio.buscarMuestra(Consola.leerTexto("  Codigo de barra de la muestra: "));
            if (muestra.getEstado() != EstadoMuestra.EN_PROCESO
                    && muestra.getEstado() != EstadoMuestra.ANALIZADA) {
                System.out.println("  [!] La muestra debe estar en proceso analitico. Estado actual: "
                        + muestra.getEstado());
                return;
            }
            for (Estudio estudio : muestra.getOrden().getEstudios()) {
                if (!estudio.getTipoMuestra().equals(muestra.getTipoMuestra())) {
                    continue;
                }
                System.out.println("\n  " + estudio.getCodigo() + " - " + estudio.getNombre());
                double valor = Consola.leerDecimal("    Valor obtenido: ");
                String unidad = Consola.leerTexto("    Unidad: ");
                double min = Consola.leerDecimal("    Referencia minima: ");
                double max = Consola.leerDecimal("    Referencia maxima: ");
                Resultado resultado = laboratorio.cargarResultado(muestra, estudio, valor, unidad, min, max);
                if (resultado.esCritico()) {
                    System.out.println("    [!!] VALOR CRITICO: se debe avisar al medico solicitante.");
                } else if (resultado.estaFueraDeRango()) {
                    System.out.println("    [!] Valor fuera del rango de referencia.");
                } else {
                    System.out.println("    [OK] Valor dentro del rango de referencia.");
                }
            }
        } catch (EntidadNoEncontradaException e) {
            System.out.println("  [X] " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ opcion 7

    private static void emitirInforme() {
        Consola.titulo("Validacion y emision de informe");
        try {
            Orden orden = laboratorio.buscarOrden(Consola.leerTexto("  Numero de orden: "));
            List<Resultado> propios = laboratorio.resultadosDeOrden(orden);
            if (propios.isEmpty()) {
                System.out.println("  [!] La orden no tiene resultados cargados.");
                return;
            }
            int validados = laboratorio.validarResultadosDeOrden(orden, usuarioActual);
            if (validados == 0 && !usuarioActual.puedeValidarResultados()) {
                System.out.println("  [X] El usuario " + usuarioActual.obtenerRol()
                        + " no esta habilitado para validar resultados.");
                return;
            }
            Consola.separador();
            System.out.println("  INFORME DE LABORATORIO - " + laboratorio.getNombre());
            System.out.println("  Orden   : " + orden.getNumero() + "   Fecha: " + orden.getFecha());
            System.out.println("  Paciente: " + orden.getPaciente().getNombreCompleto()
                    + " (DNI " + orden.getPaciente().getDni() + ", " + orden.getPaciente().getEdad() + " anios)");
            System.out.println("  Medico  : " + orden.getMedicoSolicitante());
            Consola.separador();
            for (Resultado resultado : propios) {
                System.out.println("  " + resultado);
            }
            Consola.separador();
            System.out.println("  Validado por: " + usuarioActual.getNombreCompleto()
                    + " - Mat. " + usuarioActual.getMatricula());
            System.out.println("  Resultados validados en esta operacion: " + validados);
        } catch (EntidadNoEncontradaException e) {
            System.out.println("  [X] " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ opcion 8

    private static void consultarCatalogo() {
        Consola.titulo("Catalogo de practicas");
        System.out.println("  1. Listar por codigo (arreglo ordenado por insercion)");
        System.out.println("  2. Listar por precio descendente (quicksort)");
        System.out.println("  3. Buscar por codigo (busqueda binaria)");
        int opcion = Consola.leerEntero("  Opcion: ", 1, 3);
        Consola.separador();
        if (opcion == 1) {
            for (Estudio estudio : laboratorio.getCatalogo()) {
                System.out.println("  " + estudio);
            }
        } else if (opcion == 2) {
            for (Estudio estudio : laboratorio.catalogoPorPrecioDescendente()) {
                System.out.println("  " + estudio);
            }
        } else {
            try {
                Estudio estudio = laboratorio.buscarEstudio(Consola.leerTexto("  Codigo: "));
                System.out.println("\n  " + estudio);
                System.out.println("  Tipo de muestra : " + estudio.getTipoMuestra());
                System.out.println("  Demora estimada : " + estudio.calcularHorasDemora() + " horas");
                System.out.println("  Clase concreta  : " + estudio.getClass().getSimpleName());
            } catch (EntidadNoEncontradaException e) {
                System.out.println("  [X] " + e.getMessage());
            }
        }
    }

    private static void mostrarCatalogoResumido() {
        Consola.separador();
        for (Estudio estudio : laboratorio.getCatalogo()) {
            System.out.printf("  %-6s %-34s $%9.2f%n",
                    estudio.getCodigo(), estudio.getNombre(), estudio.calcularPrecio());
        }
        Consola.separador();
    }

    // ------------------------------------------------------------------ opcion 9

    private static void verIndicadores() {
        Consola.titulo("Indicadores de gestion");
        System.out.println("  Pacientes registrados : " + laboratorio.getPacientes().size());
        System.out.println("  Turnos otorgados      : " + laboratorio.getAgenda().getTurnosOtorgados().size());
        System.out.println("  Pacientes en espera   : " + laboratorio.getAgenda().getPacientesEsperando());
        System.out.println("  Ordenes registradas   : " + laboratorio.getOrdenes().size());
        System.out.println("  Muestras generadas    : " + laboratorio.getMuestras().size());
        Consola.separador();
        for (EstadoMuestra estado : EstadoMuestra.values()) {
            System.out.printf("  %-12s %3d muestra(s)%n", estado, laboratorio.contarMuestrasEn(estado));
        }
        Consola.separador();
        System.out.printf("  TAT promedio          : %.1f minutos%n", laboratorio.calcularTatPromedio());
        System.out.println("  Ultima accion (pila)  : " + laboratorio.getPilaDeAcciones().peek());
    }

    // ---------------------------------------------------------- datos de prueba

    private static void cargarDatosDePrueba() {
        laboratorio.registrarPaciente(new Paciente("30111222", "Gomez", "Marcela",
                LocalDate.of(1983, 4, 12), "OSDE 210", "3534-556677"));
        laboratorio.registrarPaciente(new Paciente("45222333", "Suarez", "Tomas",
                LocalDate.of(2015, 9, 2), "APROSS", "3534-112233"));
        laboratorio.registrarPaciente(new Paciente("12888999", "Rios", "Alberto",
                LocalDate.of(1949, 1, 25), "PAMI", "3534-778899"));
    }
}
