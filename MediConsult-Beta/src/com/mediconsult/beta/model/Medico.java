package com.mediconsult.beta.model;

/**
 * Representa al medico del consultorio.
 * Gestiona su especialidad y disponibilidad para ser agendado.
 */
public class Medico extends Persona {

    private String especialidad;
    private boolean disponible;

    public Medico(int id, String nombre, String especialidad, boolean disponible) {
        super(id, nombre);
        this.especialidad = especialidad;
        this.disponible = disponible;
    }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    @Override
    public String getResumen() {
        return "Dr. " + getNombre() + " - " + especialidad;
    }

    @Override
    public String toString() {
        return "Medico{id=" + getId() + ", nombre='" + getNombre() +
               "', especialidad='" + especialidad + "', disponible=" + disponible + "}";
    }
}