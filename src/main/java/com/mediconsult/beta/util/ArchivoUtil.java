package com.mediconsult.beta.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad central de lectura y escritura de archivos CSV para MediConsult-Beta.
 *
 * <p>Reemplaza a {@code ConexionDB} de la versión JavaFX/MySQL: en lugar de
 * abrir una conexión JDBC, esta clase proporciona métodos estáticos para
 * leer y escribir los archivos {@code .csv} almacenados en la carpeta
 * {@code data/} relativa al directorio de ejecución.</p>
 *
 * <p><strong>Formato CSV:</strong> Los campos se separan con punto y coma
 * ({@value #SEPARADOR}). Los valores que puedan contener ese carácter o saltos
 * de línea deben pasarse previamente por {@link #escapar(String)} antes de
 * escribirse, y por {@link #desescapar(String)} al leerse.</p>
 *
 * <p><strong>Regla de capa:</strong> Solo las clases de {@code dao.impl}
 * deben invocar esta clase. Los {@code service} y las vistas no acceden
 * directamente a archivos.</p>
 *
 * @author MediConsult Team
 * @version 0.1
 */
public final class ArchivoUtil {

    // ── Constantes ────────────────────────────────────────────────────────

    /** Separador de campos en el CSV. */
    public static final String SEPARADOR = ";";

    /** Carpeta donde se almacenan todos los archivos CSV. */
    private static final String CARPETA_DATA = "data";

    /**
     * Marcador interno para representar punto y coma dentro de un campo.
     * Se usa al escapar/desescapar valores que puedan contener {@code ;}.
     */
    private static final String MARCA_SC = "[SC]";

    /**
     * Marcador interno para representar salto de línea dentro de un campo.
     * Se usa al escapar/desescapar valores que puedan contener {@code \n}.
     */
    private static final String MARCA_NL = "[NL]";

    // ── Bloque estático de inicialización ─────────────────────────────────

    static {
        asegurarCarpetaData();
    }

    /** Constructor privado: clase de utilidad, no instanciable. */
    private ArchivoUtil() {}

    // ── Métodos públicos principales ──────────────────────────────────────

    /**
     * Lee todas las líneas no vacías de un archivo CSV dentro de {@code data/}.
     *
     * <p>Si el archivo no existe, lo crea vacío y devuelve una lista vacía.</p>
     *
     * @param nombreArchivo nombre del archivo (p. ej. {@code "pacientes.csv"})
     * @return lista de líneas; lista vacía si el archivo está vacío o no existe
     * @throws RuntimeException si ocurre un {@code IOException} al leer
     */
    public static List<String> leerLineas(String nombreArchivo) {
        Path ruta = resolverRuta(nombreArchivo);
        try {
            if (!Files.exists(ruta)) {
                Files.createFile(ruta);
                return new ArrayList<>();
            }
            List<String> todasLasLineas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
            List<String> resultado = new ArrayList<>();
            for (String linea : todasLasLineas) {
                if (!linea.isBlank()) {
                    resultado.add(linea);
                }
            }
            return resultado;
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo: " + ruta, e);
        }
    }

    /**
     * Sobreescribe un archivo CSV con la lista de líneas proporcionada.
     *
     * <p>Se agrega un salto de línea final después de cada línea.
     * Si el archivo no existe, se crea.</p>
     *
     * @param nombreArchivo nombre del archivo (p. ej. {@code "pacientes.csv"})
     * @param lineas        contenido completo a escribir
     * @throws RuntimeException si ocurre un {@code IOException} al escribir
     */
    public static void escribirLineas(String nombreArchivo, List<String> lineas) {
        Path ruta = resolverRuta(nombreArchivo);
        try {
            StringBuilder sb = new StringBuilder();
            for (String linea : lineas) {
                sb.append(linea).append(System.lineSeparator());
            }
            Files.writeString(ruta, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Error al escribir el archivo: " + ruta, e);
        }
    }

    /**
     * Añade una línea al final de un archivo CSV sin cargar todo su contenido.
     *
     * <p>Si el archivo no existe, se crea automáticamente.</p>
     *
     * @param nombreArchivo nombre del archivo (p. ej. {@code "pacientes.csv"})
     * @param linea         línea a añadir (sin salto de línea al final)
     * @throws RuntimeException si ocurre un {@code IOException} al escribir
     */
    public static void agregarLinea(String nombreArchivo, String linea) {
        Path ruta = resolverRuta(nombreArchivo);
        try {
            Files.writeString(ruta, linea + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Error al agregar línea al archivo: " + ruta, e);
        }
    }

    /**
     * Calcula el siguiente identificador autoincremental para un archivo CSV.
     *
     * <p>Lee todas las líneas del archivo, extrae el primer campo (id) de cada
     * una y devuelve {@code max(ids) + 1}. Si el archivo está vacío, devuelve
     * {@code 1}.</p>
     *
     * @param nombreArchivo nombre del archivo CSV
     * @return identificador único siguiente (mínimo {@code 1})
     * @throws RuntimeException si ocurre un {@code IOException} al leer
     */
    public static int siguienteId(String nombreArchivo) {
        List<String> lineas = leerLineas(nombreArchivo);
        int maxId = 0;
        for (String linea : lineas) {
            String[] campos = linea.split(SEPARADOR, -1);
            if (campos.length > 0) {
                try {
                    int id = Integer.parseInt(campos[0].trim());
                    if (id > maxId) {
                        maxId = id;
                    }
                } catch (NumberFormatException ignored) {
                    // línea con id no numérico; se ignora
                }
            }
        }
        return maxId + 1;
    }

    // ── Escapado / desescapado ─────────────────────────────────────────────

    /**
     * Escapa un valor de campo para que sea seguro dentro del CSV.
     *
     * <p>Reemplaza:</p>
     * <ul>
     *   <li>{@code ;} → {@value #MARCA_SC}</li>
     *   <li>{@code \n} y {@code \r\n} → {@value #MARCA_NL}</li>
     * </ul>
     *
     * <p>Usar siempre antes de serializar campos como {@code historial}
     * o {@code resultadoIa} que pueden contener esos caracteres.</p>
     *
     * @param valor valor original del campo; si es {@code null} devuelve cadena vacía
     * @return valor escapado, seguro para incluir en una línea CSV
     */
    public static String escapar(String valor) {
        if (valor == null) return "";
        return valor
                .replace("\r\n", MARCA_NL)
                .replace("\n",   MARCA_NL)
                .replace(SEPARADOR, MARCA_SC);
    }

    /**
     * Restaura un valor de campo leído del CSV a su forma original.
     *
     * <p>Reemplaza:</p>
     * <ul>
     *   <li>{@value #MARCA_SC} → {@code ;}</li>
     *   <li>{@value #MARCA_NL} → {@code \n}</li>
     * </ul>
     *
     * @param valor valor leído del CSV; si es {@code null} devuelve cadena vacía
     * @return valor con los marcadores restaurados a sus caracteres originales
     */
    public static String desescapar(String valor) {
        if (valor == null) return "";
        return valor
                .replace(MARCA_SC, SEPARADOR)
                .replace(MARCA_NL, "\n");
    }

    // ── Métodos privados ──────────────────────────────────────────────────

    /**
     * Resuelve la ruta absoluta de un archivo dentro de {@code data/}.
     *
     * @param nombreArchivo nombre del archivo
     * @return {@link Path} completo al archivo
     */
    private static Path resolverRuta(String nombreArchivo) {
        return Paths.get(CARPETA_DATA, nombreArchivo);
    }

    /**
     * Crea la carpeta {@code data/} si aún no existe.
     * Se invoca una sola vez desde el bloque estático al cargar la clase.
     */
    private static void asegurarCarpetaData() {
        try {
            Path carpeta = Paths.get(CARPETA_DATA);
            if (!Files.exists(carpeta)) {
                Files.createDirectories(carpeta);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la carpeta 'data/'", e);
        }
    }
}
