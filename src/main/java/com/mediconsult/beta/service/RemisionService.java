package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.RemisionDAOImpl;
import com.mediconsult.beta.dao.interfaces.IRemisionDAO;
import com.mediconsult.beta.model.Remision;

import java.util.List;
import java.util.Optional;

public class RemisionService {

    private final IRemisionDAO dao = new RemisionDAOImpl();

    public Optional<Remision> buscarPorId(int id) {
        return dao.buscarPorId(id);
    }

    public void guardar(Remision remision) {
        if (remision.getEspecialidadDestino() == null || remision.getEspecialidadDestino().isBlank())
            throw new IllegalArgumentException("La especialidad de destino es obligatoria.");
        if (remision.getMotivoRemision() == null || remision.getMotivoRemision().isBlank())
            throw new IllegalArgumentException("El motivo de remisión es obligatorio.");
        dao.guardar(remision);
    }

    public List<Remision> listarPorCita(int citaId) {
        return dao.listarPorCita(citaId);
    }

    public List<Remision> listarPorPaciente(int pacienteId) {
        return dao.listarPorPaciente(pacienteId);
    }

    public List<Remision> listarPorEspecialidad(String especialidad) {
        return dao.listarPorEspecialidad(especialidad);
    }

    public List<Remision> listarPorEspecialista(int medicoEspecialistaId) {
        return dao.listarPorEspecialista(medicoEspecialistaId);
    }

    public List<Remision> listarTodas() {
        return dao.listarTodas();
    }

    public void actualizar(Remision remision) {
        dao.actualizar(remision);
    }

    public void eliminar(int id) {
        dao.eliminar(id);
    }
}
