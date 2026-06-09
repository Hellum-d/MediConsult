package com.mediconsult.beta.model;

import java.time.LocalDateTime;

/**
 * Representa un autodiagnóstico iniciado por el propio paciente en CLINEX.
 *
 * <p>A diferencia de {@link Diagnostico}, este no está vinculado a una cita
 * médica. Es un análisis preliminar de síntomas realizado directamente por el
 * paciente desde su portal. El resultado sirve de referencia para el médico
 * durante la consulta posterior.</p>
 *
 * @author MediConsult Team
 */
public class Autodiagnostico {

    private int           id;
    private int           pacienteId;
    private String        sintomas;
    private String        resultadoIa;
    private LocalDateTime fechaRegistro;
    /** Ruta al archivo de imagen/radiografía cargado por el paciente (puede ser null). */
    private String        imagenPath;

    public Autodiagnostico(int id, int pacienteId, String sintomas,
                           String resultadoIa, LocalDateTime fechaRegistro) {
        this.id            = id;
        this.pacienteId    = pacienteId;
        this.sintomas      = sintomas;
        this.resultadoIa   = resultadoIa;
        this.fechaRegistro = fechaRegistro;
    }

    public Autodiagnostico(int pacienteId, String sintomas) {
        this(0, pacienteId, sintomas, "", null);
    }

    public int           getId()            { return id; }
    public void          setId(int id)      { this.id = id; }

    public int           getPacienteId()             { return pacienteId; }
    public void          setPacienteId(int p)        { this.pacienteId = p; }

    public String        getSintomas()               { return sintomas; }
    public void          setSintomas(String s)       { this.sintomas = s; }

    public String        getResultadoIa()            { return resultadoIa; }
    public void          setResultadoIa(String r)    { this.resultadoIa = r; }

    public LocalDateTime getFechaRegistro()          { return fechaRegistro; }
    public void          setFechaRegistro(LocalDateTime f) { this.fechaRegistro = f; }

    public String        getImagenPath()             { return imagenPath; }
    public void          setImagenPath(String p)     { this.imagenPath = p; }

    /** @return true si este autodiagnóstico tiene una imagen adjunta. */
    public boolean       tieneImagen()               { return imagenPath != null && !imagenPath.isBlank(); }

    @Override
    public String toString() {
        return "Autodiagnostico{id=" + id + ", pacienteId=" + pacienteId
                + ", sintomas='" + sintomas + "'}";
    }
}
