package com.mediconsult.model;

/**
 * Representa al paciente del consultorio.
 * Extiende Persona agregando datos clinicos y demograficos.
 */
public class Paciente extends Persona {

    private int edad;
    private String telefono;
    private String historial;

    public Paciente(int id, String nombre, int edad, String telefono, String historial) {
        super(id, nombre);
        this.edad = edad;
        this.telefono = telefono;
        this.historial = historial;
    }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getHistorial() { return historial; }
    public void setHistorial(String historial) { this.historial = historial; }

    @Override
    public String getResumen() {
        return "Paciente: " + getNombre() + ", edad " + edad + " anos";
    }

    @Override
    public String toString() {
        return "Paciente{id=" + getId() + ", nombre='" + getNombre() +
               "', edad=" + edad + ", telefono='" + telefono + "'}";
    }
}
