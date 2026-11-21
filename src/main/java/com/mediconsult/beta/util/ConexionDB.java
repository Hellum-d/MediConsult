package com.mediconsult.beta.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton que gestiona la conexión JDBC a MySQL para Clinex.
 *
 * <p>Solo existe una conexión activa en toda la aplicación.
 * Todos los DAOs la obtienen a través de {@link #getConexion()}.</p>
 *
 * <p><strong>Configuración:</strong> los datos de conexión se leen de
 * variables de entorno; si no están definidas se usan los valores por defecto.</p>
 * <ul>
 *   <li>{@code MYSQL_URL}      — por defecto {@code jdbc:mysql://localhost:3306/clinex?...}</li>
 *   <li>{@code MYSQL_USER}     — por defecto {@code root}</li>
 *   <li>{@code MYSQL_PASSWORD} — por defecto {@code 1234}</li>
 * </ul>
 *
 * @author MediConsult Team
 * @version 0.3 — configuración por variables de entorno
 */
public class ConexionDB {

    // ── Configuración de conexión ──────────────────────────────────────────
    private static final String URL_POR_DEFECTO =
            "jdbc:mysql://localhost:3306/clinex?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private static final String URL      = leerVariable("MYSQL_URL",      URL_POR_DEFECTO);
    private static final String USER     = leerVariable("MYSQL_USER",     "root");
    private static final String PASSWORD = leerVariable("MYSQL_PASSWORD", "1234");

    // ── Instancia única ────────────────────────────────────────────────────
    private static ConexionDB instancia;
    private Connection         conexion;

    // ── Constructor privado ────────────────────────────────────────────────
    private ConexionDB() {
        try {
            this.conexion = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[ConexionDB] Conexión a MySQL establecida correctamente.");
        } catch (SQLException e) {
            throw new RuntimeException("[ConexionDB] No se pudo conectar a MySQL: " + e.getMessage(), e);
        }
    }

    /** Devuelve el valor de la variable de entorno, o {@code porDefecto} si no existe o está vacía. */
    private static String leerVariable(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return (valor == null || valor.isBlank()) ? porDefecto : valor;
    }

    // ── Punto de acceso único ──────────────────────────────────────────────

    /**
     * Devuelve la instancia única del Singleton.
     * Si la conexión se cerró o perdió, la reconecta automáticamente.
     *
     * @return instancia singleton de ConexionDB
     */
    public static ConexionDB getInstance() {
        try {
            if (instancia == null || instancia.conexion.isClosed()) {
                instancia = new ConexionDB();
            }
        } catch (SQLException e) {
            instancia = new ConexionDB();
        }
        return instancia;
    }

    /**
     * Devuelve el objeto {@link Connection} JDBC listo para usar.
     *
     * @return conexión activa a MySQL
     */
    public Connection getConexion() {
        return conexion;
    }

    /**
     * Describe a dónde intenta conectarse la aplicación (sin mostrar la contraseña).
     *
     * @return texto con la URL y el usuario configurados
     */
    public static String descripcionConfiguracion() {
        return "URL: " + URL + "\nUsuario: " + USER;
    }

    /**
     * Cierra la conexión al finalizar la aplicación.
     * Llamar desde {@code Main.java} al salir.
     */
    public static void cerrar() {
        try {
            if (instancia != null && !instancia.conexion.isClosed()) {
                instancia.conexion.close();
                System.out.println("[ConexionDB] Conexión cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("[ConexionDB] Error al cerrar: " + e.getMessage());
        }
    }
}
