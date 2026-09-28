package trazalab.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MANIPULACION DE ARCHIVOS. Exporta la trazabilidad de una muestra a un archivo
 * de texto, que es el comprobante que el laboratorio entrega ante una auditoria.
 *
 * Usa la jerarquia de java.io: FileWriter (flujo de salida a archivo),
 * BufferedWriter (buffer intermedio) y PrintWriter (metodos println).
 *
 * @author Alday, Rodrigo Matias
 */
public final class ExportadorArchivos {

    private static final String CARPETA = "salidas";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ExportadorArchivos() {
    }

    /**
     * Escribe el historial completo de una muestra en salidas/trazabilidad-*.txt.
     *
     * @return la ruta absoluta del archivo generado.
     * @throws IOException si no se puede crear la carpeta o escribir el archivo.
     */
    public static String exportarTrazabilidad(String codigoBarra, String paciente,
                                              String orden, List<String> historial) throws IOException {
        File carpeta = new File(CARPETA);
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getAbsolutePath());
        }
        File archivo = new File(carpeta,
                "trazabilidad-" + codigoBarra + "-" + LocalDateTime.now().format(FMT) + ".txt");

        // try-with-resources: cierra los flujos aunque se produzca una excepcion.
        try (PrintWriter salida = new PrintWriter(new BufferedWriter(new FileWriter(archivo)))) {
            salida.println("=".repeat(78));
            salida.println("TRAZALAB - CERTIFICADO DE TRAZABILIDAD DE MUESTRA");
            salida.println("Laboratorio Bioquimico San Rafael");
            salida.println("=".repeat(78));
            salida.println("Muestra   : " + codigoBarra);
            salida.println("Orden     : " + orden);
            salida.println("Paciente  : " + paciente);
            salida.println("Emitido   : " + LocalDateTime.now());
            salida.println("-".repeat(78));
            salida.println("HISTORIAL DE EVENTOS");
            salida.println("-".repeat(78));
            if (historial.isEmpty()) {
                salida.println("Sin eventos registrados.");
            } else {
                for (String evento : historial) {
                    salida.println(evento);
                }
            }
            salida.println("-".repeat(78));
            salida.println("Total de eventos: " + historial.size());
            salida.println("Documento generado automaticamente por el sistema TRAZALAB.");
            salida.println("=".repeat(78));
        }
        return archivo.getAbsolutePath();
    }

    /** Exporta el listado del catalogo en formato CSV, apto para planilla de calculo. */
    public static String exportarCsv(String nombreBase, String encabezado, List<String> filas)
            throws IOException {
        File carpeta = new File(CARPETA);
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta " + carpeta.getAbsolutePath());
        }
        File archivo = new File(carpeta, nombreBase + "-" + LocalDateTime.now().format(FMT) + ".csv");
        try (PrintWriter salida = new PrintWriter(new BufferedWriter(new FileWriter(archivo)))) {
            salida.println(encabezado);
            filas.forEach(salida::println);
        }
        return archivo.getAbsolutePath();
    }
}
