package com.mediconsult.beta.model;

/**
 * Representa un paciente en el sistema MediConsult-Beta.
 *
 * Contiene datos básicos del paciente y su historial médico en texto libre.
 * El historial se va actualizando con cada diagnóstico.
 */
public class Paciente extends Persona {

    /** Edad del paciente. */
    private int edad;

    /** Teléfono de contacto. */
    private String telefono;

    /**
     * Historial médico del paciente.
     * Guarda diagnósticos y registros previos en texto.
     */
    private String historial;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Constructor usado al cargar desde CSV.
     * @param id
     * @param nombre
     * @param edad
     * @param telefono
     * @param historial
     */
    public Paciente(int id, String nombre, int edad, String telefono, String historial) {
        super(id, nombre);
        this.edad      = edad;
        this.telefono  = telefono;
        this.historial = historial;
    }

    /**
     * Constructor para crear pacientes nuevos sin historial previo.
     * @param nombre
     * @param edad
     * @param telefono
     */
    public Paciente(String nombre, int edad, String telefono) {
        this(0, nombre, edad, telefono, "");
    }

    // ── Método heredado ───────────────────────────────────────────────────

    /**
     * Resumen básico del paciente.
     * @return 
     */
    @Override
    public String getResumen() {
        return "Paciente: " + getNombre() + ", " + edad + " años";
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getHistorial() {
        return historial;
    }

    public void setHistorial(String historial) {
        this.historial = historial;
    }

    // ── Debug ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Paciente{id=" + getId()
                + ", nombre='" + getNombre() + "'"
                + ", edad=" + edad
                + ", telefono='" + telefono + "'"
                + ", historial='" + historial + "'}";
    }
}