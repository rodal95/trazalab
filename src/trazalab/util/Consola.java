package trazalab.util;

import java.util.InputMismatchException;
import java.util.Scanner;
import trazalab.excepciones.VolverAlMenuException;

/**
 * Utilidades de entrada y salida por consola con MANEJO DE EXCEPCIONES.
 * Centraliza la lectura para que ningun dato mal tipeado interrumpa el programa.
 *
 * @author Alday, Rodrigo Matias
 */
public final class Consola {

    private static final Scanner ENTRADA = new Scanner(System.in);

    /** Version web: escribir "volver" en cualquier dato cancela la operacion. */
    private static final boolean VOLVER_ACTIVO = Boolean.getBoolean("trazalab.web");

    private Consola() {
    }

    /** Si el texto es "volver" (version web), cancela la operacion en curso. */
    private static void revisarVolver(String texto) {
        if (VOLVER_ACTIVO && texto != null && texto.trim().equalsIgnoreCase("volver")) {
            throw new VolverAlMenuException();
        }
    }

    /**
     * Lee un entero validando el tipo de dato y el rango admitido.
     * Captura InputMismatchException y vuelve a solicitar el valor.
     */
    public static int leerEntero(String mensaje, int minimo, int maximo) {
        int valor = minimo - 1;
        boolean valido = false;
        while (!valido) {
            System.out.print(mensaje);
            System.out.flush();   // el mensaje queda completo antes de leer
            try {
                if (!ENTRADA.hasNextInt() && VOLVER_ACTIVO) {
                    // version web: "volver" cancela y regresa al menu anterior
                    String texto = ENTRADA.nextLine();
                    revisarVolver(texto);
                    System.out.println("  [!] Opcion no valida: se esperaba un numero entero.");
                    continue;
                }
                valor = ENTRADA.nextInt();
                ENTRADA.nextLine();
                if (valor < minimo || valor > maximo) {
                    System.out.println("  [!] El valor debe estar entre " + minimo + " y " + maximo + ".");
                } else {
                    valido = true;
                }
            } catch (InputMismatchException e) {
                System.out.println("  [!] Opcion no valida: se esperaba un numero entero.");
                ENTRADA.nextLine(); // limpia el buffer para no entrar en bucle infinito
            }
        }
        return valor;
    }

    /**
     * Lee un numero real. Acepta coma o punto decimal (2,1 o 2.1): en la version web
     * cada visitante escribe con la convencion de su teclado.
     */
    public static double leerDecimal(String mensaje) {
        double valor = 0;
        boolean valido = false;
        while (!valido) {
            System.out.print(mensaje);
            System.out.flush();   // el mensaje queda completo antes de leer
            String texto = ENTRADA.nextLine().trim();
            revisarVolver(texto);
            try {
                valor = Double.parseDouble(texto.replace(',', '.'));
                valido = true;
            } catch (NumberFormatException e) {
                System.out.println("  [!] Valor no valido: se esperaba un numero.");
            }
        }
        return valor;
    }

    /** Lee una linea de texto no vacia. */
    public static String leerTexto(String mensaje) {
        String texto = "";
        while (texto.trim().isEmpty()) {
            System.out.print(mensaje);
            System.out.flush();   // el mensaje queda completo antes de leer
            texto = ENTRADA.nextLine();
            revisarVolver(texto);
            if (texto.trim().isEmpty()) {
                System.out.println("  [!] El dato no puede quedar vacio.");
            }
        }
        return texto.trim();
    }

    /** Pregunta por si o por no. */
    public static boolean confirmar(String mensaje) {
        String respuesta = leerTexto(mensaje + " (s/n): ");
        return respuesta.equalsIgnoreCase("s") || respuesta.equalsIgnoreCase("si");
    }

    public static void titulo(String texto) {
        System.out.println();
        System.out.println("=".repeat(72));
        System.out.println("  " + texto.toUpperCase());
        System.out.println("=".repeat(72));
    }

    public static void separador() {
        System.out.println("-".repeat(72));
    }

    public static void pausa() {
        System.out.print("\n  [Enter] para continuar...");
        ENTRADA.nextLine();
    }
}
