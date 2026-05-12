package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.MedicoDAOImpl;
import com.mediconsult.beta.dao.interfaces.IMedicoDAO;
import com.mediconsult.beta.model.Medico;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar médicos.
 */
public class MedicoService {

    private final IMedicoDAO medicoDAO;

    public MedicoService() {
        this.medicoDAO = new MedicoDAOImpl();
    }

    public MedicoService(IMedicoDAO medicoDAO) {
        this.medicoDAO = medicoDAO;
    }

    public Medico registrarMedico(String nombre, String especialidad) {
        validar(nombre != null && !nombre.isBlank(), "Nombre vacío");
        validar(especialidad != null && !especialidad.isBlank(), "Especialidad vacía");

        Medico medico = new Medico(nombre.trim(), especialidad.trim());
        medicoDAO.guardar(medico);
        return medico;
    }

    public void actualizarMedico(Medico medico) {
        validar(medico != null, "Médico nulo");
        validar(medico.getId() != 0, "ID inválido");
        validar(medico.getNombre() != null && !medico.getNombre().isBlank(), "Nombre vacío");
        validar(medico.getEspecialidad() != null && !medico.getEspecialidad().isBlank(), "Especialidad vacía");

        medicoDAO.actualizar(medico);
    }

    public void eliminarMedico(int id) {
        medicoDAO.eliminar(id);
    }

    public void cambiarDisponibilidad(int medicoId, boolean disponible) {
        Medico medico = medicoDAO.buscarPorId(medicoId)
                .orElseThrow(() -> new IllegalArgumentException("Médico no encontrado"));
        medico.setDisponible(disponible);
        medicoDAO.actualizar(medico);
    }

    public Optional<Medico> obtenerPorId(int id) {
        return medicoDAO.buscarPorId(id);
    }

    public List<Medico> listarTodos() {
        return medicoDAO.listarTodos();
    }

    public List<Medico> listarDisponibles() {
        return medicoDAO.listarDisponibles();
    }

    public List<Medico> buscarPorEspecialidad(String especialidad) {
        validar(especialidad != null && !especialidad.isBlank(), "Búsqueda vacía");
        return medicoDAO.buscarPorEspecialidad(especialidad.trim());
    }

    private void validar(boolean condicion, String mensaje) {
        if (!condicion) throw new IllegalArgumentException(mensaje);
    }
}