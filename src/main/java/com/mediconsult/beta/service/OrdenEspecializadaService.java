package com.mediconsult.beta.service;

import com.mediconsult.beta.dao.impl.OrdenEspecializadaDAOImpl;
import com.mediconsult.beta.dao.interfaces.IOrdenEspecializadaDAO;
import com.mediconsult.beta.model.OrdenEspecializada;

import java.util.List;

public class OrdenEspecializadaService {

    private final IOrdenEspecializadaDAO dao = new OrdenEspecializadaDAOImpl();

    public void guardar(OrdenEspecializada orden) {
        if (orden.getTipo() == null || orden.getTipo().isBlank())
            throw new IllegalArgumentException("El tipo de orden es obligatorio.");
        if (orden.getDescripcion() == null || orden.getDescripcion().isBlank())
            throw new IllegalArgumentException("La descripción es obligatoria.");
        dao.guardar(orden);
    }

    public List<OrdenEspecializada> listarPorConsulta(int consultaEspecializadaId) {
        return dao.listarPorConsulta(consultaEspecializadaId);
    }

    public List<OrdenEspecializada> listarPorPaciente(int pacienteId) {
        return dao.listarPorPaciente(pacienteId);
    }

    public void eliminar(int id) {
        dao.eliminar(id);
    }
}
