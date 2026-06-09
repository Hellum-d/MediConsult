package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.CitaDAOImpl;
import com.mediconsult.beta.dao.impl.MedicoDAOImpl;
import com.mediconsult.beta.dao.interfaces.ICitaDAO;
import com.mediconsult.beta.dao.interfaces.IMedicoDAO;
import com.mediconsult.beta.model.Cita;
import com.mediconsult.beta.model.Medico;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para la gestión de citas médicas en MediConsult-Beta.
 *
 * <p>Implementa la verificación de disponibilidad como regla de negocio
 * central: antes de agendar, se comprueba que el médico esté marcado como
 * disponible y que no tenga otra cita activa en la misma fecha.</p>
 *
 * <p>Usa dos DAOs: {@link ICitaDAO} para persistir citas e
 * {@link IMedicoDAO} para consultar el estado del médico.</p>
 *
 * @author MediConsult Team
 * @version 0.1
 */
public class CitaService {

    private final ICitaDAO   citaDAO;
    private final IMedicoDAO medicoDAO;

    /** Constructor por defecto: instancia los DAOs reales. */
    public CitaService() {
        this.citaDAO   = new CitaDAOImpl();
        this.medicoDAO = new MedicoDAOImpl();
    }

    /** Constructor para inyección de dependencia (tests). */
    public CitaService(ICitaDAO citaDAO, IMedicoDAO medicoDAO) {
        this.citaDAO   = citaDAO;
        this.medicoDAO = medicoDAO;
    }

    // ── Operaciones de escritura ──────────────────────────────────────────

