package trazalab.pruebas;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import trazalab.app.MenuPrincipal;

/**
 * Ejecuta el prototipo completo (MenuPrincipal) con las entradas de un guion de
 * prueba de sistema, como si las tipeara un usuario.
 *
 * Entrega el guion de a una linea por vez, recien cuando el programa la pide, y
 * la repite en pantalla: la salida queda igual que la de una sesion interactiva,
 * con cada respuesta a continuacion de su pregunta. Las lineas que empiezan con
 * '#' son comentarios del guion y no se envian.
 *
 *     java -cp clases trazalab.pruebas.EjecutorSistema test/sistema/cps06.txt
 *
 * @author Alday, Rodrigo Matias
 */
public class EjecutorSistema {

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.forLanguageTag("es-AR"));
        List<String> lineas = new ArrayList<>();
        for (String linea : Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8)) {
            if (!linea.startsWith("#")) {
                lineas.add(linea);
            }
        }
        System.setIn(new EntradaGuionada(lineas));
        MenuPrincipal.main(new String[0]);
    }

    /** Flujo de entrada que devuelve una linea del guion por cada lectura y la repite en pantalla. */
    private static final class EntradaGuionada extends InputStream {

        private final List<String> lineas;
        private int siguiente;
        private byte[] actual = new byte[0];
        private int posicion;

        EntradaGuionada(List<String> lineas) {
            this.lineas = lineas;
        }

        private boolean cargarLinea() {
            if (posicion < actual.length) {
                return true;
            }
            if (siguiente >= lineas.size()) {
                return false;
            }
            String linea = lineas.get(siguiente++);
            System.out.println(linea);            // eco de la respuesta, como en una terminal
            actual = (linea + "\n").getBytes(StandardCharsets.UTF_8);
            posicion = 0;
            return true;
        }

        @Override
        public int read() {
            return cargarLinea() ? actual[posicion++] & 0xFF : -1;
        }

        /** Nunca entrega mas de una linea: asi el programa la pide recien despues de su pregunta. */
        @Override
        public int read(byte[] destino, int desde, int cantidad) {
            if (cantidad == 0) {
                return 0;
            }
            if (!cargarLinea()) {
                return -1;
            }
            int n = Math.min(cantidad, actual.length - posicion);
            System.arraycopy(actual, posicion, destino, desde, n);
            posicion += n;
            return n;
        }
    }
}
