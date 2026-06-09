package com.mediconsult.beta.model;

import java.time.LocalDateTime;

public class OrdenMedica {

    public static final String TIPO_MEDICAMENTO   = "MEDICAMENTO";
    public static final String TIPO_PROCEDIMIENTO = "PROCEDIMIENTO";
    public static final String TIPO_EXAMEN        = "EXAMEN";

    public static final String ESTADO_ACTIVA    = "ACTIVA";
    public static final String ESTADO_APLICADA  = "APLICADA";
    public static final String ESTADO_CANCELADA = "CANCELADA";

    private int            id;
    private int            citaId;
    private int            pacienteId;
    private int            medicoId;
    private Integer        historiaClinicaId;
    private String         tipo;
    private String         descripcion;
    private String         observaciones;
    private String         estado;
    private LocalDateTime  fechaRegistro;

    public OrdenMedica() {}

    public OrdenMedica(int citaId, int pacienteId, int medicoId) {
        this.citaId     = citaId;
        this.pacienteId = pacienteId;
        this.medicoId   = medicoId;
        this.estado     = ESTADO_ACTIVA;
    }

    public int           getId()                 { return id; }
    public void          setId(int id)           { this.id = id; }

    public int           getCitaId()             { return citaId; }
    public void          setCitaId(int citaId)   { this.citaId = citaId; }

    public int           getPacienteId()                   { return pacienteId; }
    public void          setPacienteId(int pacienteId)     { this.pacienteId = pacienteId; }

    public int           getMedicoId()                     { return medicoId; }
    public void          setMedicoId(int medicoId)         { this.medicoId = medicoId; }

    public Integer       getHistoriaClinicaId()                        { return historiaClinicaId; }
    public void          setHistoriaClinicaId(Integer historiaClinicaId) { this.historiaClinicaId = historiaClinicaId; }

    public String        getTipo()               { return tipo; }
    public void          setTipo(String tipo)    { this.tipo = tipo; }

    public String        getDescripcion()                    { return descripcion; }
    public void          setDescripcion(String descripcion)  { this.descripcion = descripcion; }

    public String        getObservaciones()                      { return observaciones; }
    public void          setObservaciones(String observaciones)  { this.observaciones = observaciones; }

    public String        getEstado()                  { return estado; }
    public void          setEstado(String estado)     { this.estado = estado; }

    public LocalDateTime getFechaRegistro()                        { return fechaRegistro; }
    public void          setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
