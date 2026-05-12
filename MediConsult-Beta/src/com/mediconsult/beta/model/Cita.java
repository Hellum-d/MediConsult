package com.mediconsult.beta.model;

import java.time.LocalDate;

/**
 * Cita médica entre un paciente y un médico.
 *
 * Representa una cita agendada con fecha y estado.
 */
public class Cita {

    // ── Estados ───────────────────────────────────────────────────────────

    /** Cita recién creada. */
    public static final String ESTADO_PENDIENTE  = "PENDIENTE";

    /** Cita ya atendida. */
    public static final String ESTADO_COMPLETADA = "COMPLETADA";

    /** Cita cancelada. */
    public static final String ESTADO_CANCELADA  = "CANCELADA";

    // ── Datos ─────────────────────────────────────────────────────────────

    /** ID de la cita (0 si aún no se ha guardado). */
    private int id;

    /** ID del paciente relacionado. */
    private int pacienteId;

    /** ID del médico relacionado. */
    private int medicoId;

    /** Fecha de la cita. */
    private LocalDate fecha;

    /** Estado actual de la cita. */
    private String estado;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Constructor usado al cargar desde CSV.
     * @param id
     * @param pacienteId
     * @param medicoId
     * @param fecha
     * @param estado
     */
    public Cita(int id, int pacienteId, int medicoId, LocalDate fecha, String estado) {
        this.id         = id;
        this.pacienteId = pacienteId;
        this.medicoId   = medicoId;
        this.fecha      = fecha;
        this.estado     = estado;
    }

    /**
     * Constructor para crear citas nuevas (empiezan en PENDIENTE).
     */
    public Cita(int pacienteId, int medicoId, LocalDate fecha) {
        this(0, pacienteId, medicoId, fecha, ESTADO_PENDIENTE);
    }

    // ── Getters / Setters ────────────────────────────────────────────────

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

    // ── Debug ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Cita{id=" + id
                + ", pacienteId=" + pacienteId
                + ", medicoId=" + medicoId
                + ", fecha=" + fecha
                + ", estado='" + estado + "'}";
    }
}