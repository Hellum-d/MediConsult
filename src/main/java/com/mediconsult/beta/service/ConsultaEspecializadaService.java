package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.ConsultaEspecializadaDAOImpl;
import com.mediconsult.beta.dao.impl.PacienteDAOImpl;
import com.mediconsult.beta.dao.interfaces.IConsultaEspecializadaDAO;
import com.mediconsult.beta.dao.interfaces.IPacienteDAO;
import com.mediconsult.beta.model.ConsultaEspecializada;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ConsultaEspecializadaService {

    private final IConsultaEspecializadaDAO dao        = new ConsultaEspecializadaDAOImpl();
    private final IPacienteDAO              pacienteDAO = new PacienteDAOImpl();

    public ConsultaEspecializada guardarOActualizar(ConsultaEspecializada c) {
        if (c.getRemisionId() <= 0)
            throw new IllegalArgumentException("La consulta debe estar vinculada a una remisión.");
        if (c.getPacienteId() <= 0)
            throw new IllegalArgumentException("La consulta debe estar vinculada a un paciente.");
        if (c.getMedicoEspecialistaId() <= 0)
            throw new IllegalArgumentException("La consulta debe estar vinculada a un médico especialista.");

        Optional<ConsultaEspecializada> existente = dao.buscarPorRemision(c.getRemisionId());
        if (existente.isPresent()) {
            c.setId(existente.get().getId());
            dao.actualizar(c);
        } else {
            dao.guardar(c);
            agregarAlHistorialPaciente(c);
        }
        return c;
    }

    public Optional<ConsultaEspecializada> obtenerPorRemision(int remisionId) {
        return dao.buscarPorRemision(remisionId);
    }

    public List<ConsultaEspecializada> listarPorPaciente(int pacienteId) {
        return dao.listarPorPaciente(pacienteId);
    }

    private void agregarAlHistorialPaciente(ConsultaEspecializada c) {
        try {
            pacienteDAO.buscarPorId(c.getPacienteId()).ifPresent(p -> {
                String actual  = p.getHistorial() != null ? p.getHistorial() : "";
                String entrada = "[" + LocalDate.now() + "] Consulta Especializada — Dx: "
                        + (c.getDiagnostico().isBlank() ? "pendiente" : c.getDiagnostico())
                        + " — Tx: " + (c.getTratamiento().isBlank() ? "—" : c.getTratamiento());
                String nuevo = actual.trim().isEmpty() ? entrada : entrada + "\n" + actual;
                pacienteDAO.actualizarHistorial(c.getPacienteId(), nuevo);
            });
        } catch (Exception e) {
            System.err.println("[ConsultaEspService] No se pudo actualizar historial: " + e.getMessage());
        }
    }
}
