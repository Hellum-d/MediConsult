package com.mediconsult.beta.model;

import java.time.LocalDateTime;

/**
 * Representa la historia clínica registrada por un médico durante una consulta.
 *
 * <p>Vinculada a una {@code cita}, un {@code paciente} y un {@code medico}.
 * Contiene los cinco campos clínicos del proceso de consulta:</p>
 * <ul>
 *   <li>Anamnesis — relato del paciente sobre sus síntomas</li>
 *   <li>Examen físico — hallazgos del médico en la exploración</li>
 *   <li>Diagnóstico — conclusión clínica del médico</li>
 *   <li>Tratamiento — plan terapéutico indicado</li>
 *   <li>Observaciones — notas adicionales y seguimiento</li>
 * </ul>
 *
 * @author MediConsult Team
 */
public class HistoriaClinica {

    private int           id;
    private int           citaId;
    private int           pacienteId;
    private int           medicoId;
    private String        anamnesis;
    private String        examenFisico;
    private String        diagnostico;
    private String        tratamiento;
    private String        observaciones;
    private LocalDateTime fechaRegistro;

    public HistoriaClinica(int id, int citaId, int pacienteId, int medicoId,
                           String anamnesis, String examenFisico, String diagnostico,
                           String tratamiento, String observaciones,
                           LocalDateTime fechaRegistro) {
        this.id            = id;
        this.citaId        = citaId;
        this.pacienteId    = pacienteId;
        this.medicoId      = medicoId;
        this.anamnesis     = nvl(anamnesis);
        this.examenFisico  = nvl(examenFisico);
        this.diagnostico   = nvl(diagnostico);
        this.tratamiento   = nvl(tratamiento);
        this.observaciones = nvl(observaciones);
        this.fechaRegistro = fechaRegistro;
    }

    public HistoriaClinica(int citaId, int pacienteId, int medicoId) {
        this(0, citaId, pacienteId, medicoId, "", "", "", "", "", null);
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    public int           getId()               { return id; }
    public void          setId(int id)         { this.id = id; }

    public int           getCitaId()           { return citaId; }
    public void          setCitaId(int c)      { this.citaId = c; }

    public int           getPacienteId()       { return pacienteId; }
    public void          setPacienteId(int p)  { this.pacienteId = p; }

    public int           getMedicoId()         { return medicoId; }
    public void          setMedicoId(int m)    { this.medicoId = m; }

    public String        getAnamnesis()        { return anamnesis; }
    public void          setAnamnesis(String s){ this.anamnesis = nvl(s); }

    public String        getExamenFisico()     { return examenFisico; }
    public void          setExamenFisico(String s){ this.examenFisico = nvl(s); }

    public String        getDiagnostico()      { return diagnostico; }
    public void          setDiagnostico(String s){ this.diagnostico = nvl(s); }

    public String        getTratamiento()      { return tratamiento; }
    public void          setTratamiento(String s){ this.tratamiento = nvl(s); }

    public String        getObservaciones()    { return observaciones; }
    public void          setObservaciones(String s){ this.observaciones = nvl(s); }

    public LocalDateTime getFechaRegistro()    { return fechaRegistro; }
    public void          setFechaRegistro(LocalDateTime f){ this.fechaRegistro = f; }
}
