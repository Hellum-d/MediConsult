package com.mediconsult.beta.util;

import java.util.Scanner;

/**
 * Utilidad para imprimir en consola y leer entradas del usuario.
 *
 * Centraliza encabezados, separadores, mensajes y lectura validada.
 * Solo debe usarse desde la capa view.
 */
public final class ConsoleUtil {

    /** Ancho total del recuadro del encabezado. */
    private static final int ANCHO = 38;

    /** Caracteres usados para dibujar el marco. */
    private static final char ESQ_SUP_IZQ = '╔';
    private static final char ESQ_SUP_DER = '╗';
    private static final char ESQ_INF_IZQ = '╚';
    private static final char ESQ_INF_DER = '╝';
    private static final char LINEA_H = '═';
    private static final char LINEA_V = '║';

    /** Separador horizontal simple. */
    private static final char SEP_SIMPLE = '─';

    // Clase de utilidad: no se instancia
    private ConsoleUtil() {}

    /**
     * Imprime un encabezado enmarcado y centrado.
     * @param titulo
     */
    public static void imprimirEncabezado(String titulo) {
        String tituloMayus = titulo.toUpperCase();
        int espacioInterior = ANCHO - 2;
        String lineaH = repetir(LINEA_H, espacioInterior);

        System.out.println(ESQ_SUP_IZQ + lineaH + ESQ_SUP_DER);
        System.out.println(LINEA_V + centrar(tituloMayus, espacioInterior) + LINEA_V);
        System.out.println(ESQ_INF_IZQ + lineaH + ESQ_INF_DER);
    }

    /**
     * Imprime una línea separadora horizontal.
     */
    public static void imprimirSeparador() {
        System.out.println(repetir(SEP_SIMPLE, ANCHO));
    }

    /**
     * Muestra un mensaje de error.
     * @param mensaje
     */
    public static void imprimirError(String mensaje) {
        System.out.println("[✗] " + mensaje);
    }

    /**
     * Muestra un mensaje de éxito.
     * @param mensaje
     */
    public static void imprimirExito(String mensaje) {
        System.out.println("[✓] " + mensaje);
    }

    /**
     * Muestra un mensaje informativo.
     * @param mensaje
     */
    public static void imprimirInfo(String mensaje) {
        System.out.println("[i] " + mensaje);
    }

    /**
     * Lee un número entero. Reintenta hasta que sea válido.
     * @param sc
     * @param prompt
     * @return 
     */
    public static int leerEntero(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String linea = sc.nextLine().trim();
            try {
                return Integer.parseInt(linea);
            } catch (NumberFormatException e) {
                imprimirError("Entrada inválida. Ingrese un número entero.");
            }
        }
    }

    /**
     * Lee un texto obligatorio (no vacío).
     * @param sc
     * @param prompt
     * @return 
     */
    public static String leerTexto(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String linea = sc.nextLine().trim();
            if (!linea.isEmpty()) {
                return linea;
            }
            imprimirError("El campo no puede estar vacío. Intente de nuevo.");
        }
    }

    /**
     * Lee un texto que puede quedar vacío.
     * @param sc
     * @param prompt
     * @return 
     */
    public static String leerTextoOpcional(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    /**
     * Lee una confirmación tipo s/n.
     * @param sc
     * @param prompt
     * @return 
     */
    public static boolean leerConfirmacion(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt + " (s/n): ");
            String linea = sc.nextLine().trim().toLowerCase();
            if (linea.equals("s")) return true;
            if (linea.equals("n")) return false;
            imprimirError("Responda 's' para sí o 'n' para no.");
        }
    }

    /** Repite un carácter n veces. */
    private static String repetir(char c, int n) {
        return String.valueOf(c).repeat(Math.max(0, n));
    }

    /**
     * Centra un texto en un ancho dado.
     * Si es más largo, lo corta.
     */
    private static String centrar(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto.substring(0, ancho);
        }
        int relleno = ancho - texto.length();
        int izq = relleno / 2;
        int der = relleno - izq;
        return " ".repeat(izq) + texto + " ".repeat(der);
    }
}