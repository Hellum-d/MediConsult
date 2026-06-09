package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.OrdenMedicaDAOImpl;
import com.mediconsult.beta.dao.interfaces.IOrdenMedicaDAO;
import com.mediconsult.beta.model.OrdenMedica;

import java.util.List;

public class OrdenMedicaService {

    private final IOrdenMedicaDAO dao = new OrdenMedicaDAOImpl();

    public void guardar(OrdenMedica orden) {
        if (orden.getTipo() == null || orden.getTipo().isBlank())
            throw new IllegalArgumentException("El tipo de orden es obligatorio.");
        if (orden.getDescripcion() == null || orden.getDescripcion().isBlank())
            throw new IllegalArgumentException("La descripción es obligatoria.");
        dao.guardar(orden);
    }

    public List<OrdenMedica> listarPorCita(int citaId) {
        return dao.listarPorCita(citaId);
    }

    public List<OrdenMedica> listarPorPaciente(int pacienteId) {
        return dao.listarPorPaciente(pacienteId);
    }

    public void eliminar(int id) {
        dao.eliminar(id);
    }
}
