package com.mediconsult.beta.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Usuario del sistema MediConsult-Beta.
 *
 * Contiene credenciales y rol para control de acceso.
 * La contraseña siempre se maneja como hash SHA-256.
 */
public class Usuario {

    // ── Roles ─────────────────────────────────────────────────────────────

    /** Acceso total al sistema. */
    public static final String ROL_ADMIN  = "ADMIN";

    /** Acceso limitado al módulo médico. */
    public static final String ROL_MEDICO = "MEDICO";

    // ── Datos ─────────────────────────────────────────────────────────────

    /** ID del usuario (0 si aún no está guardado). */
    private int id;

    /** Nombre de usuario único. */
    private String username;

    /**
     * Contraseña en formato hash (SHA-256).
     * Nunca se guarda texto plano.
     */
    private String password;

    /** Rol del usuario en el sistema. */
    private String rol;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Constructor usado al cargar desde CSV.
     * Se asume que la contraseña ya está hasheada.
     * @param id
     * @param username
     * @param password
     * @param rol
     */
    public Usuario(int id, String username, String password, String rol) {
        this.id       = id;
        this.username = username;
        this.password = password;
        this.rol      = rol;
    }

    /**
     * Constructor para registro de nuevos usuarios.
     * Hashea automáticamente la contraseña.
     * @param username
     * @param passwordPlainText
     * @param rol
     */
    public Usuario(String username, String passwordPlainText, String rol) {
        this(0, username, hashPassword(passwordPlainText), rol);
    }

    // ── Seguridad ─────────────────────────────────────────────────────────

    /**
     * Convierte una contraseña en hash SHA-256.
     * @param input
     * @return 
     */
    public static String hashPassword(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : hashBytes) {
                hexBuilder.append(String.format("%02x", b));
            }
            return hexBuilder.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible en esta JVM", e);
        }
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    /**
     * Devuelve el hash de la contraseña (no texto plano).
     * @return 
     */
    public String getPassword() { return password; }

    /**
     * Recibe contraseña en texto plano y la hashea.
     * @param passwordPlainText
     */
    public void setPassword(String passwordPlainText) {
        this.password = hashPassword(passwordPlainText);
    }

    /**
     * Asigna directamente un hash (uso interno al cargar CSV).
     * @param hashedPassword
     */
    public void setPasswordHash(String hashedPassword) {
        this.password = hashedPassword;
    }

    public String getRol() { return rol; }

    public void setRol(String rol) { this.rol = rol; }

    // ── Debug ─────────────────────────────────────────────────────────────

    /**
     * No incluye contraseña por seguridad.
     * @return 
     */
    @Override
    public String toString() {
        return "Usuario{id=" + id
                + ", username='" + username + "'"
                + ", rol='" + rol + "'}";
    }
}