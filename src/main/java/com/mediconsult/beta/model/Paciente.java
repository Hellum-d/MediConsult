package com.mediconsult.beta.model;

/**
 * Representa a un paciente dentro del sistema Clinex.
 *
 * <p>Extiende {@link Persona} añadiendo edad, historial clínico,
 * tipo de sangre y alergias.</p>
 *
 * @author MediConsult Team
 * @version 0.3 — campos médicos agregados
 */
public class Paciente extends Persona {

    private int    edad;
    private String historial;
    private String tipoSangre;   // Ej: "O+", "A-", "AB+"
    private String alergias;     // Lista separada por comas: "Penicilina,Polvo"

    // ── Constructor completo (usado por el DAO al leer de MySQL) ───────────

    public Paciente(int id, String nombre, String apellido,
                    String telefono, String direccion, String correo,
                    int edad, String historial,
                    String tipoSangre, String alergias) {
        super(id, nombre, apellido, telefono, direccion, correo);
        this.edad       = edad;
        this.historial  = historial;
        this.tipoSangre = tipoSangre;
        this.alergias   = alergias;
    }

    /**
     * Constructor legacy (sin datos médicos) — mantiene compatibilidad
     * con código existente que no pasa tipoSangre/alergias.
     */
    public Paciente(int id, String nombre, String apellido,
                    String telefono, String direccion, String correo,
                    int edad, String historial) {
        this(id, nombre, apellido, telefono, direccion, correo, edad, historial, "", "");
    }

    /** Constructor conveniente para registrar un paciente nuevo. */
    public Paciente(String nombre, String apellido, int edad,
                    String telefono, String correo,
                    String tipoSangre, String alergias) {
        this(0, nombre, apellido, telefono, "", correo, edad, "",
             tipoSangre != null ? tipoSangre : "",
             alergias   != null ? alergias   : "");
    }

    /** Constructor conveniente legacy (sin datos médicos). */
    public Paciente(String nombre, String apellido, int edad,
                    String telefono, String correo) {
        this(nombre, apellido, edad, telefono, correo, "", "");
    }

    // ── Método abstracto heredado ──────────────────────────────────────────

    @Override
    public String getResumen() {
        String ts = (tipoSangre != null && !tipoSangre.isBlank()) ? " · " + tipoSangre : "";
        return "Paciente: " + getNombreCompleto() + ", " + edad + " años" + ts;
    }

    // ── Getters y Setters ──────────────────────────────────────────────────

    public int    getEdad()           { return edad; }
    public void   setEdad(int edad)   { this.edad = edad; }

    public String getHistorial()                   { return historial; }
    public void   setHistorial(String historial)   { this.historial = historial; }

    public String getTipoSangre()                      { return tipoSangre; }
    public void   setTipoSangre(String tipoSangre)     { this.tipoSangre = tipoSangre; }

    public String getAlergias()                    { return alergias; }
    public void   setAlergias(String alergias)     { this.alergias = alergias; }

    @Override
    public String toString() {
        return "Paciente{id=" + getId()
                + ", nombre='" + getNombreCompleto() + "'"
                + ", edad=" + edad
                + ", tipoSangre='" + tipoSangre + "'"
                + ", telefono='" + getTelefono() + "'}";
    }
}