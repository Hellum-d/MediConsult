package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.CitaDAOImpl;
import com.mediconsult.beta.dao.impl.MedicoDAOImpl;
import com.mediconsult.beta.dao.interfaces.ICitaDAO;
import com.mediconsult.beta.dao.interfaces.IMedicoDAO;
import com.mediconsult.beta.model.Cita;
import com.mediconsult.beta.model.Medico;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio para manejar las citas médicas.
 *
 * Antes de agendar, valida que el médico exista, esté disponible
 * y no tenga otra cita activa ese día.
 */
public class CitaService {

    private final ICitaDAO   citaDAO;
    private final IMedicoDAO medicoDAO;

    /** Usa implementaciones reales de los DAOs. */
    public CitaService() {
        this.citaDAO   = new CitaDAOImpl();
        this.medicoDAO = new MedicoDAOImpl();
    }

    /** Permite inyectar DAOs (útil para pruebas). */
    public CitaService(ICitaDAO citaDAO, IMedicoDAO medicoDAO) {
        this.citaDAO   = citaDAO;
        this.medicoDAO = medicoDAO;
    }

    // ── Escritura ─────────────────────────────────────────────────────────

    /**
     * Agenda una cita validando disponibilidad del médico y fecha.
     *
     * Reglas:
     * - El médico debe existir.
     * - Debe estar disponible.
     * - No debe tener otra cita activa ese día.
     * - La fecha no puede ser pasada.
     * @param pacienteId
     * @param medicoId
     * @param fecha
     * @return 
     */
    public Cita agendarCita(int pacienteId, int medicoId, LocalDate fecha) {
        validar(fecha != null, "La fecha de la cita no puede ser nula.");
        validar(!fecha.isBefore(LocalDate.now()), "No se pueden agendar citas en fechas pasadas.");

        Medico medico = medicoDAO.buscarPorId(medicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un médico con id=" + medicoId));

        if (!medico.isDisponible()) {
            throw new IllegalStateException(
                    "El médico " + medico.getNombre() + " no está disponible para nuevas citas.");
        }

        if (citaDAO.existeCitaEnFecha(medicoId, fecha)) {
            throw new IllegalStateException(
                    "El médico " + medico.getNombre()
                    + " ya tiene una cita activa el " + fecha + ".");
        }

        Cita cita = new Cita(pacienteId, medicoId, fecha);
        citaDAO.guardar(cita);
        return cita;
    }

    /**
     * Cambia el estado de una cita a CANCELADA.
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
     * Cambia el estado de una cita a COMPLETADA.
     * @param citaId
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
     * Elimina físicamente una cita del CSV.
     * Preferir cancelar para conservar historial.
     * @param citaId
     */
    public void eliminarCita(int citaId) {
        citaDAO.eliminar(citaId);
    }

    // ── Lectura ───────────────────────────────────────────────────────────

    /** Busca una cita por id.
     * @param id
     * @return  */
    public Optional<Cita> obtenerPorId(int id) {
        return citaDAO.buscarPorId(id);
    }

    /** Lista todas las citas.
     * @return  */
    public List<Cita> listarTodas() {
        return citaDAO.listarTodos();
    }

    /** Historial de citas de un paciente.
     * @param pacienteId
     * @return  */
    public List<Cita> listarPorPaciente(int pacienteId) {
        return citaDAO.listarPorPaciente(pacienteId);
    }

    /** Agenda completa de un médico.
     * @param medicoId
     * @return  */
    public List<Cita> listarPorMedico(int medicoId) {
        return citaDAO.listarPorMedico(medicoId);
    }

    /**
     * Indica si un médico puede recibir cita en una fecha.
     * @param medicoId
     * @param fecha
     * @return 
     */
    public boolean verificarDisponibilidad(int medicoId, LocalDate fecha) {
        Optional<Medico> opt = medicoDAO.buscarPorId(medicoId);
        if (opt.isEmpty() || !opt.get().isDisponible()) return false;
        return !citaDAO.existeCitaEnFecha(medicoId, fecha);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Cita obtenerCitaOFallar(int citaId) {
        return citaDAO.buscarPorId(citaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una cita con id=" + citaId));
    }

    private void validar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalArgumentException(mensaje);
    }
}