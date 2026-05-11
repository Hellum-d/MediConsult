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
 * Utilidad para leer y escribir archivos CSV dentro de la carpeta data/.
 *
 * Reemplaza el uso de base de datos: aquí todo se persiste en archivos.
 * Solo las clases dao.impl deben usar esta clase directamente.
 */
public final class ArchivoUtil {

    /** Separador de campos del CSV. */
    public static final String SEPARADOR = ";";

    /** Carpeta donde viven todos los CSV. */
    private static final String CARPETA_DATA = "data";

    /** Marca usada para guardar ; dentro de un campo. */
    private static final String MARCA_SC = "[SC]";

    /** Marca usada para guardar saltos de línea dentro de un campo. */
    private static final String MARCA_NL = "[NL]";

    // Se asegura de que exista la carpeta data/ al cargar la clase
    static {
        asegurarCarpetaData();
    }

    // Clase de utilidad: no se instancia
    private ArchivoUtil() {}

    /**
     * Lee las líneas no vacías de un CSV.
     * Si no existe, lo crea vacío.
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
     * Sobrescribe completamente el contenido del archivo con las líneas dadas.
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
     * Agrega una línea al final del archivo sin cargarlo completo en memoria.
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
     * Calcula el siguiente id autoincremental leyendo el primer campo de cada línea.
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
                    // Si el id no es numérico, se ignora
                }
            }
        }
        return maxId + 1;
    }

    /**
     * Reemplaza caracteres problemáticos para que el campo pueda guardarse en el CSV.
     */
    public static String escapar(String valor) {
        if (valor == null) return "";
        return valor
                .replace("\r\n", MARCA_NL)
                .replace("\n",   MARCA_NL)
                .replace(SEPARADOR, MARCA_SC);
    }

    /**
     * Restaura el valor original leído del CSV.
     */
    public static String desescapar(String valor) {
        if (valor == null) return "";
        return valor
                .replace(MARCA_SC, SEPARADOR)
                .replace(MARCA_NL, "\n");
    }

    /** Construye la ruta completa dentro de data/. */
    private static Path resolverRuta(String nombreArchivo) {
        return Paths.get(CARPETA_DATA, nombreArchivo);
    }

    /** Crea la carpeta data/ si no existe. */
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