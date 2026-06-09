package com.mediconsult.beta.model;

import java.time.LocalDateTime;

public class ConsultaEspecializada {

    public static final String ESTADO_ACTIVA     = "ACTIVA";
    public static final String ESTADO_COMPLETADA = "COMPLETADA";

    private int           id;
    private int           remisionId;
    private int           pacienteId;
    private int           medicoEspecialistaId;
    private String        motivoConsulta;
    private String        hallazgos;
    private String        diagnostico;
    private String        tratamiento;
    private String        evolucion;
    private String        observaciones;
    private String        estado;
    private LocalDateTime fechaRegistro;

    public ConsultaEspecializada() {}

    public ConsultaEspecializada(int remisionId, int pacienteId, int medicoEspecialistaId) {
        this.remisionId           = remisionId;
        this.pacienteId           = pacienteId;
        this.medicoEspecialistaId = medicoEspecialistaId;
        this.estado               = ESTADO_ACTIVA;
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    public int           getId()                              { return id; }
    public void          setId(int id)                        { this.id = id; }

    public int           getRemisionId()                      { return remisionId; }
    public void          setRemisionId(int remisionId)        { this.remisionId = remisionId; }

    public int           getPacienteId()                      { return pacienteId; }
    public void          setPacienteId(int pacienteId)        { this.pacienteId = pacienteId; }

    public int           getMedicoEspecialistaId()                       { return medicoEspecialistaId; }
    public void          setMedicoEspecialistaId(int medicoEspecialistaId){ this.medicoEspecialistaId = medicoEspecialistaId; }

    public String        getMotivoConsulta()                             { return nvl(motivoConsulta); }
    public void          setMotivoConsulta(String motivoConsulta)        { this.motivoConsulta = nvl(motivoConsulta); }

    public String        getHallazgos()                                  { return nvl(hallazgos); }
    public void          setHallazgos(String hallazgos)                  { this.hallazgos = nvl(hallazgos); }

    public String        getDiagnostico()                                { return nvl(diagnostico); }
    public void          setDiagnostico(String diagnostico)              { this.diagnostico = nvl(diagnostico); }

    public String        getTratamiento()                                { return nvl(tratamiento); }
    public void          setTratamiento(String tratamiento)              { this.tratamiento = nvl(tratamiento); }

    public String        getEvolucion()                                  { return nvl(evolucion); }
    public void          setEvolucion(String evolucion)                  { this.evolucion = nvl(evolucion); }

    public String        getObservaciones()                              { return nvl(observaciones); }
    public void          setObservaciones(String observaciones)          { this.observaciones = nvl(observaciones); }

    public String        getEstado()                          { return estado; }
    public void          setEstado(String estado)             { this.estado = estado; }

    public LocalDateTime getFechaRegistro()                              { return fechaRegistro; }
    public void          setFechaRegistro(LocalDateTime fechaRegistro)   { this.fechaRegistro = fechaRegistro; }
}
