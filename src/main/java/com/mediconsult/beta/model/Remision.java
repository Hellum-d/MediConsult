package com.mediconsult.beta.model;

import java.time.LocalDateTime;

public class Remision {

    public static final String ESTADO_PENDIENTE    = "PENDIENTE";
    public static final String ESTADO_ACEPTADA     = "ACEPTADA";
    public static final String ESTADO_EN_ATENCION  = "EN_ATENCION";
    public static final String ESTADO_COMPLETADA   = "COMPLETADA";
    public static final String ESTADO_CANCELADA    = "CANCELADA";

    private int           id;
    private int           citaId;
    private int           pacienteId;
    private int           medicoRemitenteId;
    private Integer       medicoEspecialistaId;
    private String        especialidadDestino;
    private String        motivoRemision;
    private String        observaciones;
    private String        estado;
    private LocalDateTime fechaRegistro;

    public Remision() {}

    public Remision(int citaId, int pacienteId, int medicoRemitenteId) {
        this.citaId             = citaId;
        this.pacienteId         = pacienteId;
        this.medicoRemitenteId  = medicoRemitenteId;
        this.estado             = ESTADO_PENDIENTE;
    }

    public int           getId()                              { return id; }
    public void          setId(int id)                        { this.id = id; }

    public int           getCitaId()                          { return citaId; }
    public void          setCitaId(int citaId)                { this.citaId = citaId; }

    public int           getPacienteId()                      { return pacienteId; }
    public void          setPacienteId(int pacienteId)        { this.pacienteId = pacienteId; }

    public int           getMedicoRemitenteId()               { return medicoRemitenteId; }
    public void          setMedicoRemitenteId(int id)         { this.medicoRemitenteId = id; }

    public Integer       getMedicoEspecialistaId()                           { return medicoEspecialistaId; }
    public void          setMedicoEspecialistaId(Integer medicoEspecialistaId) { this.medicoEspecialistaId = medicoEspecialistaId; }

    public String        getEspecialidadDestino()                          { return especialidadDestino; }
    public void          setEspecialidadDestino(String especialidadDestino){ this.especialidadDestino = especialidadDestino; }

    public String        getMotivoRemision()                               { return motivoRemision; }
    public void          setMotivoRemision(String motivoRemision)          { this.motivoRemision = motivoRemision; }

    public String        getObservaciones()                                { return observaciones; }
    public void          setObservaciones(String observaciones)            { this.observaciones = observaciones; }

    public String        getEstado()                          { return estado; }
    public void          setEstado(String estado)             { this.estado = estado; }

    public LocalDateTime getFechaRegistro()                               { return fechaRegistro; }
    public void          setFechaRegistro(LocalDateTime fechaRegistro)    { this.fechaRegistro = fechaRegistro; }
}
