package com.mediconsult.beta.util;

import java.util.Scanner;

/**
 * Utilidad de formato y lectura para la interfaz de consola de MediConsult-Beta.
 *
 * <p>Centraliza la lógica de impresión de encabezados, separadores y
 * mensajes de estado, así como la lectura validada de entradas del usuario.
 * Todos los métodos son {@code static}; la clase no es instanciable.</p>
 *
 * <p><strong>Regla de capa:</strong> Solo las clases de {@code view} deben
 * invocar esta clase.</p>
 *
 * @author MediConsult Team
 * @version 0.1
 */
public final class ConsoleUtil {

    // ── Constantes de formato ─────────────────────────────────────────────

    /** Ancho total del recuadro de encabezado (incluyendo bordes). */
    private static final int ANCHO = 38;

    /** Carácter de esquina superior izquierda. */
    private static final char ESQ_SUP_IZQ = '╔';

    /** Carácter de esquina superior derecha. */
    private static final char ESQ_SUP_DER = '╗';

    /** Carácter de esquina inferior izquierda. */
    private static final char ESQ_INF_IZQ = '╚';

    /** Carácter de esquina inferior derecha. */
    private static final char ESQ_INF_DER = '╝';

    /** Carácter de línea horizontal doble. */
    private static final char LINEA_H = '═';

    /** Carácter de línea vertical doble. */
    private static final char LINEA_V = '║';

    /** Carácter de separador simple. */
    private static final char SEP_SIMPLE = '─';

    /** Constructor privado: clase de utilidad, no instanciable. */
    private ConsoleUtil() {}

    // ── Impresión de estructura ───────────────────────────────────────────

    /**
     * Imprime un encabezado enmarcado con bordes dobles y el título centrado.
     *
     * <p>Ejemplo de salida:</p>
     * <pre>
     * ╔══════════════════════════════════════╗
     * ║        GESTIÓN DE PACIENTES          ║
     * ╚══════════════════════════════════════╝
     * </pre>
     *
     * @param titulo texto a mostrar como título; se convierte a mayúsculas
     */
    public static void imprimirEncabezado(String titulo) {
        String tituloMayus = titulo.toUpperCase();
        int espacioInterior = ANCHO - 2; // sin los dos bordes laterales
        String lineaH = repetir(LINEA_H, espacioInterior);

        System.out.println(ESQ_SUP_IZQ + lineaH + ESQ_SUP_DER);
        System.out.println(LINEA_V + centrar(tituloMayus, espacioInterior) + LINEA_V);
        System.out.println(ESQ_INF_IZQ + lineaH + ESQ_INF_DER);
    }

    /**
     * Imprime una línea separadora horizontal de caracteres simples.
     *
     * <p>Ejemplo: {@code ──────────────────────────────────────}</p>
     */
    public static void imprimirSeparador() {
        System.out.println(repetir(SEP_SIMPLE, ANCHO));
    }

    /**
     * Imprime un mensaje de error precedido por {@code [✗]}.
     *
     * @param mensaje descripción del error
     */
    public static void imprimirError(String mensaje) {
        System.out.println("[✗] " + mensaje);
    }

    /**
     * Imprime un mensaje de éxito precedido por {@code [✓]}.
     *
     * @param mensaje descripción de la operación exitosa
     */
    public static void imprimirExito(String mensaje) {
        System.out.println("[✓] " + mensaje);
    }

    /**
     * Imprime un mensaje informativo precedido por {@code [i]}.
     *
     * @param mensaje información a mostrar
     */
    public static void imprimirInfo(String mensaje) {
        System.out.println("[i] " + mensaje);
    }

    // ── Lectura validada de input ─────────────────────────────────────────

    /**
     * Muestra un prompt y lee un número entero del usuario.
     *
     * <p>Si la entrada no es un entero válido, imprime un mensaje de error
     * y vuelve a pedir el dato hasta obtener una respuesta válida.</p>
     *
     * @param sc     instancia de {@link Scanner} conectada a {@code System.in}
     * @param prompt texto a mostrar antes de leer
     * @return entero ingresado por el usuario
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
     * Muestra un prompt y lee una cadena de texto no vacía del usuario.
     *
     * <p>Si el usuario pulsa Enter sin escribir nada, imprime un mensaje de
     * error y vuelve a pedir el dato.</p>
     *
     * @param sc     instancia de {@link Scanner} conectada a {@code System.in}
     * @param prompt texto a mostrar antes de leer
     * @return cadena no vacía ingresada por el usuario
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
     * Muestra un prompt y lee una cadena de texto que puede ser vacía.
     *
     * <p>A diferencia de {@link #leerTexto(Scanner, String)}, este método
     * acepta que el usuario pulse Enter sin escribir nada (devuelve cadena vacía).</p>
     *
     * @param sc     instancia de {@link Scanner} conectada a {@code System.in}
     * @param prompt texto a mostrar antes de leer
     * @return cadena ingresada por el usuario; puede ser vacía
     */
    public static String leerTextoOpcional(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    /**
     * Muestra un prompt y lee una respuesta de tipo sí/no ({@code s/n}).
     *
     * <p>Acepta: {@code s}, {@code S}, {@code n}, {@code N}.
     * Reintenta si la entrada no es ninguna de las anteriores.</p>
     *
     * @param sc     instancia de {@link Scanner} conectada a {@code System.in}
     * @param prompt texto a mostrar antes de leer
     * @return {@code true} si el usuario respondió {@code s/S},
     *         {@code false} si respondió {@code n/N}
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

    // ── Helpers privados ──────────────────────────────────────────────────

    /**
     * Devuelve una cadena con el carácter {@code c} repetido {@code n} veces.
     *
     * @param c carácter a repetir
     * @param n número de repeticiones
     * @return cadena resultante
     */
    private static String repetir(char c, int n) {
        return String.valueOf(c).repeat(Math.max(0, n));
    }

    /**
     * Centra un texto dentro de un ancho dado, rellenando con espacios.
     * Si el texto supera el ancho, se trunca.
     *
     * @param texto texto a centrar
     * @param ancho ancho total disponible (en caracteres)
     * @return texto centrado con espacios de relleno a ambos lados
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