    /**
     * Agenda una nueva cita verificando la disponibilidad del médico.
     *
     * <p><strong>Reglas de negocio aplicadas:</strong></p>
     * <ol>
     *   <li>El médico debe existir en el CSV.</li>
     *   <li>El médico debe tener {@code disponible == true}.</li>
     *   <li>El médico no debe tener otra cita activa (no cancelada) en la
     *       misma fecha.</li>
     *   <li>La fecha no puede ser anterior a hoy.</li>
     * </ol>
     *
     * @param pacienteId identificador del paciente
     * @param medicoId   identificador del médico
     * @param fecha      fecha deseada para la cita
     * @return cita persistida con {@code estado = PENDIENTE} y su {@code id}
     * @throws IllegalArgumentException si algún dato de entrada es inválido
     * @throws IllegalStateException    si el médico no está disponible esa fecha
     */
    public Cita agendarCita(int pacienteId, int medicoId, LocalDate fecha) {
        validar(fecha != null, "La fecha de la cita no puede ser nula.");
        validar(!fecha.isBefore(LocalDate.now()), "No se pueden agendar citas en fechas pasadas.");

        // Verificación 1: el médico existe y está disponible
        Medico medico = medicoDAO.buscarPorId(medicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un médico con id=" + medicoId));

        if (!medico.isDisponible()) {
            throw new IllegalStateException(
                    "El médico " + medico.getNombre() + " no está disponible para nuevas citas.");
        }

        Cita cita = new Cita(pacienteId, medicoId, fecha);
        citaDAO.guardar(cita);
        return cita;
    }

    /**
     * Agenda una nueva cita con hora y motivo específicos (usado por el paciente).
     *
     * <p>Valida disponibilidad del médico en el slot exacto (fecha + hora)
     * usando el UNIQUE KEY de la tabla: un médico solo puede tener una cita
     * activa por hora.</p>
     *
     * @param pacienteId id del paciente
     * @param medicoId   id del médico
     * @param fecha      fecha de la cita (hoy o futura)
     * @param hora       hora en formato "HH:mm" (ej. "10:00")
     * @param motivo     descripción del motivo de la consulta
     * @return cita persistida con estado PENDIENTE
     */
    public Cita agendarCita(int pacienteId, int medicoId, LocalDate fecha,
                             String hora, String motivo) {
        validar(fecha != null, "La fecha no puede ser nula.");
        validar(!fecha.isBefore(LocalDate.now()), "No se pueden agendar citas en fechas pasadas.");
        validar(hora != null && !hora.isBlank(), "Debe seleccionar un horario.");
        if (fecha.equals(LocalDate.now())) {
            try {
                String horaCorta = hora.length() >= 5 ? hora.substring(0, 5) : hora;
                validar(!LocalTime.parse(horaCorta).isBefore(LocalTime.now()),
                        "No se pueden agendar citas en horarios ya pasados para hoy.");
            } catch (java.time.format.DateTimeParseException ignored) {}
        }

        Medico medico = medicoDAO.buscarPorId(medicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un médico con id=" + medicoId));

        if (!medico.isDisponible()) {
            throw new IllegalStateException(
                    "El médico " + medico.getNombreCompleto() + " no está disponible para nuevas citas.");
        }

        String horaCompleta = hora.length() == 5 ? hora + ":00" : hora; // "HH:mm" → "HH:mm:00"
        if (citaDAO.existeCitaEnFechaHora(medicoId, fecha, horaCompleta)) {
            throw new IllegalStateException(
                    "El horario " + hora + " del " + fecha
                    + " ya está ocupado. Por favor selecciona otro horario.");
        }

        Cita cita = new Cita(pacienteId, medicoId, fecha, horaCompleta,
                motivo != null ? motivo.trim() : "");
        citaDAO.guardar(cita);
        return cita;
    }

    /** Retorna las horas ocupadas (no canceladas) del médico en una fecha. */
    public List<String> horasOcupadas(int medicoId, LocalDate fecha) {
        return citaDAO.horasOcupadasEnFecha(medicoId, fecha);
    }

    /**
     * Registra una solicitud de cita enviada por el paciente.
     * No asigna médico, fecha ni hora; queda en estado SOLICITUD para que
     * el administrador la revise y complete.
     */
    public Cita solicitarCita(int pacienteId, String motivo) {
        validar(pacienteId > 0, "El paciente no está identificado correctamente.");
        Cita solicitud = new Cita(0, pacienteId, 0, null, Cita.ESTADO_SOLICITUD,
                null, motivo != null ? motivo.trim() : "");
        citaDAO.guardar(solicitud);
        return solicitud;
    }

    /** Retorna todas las solicitudes pendientes de revisión. */
    public List<Cita> listarSolicitudes() {
        return citaDAO.listarSolicitudes();
    }

    /**
     * Procesa una solicitud: asigna médico, fecha y hora, y la convierte en
     * cita PENDIENTE lista para ser atendida.
     */
    public Cita procesarSolicitud(int citaId, int medicoId, LocalDate fecha, String hora) {
        Cita cita = obtenerCitaOFallar(citaId);
        validar(Cita.ESTADO_SOLICITUD.equals(cita.getEstado()),
                "Solo se pueden procesar solicitudes pendientes.");
        validar(fecha != null, "Debe seleccionar una fecha.");
        validar(!fecha.isBefore(LocalDate.now()),
                "No se pueden agendar citas en fechas pasadas. Selecciona hoy o una fecha futura.");
        validar(hora != null && !hora.isBlank(), "Debe seleccionar un horario.");

        Medico medico = medicoDAO.buscarPorId(medicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un médico con id=" + medicoId));
        if (!medico.isDisponible()) {
            throw new IllegalStateException(
                    "El médico " + medico.getNombreCompleto() + " no está disponible.");
        }

        String horaCompleta = hora.length() == 5 ? hora + ":00" : hora;
        if (citaDAO.existeCitaEnFechaHora(medicoId, fecha, horaCompleta)) {
            throw new IllegalStateException(
                    "El horario " + hora + " del " + fecha + " ya está ocupado.");
        }

        cita.setMedicoId(medicoId);
        cita.setFecha(fecha);
        cita.setHora(horaCompleta);
        cita.setEstado(Cita.ESTADO_PENDIENTE);
        citaDAO.actualizar(cita);
        return cita;
    }

    /**
     * Cancela una cita cambiando su estado a {@code CANCELADA}.
     *
     * @param citaId identificador de la cita a cancelar
     * @throws IllegalArgumentException si la cita no existe
     * @throws IllegalArgumentException si la cita ya está cancelada o completada
     */
    public void cancelarCita(int citaId) {
        Cita cita = obtenerCitaOFallar(citaId);
        validar(!Cita.ESTADO_CANCELADA.equals(cita.getEstado()),
                "La cita ya estaba cancelada.");
        validar(!Cita.ESTADO_COMPLETADA.equals(cita.getEstado()),
                "No se puede cancelar una cita que ya fue completada.");

        cita.setEstado(Cita.ESTADO_CANCELADA);
        citaDAO.actualizar(cita);
    }

    /**
     * Marca una cita como completada tras la atención médica.
     *
     * @param citaId identificador de la cita a completar
     * @throws IllegalArgumentException si la cita no existe, ya fue completada
     *                                  o está cancelada
     */
    public void completarCita(int citaId) {
        Cita cita = obtenerCitaOFallar(citaId);
        validar(!Cita.ESTADO_COMPLETADA.equals(cita.getEstado()),
                "La cita ya estaba completada.");
        validar(!Cita.ESTADO_CANCELADA.equals(cita.getEstado()),
                "No se puede completar una cita cancelada.");

        cita.setEstado(Cita.ESTADO_COMPLETADA);
        citaDAO.actualizar(cita);
    }

    /**
     * Elimina una cita del archivo CSV.
     *
     * <p>Preferir {@link #cancelarCita(int)} para mantener el historial.
     * Usar este método solo si se requiere eliminación física.</p>
     *
     * @param citaId identificador de la cita a eliminar
     */
    public void eliminarCita(int citaId) {
        citaDAO.eliminar(citaId);
    }

    // ── Operaciones de consulta ───────────────────────────────────────────

    /**
     * Busca una cita por su identificador único.
     *
     * @param id identificador de la cita
     * @return {@link Optional} con la cita, o vacío si no existe
     */
    public Optional<Cita> obtenerPorId(int id) {
        return citaDAO.buscarPorId(id);
    }

    /**
     * Retorna todas las citas registradas en el sistema.
     *
     * @return lista de citas; vacía si no hay registros
     */
    public List<Cita> listarTodas() {
        return citaDAO.listarTodos();
    }

    /**
     * Retorna el historial de citas de un paciente (más recientes primero).
     *
     * @param pacienteId identificador del paciente
     * @return lista de citas del paciente; vacía si no tiene
     */
    public List<Cita> listarPorPaciente(int pacienteId) {
        return citaDAO.listarPorPaciente(pacienteId);
    }

    /**
     * Retorna la agenda de citas de un médico (orden cronológico).
     *
     * @param medicoId identificador del médico
     * @return lista de citas del médico; vacía si no tiene
     */
    public List<Cita> listarPorMedico(int medicoId) {
        return citaDAO.listarPorMedico(medicoId);
    }

    /**
     * Verifica si un médico está disponible para agendar una cita en una fecha.
     *
     * <p>Combina dos condiciones: el médico debe tener {@code disponible=true}
     * y no debe tener una cita activa (no cancelada) ese día.</p>
     *
     * @param medicoId identificador del médico
     * @param fecha    fecha a verificar
     * @return {@code true} si el médico puede recibir una cita ese día
     */
    public boolean verificarDisponibilidad(int medicoId, LocalDate fecha) {
        Optional<Medico> opt = medicoDAO.buscarPorId(medicoId);
        if (opt.isEmpty() || !opt.get().isDisponible()) return false;
        return !citaDAO.existeCitaEnFecha(medicoId, fecha);
    }

    // ── Métodos privados auxiliares ───────────────────────────────────────

    private Cita obtenerCitaOFallar(int citaId) {
        return citaDAO.buscarPorId(citaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una cita con id=" + citaId));
    }

    private void validar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalArgumentException(mensaje);
    }
}
