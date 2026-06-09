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
 * <p><strong>Configuración:</strong> Cambia las constantes URL, USER y PASSWORD
 * según tu entorno local antes de ejecutar.</p>
 *
 * @author MediConsult Team
 * @version 0.2 — migración JDBC
 */
public class ConexionDB {

    // ── Configuración de conexión ──────────────────────────────────────────
    private static final String URL      = "jdbc:mysql://localhost:3306/clinex?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "1234"; //

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