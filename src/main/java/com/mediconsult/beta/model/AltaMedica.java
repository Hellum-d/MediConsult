package com.mediconsult.beta.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AltaMedica {

    public static final String TIPO_CURADO      = "CURADO";
    public static final String TIPO_MEJORADO    = "MEJORADO";
    public static final String TIPO_SEGUIMIENTO = "SEGUIMIENTO_AMBULATORIO";
    public static final String TIPO_TRASLADO    = "TRASLADO";
    public static final String TIPO_VOLUNTARIA  = "VOLUNTARIA";

    private int           id;
    private int           remisionId;
    private int           consultaEspecializadaId;
    private int           pacienteId;
    private int           medicoEspecialistaId;
    private String        diagnosticoFinal;
    private String        tipoAlta;
    private String        indicacionesPaciente;
    private String        indicacionesMedicoRemitente;
    private String        observaciones;
    private LocalDate     fechaAlta;
    private LocalDateTime fechaRegistro;

    public AltaMedica() {}

    public AltaMedica(int remisionId, int consultaEspecializadaId,
                      int pacienteId, int medicoEspecialistaId) {
        this.remisionId              = remisionId;
        this.consultaEspecializadaId = consultaEspecializadaId;
        this.pacienteId              = pacienteId;
        this.medicoEspecialistaId    = medicoEspecialistaId;
        this.fechaAlta               = LocalDate.now();
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    public int           getId()                                             { return id; }
    public void          setId(int id)                                       { this.id = id; }

    public int           getRemisionId()                                     { return remisionId; }
    public void          setRemisionId(int remisionId)                       { this.remisionId = remisionId; }

    public int           getConsultaEspecializadaId()                                        { return consultaEspecializadaId; }
    public void          setConsultaEspecializadaId(int consultaEspecializadaId)             { this.consultaEspecializadaId = consultaEspecializadaId; }

    public int           getPacienteId()                                     { return pacienteId; }
    public void          setPacienteId(int pacienteId)                       { this.pacienteId = pacienteId; }

    public int           getMedicoEspecialistaId()                                           { return medicoEspecialistaId; }
    public void          setMedicoEspecialistaId(int medicoEspecialistaId)                   { this.medicoEspecialistaId = medicoEspecialistaId; }

    public String        getDiagnosticoFinal()                               { return nvl(diagnosticoFinal); }
    public void          setDiagnosticoFinal(String diagnosticoFinal)        { this.diagnosticoFinal = nvl(diagnosticoFinal); }

    public String        getTipoAlta()                                       { return tipoAlta; }
    public void          setTipoAlta(String tipoAlta)                        { this.tipoAlta = tipoAlta; }

    public String        getIndicacionesPaciente()                                           { return nvl(indicacionesPaciente); }
    public void          setIndicacionesPaciente(String indicacionesPaciente)                { this.indicacionesPaciente = nvl(indicacionesPaciente); }

    public String        getIndicacionesMedicoRemitente()                                    { return nvl(indicacionesMedicoRemitente); }
    public void          setIndicacionesMedicoRemitente(String indicacionesMedicoRemitente)  { this.indicacionesMedicoRemitente = nvl(indicacionesMedicoRemitente); }

    public String        getObservaciones()                                  { return nvl(observaciones); }
    public void          setObservaciones(String observaciones)              { this.observaciones = nvl(observaciones); }

    public LocalDate     getFechaAlta()                                      { return fechaAlta; }
    public void          setFechaAlta(LocalDate fechaAlta)                   { this.fechaAlta = fechaAlta; }

    public LocalDateTime getFechaRegistro()                                  { return fechaRegistro; }
    public void          setFechaRegistro(LocalDateTime fechaRegistro)       { this.fechaRegistro = fechaRegistro; }
}
