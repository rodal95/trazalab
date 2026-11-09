package trazalab.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Flujo de salida para la version web: colorea las lineas de resultado con
 * codigos ANSI, que la terminal del navegador interpreta. [OK] en verde, [X] en
 * rojo, [!] en amarillo e [i] en celeste. El resto pasa sin cambios y los
 * mensajes que piden un dato (sin salto de linea) se muestran de inmediato.
 *
 * @author Alday, Rodrigo Matias
 */
public class SalidaColoreada extends FilterOutputStream {

    private static final byte[] FIN = "\u001b[0m".getBytes();

    /** Principio de linea retenido hasta saber si es una marca ([OK], [X], ...). */
    private final StringBuilder inicio = new StringBuilder();
    private boolean decidido;
    private boolean coloreada;

    public SalidaColoreada(OutputStream destino) {
        super(destino);
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\n') {
            soltar(null);
            if (coloreada) {
                out.write(FIN);
            }
            out.write(b);
            decidido = false;
            coloreada = false;
            return;
        }
        if (decidido) {
            out.write(b);
            return;
        }
        inicio.append((char) b);
        String texto = inicio.toString().trim();
        if (texto.isEmpty()) {
            return;
        }
        if (texto.charAt(0) != '[') {
            soltar(null);
        } else if (texto.indexOf(']') > 0 || texto.length() > 5) {
            soltar(color(texto));
        }
    }

    @Override
    public void flush() throws IOException {
        if (!decidido && inicio.length() > 0 && inicio.toString().trim().isEmpty()) {
            // solo espacios hasta ahora: se esperan mas datos de la misma linea
            out.flush();
            return;
        }
        if (!decidido && inicio.length() > 0) {
            soltar(null);
        }
        // Un mensaje que pide un dato termina el renglon: lo que se imprima despues de
        // la respuesta empieza un renglon nuevo (el salto lo puso la terminal al escribir).
        if (decidido) {
            if (coloreada) {
                out.write(FIN);
            }
            decidido = false;
            coloreada = false;
        }
        out.flush();
    }

    private void soltar(String color) throws IOException {
        if (color != null) {
            out.write(color.getBytes());
            coloreada = true;
        }
        for (int i = 0; i < inicio.length(); i++) {
            out.write(inicio.charAt(i));
        }
        inicio.setLength(0);
        decidido = true;
    }

    private static String color(String marca) {
        if (marca.startsWith("[OK]")) {
            return "\u001b[32m";
        }
        if (marca.startsWith("[X]")) {
            return "\u001b[31m";
        }
        if (marca.startsWith("[!")) {
            return "\u001b[33m";
        }
        if (marca.startsWith("[i]")) {
            return "\u001b[36m";
        }
        return null;
    }
}
