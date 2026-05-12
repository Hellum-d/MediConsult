package com.mediconsult.beta.model;

import java.time.LocalDate;

/**
 * Representa una cita agendada entre un paciente y un medico.
 * Usa IDs en lugar de objetos para evitar carga relacional en la capa model.
 */
public class Cita {

    private int id;
    private int pacienteId;
    private int medicoId;
    private LocalDate fecha;
    private String estado; // PENDIENTE | COMPLETADA | CANCELADA

    public Cita(int id, int pacienteId, int medicoId, LocalDate fecha, String estado) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.fecha = fecha;
        this.estado = estado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPacienteId() { return pacienteId; }
    public void setPacienteId(int pacienteId) { this.pacienteId = pacienteId; }

    public int getMedicoId() { return medicoId; }
    public void setMedicoId(int medicoId) { this.medicoId = medicoId; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() {
        return "Cita{id=" + id + ", pacienteId=" + pacienteId +
               ", medicoId=" + medicoId + ", fecha=" + fecha +
               ", estado='" + estado + "'}";
    }
}
