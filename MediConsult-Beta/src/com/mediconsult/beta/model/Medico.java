package com.mediconsult.beta.model;

/**
 * Representa un médico en el sistema MediConsult-Beta.
 *
 * Contiene información básica, su especialidad y disponibilidad
 * para recibir citas.
 */
public class Medico extends Persona {

    /** Especialidad médica del doctor. */
    private String especialidad;

    /**
     * Indica si el médico puede recibir citas.
     */
    private boolean disponible;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Constructor usado al cargar desde CSV.
     * @param id
     * @param nombre
     * @param especialidad
     * @param disponible
     */
    public Medico(int id, String nombre, String especialidad, boolean disponible) {
        super(id, nombre);
        this.especialidad = especialidad;
        this.disponible   = disponible;
    }

    /**
     * Constructor para registrar un médico nuevo (por defecto disponible).
     * @param nombre
     * @param especialidad
     */
    public Medico(String nombre, String especialidad) {
        this(0, nombre, especialidad, true);
    }

    // ── Método heredado ───────────────────────────────────────────────────

    /**
     * Resumen simple del médico.
     */
    @Override
    public String getResumen() {
        return "Dr(a). " + getNombre() + " — " + especialidad;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    // ── Debug ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Medico{id=" + getId()
                + ", nombre='" + getNombre() + "'"
                + ", especialidad='" + especialidad + "'"
                + ", disponible=" + disponible + "}";
    }
}