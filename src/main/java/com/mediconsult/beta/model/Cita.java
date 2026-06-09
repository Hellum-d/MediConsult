package com.mediconsult.beta.model;

import java.time.LocalDate;

/**
 * Representa una cita médica agendada en el sistema MediConsult-Beta.
 *
 * <p>Una cita vincula a un paciente con un médico en una fecha específica y
 * tiene un estado que evoluciona durante su ciclo de vida:</p>
 * <ul>
 *   <li>{@code PENDIENTE}   — recién creada, aún no atendida</li>
 *   <li>{@code COMPLETADA}  — el médico ya atendió al paciente</li>
 *   <li>{@code CANCELADA}   — anulada por el paciente o el sistema</li>
 * </ul>
 *
 * <p><strong>Decisión de diseño:</strong> Los campos {@code pacienteId} y
 * {@code medicoId} almacenan claves foráneas como {@code int} en lugar de
 * objetos completos, evitando carga relacional en la capa model. La
 * reconstrucción de objetos relacionados es responsabilidad del DAO/Service.</p>
 *
 * <p>La fecha se serializa en el CSV con el formato ISO-8601 ({@code yyyy-MM-dd})
 * usando {@link LocalDate#toString()} / {@link LocalDate#parse(CharSequence)}.</p>
 *
 * <p><strong>Regla de capa:</strong> Esta clase no importa ningún paquete de
 * las capas superiores (dao, service, view).</p>
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class Cita {

    // ── Constantes de estado ───────────────────────────────────────────────

    /** Estado inicial de toda cita recién creada. */
    public static final String ESTADO_PENDIENTE  = "PENDIENTE";

    /** Estado cuando el médico ha atendido la cita. */
    public static final String ESTADO_COMPLETADA = "COMPLETADA";

    /** Estado cuando la cita ha sido cancelada. */
    public static final String ESTADO_CANCELADA  = "CANCELADA";

    /** Solicitud de cita enviada por el paciente; pendiente de asignación por el admin. */
    public static final String ESTADO_SOLICITUD  = "SOLICITUD";

    // ── Campos ────────────────────────────────────────────────────────────

    /** Identificador único. Valor {@code 0} significa "no persistido aún". */
    private int id;

    /** Clave foránea al registro de pacientes ({@code pacientes.csv → id}). */
    private int pacienteId;

    /** Clave foránea al registro de médicos ({@code medicos.csv → id}). */
    private int medicoId;

    /** Fecha de la cita (sin componente horario). Serializada como ISO-8601 en el CSV. */
    private LocalDate fecha;

    /** Hora de la cita en formato HH:mm:ss. */
    private String hora;

    /** Motivo o descripción de la consulta. */
    private String motivo;

    /**
     * Estado actual de la cita.
     * Valores válidos: {@link #ESTADO_PENDIENTE}, {@link #ESTADO_COMPLETADA},
     * {@link #ESTADO_CANCELADA}.
     */
    private String estado;

    // ── Constructores ──────────────────────────────────────────────────────

    /**
     * Constructor completo con hora y motivo (usado por el DAO al leer de MySQL).
     */
    public Cita(int id, int pacienteId, int medicoId, LocalDate fecha, String estado,
                String hora, String motivo) {
        this.id         = id;
        this.pacienteId = pacienteId;
        this.medicoId   = medicoId;
        this.fecha      = fecha;
        this.estado     = estado;
        this.hora       = hora; // null permitido para SOLICITUD (fecha/hora por asignar)
        this.motivo     = motivo != null ? motivo : "";
    }

    /** Constructor legacy (backward-compatible): usa hora 08:00 y motivo vacío. */
    public Cita(int id, int pacienteId, int medicoId, LocalDate fecha, String estado) {
        this(id, pacienteId, medicoId, fecha, estado, "08:00:00", "");
    }

    /** Constructor con hora y motivo para nuevas citas de pacientes. */
    public Cita(int pacienteId, int medicoId, LocalDate fecha, String hora, String motivo) {
        this(0, pacienteId, medicoId, fecha, ESTADO_PENDIENTE, hora, motivo);
    }

    /** Constructor legacy para nuevas citas (hora 08:00, sin motivo). */
    public Cita(int pacienteId, int medicoId, LocalDate fecha) {
        this(0, pacienteId, medicoId, fecha, ESTADO_PENDIENTE, "08:00:00", "");
    }

    // ── Getters y Setters ──────────────────────────────────────────────────

    /**
     * @return identificador único de la cita
     */
    public int getId() {
        return id;
    }

    /**
     * @param id nuevo identificador (asignado tras persistir en CSV)
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return FK al paciente asociado
     */
    public int getPacienteId() {
        return pacienteId;
    }

    /**
     * @param pacienteId nuevo FK al paciente
     */
    public void setPacienteId(int pacienteId) {
        this.pacienteId = pacienteId;
    }

    /**
     * @return FK al médico asociado
     */
    public int getMedicoId() {
        return medicoId;
    }

    /**
     * @param medicoId nuevo FK al médico
     */
    public void setMedicoId(int medicoId) {
        this.medicoId = medicoId;
    }

    /**
     * @return fecha de la cita
     */
    public LocalDate getFecha() {
        return fecha;
    }

    /**
     * @param fecha nueva fecha de la cita
     */
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    /**
     * @return estado actual de la cita
     */
    public String getEstado() {
        return estado;
    }

    /**
     * @param estado nuevo estado; debe ser uno de los valores definidos como constante
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getHora()               { return hora; }
    public void   setHora(String hora)    { this.hora = hora; }

    public String getMotivo()             { return motivo; }
    public void   setMotivo(String m)     { this.motivo = m; }

    // ── toString ───────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Cita{id=" + id
                + ", pacienteId=" + pacienteId
                + ", medicoId=" + medicoId
                + ", fecha=" + fecha
                + ", estado='" + estado + "'}";
    }
}
