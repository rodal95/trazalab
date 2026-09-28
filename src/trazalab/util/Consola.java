package trazalab.util;

import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Utilidades de entrada y salida por consola con MANEJO DE EXCEPCIONES.
 * Centraliza la lectura para que ningun dato mal tipeado interrumpa el programa.
 *
 * @author Alday, Rodrigo Matias
 */
public final class Consola {

    private static final Scanner ENTRADA = new Scanner(System.in);

    private Consola() {
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
            try {
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

    /** Lee un numero real con el mismo tratamiento de excepciones. */
    public static double leerDecimal(String mensaje) {
        double valor = 0;
        boolean valido = false;
        while (!valido) {
            System.out.print(mensaje);
            try {
                valor = ENTRADA.nextDouble();
                ENTRADA.nextLine();
                valido = true;
            } catch (InputMismatchException e) {
                System.out.println("  [!] Valor no valido: se esperaba un numero.");
                ENTRADA.nextLine();
            }
        }
        return valor;
    }

    /** Lee una linea de texto no vacia. */
    public static String leerTexto(String mensaje) {
        String texto = "";
        while (texto.trim().isEmpty()) {
            System.out.print(mensaje);
            texto = ENTRADA.nextLine();
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
