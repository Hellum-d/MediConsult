package com.mediconsult.beta.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Representa a un usuario del sistema MediConsult-Beta con credenciales de acceso.
 *
 * <p>Contiene el nombre de usuario, la contraseña almacenada como hash SHA-256
 * y el rol que determina los permisos dentro de la aplicación:</p>
 * <ul>
 *   <li>{@code ADMIN}  — acceso total: gestión de pacientes, médicos y citas</li>
 *   <li>{@code MEDICO} — acceso a citas propias y módulo de diagnóstico</li>
 * </ul>
 *
 * <p><strong>Seguridad:</strong> La contraseña nunca se almacena en texto plano.
 * El método estático {@link #hashPassword(String)} aplica SHA-256 con
 * {@link MessageDigest} de la librería estándar de Java.</p>
 *
 * <p><strong>Regla de capa:</strong> Esta clase no importa ningún paquete de
 * las capas superiores (dao, service, view).</p>
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class Usuario {

    // ── Constantes de rol ──────────────────────────────────────────────────

    /** Rol con acceso total al sistema (también cumple funciones de recepción). */
    public static final String ROL_ADMIN               = "ADMIN";

    /** Rol legacy — equivalente a MEDICO_GENERAL, mantenido por compatibilidad. */
    public static final String ROL_MEDICO              = "MEDICO";

    /** Médico General: diagnóstico, órdenes, remisiones. */
    public static final String ROL_MEDICO_GENERAL      = "MEDICO_GENERAL";

    /** Médico Especialista: consultas especializadas, altas médicas. */
    public static final String ROL_MEDICO_ESPECIALISTA = "MEDICO_ESPECIALISTA";

    /** Paciente: portal propio, autodiagnóstico IA, historial. */
    public static final String ROL_PACIENTE            = "PACIENTE";

    // ── Campos ────────────────────────────────────────────────────────────

    /** Identificador único. Valor {@code 0} significa "no persistido aún". */
    private int id;

    /** Nombre de usuario único en el sistema. */
    private String username;

    /** Contraseña almacenada como hash SHA-256 en formato hexadecimal. */
    private String password;

    /** Rol del usuario. */
    private String rol;

    /**
     * FK al paciente asociado. 0 si el usuario no es un paciente.
     * Enlaza la cuenta de acceso con el expediente clínico.
     */
    private int pacienteId;

    /**
     * FK al médico asociado. 0 si el usuario no es un médico.
     * Enlaza la cuenta de acceso con el expediente del profesional.
     */
    private int medicoId;

    // ── Constructores ──────────────────────────────────────────────────────

    /**
     * Constructor completo; se usa al cargar un usuario desde el archivo CSV.
     * Se asume que {@code password} ya está hasheado.
     *
     * @param id       identificador único (0 si es nuevo)
     * @param username nombre de usuario
     * @param password hash SHA-256 de la contraseña en formato hexadecimal
     * @param rol      rol del usuario ({@code ADMIN | MEDICO})
     */
    public Usuario(int id, String username, String password, String rol) {
        this.id       = id;
        this.username = username;
        this.password = password;
        this.rol      = rol;
    }

    /**
     * Constructor conveniente para registrar un usuario nuevo.
     * La contraseña recibida se hashea automáticamente con SHA-256.
     *
     * @param username          nombre de usuario
     * @param passwordPlainText contraseña en texto plano; se hashea internamente
     * @param rol               rol del usuario ({@code ADMIN | MEDICO})
     */
    public Usuario(String username, String passwordPlainText, String rol) {
        this(0, username, hashPassword(passwordPlainText), rol);
    }

    // ── Hashing de contraseña ──────────────────────────────────────────────

    /**
     * Aplica el algoritmo SHA-256 a la cadena recibida y devuelve
     * el resultado en formato hexadecimal de 64 caracteres.
     *
     * <p>Este método es {@code public static} para que pueda ser invocado
     * desde la vista de login al comparar la contraseña ingresada
     * con el hash almacenado en el CSV.</p>
     *
     * @param input cadena a hashear (contraseña en texto plano)
     * @return hash SHA-256 en representación hexadecimal
     * @throws RuntimeException si el algoritmo SHA-256 no está disponible (JVM no estándar)
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
            // SHA-256 está garantizado en toda JVM estándar (Java SE 1.4+)
            throw new RuntimeException("SHA-256 no disponible en esta JVM", e);
        }
    }

    // ── Getters y Setters ──────────────────────────────────────────────────

    /** @return identificador único del usuario */
    public int getId() { return id; }

    /** @param id nuevo identificador (asignado tras persistir en CSV) */
    public void setId(int id) { this.id = id; }

    /** @return nombre de usuario único */
    public String getUsername() { return username; }

    /** @param username nuevo nombre de usuario */
    public void setUsername(String username) { this.username = username; }

    /**
     * Devuelve el hash SHA-256 de la contraseña.
     * <strong>Nunca devuelve la contraseña en texto plano.</strong>
     *
     * @return hash SHA-256 en formato hexadecimal
     */
    public String getPassword() { return password; }

    /**
     * Reemplaza la contraseña hasheando el texto plano recibido.
     *
     * @param passwordPlainText nueva contraseña en texto plano; se hashea internamente
     */
    public void setPassword(String passwordPlainText) {
        this.password = hashPassword(passwordPlainText);
    }

    /**
     * Reemplaza directamente el hash almacenado.
     * Usar solo al cargar datos desde el CSV.
     *
     * @param hashedPassword hash SHA-256 previamente calculado
     */
    public void setPasswordHash(String hashedPassword) {
        this.password = hashedPassword;
    }

    /** @return rol del usuario */
    public String getRol() { return rol; }

    /** @param rol nuevo rol del usuario */
    public void setRol(String rol) { this.rol = rol; }

    /** @return id del paciente vinculado, 0 si no es paciente */
    public int getPacienteId() { return pacienteId; }

    /** @param pacienteId FK al registro de paciente */
    public void setPacienteId(int pacienteId) { this.pacienteId = pacienteId; }

    /** @return id del médico vinculado, 0 si no es médico */
    public int getMedicoId() { return medicoId; }

    /** @param medicoId FK al registro de médico */
    public void setMedicoId(int medicoId) { this.medicoId = medicoId; }

    /** @return true si el usuario tiene algún rol médico */
    public boolean esMedico() {
        return ROL_MEDICO.equals(rol)
            || ROL_MEDICO_GENERAL.equals(rol)
            || ROL_MEDICO_ESPECIALISTA.equals(rol);
    }

    /** @return true si el usuario es paciente */
    public boolean esPaciente() { return ROL_PACIENTE.equals(rol); }

    /** @return true si el usuario es administrador */
    public boolean esAdmin() { return ROL_ADMIN.equals(rol); }

    // ── toString ───────────────────────────────────────────────────────────

    /**
     * No expone la contraseña (ni el hash) por seguridad.
     *
     * @return representación textual sin datos sensibles
     */
    @Override
    public String toString() {
        return "Usuario{id=" + id
                + ", username='" + username + "'"
                + ", rol='" + rol + "'}";
    }
}
