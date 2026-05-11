package com.mediconsult.model;

/**
 * Clase base abstracta de la jerarquia de dominio.
 * Encapsula los atributos comunes a Paciente y Medico.
 */
public abstract class Persona {

    private int id;
    private String nombre;

    public Persona(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    /**
     * Retorna un resumen textual de la entidad.
     * Cada subclase define su propio formato.
     */
    public abstract String getResumen();

    @Override
    public String toString() {
        return "Persona{id=" + id + ", nombre='" + nombre + "'}";
    }
}
