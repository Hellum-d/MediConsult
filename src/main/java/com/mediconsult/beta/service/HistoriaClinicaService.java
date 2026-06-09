package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.HistoriaClinicaDAOImpl;
import com.mediconsult.beta.dao.impl.PacienteDAOImpl;
import com.mediconsult.beta.dao.interfaces.IHistoriaClinicaDAO;
import com.mediconsult.beta.dao.interfaces.IPacienteDAO;
import com.mediconsult.beta.model.HistoriaClinica;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de historia clínica — CLINEX.
 *
 * <p>Orquesta la creación/actualización de las historias clínicas y
 * actualiza el historial narrativo del paciente en la tabla {@code paciente}.</p>
 */
public class HistoriaClinicaService {

    private final IHistoriaClinicaDAO historiaDAO;
    private final IPacienteDAO        pacienteDAO;

    public HistoriaClinicaService() {
        this.historiaDAO = new HistoriaClinicaDAOImpl();
        this.pacienteDAO = new PacienteDAOImpl();
    }

    // ── Guardar o actualizar ──────────────────────────────────────────────

    /**
     * Persiste o actualiza la historia clínica de una cita.
     *
     * <p>Si ya existe una historia para esa cita, la actualiza;
     * si no, la crea nueva y añade una entrada al historial del paciente.</p>
     *
     * @param historia objeto con los datos completados por el médico
     * @return historia clínica guardada con su id
     */
    public HistoriaClinica guardarOActualizar(HistoriaClinica historia) {
        validar(historia.getCitaId() > 0, "La historia debe estar vinculada a una cita.");
        validar(historia.getPacienteId() > 0, "La historia debe estar vinculada a un paciente.");
        validar(historia.getMedicoId() > 0, "La historia debe estar vinculada a un médico.");

        Optional<HistoriaClinica> existente = historiaDAO.buscarPorCita(historia.getCitaId());

        if (existente.isPresent()) {
            historia.setId(existente.get().getId());
            historiaDAO.actualizar(historia);
        } else {
            historiaDAO.guardar(historia);
            agregarAlHistorialPaciente(historia);
        }
        return historia;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public Optional<HistoriaClinica> obtenerPorCita(int citaId) {
        return historiaDAO.buscarPorCita(citaId);
    }

    public List<HistoriaClinica> listarPorPaciente(int pacienteId) {
        return historiaDAO.listarPorPaciente(pacienteId);
    }

    public List<HistoriaClinica> listarPorMedico(int medicoId) {
        return historiaDAO.listarPorMedico(medicoId);
    }

    // ── Historial narrativo ───────────────────────────────────────────────

    private void agregarAlHistorialPaciente(HistoriaClinica h) {
        try {
            pacienteDAO.buscarPorId(h.getPacienteId()).ifPresent(p -> {
                String actual = p.getHistorial() != null ? p.getHistorial() : "";
                String entrada = "[" + LocalDate.now() + "] Consulta médica — Dx: "
                        + (h.getDiagnostico().isBlank() ? "pendiente" : h.getDiagnostico())
                        + " — Tx: " + (h.getTratamiento().isBlank() ? "—" : h.getTratamiento());
                String nuevo = actual.trim().isEmpty() ? entrada : entrada + "\n" + actual;
                pacienteDAO.actualizarHistorial(h.getPacienteId(), nuevo);
            });
        } catch (Exception e) {
            System.err.println("[HistoriaClinicaService] No se pudo actualizar historial: " + e.getMessage());
        }
    }

    private void validar(boolean cond, String msg) {
        if (!cond) throw new IllegalArgumentException(msg);
    }
}
