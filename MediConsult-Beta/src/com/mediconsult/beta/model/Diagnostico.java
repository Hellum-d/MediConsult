package com.mediconsult.beta.model;

/**
 * Almacena los sintomas y la respuesta generada por Perplexity AI para una cita
 * especifica.
 */
public class Diagnostico {

    private int id;
    private int citaId;
    private String sintomas;
    private String resultadoIa;

    public Diagnostico(int id, int citaId, String sintomas, String resultadoIa) {
        this.id = id;
        this.citaId = citaId;
        this.sintomas = sintomas;
        this.resultadoIa = resultadoIa;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCitaId() {
        return citaId;
    }

    public void setCitaId(int citaId) {
        this.citaId = citaId;
    }

    public String getSintomas() {
        return sintomas;
    }

    public void setSintomas(String sintomas) {
        this.sintomas = sintomas;
    }

    public String getResultadoIa() {
        return resultadoIa;
    }

    public void setResultadoIa(String resultadoIa) {
        this.resultadoIa = resultadoIa;
    }

    @Override
    public String toString() {
        return "Diagnostico{id=" + id + ", citaId=" + citaId
                + ", sintomas='" + sintomas + "'}";
    }
}
