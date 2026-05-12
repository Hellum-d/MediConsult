package com.mediconsult.beta.model;

/**
 * Diagnóstico generado para una cita médica.
 *
 * Guarda los síntomas del paciente y la respuesta generada por la IA.
 */
public class Diagnostico {

    /** ID del diagnóstico (0 si aún no está guardado). */
    private int id;

    /** ID de la cita asociada. */
    private int citaId;

    /** Síntomas ingresados por el médico. */
    private String sintomas;

    /**
     * Resultado generado por la IA.
     * Puede estar vacío si aún no se ha consultado.
     */
    private String resultadoIa;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Constructor usado al cargar desde CSV.
     * @param id
     * @param citaId
     * @param sintomas
     * @param resultadoIa
     */
    public Diagnostico(int id, int citaId, String sintomas, String resultadoIa) {
        this.id          = id;
        this.citaId      = citaId;
        this.sintomas    = sintomas;
        this.resultadoIa = resultadoIa;
    }

    /**
     * Constructor para crear un diagnóstico antes de consultar la IA.
     * @param citaId
     * @param sintomas
     */
    public Diagnostico(int citaId, String sintomas) {
        this(0, citaId, sintomas, "");
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public int getCitaId() { return citaId; }

    public void setCitaId(int citaId) { this.citaId = citaId; }

    public String getSintomas() { return sintomas; }

    public void setSintomas(String sintomas) { this.sintomas = sintomas; }

    public String getResultadoIa() { return resultadoIa; }

    public void setResultadoIa(String resultadoIa) { this.resultadoIa = resultadoIa; }

    // ── Debug ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Diagnostico{id=" + id
                + ", citaId=" + citaId
                + ", sintomas='" + sintomas + "'"
                + ", resultadoIa='" + resultadoIa + "'}";
    }
}