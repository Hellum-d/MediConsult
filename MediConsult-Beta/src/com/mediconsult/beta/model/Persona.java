package com.mediconsult.beta.model;

/**
 * Clase base de la jerarquía de personas del sistema MediConsult-Beta.
 *
 * Paciente y Medico heredan de aquí porque comparten los campos básicos
 * de identidad: id y nombre. El id = 0 indica que aún no ha sido guardado
 * en el CSV.
 *
 * Regla de diseño: esta clase no depende de capas superiores (dao, service, view).
 */
public abstract class Persona {

    /** Identificador único. 0 si aún no se ha persistido. */
    private int id;

    /** Nombre completo de la persona. */
    private String nombre;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea una persona base.
     *
     * @param id     id desde CSV (o 0 si es nuevo)
     * @param nombre nombre completo
     */
    public Persona(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // ── Método abstracto ──────────────────────────────────────────────────

    /**
     * Devuelve un resumen legible de la persona.
     * Cada clase hija define su propio formato.
     */
    public abstract String getResumen();

    // ── Getters y setters ────────────────────────────────────────────────

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    // ── Representación ────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Persona{id=" + id + ", nombre='" + nombre + "'}";
    }
}